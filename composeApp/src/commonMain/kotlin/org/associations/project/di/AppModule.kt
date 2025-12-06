package org.associations.project.di

import org.associations.project.database.AppDatabase
import org.associations.project.database.DatabaseDriverFactory
import org.associations.project.repository.AppRepository
import org.associations.project.viewmodel.AppViewModel
import org.associations.project.dashboard.DashboardViewModel
import org.associations.project.members.MembersViewModel
import org.associations.project.settings.SettingsViewModel
import org.associations.project.meter.MeterReadingViewModel
import org.associations.project.billing.InvoicesViewModel
import org.associations.project.treasury.TreasuryViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    includes(platformModule)
    single { AppDatabase(get()) }
    singleOf(::AppRepository)
    viewModelOf(::AppViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::MembersViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MeterReadingViewModel)
    viewModelOf(::InvoicesViewModel)
    viewModelOf(::TreasuryViewModel)
}


