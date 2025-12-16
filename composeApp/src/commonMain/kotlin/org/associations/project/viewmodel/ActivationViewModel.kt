package org.associations.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.associations.project.repository.LicenseRepository
import org.associations.project.repository.LicenseResult

class ActivationViewModel(private val repository: LicenseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<LicenseResult?>(null)
    val uiState = _uiState.asStateFlow()

    fun checkActivation(): Boolean {
        return repository.isActivated()
    }

    fun activate(key: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.activateLicense(key).collect { result ->
                _uiState.value = result
                if (result is LicenseResult.Success) {
                    onSuccess()
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = null
    }
}
