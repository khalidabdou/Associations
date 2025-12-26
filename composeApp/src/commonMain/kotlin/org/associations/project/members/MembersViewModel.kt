package org.associations.project.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.database.GetAllSubscribers
import org.associations.project.database.Invoice
import org.associations.project.database.Zone
import org.associations.project.repository.AppRepository

class MembersViewModel(private val repository: AppRepository) : ViewModel() {
    val subscribers: StateFlow<List<GetAllSubscribers>> =
            repository
                    .getSubscribers()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val zones: StateFlow<List<Zone>> =
            repository
                    .getZones()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For member details - invoices/history
    private val _selectedSubscriberId = MutableStateFlow<Long?>(null)

    val subscriberInvoices: StateFlow<List<Invoice>> =
            _selectedSubscriberId
                    .filterNotNull()
                    .flatMapLatest { subscriberId ->
                        repository.getInvoicesBySubscriber(subscriberId)
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSubscriber(subscriberId: Long) {
        _selectedSubscriberId.value = subscriberId
    }

    fun addSubscriber(
            fullName: String,
            phone: String?,
            meterNumber: String,
            address: String?,
            zoneId: Long
    ) {
        viewModelScope.launch {
            repository.insertSubscriber(fullName, phone, meterNumber, address, zoneId)
        }
    }

    fun updateSubscriber(
            id: Long,
            fullName: String,
            phone: String?,
            meterNumber: String,
            address: String?,
            zoneId: Long,
            isActive: Long
    ) {
        viewModelScope.launch {
            repository.updateSubscriber(id, fullName, phone, meterNumber, address, zoneId, isActive)
        }
    }

    fun deleteSubscriber(id: Long) {
        viewModelScope.launch { repository.deleteSubscriber(id) }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch { repository.deleteInvoice(id) }
    }
}
