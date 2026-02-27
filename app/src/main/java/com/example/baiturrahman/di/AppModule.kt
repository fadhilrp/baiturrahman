package com.example.baiturrahman.di

import com.example.baiturrahman.data.local.AppDatabase
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.data.repository.AccountRepository
import com.example.baiturrahman.data.repository.ImageRepository
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import com.example.baiturrahman.ui.viewmodel.AuthViewModel
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import com.example.baiturrahman.utils.AccountPreferences
import com.example.baiturrahman.utils.NetworkConnectivityObserver
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { AppDatabase.getDatabase(androidApplication()) }
    single { get<AppDatabase>().mosqueSettingsDao() }
    single { get<AppDatabase>().mosqueImageDao() }

    // Preferences
    single { AccountPreferences(androidApplication()) }

    // Network
    single { NetworkConnectivityObserver(androidApplication()) }

    // Repositories
    single { PrayerTimeRepository() }
    single { SupabasePostgresRepository() }
    single { ImageRepository(androidApplication(), get()) }
    single { MosqueSettingsRepository(get(), get(), get(), get()) }
    single { AccountRepository(get(), get()) }

    // Sync Service
    single { SupabaseSyncService(get(), get(), get(), get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get()) }
    viewModel { MosqueDashboardViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
