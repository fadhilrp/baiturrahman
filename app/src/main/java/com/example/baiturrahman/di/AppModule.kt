package com.example.baiturrahman.di

import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.ui.viewmodel.MosqueDashboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Single instances
    single { PrayerTimeRepository() }

    // ViewModels
    viewModel { MosqueDashboardViewModel(get()) }
}

