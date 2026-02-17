package com.example.baiturrahman.di

import com.example.baiturrahman.data.local.AppDatabase
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.data.repository.ImageRepository
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import com.example.baiturrahman.utils.DevicePreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { AppDatabase.getDatabase(androidApplication()) }
    single { get<AppDatabase>().mosqueSettingsDao() }
    single { get<AppDatabase>().mosqueImageDao() }

    // Repositories
    single { PrayerTimeRepository() }
    single { SupabasePostgresRepository() }
    single { ImageRepository(androidApplication(), get()) }
    single { MosqueSettingsRepository(get(), get(), get(), get()) }

    // Sync Service
    single { SupabaseSyncService(get(), get(), get(), get()) }

    // ViewModels
    viewModel { MosqueDashboardViewModel(get(), get(), get(), get(), androidApplication(), get()) }

    // Preferences
    single { DevicePreferences(androidApplication()) }
}
