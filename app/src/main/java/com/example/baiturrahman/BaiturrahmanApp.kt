package com.example.baiturrahman

import android.app.Application
import com.example.baiturrahman.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaiturrahmanApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            androidContext(this@BaiturrahmanApp)
            modules(appModule)
        }
    }
}