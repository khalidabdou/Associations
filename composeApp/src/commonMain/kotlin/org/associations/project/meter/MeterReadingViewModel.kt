package org.associations.project.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.associations.project.billing.PrintService
import org.associations.project.billing.ShareService
import org.associations.project.database.GetSubscribersByZone
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.Zone
import org.associations.project.repository.AppRepository
import org.associations.project.utils.MonthYear
import java.io.InputStream

data class MeterReadingEntry(
        val subscriberId: Long,
        val subscriberName: String,
        val meterNumber: String,
        val previousReading: Long = 0,
        val currentReading: String = "",
        val consumption: Long = 0,
        val hasInvoice: Boolean = false,
        val invoiceId: Long? = null,
        val invoiceAmount: Double? = null
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
        val isPrinting: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
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
            val allReadings = readings.values.sortedWith(compareBy(naturalOrder<String>()) { it.meterNumber })
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

    val invoicedReadings: List<MeterReadingEntry>
        get() = filteredReadings.filter { it.hasInvoice && it.invoiceId != null }

    val isAllInvoicedSelected: Boolean
        get() {
            val ids = invoicedReadings.map { it.subscriberId }
            return ids.isNotEmpty() && selectedIds.containsAll(ids)
        }
}

class MeterReadingViewModel(
        private val repository: AppRepository,
        private val settings: Settings,
        private val shareService: ShareService,
        private val printService: PrintService
) : ViewModel() {
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

    fun shareNotification(subscriberId: Long) {
        viewModelScope.launch {
            try {
                val entry = _uiState.value.readings[subscriberId]
                if (entry == null || entry.invoiceId == null || !entry.hasInvoice) {
                    showMessage("حفظ القراءة أولا لإرسال الإشعار")
                    return@launch
                }

                val appSettings = repository.getSettings().first()
                if (appSettings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }

                val details = repository.getInvoiceDetailsOnce(entry.invoiceId)
                if (details == null) {
                    showMessage("خطأ: الفاتورة غير موجودة")
                    return@launch
                }

                val invoice = Invoice(
                        id = details.id,
                        subscriberId = details.subscriberId,
                        previousReading = details.previousReading,
                        currentReading = details.currentReading,
                        consumption = details.consumption,
                        totalAmount = details.totalAmount,
                        status = details.status,
                        issueDate = details.issueDate,
                        dueDate = details.dueDate,
                        isPenaltyApplied = details.isPenaltyApplied
                )
                val subscriber = Subscriber(
                        id = details.subscriberId,
                        fullName = details.subscriberName ?: entry.subscriberName,
                        phone = null,
                        meterNumber = details.meterNumber ?: entry.meterNumber,
                        address = details.address,
                        zoneId = 0,
                        isActive = 1,
                        createdAt = 0
                )
                shareService.shareInvoice(invoice, subscriber, appSettings)
                showMessage("تم فتح إشعار الدفع للمشاركة")
            } catch (e: Exception) {
                showMessage("خطأ: ${e.message}")
            }
        }
    }

    fun printNotification(subscriberId: Long) {
        viewModelScope.launch {
            try {
                val entry = _uiState.value.readings[subscriberId]
                if (entry == null || entry.invoiceId == null || !entry.hasInvoice) {
                    showMessage("حفظ القراءة أولا لطباعة الإشعار")
                    return@launch
                }

                val appSettings = repository.getSettings().first()
                if (appSettings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }

                val details = repository.getInvoiceDetailsOnce(entry.invoiceId)
                if (details == null) {
                    showMessage("خطأ: الفاتورة غير موجودة")
                    return@launch
                }

                val invoice = Invoice(
                        id = details.id,
                        subscriberId = details.subscriberId,
                        previousReading = details.previousReading,
                        currentReading = details.currentReading,
                        consumption = details.consumption,
                        totalAmount = details.totalAmount,
                        status = details.status,
                        issueDate = details.issueDate,
                        dueDate = details.dueDate,
                        isPenaltyApplied = details.isPenaltyApplied
                )
                val subscriber = Subscriber(
                        id = details.subscriberId,
                        fullName = details.subscriberName ?: entry.subscriberName,
                        phone = null,
                        meterNumber = details.meterNumber ?: entry.meterNumber,
                        address = details.address,
                        zoneId = 0,
                        isActive = 1,
                        createdAt = 0
                )
                if (appSettings.printFormat == "POS") {
                    val connectionType = settings.getString("printer_connection_type", "BLUETOOTH")
                    if (connectionType == "USB") {
                        val deviceId = settings.getInt("usb_printer_device_id", -1)
                        if (deviceId >= 0) {
                            val result = printService.printNotificationViaUsb(invoice, subscriber, appSettings, deviceId)
                            result.fold(
                                onSuccess = { showMessage("تمت طباعة الإشعار عبر USB") },
                                onFailure = { showMessage("فشل الطباعة: ${it.message}") }
                            )
                        } else {
                            showMessage("يرجى إعداد واختبار طابعة USB في الإعدادات أولاً")
                        }
                    } else {
                        val savedAddress = settings.getString("bluetooth_printer_address", "")
                        if (savedAddress.isNotEmpty()) {
                            val result = printService.printNotificationViaBluetooth(invoice, subscriber, appSettings, savedAddress)
                            result.fold(
                                onSuccess = { showMessage("تمت طباعة الإشعار عبر البلوتوث") },
                                onFailure = { showMessage("فشل الطباعة: ${it.message}") }
                            )
                        } else {
                            showMessage("يرجى إعداد واختبار طابعة البلوتوث في الإعدادات أولاً")
                        }
                    }
                } else {
                    printService.printNotification(invoice, subscriber, appSettings)
                    showMessage("تم إرسال إشعار الدفع للطباعة")
                }
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
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

    /** Surface a message originating from outside the VM (e.g. file picker results). */
    fun showExternalMessage(message: String) {
        showMessage(message)
    }

    // ----- Selection -----
    fun toggleSelection(subscriberId: Long) {
        val entry = _uiState.value.readings[subscriberId] ?: return
        if (!entry.hasInvoice || entry.invoiceId == null) return
        _uiState.update { state ->
            val newSet = if (subscriberId in state.selectedIds)
                state.selectedIds - subscriberId
            else state.selectedIds + subscriberId
            state.copy(selectedIds = newSet)
        }
    }

    fun toggleSelectAllInvoiced() {
        val state = _uiState.value
        val invoicedIds = state.invoicedReadings.map { it.subscriberId }.toSet()
        val newSelected = if (state.selectedIds.containsAll(invoicedIds) && invoicedIds.isNotEmpty())
            state.selectedIds - invoicedIds
        else state.selectedIds + invoicedIds
        _uiState.update { it.copy(selectedIds = newSelected) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    // ----- Bulk print -----
    fun printSelected(copies: Int = 1) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) {
            showMessage("لم يتم تحديد أي قراءة")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            try {
                val appSettings = repository.getSettings().first()
                if (appSettings == null) {
                    showMessage("خطأ: لم يتم العثور على الإعدادات")
                    return@launch
                }
                val items = mutableListOf<Pair<Invoice, Subscriber>>()
                for (subscriberId in ids) {
                    val entry = _uiState.value.readings[subscriberId] ?: continue
                    val invoiceId = entry.invoiceId ?: continue
                    val details = repository.getInvoiceDetailsOnce(invoiceId) ?: continue
                    val invoice = Invoice(
                            id = details.id,
                            subscriberId = details.subscriberId,
                            previousReading = details.previousReading,
                            currentReading = details.currentReading,
                            consumption = details.consumption,
                            totalAmount = details.totalAmount,
                            status = details.status,
                            issueDate = details.issueDate,
                            dueDate = details.dueDate,
                            isPenaltyApplied = details.isPenaltyApplied
                    )
                    val subscriber = Subscriber(
                            id = details.subscriberId,
                            fullName = details.subscriberName ?: entry.subscriberName,
                            phone = null,
                            meterNumber = details.meterNumber ?: entry.meterNumber,
                            address = details.address,
                            zoneId = 0,
                            isActive = 1,
                            createdAt = 0
                    )
                    items.add(invoice to subscriber)
                }
                if (items.isEmpty()) {
                    showMessage("لا توجد فواتير صالحة للطباعة")
                    return@launch
                }
                // Duplicate items for multiple copies
                val printItems = if (copies > 1) items.flatMap { item -> List(copies) { item } } else items
                val totalPages = printItems.size
                val month = _uiState.value.selectedMonth

                if (appSettings.printFormat == "POS") {
                    val connectionType = settings.getString("printer_connection_type", "BLUETOOTH")
                    if (connectionType == "USB") {
                        val deviceId = settings.getInt("usb_printer_device_id", -1)
                        if (deviceId < 0) {
                            showMessage("يرجى إعداد واختبار طابعة USB في الإعدادات أولاً")
                            return@launch
                        }
                        var successCount = 0
                        for ((invoice, subscriber) in printItems) {
                            val result = printService.printInvoiceViaUsb(invoice, subscriber, appSettings, deviceId)
                            result.fold(
                                onSuccess = { successCount++ },
                                onFailure = {
                                    showMessage("فشل الطباعة للمشترك ${subscriber.fullName}: ${it.message}")
                                    return@launch
                                }
                            )
                        }
                        showMessage("تمت طباعة $successCount فاتورة عبر USB")
                    } else {
                        // Bluetooth path — print each invoice sequentially.
                        // BluetoothThermalPrinter.printMutex ensures they don't run concurrently.
                        val savedAddress = settings.getString("bluetooth_printer_address", "")
                        if (savedAddress.isEmpty()) {
                            showMessage("يرجى إعداد واختبار طابعة البلوتوث في الإعدادات أولاً")
                            return@launch
                        }
                        var successCount = 0
                        for ((invoice, subscriber) in printItems) {
                            val result = printService.printInvoiceViaBluetooth(invoice, subscriber, appSettings, savedAddress)
                            result.fold(
                                onSuccess = { successCount++ },
                                onFailure = {
                                    showMessage("فشل الطباعة للمشترك ${subscriber.fullName}: ${it.message}")
                                    return@launch
                                }
                            )
                        }
                        showMessage("تمت طباعة $successCount فاتورة عبر البلوتوث")
                    }
                } else {
                    // System print path (A4 / A5) — requires Activity context, not Application context.
                    printService.printInvoices(
                            items = printItems,
                            settings = appSettings,
                            jobName = "Invoices ${month.month}-${month.year} (${items.size} × ${copies})"
                    )
                    showMessage("تم إرسال ${items.size} فاتورة × ${copies} نسخة ($totalPages صفحة) للطباعة")
                }
                clearSelection()
            } catch (e: Exception) {
                showMessage("خطأ في الطباعة: ${e.message}")
            } finally {
                _uiState.update { it.copy(isPrinting = false) }
            }
        }
    }

    // ----- CSV import -----
    /**
     * Expected CSV format (UTF-8, comma separated):
     *
     *   month,year,meter_number,subscriber_name,current_reading
     *   5,2026,8837883,abdellah,2000
     *
     * - The first non-empty line is treated as the header (case-insensitive).
     * - Every data row must have month/year matching the currently selected month;
     *   rows belonging to a different month are rejected.
     * - Subscribers are matched by meter_number (primary) within the selected zone,
     *   then fall back to subscriber_name.
     * - Imported readings are auto-saved as invoices for the current zone/month.
     */
    fun importReadingsFromCsv(input: InputStream): Result<String> {
        return try {
            val state = _uiState.value
            val zoneId = state.selectedZoneId
                    ?: return Result.failure(IllegalStateException("اختر منطقة أولا"))
            if (!state.isEditableMonth) {
                return Result.failure(IllegalStateException(
                        "لا يمكن استيراد قراءات لشهر سابق. يمكن الاستيراد للشهر الحالي والقادم فقط."
                ))
            }

            val text = input.bufferedReader(Charsets.UTF_8).readText()
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) {
                return Result.failure(IllegalStateException("الملف فارغ أو لا يحتوي على بيانات"))
            }

            val header = lines.first().split(',').map { it.trim().lowercase() }
            val idxMonth = header.indexOf("month")
            val idxYear = header.indexOf("year")
            val idxMeter = header.indexOfFirst { it == "meter_number" || it == "meter" }
            val idxName = header.indexOfFirst { it == "subscriber_name" || it == "name" || it == "customer" || it == "customer_name" }
            val idxReading = header.indexOfFirst { it == "current_reading" || it == "reading" || it == "current" }
            if (idxMonth < 0 || idxYear < 0 || idxMeter < 0 || idxReading < 0) {
                return Result.failure(IllegalStateException(
                        "رؤوس الأعمدة المطلوبة: month,year,meter_number,subscriber_name,current_reading"
                ))
            }

            val targetMonth = state.selectedMonth.month
            val targetYear = state.selectedMonth.year

            data class Row(val meter: String, val name: String, val reading: Long)
            val rows = mutableListOf<Row>()
            val errors = mutableListOf<String>()

            lines.drop(1).forEachIndexed { i, raw ->
                val cols = raw.split(',').map { it.trim() }
                if (cols.size <= maxOf(idxMonth, idxYear, idxMeter, idxReading)) {
                    errors.add("سطر ${i + 2}: عدد الأعمدة غير صحيح")
                    return@forEachIndexed
                }
                val m = cols[idxMonth].toIntOrNull()
                val y = cols[idxYear].toIntOrNull()
                if (m == null || y == null) {
                    errors.add("سطر ${i + 2}: شهر/سنة غير صالحين")
                    return@forEachIndexed
                }
                if (m != targetMonth || y != targetYear) {
                    errors.add("سطر ${i + 2}: لا يطابق الشهر المحدد ($targetMonth/$targetYear)")
                    return@forEachIndexed
                }
                val meter = cols[idxMeter]
                val name = if (idxName >= 0 && idxName < cols.size) cols[idxName] else ""
                val readingValue = cols[idxReading].toLongOrNull()
                if (readingValue == null || readingValue < 0) {
                    errors.add("سطر ${i + 2}: قراءة غير صالحة")
                    return@forEachIndexed
                }
                rows.add(Row(meter, name, readingValue))
            }

            if (rows.isEmpty()) {
                return Result.failure(IllegalStateException(
                        "لا توجد صفوف صالحة للشهر المحدد" + if (errors.isNotEmpty()) "\n" + errors.take(3).joinToString("\n") else ""
                ))
            }

            // Match against current readings map by meter, then by name
            val readings = state.readings.values
            val byMeter = readings.associateBy { it.meterNumber.trim() }
            val byName = readings.associateBy { it.subscriberName.trim() }

            var applied = 0
            var skippedInvalid = 0
            val updates = mutableMapOf<Long, MeterReadingEntry>()
            for (row in rows) {
                val match = byMeter[row.meter] ?: byName[row.name]
                if (match == null) {
                    errors.add("غير موجود: ${row.meter} / ${row.name}")
                    continue
                }
                if (row.reading < match.previousReading) {
                    skippedInvalid++
                    errors.add("${match.subscriberName}: القراءة (${row.reading}) أقل من السابقة (${match.previousReading})")
                    continue
                }
                val consumption = row.reading - match.previousReading
                updates[match.subscriberId] = match.copy(
                        currentReading = row.reading.toString(),
                        consumption = consumption
                )
                applied++
            }

            if (updates.isEmpty()) {
                return Result.failure(IllegalStateException("لم يتم العثور على أي مشترك مطابق"))
            }

            // Apply to UI state, enable edit mode
            _uiState.update { s ->
                s.copy(
                        readings = s.readings + updates,
                        isEditMode = true
                )
            }

            // Auto-save
            saveAllReadings()

            val warn = if (errors.isNotEmpty()) " (تحذيرات: ${errors.size})" else ""
            Result.success("تم استيراد $applied قراءة$warn")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
