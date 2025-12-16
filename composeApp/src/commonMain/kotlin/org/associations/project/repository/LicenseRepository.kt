package org.associations.project.repository

import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import org.associations.project.security.DeviceFingerprint

@Serializable
data class License(
        val key: String,
        val hardware_id: String?,
        val client_name: String,
        val is_active: Boolean
)

sealed class LicenseResult {
    object Success : LicenseResult()
    data class Error(val message: String) : LicenseResult()
    object Loading : LicenseResult()
}

class LicenseRepository(
        private val supabase: SupabaseClient,
        private val deviceFingerprint: DeviceFingerprint,
        private val settings: Settings
) {
    private val KEY_IS_ACTIVATED = "is_license_activated"

    fun isActivated(): Boolean {
        return settings.getBoolean(KEY_IS_ACTIVATED, false)
    }

    fun activateLicense(licenseKey: String): Flow<LicenseResult> = flow {
        emit(LicenseResult.Loading)
        try {
            val deviceId = deviceFingerprint.getDeviceId()

            // Query for the license
            val result =
                    supabase.postgrest
                            .from("licenses")
                            .select(
                                    columns =
                                            Columns.list(
                                                    "key",
                                                    "hardware_id",
                                                    "client_name",
                                                    "is_active"
                                            )
                            ) { filter { eq("key", licenseKey) } }
                            .decodeSingleOrNull<License>()

            if (result == null) {
                emit(LicenseResult.Error("Invalid License Key"))
                return@flow
            }

            if (!result.is_active) {
                emit(LicenseResult.Error("License is inactive"))
                return@flow
            }

            if (result.hardware_id == null) {
                // Bind this machine
                supabase.postgrest.from("licenses").update({ set("hardware_id", deviceId) }) {
                    filter { eq("key", licenseKey) }
                }
                settings.putBoolean(KEY_IS_ACTIVATED, true)
                emit(LicenseResult.Success)
            } else if (result.hardware_id == deviceId) {
                // Already bound to this machine
                settings.putBoolean(KEY_IS_ACTIVATED, true)
                emit(LicenseResult.Success)
            } else {
                // Different hardware ID
                emit(LicenseResult.Error("License already used on another device"))
            }
        } catch (e: Exception) {
            emit(LicenseResult.Error("Network error: ${e.message}"))
        }
    }
}
