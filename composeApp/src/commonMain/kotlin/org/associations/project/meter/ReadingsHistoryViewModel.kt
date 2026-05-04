package org.associations.project.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.database.GetAllInvoices
import org.associations.project.database.GetAllInvoicesByMonth
import org.associations.project.repository.AppRepository
import org.associations.project.utils.MonthYear

data class ReadingsHistoryUiState(
    val allInvoices: List<GetAllInvoices> = emptyList(),
    val selectedMonth: MonthYear? = null,
    val availableMonths: List<MonthYear> = MonthYear.generateLast12Months(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class ReadingsHistoryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingsHistoryUiState())
    val uiState: StateFlow<ReadingsHistoryUiState> = _uiState.asStateFlow()

    private val selectedMonthFlow = MutableStateFlow<MonthYear?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            selectedMonthFlow.flatMapLatest { month ->
                if (month == null) {
                    repository.getAllInvoices()
                } else {
                    repository.getInvoicesByDateRange(month.startEpochMillis, month.endEpochMillis)
                        .map { list -> list.map { it.toGetAllInvoices() } }
                }
            }.collect { invoices ->
                _uiState.update {
                    it.copy(
                        allInvoices = invoices,
                        selectedMonth = selectedMonthFlow.value,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectMonth(month: MonthYear?) {
        selectedMonthFlow.value = month
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

private fun GetAllInvoicesByMonth.toGetAllInvoices() = GetAllInvoices(
    id = id,
    subscriberId = subscriberId,
    previousReading = previousReading,
    currentReading = currentReading,
    consumption = consumption,
    totalAmount = totalAmount,
    status = status,
    issueDate = issueDate,
    dueDate = dueDate,
    isPenaltyApplied = isPenaltyApplied,
    subscriberName = subscriberName,
    meterNumber = meterNumber
)
