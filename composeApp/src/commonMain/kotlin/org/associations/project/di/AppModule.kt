package org.associations.project.di

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.associations.project.billing.InvoicesViewModel
import org.associations.project.dashboard.DashboardViewModel
import org.associations.project.database.AppDatabase
import org.associations.project.members.MembersViewModel
import org.associations.project.meter.MeterReadingViewModel
import org.associations.project.repository.AppRepository
import org.associations.project.repository.LicenseRepository
import org.associations.project.settings.SettingsViewModel
import org.associations.project.treasury.TreasuryViewModel
import org.associations.project.viewmodel.ActivationViewModel
import org.associations.project.viewmodel.AppViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        createSupabaseClient(
                supabaseUrl = "https://kcmeakdqbocpzyxwkfao.supabase.co",
                supabaseKey =
                        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtjbWVha2RxYm9jcHp5eHdrZmFvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU1NTg3NzcsImV4cCI6MjA4MTEzNDc3N30.VjPH-yimEeJj-8P53KjElPZIwUyp6FyBhuHG1FURANA"
        ) { install(Postgrest) }
    }
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

    singleOf(::LicenseRepository)
    viewModelOf(::ActivationViewModel)
}
