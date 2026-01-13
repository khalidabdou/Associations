package org.associations.project.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.associations.project.database.PricingTier
import org.associations.project.database.Zone
import org.associations.project.repository.AppRepository
import org.associations.project.repository.LicenseRepository

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
        val message: String? = null
)

class SettingsViewModel(
        private val repository: AppRepository,
        private val licenseRepository: LicenseRepository,
        private val settings: Settings
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_ALLOW_PAST_MONTH_EDITING = "allow_past_month_editing"
    }

    init {
        // Check activation status and load settings
        _uiState.update {
            it.copy(
                    isActivated = licenseRepository.isActivated(),
                    allowPastMonthEditing = settings.getBoolean(KEY_ALLOW_PAST_MONTH_EDITING, false)
            )
        }
        loadData()
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
                        message = _uiState.value.message
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

    fun exportData() {
        // Platform specific
        // Generate filename with timestamp
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() // unique enough
        val filename = "Association_Backup_$timestamp.db"

        // Use Platform to get downloads dir (we need to expect/actual this or use System property)
        val home = System.getProperty("user.home")
        val downloadsPath = if (home != null) "$home/Downloads" else null

        if (downloadsPath != null) {
            val fullPath = "$downloadsPath/$filename"
            viewModelScope.launch {
                try {
                    repository.exportDatabase(fullPath)
                    showMessage("تم تصدير البيانات بنجاح إلى $fullPath")
                } catch (e: Exception) {
                    showMessage("فشل التصدير: ${e.message}")
                }
            }
        } else {
            // Fallback to picker if downloads not found
            val path = org.associations.project.utils.FilePicker.pickDirectory()
            if (path != null) {
                viewModelScope.launch {
                    try {
                        repository.exportDatabase(path)
                        showMessage("تم تصدير البيانات بنجاح إلى $path")
                    } catch (e: Exception) {
                        showMessage("فشل التصدير: ${e.message}")
                    }
                }
            } else {
                showMessage("لم يتم تحديد مسار التصدير")
            }
        }
    }

    fun importData() {
        val path = org.associations.project.utils.FilePicker.pickFile()
        if (path != null) {
            viewModelScope.launch {
                try {
                    repository.importDatabase(path)
                    showMessage("تم استيراد البيانات بنجاح. يرجى إعادة تشغيل التطبيق.")
                    // Trigger a reload or restart if possible, for now just message
                } catch (e: Exception) {
                    showMessage("فشل الاستيراد: ${e.message}")
                }
            }
        }
    }

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
}
