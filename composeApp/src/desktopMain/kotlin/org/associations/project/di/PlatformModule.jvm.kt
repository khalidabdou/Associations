package org.associations.project.di

import com.russhwolf.settings.Settings
import org.associations.project.billing.DesktopPrintService
import org.associations.project.billing.PrintService
import org.associations.project.database.DatabaseDriverFactory
import org.associations.project.security.DesktopDeviceFingerprint
import org.associations.project.security.DeviceFingerprint
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::DatabaseDriverFactory)
    single { get<DatabaseDriverFactory>().createDriver() }
    singleOf(::DesktopPrintService) bind PrintService::class

    single<DeviceFingerprint> { DesktopDeviceFingerprint() }
    single<Settings> {
        com.russhwolf.settings.PreferencesSettings(
                java.util.prefs.Preferences.userRoot().node("org.associations.project")
        )
    }
}
