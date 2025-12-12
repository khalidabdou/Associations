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
        viewModelScope.launch { repository.initializeDatabase() }
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Combine all flows for stats
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
                        recentActivity = emptyList(), // Will be updated below
                        isLoading = false
                )
            }
                    .collect { state -> _uiState.value = state }
        }

        // Load recent activities separately
        viewModelScope.launch {
            combine(
                            repository.getRecentPaidInvoices(),
                            repository.getRecentSubscribers(),
                            repository.getRecentTransactions()
                    ) { paidInvoices, newSubscribers, transactions ->
                val activities = mutableListOf<String>()

                // Add recent paid invoices
                paidInvoices.take(2).forEach { invoice ->
                    val zoneName = invoice.zoneName ?: "غير محدد"
                    activities.add("تم استلام دفعة من ${invoice.subscriberName} (${zoneName})")
                }

                // Add recently added subscribers
                newSubscribers.take(2).forEach { subscriber ->
                    val zoneName = subscriber.zoneName ?: "غير محدد"
                    activities.add("تمت إضافة مشترك جديد: ${subscriber.fullName} (${zoneName})")
                }

                // Add recent transactions (expenses)
                transactions.filter { it.type == "EXPENSE" }.take(2).forEach { tx ->
                    val desc = tx.description ?: tx.category
                    activities.add("مصروف: $desc")
                }

                // If no activities yet, show placeholder message
                if (activities.isEmpty()) {
                    activities.add("لا توجد أنشطة حديثة")
                }

                activities
            }
                    .collect { activities ->
                        _uiState.update { it.copy(recentActivity = activities) }
                    }
        }
    }
}
