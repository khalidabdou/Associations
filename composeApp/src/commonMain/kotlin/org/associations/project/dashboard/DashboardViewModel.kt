package org.associations.project.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.repository.AppRepository

data class DashboardUiState(
    val totalIncome: Double = 0.0,
    val totalUnpaid: Double = 0.0,
    val totalMembers: Long = 0,
    val waterConsumption: Long = 0,
    val recentActivity: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Combine all flows
            combine(
                repository.getTotalIncome(),
                repository.getTotalUnpaidAmount(),
                repository.getSubscriberCount(),
                repository.getTotalConsumption()
            ) { income, unpaid, members, consumption ->
                DashboardUiState(
                    totalIncome = income,
                    totalUnpaid = unpaid,
                    totalMembers = members,
                    waterConsumption = consumption,
                    recentActivity = listOf(
                        "تم استلام دفعة من أحمد (المنطقة أ)",
                        "تمت إضافة مشترك جديد: فاطمة (المنطقة ب)",
                        "تم حل تذكرة صيانة: تسرب في القطاع 3",
                        "تم إنشاء فواتير شهر أكتوبر",
                        "مصروف: مواد إصلاح الأنابيب"
                    ),
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
