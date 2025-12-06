package org.associations.project.treasury

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.associations.project.database.TransactionTable
import org.associations.project.repository.AppRepository
import org.associations.project.utils.MonthYear

data class TreasuryUiState(
    val transactions: List<TransactionTable> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val selectedMonth: MonthYear? = null, // Null means "All Months"
    val availableMonths: List<MonthYear> = MonthYear.generateLast12Months(),
    val editingTransaction: TransactionTable? = null,
    val isLoading: Boolean = true,
    val message: String? = null
)

class TreasuryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TreasuryUiState())
    val uiState: StateFlow<TreasuryUiState> = _uiState.asStateFlow()

    private val selectedMonthFlow = MutableStateFlow<MonthYear?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                selectedMonthFlow,
                repository.getTotalIncome(),
                repository.getTotalExpenses(),
                repository.getBalance()
            ) { month, income, expenses, balance ->
                Triple(month, income to expenses, balance)
            }.flatMapLatest { (month, stats, balance) ->
                val (income, expenses) = stats
                val transactionsFlow = if (month == null) {
                    repository.getAllTransactions()
                } else {
                    repository.getTransactionsByDateRange(month.startEpochMillis, month.endEpochMillis)
                }
                
                transactionsFlow.map { transactions ->
                     TreasuryUiState(
                        transactions = transactions,
                        totalIncome = income, // Overall stats (could be filtered too if requirements change)
                        totalExpenses = expenses,
                        balance = balance,
                        selectedMonth = month,
                        availableMonths = MonthYear.generateLast12Months(),
                        editingTransaction = _uiState.value.editingTransaction,
                        isLoading = false,
                        message = _uiState.value.message
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectMonth(month: MonthYear?) {
        selectedMonthFlow.value = month
    }

    fun addIncome(amount: Double, category: String, description: String?) {
        viewModelScope.launch {
            try {
                val date = Clock.System.now().toEpochMilliseconds()
                repository.insertTransaction("INCOME", category, amount, description, date)
                showMessage("تمت إضافة الدخل بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun addExpense(amount: Double, category: String, description: String?) {
        viewModelScope.launch {
            try {
                val date = Clock.System.now().toEpochMilliseconds()
                repository.insertTransaction("EXPENSE", category, amount, description, date)
                showMessage("تمت إضافة المصروف بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }
    
    fun showEditDialog(transaction: TransactionTable) {
        _uiState.update { it.copy(editingTransaction = transaction) }
    }
    
    fun dismissEditDialog() {
        _uiState.update { it.copy(editingTransaction = null) }
    }
    
    fun updateTransaction(id: Long, type: String, category: String, amount: Double, description: String?) {
        viewModelScope.launch {
            try {
                val existing = _uiState.value.transactions.find { it.id == id }
                val date = existing?.date ?: Clock.System.now().toEpochMilliseconds()
                repository.updateTransaction(id, type, category, amount, description, date)
                dismissEditDialog()
                showMessage("تم تحديث المعاملة")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                showMessage("تم حذف المعاملة")
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
