package org.associations.project.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.associations.project.database.GetSubscribersByZone
import org.associations.project.database.Zone
import org.associations.project.repository.AppRepository
import org.associations.project.utils.MonthYear

data class MeterReadingEntry(
        val subscriberId: Long,
        val subscriberName: String,
        val meterNumber: String,
        val previousReading: Long = 0,
        val currentReading: String = "",
        val consumption: Long = 0,
        val hasInvoice: Boolean = false,
        val invoiceId: Long? = null
)

data class MeterReadingUiState(
        val zones: List<Zone> = emptyList(),
        val selectedZoneId: Long? = null,
        val subscribers: List<GetSubscribersByZone> = emptyList(),
        val readings: Map<Long, MeterReadingEntry> = emptyMap(),
        val searchQuery: String = "",
        val selectedMonth: MonthYear = MonthYear.current(),
        val selectedYear: Int = MonthYear.current().year,
        val availableYears: List<Int> = MonthYear.getAvailableYears(),
        val availableMonths: List<MonthYear> =
                MonthYear.generateMonthsForYear(MonthYear.current().year),
        val isEditMode: Boolean = false,
        val allowPastMonthEditing: Boolean = false,
        val isLoading: Boolean = true,
        val isSaving: Boolean = false,
        val message: String? = null
) {
    val isEditableMonth: Boolean
        get() {
            // If past month editing is allowed, all months are editable
            if (allowPastMonthEditing) return true

            val current = MonthYear.current()
            // Allow editing for current month
            if (selectedMonth == current) return true
            // Allow editing for next month (forward planning)
            val nextMonth =
                    if (current.month == 12) MonthYear(1, current.year + 1)
                    else MonthYear(current.month + 1, current.year)
            if (selectedMonth == nextMonth) return true
            return false
        }

    // Keep for backwards compatibility
    val isCurrentMonth: Boolean
        get() = isEditableMonth

    val filteredReadings: List<MeterReadingEntry>
        get() {
            val allReadings = readings.values.toList()
            return if (searchQuery.isBlank()) {
                allReadings
            } else {
                allReadings.filter { entry ->
                    entry.subscriberName.contains(searchQuery, ignoreCase = true) ||
                            entry.meterNumber.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    val enteredCount: Int
        get() = readings.values.count { it.currentReading.isNotBlank() }
    val totalConsumption: Long
        get() = readings.values.sumOf { it.consumption }
}

class MeterReadingViewModel(private val repository: AppRepository, private val settings: Settings) :
        ViewModel() {
    private val _uiState = MutableStateFlow(MeterReadingUiState())
    val uiState: StateFlow<MeterReadingUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_ALLOW_PAST_MONTH_EDITING = "allow_past_month_editing"
    }

    init {
        // Load settings
        _uiState.update {
            it.copy(
                    allowPastMonthEditing = settings.getBoolean(KEY_ALLOW_PAST_MONTH_EDITING, false)
            )
        }
        loadZones()
    }

    private fun loadZones() {
        viewModelScope.launch {
            repository.getZones().collect { zones ->
                _uiState.update { it.copy(zones = zones, isLoading = false) }
                if (zones.isNotEmpty() && _uiState.value.selectedZoneId == null) {
                    selectZone(zones.first().id)
                }
            }
        }
    }

    fun selectYear(year: Int) {
        val months = MonthYear.generateMonthsForYear(year)
        val defaultMonth =
                if (year == MonthYear.current().year) MonthYear.current() else months.first()

        _uiState.update {
            it.copy(
                    selectedYear = year,
                    availableMonths = months,
                    selectedMonth = defaultMonth,
                    isEditMode = false
            )
        }
        _uiState.value.selectedZoneId?.let { selectZone(it) }
    }

    fun selectZone(zoneId: Long) {
        _uiState.update { it.copy(selectedZoneId = zoneId, isLoading = true) }
        loadReadingsForZone(zoneId)
    }

    private fun loadReadingsForZone(zoneId: Long) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val monthStart = currentState.selectedMonth.startEpochMillis
            val monthEnd = currentState.selectedMonth.endEpochMillis

            repository.getSubscribersByZone(zoneId).collect { subscribers ->
                val entriesMap =
                        subscribers.associate { subscriber ->
                            // 1. Check if invoice already exists for this month
                            val existingInvoice =
                                    repository.checkExistingInvoice(
                                            subscriber.id,
                                            monthStart,
                                            monthEnd
                                    )

                            if (existingInvoice != null) {
                                // Invoice exists - load it.
                                // We set hasInvoice=true for visual cues, but we also store
                                // invoiceId for updates.
                                subscriber.id to
                                        MeterReadingEntry(
                                                subscriberId = subscriber.id,
                                                subscriberName = subscriber.fullName,
                                                meterNumber = subscriber.meterNumber,
                                                previousReading = existingInvoice.previousReading,
                                                currentReading =
                                                        existingInvoice.currentReading.toString(),
                                                consumption = existingInvoice.consumption,
                                                hasInvoice = true,
                                                invoiceId = existingInvoice.id
                                        )
                            } else {
                                // No invoice - prepare for entry
                                // 2. Get previous reading from latest invoice BEFORE this month
                                // using monthStart ensures we get the latest invoice from previous
                                // months
                                val priorInvoice =
                                        repository.getLatestInvoiceBeforeDate(
                                                subscriber.id,
                                                monthStart
                                        )
                                val prevReading = priorInvoice?.currentReading ?: 0L

                                subscriber.id to
                                        MeterReadingEntry(
                                                subscriberId = subscriber.id,
                                                subscriberName = subscriber.fullName,
                                                meterNumber = subscriber.meterNumber,
                                                previousReading = prevReading,
                                                currentReading = "",
                                                consumption = 0,
                                                hasInvoice = false,
                                                invoiceId = null
                                        )
                            }
                        }
                _uiState.update {
                    it.copy(subscribers = subscribers, readings = entriesMap, isLoading = false)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectMonth(month: MonthYear) {
        _uiState.update {
            it.copy(selectedMonth = month, isEditMode = false, selectedYear = month.year)
        }
        _uiState.value.selectedZoneId?.let { selectZone(it) }
    }

    fun toggleEditMode() {
        if (_uiState.value.isEditableMonth) {
            _uiState.update { it.copy(isEditMode = !it.isEditMode) }
        } else {
            showMessage(
                    "لا يمكن التعديل على شهور سابقة. يمكن التعديل على الشهر الحالي والشهر القادم فقط."
            )
        }
    }

    fun updateReading(subscriberId: Long, newReading: String) {
        if (!_uiState.value.isEditMode) return

        val currentEntry = _uiState.value.readings[subscriberId] ?: return
        val current = newReading.toLongOrNull() ?: 0
        val consumption =
                if (current > currentEntry.previousReading) current - currentEntry.previousReading
                else 0

        val updatedEntry = currentEntry.copy(currentReading = newReading, consumption = consumption)

        _uiState.update { state ->
            state.copy(readings = state.readings + (subscriberId to updatedEntry))
        }
    }

    fun saveAllReadings() {
        if (!_uiState.value.isEditableMonth) {
            showMessage("خطأ: لا يمكن حفظ القراءات لشهر سابق. يمكن الحفظ للشهر الحالي والقادم فقط.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Filter for readings that have a value entered
                val entriesToSave =
                        _uiState.value.readings.values.filter {
                            it.currentReading.isNotBlank() &&
                                    (it.currentReading.toLongOrNull() ?: 0) > 0
                        }

                if (entriesToSave.isEmpty()) {
                    showMessage("لا توجد قراءات صالحة للحفظ")
                    return@launch
                }

                // Validation: Check if any current reading is less than previous reading
                val invalidEntries =
                        entriesToSave.filter {
                            (it.currentReading.toLongOrNull() ?: 0) < it.previousReading
                        }

                if (invalidEntries.isNotEmpty()) {
                    val names = invalidEntries.take(3).joinToString(", ") { it.subscriberName }
                    val more = if (invalidEntries.size > 3) " وآخرين" else ""
                    showMessage("خطأ: القراءة الحالية أقل من السابقة لـ: $names$more")
                    return@launch
                }

                val currentTime = Clock.System.now().toEpochMilliseconds()
                val dueDate = currentTime + (30L * 24 * 60 * 60 * 1000) // 30 days from now

                var savedCount = 0
                entriesToSave.forEach { entry ->
                    val currentReading = entry.currentReading.toLongOrNull() ?: 0
                    val consumption = entry.consumption
                    val totalAmount = repository.calculateInvoiceAmount(consumption)

                    if (entry.invoiceId != null && entry.hasInvoice) {
                        // Update existing invoice
                        repository.updateInvoice(
                                id = entry.invoiceId,
                                currentReading = currentReading,
                                consumption = consumption,
                                totalAmount = totalAmount
                        )
                        savedCount++
                    } else {
                        // Insert new invoice
                        // Double check one last time before inserting to avoid race
                        // condition/duplicates
                        val monthStart = _uiState.value.selectedMonth.startEpochMillis
                        val monthEnd = _uiState.value.selectedMonth.endEpochMillis
                        val exists =
                                repository.checkExistingInvoice(
                                        entry.subscriberId,
                                        monthStart,
                                        monthEnd
                                )

                        if (exists != null) {
                            // Race condition handled: Update it instead
                            repository.updateInvoice(
                                    id = exists.id,
                                    currentReading = currentReading,
                                    consumption = consumption,
                                    totalAmount = totalAmount
                            )
                        } else {
                            repository.insertInvoice(
                                    subscriberId = entry.subscriberId,
                                    previousReading = entry.previousReading,
                                    currentReading = currentReading,
                                    consumption = consumption,
                                    totalAmount = totalAmount,
                                    status = "UNPAID",
                                    issueDate = currentTime,
                                    dueDate = dueDate
                            )
                        }
                        savedCount++
                    }
                }

                showMessage("تم حفظ/تحديث $savedCount فاتورة بنجاح")

                // Reset edit mode and reload
                _uiState.update { it.copy(isEditMode = false) }
                _uiState.value.selectedZoneId?.let { selectZone(it) }
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
