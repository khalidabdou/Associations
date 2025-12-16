package org.associations.project.security

import android.content.Context
import android.provider.Settings

class AndroidDeviceFingerprint(private val context: Context) : DeviceFingerprint {
    override fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_device_id"
    }
}
