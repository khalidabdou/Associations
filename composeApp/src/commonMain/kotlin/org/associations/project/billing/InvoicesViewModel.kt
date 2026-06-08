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

import com.russhwolf.settings.Settings

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
    val meterNumber: String?,
    val isPenaltyApplied: Long = 0L
)

data class InvoicesUiState(
    val allInvoices: List<InvoiceUiModel> = emptyList(),
    val unpaidInvoices: List<InvoiceUiModel> = emptyList(),
    val paidInvoices: List<InvoiceUiModel> = emptyList(),
    val selectedTab: Int = 0, // 0 = unpaid, 1 = paid, 2 = all
    val selectedMonth: MonthYear? = MonthYear.current(), // Default to current month
    val availableMonths: List<MonthYear> = MonthYear.generateLast12Months(),
    val associationName: String = "",
    val associationAddress: String = "",
    val associationPhone: String = "",
    val printFormat: String = "A4",
    val logoPath: String? = null,
    val lateFeeAmount: Double = 0.0,
    val monthlyFixedFee: Double = 0.0,
    val selectedIds: Set<Long> = emptySet(),
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val message: String? = null,
    // Bluetooth picker state
    val showBluetoothPicker: Boolean = false,
    val bluetoothPrinters: List<BluetoothPrinterInfo> = emptyList(),
    val bluetoothPickerLoading: Boolean = false,
    val pendingBluetoothInvoiceId: Long? = null
)

class InvoicesViewModel(
    private val repository: AppRepository,
    private val printService: PrintService,
    private val shareService: ShareService,
    private val settings: Settings
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

    fun shareInvoice(invoiceId: Long) {
        viewModelScope.launch {
            try {
                // Fetch full details
                val settings = repository.getSettings().first()
                if (settings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }

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
                    phone = null,
                    meterNumber = details.meterNumber ?: "",
                    address = details.address,
                    zoneId = 0,
                    isActive = 1,
                    createdAt = 0
                )

                shareService.shareInvoice(invoiceEntity, subscriberEntity, settings)
                showMessage("تم فتح الفاتورة للمشاركة")

            } catch (e: Exception) {
                showMessage("خطأ في المشاركة: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ... existing functions

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState: StateFlow<InvoicesUiState> = _uiState.asStateFlow()

    private val selectedMonthFlow = MutableStateFlow<MonthYear?>(MonthYear.current())

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
                                    it.previousReading, it.currentReading, it.meterNumber,
                                    it.isPenaltyApplied
                                ) 
                            }
                        }
                    } else {
                        repository.getInvoicesByDateRange(month.startEpochMillis, month.endEpochMillis).map { list ->
                            list.map { 
                                InvoiceUiModel(
                                    it.id, it.subscriberName, it.consumption, it.totalAmount, 
                                    it.status, it.issueDate, it.dueDate,
                                    it.previousReading, it.currentReading, it.meterNumber,
                                    it.isPenaltyApplied
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
                        logoPath = settings?.logoPath,
                        lateFeeAmount = settings?.lateFeeAmount ?: 0.0,
                        monthlyFixedFee = settings?.monthlyFixedFee ?: 0.0,
                        isLoading = false,
                        selectedMonth = selectedMonthFlow.value
                    )
                }
            }
        }
    }

    fun selectMonth(month: MonthYear?) {
        selectedMonthFlow.value = month
        _uiState.update { it.copy(currentPage = 0) }
    }

    fun goToPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, currentPage = 0) }
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

    // ===== Multi-select & bulk print =====
    fun toggleSelection(invoiceId: Long) {
        _uiState.update { st ->
            val newSet = st.selectedIds.toMutableSet().apply {
                if (!add(invoiceId)) remove(invoiceId)
            }
            st.copy(selectedIds = newSet)
        }
    }

    fun selectAllVisible() {
        _uiState.update { st ->
            val visible = when (st.selectedTab) {
                0 -> st.unpaidInvoices
                1 -> st.paidInvoices
                else -> st.allInvoices
            }
            st.copy(selectedIds = visible.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun printSelectedInvoices(copies: Int = 1) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val settings = repository.getSettings().first()
                if (settings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }
                val items = ids.mapNotNull { id ->
                    val d = repository.getInvoiceDetailsOnce(id) ?: return@mapNotNull null
                    val invoice = Invoice(
                        id = d.id,
                        subscriberId = d.subscriberId,
                        previousReading = d.previousReading,
                        currentReading = d.currentReading,
                        consumption = d.consumption,
                        totalAmount = d.totalAmount,
                        status = d.status,
                        issueDate = d.issueDate,
                        dueDate = d.dueDate,
                        isPenaltyApplied = d.isPenaltyApplied
                    )
                    val sub = Subscriber(
                        id = d.subscriberId,
                        fullName = d.subscriberName ?: "Unknown",
                        phone = null,
                        meterNumber = d.meterNumber ?: "",
                        address = d.address,
                        zoneId = 0,
                        isActive = 1,
                        createdAt = 0
                    )
                    invoice to sub
                }
                if (items.isEmpty()) {
                    showMessage("لا توجد فواتير صالحة للطباعة")
                    return@launch
                }
                // Duplicate items for multiple copies
                val printItems = if (copies > 1) items.flatMap { item -> List(copies) { item } } else items
                val totalPages = printItems.size
                printService.printInvoices(
                    items = printItems,
                    settings = settings,
                    jobName = "Invoices (${items.size} × $copies)"
                )
                showMessage("تم إرسال ${items.size} فاتورة × $copies نسخة ($totalPages صفحة) للطباعة")
                clearSelection()
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
                e.printStackTrace()
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

    // ── Bluetooth thermal printer ──

    /**
     * Opens the Bluetooth device picker for an invoice print.
     * If POS format is selected, shows device list. Otherwise prints normally.
     */
    fun printInvoiceOrBluetooth(invoiceId: Long) {
        val format = _uiState.value.printFormat
        if (format == "POS") {
            val connectionType = settings.getString("printer_connection_type", "BLUETOOTH")
            if (connectionType == "USB") {
                val deviceId = settings.getInt("usb_printer_device_id", -1)
                if (deviceId >= 0) {
                    printInvoiceViaUsbDirectly(invoiceId, deviceId)
                } else {
                    showMessage("يرجى إعداد واختبار طابعة USB في الإعدادات أولاً")
                }
            } else {
                val savedAddress = settings.getString("bluetooth_printer_address", "")
                if (savedAddress.isNotEmpty()) {
                    printInvoiceViaBluetoothDirectly(invoiceId, savedAddress)
                } else {
                    showBluetoothPrintDialog(invoiceId)
                }
            }
        } else {
            printInvoice(invoiceId)
        }
    }

    private fun printInvoiceViaUsbDirectly(invoiceId: Long, deviceId: Int) {
        viewModelScope.launch {
            try {
                val appSettings = repository.getSettings().first()
                if (appSettings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }
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
                    phone = null,
                    meterNumber = details.meterNumber ?: "",
                    address = details.address,
                    zoneId = 0,
                    isActive = 1,
                    createdAt = 0
                )

                val result = printService.printInvoiceViaUsb(invoiceEntity, subscriberEntity, appSettings, deviceId)
                result.fold(
                    onSuccess = { showMessage("تمت الطباعة عبر USB") },
                    onFailure = { showMessage("فشل الطباعة: ${it.message}") }
                )
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
            }
        }
    }

    private fun printInvoiceViaBluetoothDirectly(invoiceId: Long, address: String) {
        viewModelScope.launch {
            try {
                val appSettings = repository.getSettings().first()
                if (appSettings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }
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
                    phone = null,
                    meterNumber = details.meterNumber ?: "",
                    address = details.address,
                    zoneId = 0,
                    isActive = 1,
                    createdAt = 0
                )

                val result = printService.printInvoiceViaBluetooth(invoiceEntity, subscriberEntity, appSettings, address)
                result.fold(
                    onSuccess = { showMessage("تمت الطباعة عبر البلوتوث") },
                    onFailure = { showMessage("فشل الطباعة: ${it.message}") }
                )
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
            }
        }
    }

    private fun showBluetoothPrintDialog(invoiceId: Long) {
        _uiState.update { it.copy(showBluetoothPicker = true, pendingBluetoothInvoiceId = invoiceId, bluetoothPickerLoading = true) }
        viewModelScope.launch {
            try {
                val printers = printService.getPairedBluetoothPrinters()
                _uiState.update { it.copy(bluetoothPrinters = printers, bluetoothPickerLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(bluetoothPickerLoading = false) }
                showMessage("خطأ في البحث عن الطابعات: ${e.message}")
            }
        }
    }

    fun selectBluetoothPrinter(address: String) {
        val invoiceId = _uiState.value.pendingBluetoothInvoiceId ?: return
        _uiState.update { it.copy(showBluetoothPicker = false, pendingBluetoothInvoiceId = null, bluetoothPrinters = emptyList()) }
        settings.putString("bluetooth_printer_address", address)

        printInvoiceViaBluetoothDirectly(invoiceId, address)
    }

    fun cancelBluetoothPrint() {
        _uiState.update { it.copy(showBluetoothPicker = false, pendingBluetoothInvoiceId = null, bluetoothPrinters = emptyList()) }
    }
}
