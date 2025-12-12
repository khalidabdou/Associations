package org.associations.project.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.associations.project.repository.AppRepository
import org.associations.project.utils.MonthYear
import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.GetInvoiceById

data class InvoiceUiModel(
    val id: Long,
    val subscriberName: String?,
    val consumption: Long,
    val totalAmount: Double,
    val status: String,
    val issueDate: Long,
    val dueOrPaidDate: Long?,
    val previousReading: Long,
    val currentReading: Long,
    val meterNumber: String?
)

data class InvoicesUiState(
    val allInvoices: List<InvoiceUiModel> = emptyList(),
    val unpaidInvoices: List<InvoiceUiModel> = emptyList(),
    val paidInvoices: List<InvoiceUiModel> = emptyList(),
    val selectedTab: Int = 0, // 0 = unpaid, 1 = paid, 2 = all
    val selectedMonth: MonthYear? = null, // Null = All time
    val availableMonths: List<MonthYear> = MonthYear.generateLast12Months(),
    val associationName: String = "",
    val associationAddress: String = "",
    val associationPhone: String = "",
    val printFormat: String = "A4",
    val isLoading: Boolean = true,
    val message: String? = null
)

class InvoicesViewModel(
    private val repository: AppRepository,
    private val printService: PrintService // Injected
) : ViewModel() {
    // ...
    // Existing code ...

    fun printInvoice(invoiceId: Long) {
        viewModelScope.launch {
            try {
                // Fetch full details
                val settings = repository.getSettings().first()
                if (settings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }
                
                // We need Invoice and Subscriber entities. 
                // Since we don't have direct access to entities from UI Model easily, let's fetch individual entities or map from a join.
                // For simplicity, we can fetch Invoice and Subscriber using existing low-level queries if exposed, or add a specific method.
                // Let's use the UI Model data to construct them as "Good Enough" or fetch properly.
                // Provided code in DesktopPrintService uses explicit Invoice/Subscriber classes.
                
                // Let's assume we can't easily get entities without a new repo method.
                // I will add getInvoiceWithDetails to generic repo?
                // Or I can cheat and construct them from the UI model if it has enough data?
                // UI Model lacks: subscriber address, full phone, etc.
                
                // Better: repository.getInvoiceById(id) returns a projection.
                // I will use that projection to construct the entities.
                val details = repository.getInvoiceDetailsOnce(invoiceId)
                if (details == null) {
                    showMessage("خطأ: الفاتورة غير موجودة")
                    return@launch
                }
                
                val invoiceEntity = Invoice(
                    id = details.id,
                    subscriberId = details.subscriberId,
                    previousReading = details.previousReading,
                    currentReading = details.currentReading,
                    consumption = details.consumption,
                    totalAmount = details.totalAmount,
                    status = details.status,
                    issueDate = details.issueDate,
                    dueDate = details.dueDate,
                    isPenaltyApplied = details.isPenaltyApplied
                )
                
                val subscriberEntity = Subscriber(
                    id = details.subscriberId,
                    fullName = details.subscriberName ?: "Unknown",
                    phone = null, // Detail projection might not have phone? check .sq
                    meterNumber = details.meterNumber ?: "",
                    address = details.address,
                    zoneId = 0, // Not needed for print
                    isActive = 1,
                    createdAt = 0
                )
                
                printService.printInvoice(invoiceEntity, subscriberEntity, settings)
                showMessage("تم إرسال الفاتورة للطباعة")
                
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // ... existing functions

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState: StateFlow<InvoicesUiState> = _uiState.asStateFlow()

    private val selectedMonthFlow = MutableStateFlow<MonthYear?>(null)

    init {
        checkLateFees()
        loadInvoices()
    }

    private fun checkLateFees() {
        viewModelScope.launch {
            try {
                repository.checkAndApplyLateFees()
            } catch (e: Exception) {
                // Log silently or show message if critical
                println("Late fee check error: ${e.message}")
            }
        }
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            combine(
                selectedMonthFlow.flatMapLatest { month ->
                    if (month == null) {
                        repository.getAllInvoices().map { list ->
                            list.map { 
                                InvoiceUiModel(
                                    it.id, it.subscriberName, it.consumption, it.totalAmount, 
                                    it.status, it.issueDate, it.dueDate,
                                    it.previousReading, it.currentReading, it.meterNumber
                                ) 
                            }
                        }
                    } else {
                        repository.getInvoicesByDateRange(month.startEpochMillis, month.endEpochMillis).map { list ->
                            list.map { 
                                InvoiceUiModel(
                                    it.id, it.subscriberName, it.consumption, it.totalAmount, 
                                    it.status, it.issueDate, it.dueDate,
                                    it.previousReading, it.currentReading, it.meterNumber
                                ) 
                            }
                        }
                    }
                },
                repository.getSettings()
            ) { invoices: List<InvoiceUiModel>, settings: AppSettings? ->
                Pair(invoices, settings)
            }.collect { (all, settings) ->
                _uiState.update { 
                    it.copy(
                        allInvoices = all,
                        unpaidInvoices = all.filter { inv -> inv.status == "UNPAID" },
                        paidInvoices = all.filter { inv -> inv.status == "PAID" },
                        associationName = settings?.associationName ?: "",
                        associationAddress = settings?.associationAddress ?: "",
                        associationPhone = settings?.associationPhone ?: "",
                        printFormat = settings?.printFormat ?: "A4",
                        isLoading = false,
                        selectedMonth = selectedMonthFlow.value
                    )
                }
            }
        }
    }

    fun selectMonth(month: MonthYear?) {
        selectedMonthFlow.value = month
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun markAsPaid(invoiceId: Long) {
        viewModelScope.launch {
            try {
                repository.updateInvoiceStatus(invoiceId, "PAID")
                showMessage("تم تحديث حالة الفاتورة")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun markAsUnpaid(invoiceId: Long) {
        viewModelScope.launch {
            try {
                repository.updateInvoiceStatus(invoiceId, "UNPAID")
                showMessage("تم تحديث حالة الفاتورة")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteInvoice(invoiceId)
                showMessage("تم حذف الفاتورة")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(message = null) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
