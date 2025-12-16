package org.associations.project.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.associations.project.database.DatabaseDriverFactory
import org.associations.project.security.AndroidDeviceFingerprint
import org.associations.project.security.DeviceFingerprint
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(get()).createDriver() }

    single<DeviceFingerprint> { AndroidDeviceFingerprint(get()) }
    single<Settings> {
        val context = get<Context>()
        SharedPreferencesSettings(
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        )
    }
}
