package org.associations.project.di

import org.associations.project.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

import org.associations.project.billing.DesktopPrintService
import org.associations.project.billing.PrintService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

actual val platformModule: Module = module {
    single { DatabaseDriverFactory().createDriver() }
    singleOf(::DesktopPrintService) bind PrintService::class
}
