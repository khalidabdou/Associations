package org.associations.project.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.associations.project.database.GetAllSubscribers
import org.associations.project.database.Zone
import org.associations.project.repository.AppRepository

class MembersViewModel(private val repository: AppRepository) : ViewModel() {
    val subscribers: StateFlow<List<GetAllSubscribers>> = repository.getSubscribers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val zones: StateFlow<List<Zone>> = repository.getZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubscriber(fullName: String, phone: String?, meterNumber: String, address: String?, zoneId: Long) {
        viewModelScope.launch {
            repository.insertSubscriber(fullName, phone, meterNumber, address, zoneId)
        }
    }
}
