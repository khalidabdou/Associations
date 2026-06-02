package org.associations.project.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.billing.PrintService
import org.associations.project.database.PricingTier
import org.associations.project.database.Zone
import org.associations.project.reports.MonthlyReportData
import org.associations.project.repository.AppRepository
import org.associations.project.repository.LicenseRepository
import org.associations.project.utils.MonthYear

data class SettingsUiState(
        val zones: List<Zone> = emptyList(),
        val pricingTiers: List<PricingTier> = emptyList(),
        val lateFeePercent: Double = 5.0,
        val monthlyFixedFee: Double = 0.0,
        val gracePeriodDays: Int = 15,
        val dueDateDays: Int = 30,
        val associationName: String = "Association",
        val associationAddress: String = "",
        val associationPhone: String = "",
        val printFormat: String = "A4",
        val logoPath: String? = null,
        val allowPastMonthEditing: Boolean = false,
        val editingField: String? =
                null, // "lateFee", "monthlyFee", "gracePeriod", "dueDate", "assocName",
        // "assocAddress", "assocPhone", "printFormat", "clearData"
        val confirmationCode: String? = null,
        val userEnteredCode: String = "",
        val isLoading: Boolean = true,
        val isActivated: Boolean = false,
        val message: String? = null,
        val reportMonth: MonthYear = MonthYear.current(),
        val isPrintingReport: Boolean = false,
        val isExportingReport: Boolean = false,
        val isExportingCsv: Boolean = false
)

class SettingsViewModel(
        private val repository: AppRepository,
        private val licenseRepository: LicenseRepository,
        private val settings: Settings,
        private val printService: PrintService
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_ALLOW_PAST_MONTH_EDITING = "allow_past_month_editing"
    }

    init {
        // Seed with last known local state, then re-verify against Supabase
        _uiState.update {
            it.copy(
                    isActivated = licenseRepository.isActivated(),
                    allowPastMonthEditing = settings.getBoolean(KEY_ALLOW_PAST_MONTH_EDITING, false)
            )
        }
        refreshActivationStatus()
        loadData()
    }

    /**
     * Re-queries Supabase to confirm the license is still active and bound to this device.
     * Called on init so every entry to the settings screen revalidates activation. If the admin
     * flips `is_active=false` in Supabase, this will clear the local activation flag.
     */
    fun refreshActivationStatus() {
        viewModelScope.launch {
            val stillActive = licenseRepository.verifyActivation()
            _uiState.update { it.copy(isActivated = stillActive) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                            repository.getZones(),
                            repository.getAllPricingTiers(),
                            repository.getSettings()
                    ) { zones, tiers, settings ->
                val currentLateFee = settings?.lateFeeAmount ?: 5.0
                val currentMonthlyFee = settings?.monthlyFixedFee ?: 0.0
                val currentGraceUsers = settings?.gracePeriodDays?.toInt() ?: 15
                val currentDueDays = settings?.dueDateDays?.toInt() ?: 30
                val assocName = settings?.associationName ?: "Water Association"
                val assocAddress = settings?.associationAddress ?: ""
                val assocPhone = settings?.associationPhone ?: ""
                val format = settings?.printFormat ?: "A4"
                val logo = settings?.logoPath

                SettingsUiState(
                        zones = zones,
                        pricingTiers = tiers,
                        lateFeePercent = currentLateFee,
                        monthlyFixedFee = currentMonthlyFee,
                        gracePeriodDays = currentGraceUsers,
                        dueDateDays = currentDueDays,
                        associationName = assocName,
                        associationAddress = assocAddress,
                        associationPhone = assocPhone,
                        printFormat = format,
                        logoPath = logo,
                        isLoading = false,
                        editingField = _uiState.value.editingField,
                        message = _uiState.value.message,
                        reportMonth = _uiState.value.reportMonth,
                        isPrintingReport = _uiState.value.isPrintingReport,
                        isExportingReport = _uiState.value.isExportingReport,
                        isExportingCsv = _uiState.value.isExportingCsv
                )
            }
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                    isLoading = false,
                                    message =
                                            "خطأ في تحميل البيانات: ${e.message}\nيرجى حذف ملف قاعدة البيانات لإعادة الإنشاء."
                            )
                        }
                        e.printStackTrace()
                    }
                    .collect { state -> _uiState.value = state }
        }
    }

    // ... (Dialog helpers same)

    fun showEditDialog(field: String) {
        _uiState.update { it.copy(editingField = field) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(editingField = null) }
    }

    private fun saveSettings(
            lateFee: Double = uiState.value.lateFeePercent,
            monthlyFee: Double = uiState.value.monthlyFixedFee,
            gracePeriod: Int = uiState.value.gracePeriodDays,
            dueDays: Int = uiState.value.dueDateDays,
            assocName: String = uiState.value.associationName,
            assocAddress: String = uiState.value.associationAddress,
            assocPhone: String = uiState.value.associationPhone,
            format: String = uiState.value.printFormat,
            logo: String? = uiState.value.logoPath
    ) {
        viewModelScope.launch {
            try {
                repository.updateSettings(
                        lateFee,
                        monthlyFee,
                        gracePeriod,
                        dueDays,
                        assocName,
                        assocAddress,
                        assocPhone,
                        format,
                        logo
                )
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء حفظ الإعدادات: ${e.message}")
            }
        }
    }

    fun updateLateFee(value: String) {
        val amount = value.toDoubleOrNull() ?: return
        dismissEditDialog()
        saveSettings(lateFee = amount)
        showMessage("تم تحديث غرامة التأخير")
    }

    fun updateMonthlyFee(value: String) {
        val fee = value.toDoubleOrNull() ?: return
        dismissEditDialog()
        saveSettings(monthlyFee = fee)
        showMessage("تم تحديث الرسوم الشهرية")
    }

    fun updateGracePeriod(value: String) {
        val days = value.toIntOrNull() ?: return
        dismissEditDialog()
        saveSettings(gracePeriod = days)
        showMessage("تم تحديث فترة السماح")
    }

    fun updateDueDate(value: String) {
        val days = value.toIntOrNull() ?: return
        dismissEditDialog()
        saveSettings(dueDays = days)
        showMessage("تم تحديث أيام الاستحقاق")
    }

    fun updateAssociationDetails(name: String, address: String, phone: String) {
        dismissEditDialog()
        saveSettings(assocName = name, assocAddress = address, assocPhone = phone)
        showMessage("تم تحديث بيانات الجمعية")
    }

    fun updatePrintFormat(format: String) {
        saveSettings(format = format)
        showMessage("تم تحديث تنسيق الطباعة إلى $format")
    }

    fun updateLogo(path: String?) {
        saveSettings(logo = path)
    }

    fun updateAllowPastMonthEditing(enabled: Boolean) {
        settings.putBoolean(KEY_ALLOW_PAST_MONTH_EDITING, enabled)
        _uiState.update { it.copy(allowPastMonthEditing = enabled) }
        showMessage(
                if (enabled) "تم تفعيل تعديل الأشهر السابقة" else "تم إلغاء تعديل الأشهر السابقة"
        )
    }

    // ===== Backup & Restore =====

    /** Suggested backup filename exposed to the UI for the system file picker. */
    fun suggestedBackupFileName(): String {
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "Association_Backup_$timestamp.db"
    }

    /**
     * Exports the database into the given [OutputStream]. The stream is provided by the
     * platform-specific [org.associations.project.utils.rememberBackupLauncher] (Android SAF /
     * desktop JFileChooser). Returns a [Result] so the UI layer can show a message.
     */
    fun exportToStream(out: java.io.OutputStream): Result<String> = try {
        repository.exportDatabaseToStream(out)
        Result.success("تم تصدير البيانات بنجاح")
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Imports a database from the given [InputStream] (picked via SAF on Android, JFileChooser on
     * desktop). Returns a [Result] so the UI layer can surface a message.
     */
    fun importFromStream(input: java.io.InputStream): Result<String> = try {
        repository.importDatabaseFromStream(input)
        Result.success("تم استيراد البيانات بنجاح. يرجى إعادة تشغيل التطبيق.")
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Allows the UI layer to push a snackbar message. */
    fun postMessage(message: String) = showMessage(message)

    // ===== Clear Data logic =====
    fun showClearDataDialog() {
        val code = (1000..9999).random().toString()
        _uiState.update {
            it.copy(editingField = "clearData", confirmationCode = code, userEnteredCode = "")
        }
    }

    fun updateEnteredCode(code: String) {
        _uiState.update { it.copy(userEnteredCode = code) }
    }

    fun confirmClearData() {
        val state = uiState.value
        if (state.userEnteredCode == state.confirmationCode) {
            viewModelScope.launch {
                try {
                    repository.clearUserData() // Use the safe method
                    showMessage("تم مسح بيانات المستخدم بنجاح.")
                    dismissEditDialog()
                } catch (e: Exception) {
                    showMessage("خطأ في مسح البيانات: ${e.message}")
                }
            }
        } else {
            showMessage("رمز التأكيد غير صحيح")
        }
    }

    // Zone Operations
    fun addZone(name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.insertZone(name, description)
                showMessage("تمت إضافة المنطقة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun updateZone(id: Long, name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.updateZone(id, name, description)
                showMessage("تم تحديث المنطقة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun deleteZone(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteZone(id)
                showMessage("تم حذف المنطقة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    // Pricing Tier Operations
    fun addPricingTier(minUsage: Long, maxUsage: Long, pricePerUnit: Double) {
        viewModelScope.launch {
            try {
                repository.insertPricingTier(minUsage, maxUsage, pricePerUnit)
                showMessage("تمت إضافة الشريحة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun updatePricingTier(id: Long, minUsage: Long, maxUsage: Long, pricePerUnit: Double) {
        viewModelScope.launch {
            try {
                repository.updatePricingTier(id, minUsage, maxUsage, pricePerUnit)
                showMessage("تم تحديث الشريحة بنجاح")
            } catch (e: Exception) {
                showMessage("حدث خطأ: ${e.message}")
            }
        }
    }

    fun deletePricingTier(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePricingTier(id)
                showMessage("تم حذف الشريحة بنجاح")
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

    // ===== Monthly Report =====

    fun previousReportMonth() {
        _uiState.update { it.copy(reportMonth = it.reportMonth.previous()) }
    }

    fun nextReportMonth() {
        val next = _uiState.value.reportMonth.next()
        // Don't allow going beyond current month
        if (next.year < MonthYear.current().year ||
            (next.year == MonthYear.current().year && next.month <= MonthYear.current().month)
        ) {
            _uiState.update { it.copy(reportMonth = next) }
        }
    }

    fun suggestedReportFileName(): String {
        val month = _uiState.value.reportMonth
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "Report_${month.year}_${month.month}_$timestamp.pdf"
    }

    fun printMonthlyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingReport = true) }
            try {
                val reportData = buildMonthlyReport()
                val appSettings = repository.getSettings().first()
                if (appSettings != null && reportData != null) {
                    printService.printMonthlyReport(reportData, appSettings)
                    showMessage("تم إرسال التقرير الشهري للطباعة")
                } else {
                    showMessage("الرجاء حفظ إعدادات الجمعية أولاً")
                }
            } catch (e: Exception) {
                showMessage("خطأ في طباعة التقرير: ${e.message}")
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isPrintingReport = false) }
            }
        }
    }

    suspend fun exportMonthlyReport(outputStream: java.io.OutputStream) {
        _uiState.update { it.copy(isExportingReport = true) }
        try {
            val reportData = buildMonthlyReport()
            val appSettings = repository.getSettings().first()
            if (appSettings != null && reportData != null) {
                printService.exportMonthlyReport(reportData, appSettings, outputStream)
                showMessage("تم تصدير التقرير بنجاح")
            } else {
                showMessage("الرجاء حفظ إعدادات الجمعية أولاً")
            }
        } catch (e: Exception) {
            showMessage("خطأ في تصدير التقرير: ${e.message}")
            e.printStackTrace()
        } finally {
            _uiState.update { it.copy(isExportingReport = false) }
        }
    }

    fun suggestedCsvFileName(): String {
        val month = _uiState.value.reportMonth
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "Report_${month.year}_${month.month}_$timestamp.csv"
    }

    suspend fun exportMonthlyReportCsv(outputStream: java.io.OutputStream) {
        _uiState.update { it.copy(isExportingCsv = true) }
        try {
            val reportData = buildMonthlyReport()
            val appSettings = repository.getSettings().first()
            if (appSettings != null && reportData != null) {
                val csvText = org.associations.project.reports.MonthlyReportCsvGenerator.generate(reportData, appSettings)
                outputStream.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                showMessage("تم تصدير CSV بنجاح")
            } else {
                showMessage("الرجاء حفظ إعدادات الجمعية أولاً")
            }
        } catch (e: Exception) {
            showMessage("خطأ في تصدير CSV: ${e.message}")
            e.printStackTrace()
        } finally {
            _uiState.update { it.copy(isExportingCsv = false) }
        }
    }

    private suspend fun buildMonthlyReport(): MonthlyReportData? {
        val month = _uiState.value.reportMonth
        val startDate = month.startEpochMillis
        val endDate = month.endEpochMillis

        val invoices = repository.getInvoicesByDateRange(startDate, endDate).first()
        val transactions = repository.getTransactionsByDateRange(startDate, endDate).first()

        val totalConsumption = invoices.sumOf { it.consumption }
        val totalInvoicesCount = invoices.size.toLong()
        val totalInvoicesAmount = invoices.sumOf { it.totalAmount }
        val paidInvoices = invoices.filter { it.status == "PAID" }
        val unpaidInvoices = invoices.filter { it.status != "PAID" }
        val paidInvoicesCount = paidInvoices.size.toLong()
        val paidInvoicesAmount = paidInvoices.sumOf { it.totalAmount }
        val unpaidInvoicesCount = unpaidInvoices.size.toLong()
        val unpaidInvoicesAmount = unpaidInvoices.sumOf { it.totalAmount }
        val totalIncomeAmount = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpensesAmount = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncomeAmount - totalExpensesAmount

        return MonthlyReportData(
            monthYear = month,
            totalConsumption = totalConsumption,
            totalInvoicesCount = totalInvoicesCount,
            totalInvoicesAmount = totalInvoicesAmount,
            paidInvoicesCount = paidInvoicesCount,
            paidInvoicesAmount = paidInvoicesAmount,
            unpaidInvoicesCount = unpaidInvoicesCount,
            unpaidInvoicesAmount = unpaidInvoicesAmount,
            totalIncomeAmount = totalIncomeAmount,
            totalExpensesAmount = totalExpensesAmount,
            netBalance = netBalance,
            invoices = invoices,
            transactions = transactions
        )
    }

    // ===== Seed Test Data =====
    fun seedTestData() {
        viewModelScope.launch {
            try {
                repository.seedTestData()
                showMessage("تم إنشاء بيانات تجريبية للاختبار")
            } catch (e: Exception) {
                showMessage("خطأ في إنشاء البيانات: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
