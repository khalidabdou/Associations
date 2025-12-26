package org.associations.project.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        val editingField: String? =
                null, // "lateFee", "monthlyFee", "gracePeriod", "dueDate", "assocName",
        // "assocAddress", "assocPhone", "printFormat"
        val isLoading: Boolean = true,
        val isActivated: Boolean = false,
        val message: String? = null
)

class SettingsViewModel(
        private val repository: AppRepository,
        private val licenseRepository: LicenseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Check activation status immediately
        _uiState.update { it.copy(isActivated = licenseRepository.isActivated()) }
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
