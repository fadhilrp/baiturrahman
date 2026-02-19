package com.example.baiturrahman

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.util.DebugLogger
import com.example.baiturrahman.data.remote.SupabaseClient
import com.example.baiturrahman.data.repository.AccountRepository
import com.example.baiturrahman.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaiturrahmanApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin DI
        startKoin {
            androidContext(this@BaiturrahmanApp)
            modules(appModule)
        }

        try {
            // Initialize Supabase client
            Log.d("BaiturrahmanApp", "Initializing Supabase...")
            SupabaseClient.client
            Log.d("BaiturrahmanApp", "Supabase initialized")

            // Validate session token on startup — clear if invalid
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val accountRepository = get<AccountRepository>()
                    val valid = accountRepository.validateAndClearIfInvalid()
                    Log.d("BaiturrahmanApp", "Session valid: $valid")
                } catch (e: Exception) {
                    Log.e("BaiturrahmanApp", "Session validation error", e)
                }
            }

        } catch (e: Exception) {
            Log.e("BaiturrahmanApp", "Initialization error", e)
        }
    }

    // Coil 3 image loader with Ktor network support
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .logger(DebugLogger())
            .build()
    }
}
