package org.associations.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.associations.project.database.Zone
import org.associations.project.database.GetAllSubscribers
import org.associations.project.repository.AppRepository

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    val zones: StateFlow<List<Zone>> = repository.getZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscribers: StateFlow<List<GetAllSubscribers>> = repository.getSubscribers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addZone(name: String, description: String?) {
        viewModelScope.launch {
            repository.insertZone(name, description)
        }
    }

    fun addSubscriber(fullName: String, phone: String?, meterNumber: String, address: String?, zoneId: Long) {
        viewModelScope.launch {
            repository.insertSubscriber(fullName, phone, meterNumber, address, zoneId)
        }
    }
}
