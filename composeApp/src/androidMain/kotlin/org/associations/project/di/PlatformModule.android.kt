package org.associations.project.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.associations.project.billing.AndroidPrintService
import org.associations.project.billing.AndroidShareService
import org.associations.project.billing.PrintService
import org.associations.project.billing.ShareService
import org.associations.project.database.DatabaseDriverFactory
import org.associations.project.security.AndroidDeviceFingerprint
import org.associations.project.security.DeviceFingerprint
import org.associations.project.utils.AppUpdater
import org.associations.project.utils.AndroidAppUpdater
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(get()) }
    single { get<DatabaseDriverFactory>().createDriver() }

    single { AndroidPrintService(get<Context>()) } bind PrintService::class
    single { AndroidShareService(get<Context>()) } bind ShareService::class
    single<DeviceFingerprint> { AndroidDeviceFingerprint(get()) }
    single<Settings> {
        val context = get<Context>()
        SharedPreferencesSettings(
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        )
    }
    single<AppUpdater> { AndroidAppUpdater() }
}
