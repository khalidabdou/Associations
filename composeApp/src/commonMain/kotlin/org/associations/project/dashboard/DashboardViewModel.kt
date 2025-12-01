package org.associations.project.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    val totalIncome: Double = 0.0,
    val totalUnpaid: Double = 0.0,
    val totalMembers: Int = 0,
    val waterConsumption: Int = 0,
    val recentActivity: List<String> = emptyList()
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Mock data for now
        _uiState.value = DashboardUiState(
            totalIncome = 15000.0,
            totalUnpaid = 3200.0,
            totalMembers = 145,
            waterConsumption = 4500,
            recentActivity = listOf(
                "Payment received from Ahmed (Zone A)",
                "New member added: Fatima (Zone B)",
                "Maintenance ticket resolved: Leak in Sector 3",
                "Bill generated for Oct 2023",
                "Expense: Pipe repair materials"
            )
        )
    }
}
