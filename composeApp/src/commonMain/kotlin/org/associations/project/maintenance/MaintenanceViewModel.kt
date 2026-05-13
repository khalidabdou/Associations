package org.associations.project.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.database.GetAllMaintenanceTickets
import org.associations.project.database.GetAllSubscribers
import org.associations.project.database.GetTicketsByStatus
import org.associations.project.repository.AppRepository

data class MaintenanceUiState(
    val allTickets: List<GetAllMaintenanceTickets> = emptyList(),
    val filteredTickets: List<GetAllMaintenanceTickets> = emptyList(),
    val selectedStatus: String? = null, // null = all
    val subscribers: List<GetAllSubscribers> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class MaintenanceViewModel(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    private val selectedStatusFlow = MutableStateFlow<String?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllMaintenanceTickets(),
                selectedStatusFlow
            ) { tickets, status ->
                val filtered = if (status == null) tickets else tickets.filter { it.status == status }
                Pair(tickets, filtered)
            }.collect { (all, filtered) ->
                _uiState.update {
                    it.copy(
                        allTickets = all,
                        filteredTickets = filtered,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getSubscribers().collect { subscribers ->
                _uiState.update { it.copy(subscribers = subscribers) }
            }
        }
    }

    fun selectStatus(status: String?) {
        selectedStatusFlow.value = status
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun addTicket(subscriberId: Long?, issueType: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.insertMaintenanceTicket(subscriberId, issueType, description, "OPEN")
                showMessage("تمت إضافة التذكرة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun updateTicketStatus(id: Long, status: String) {
        viewModelScope.launch {
            try {
                repository.updateTicketStatus(id, status)
                showMessage("تم تحديث حالة التذكرة")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun deleteTicket(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTicket(id)
                showMessage("تم حذف التذكرة")
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
