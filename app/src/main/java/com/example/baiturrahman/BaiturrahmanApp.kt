package com.example.baiturrahman

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.util.DebugLogger
import com.example.baiturrahman.data.remote.SupabaseClient
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.data.repository.ImageRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import com.example.baiturrahman.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaiturrahmanApp : Application(), SingletonImageLoader.Factory {
    // Make SupabaseSyncService accessible throughout the app
    lateinit var syncService: SupabaseSyncService
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            androidContext(this@BaiturrahmanApp)
            modules(appModule)
        }

        try {
            // Configure Coil with debug logging
            Log.d("BaiturrahmanApp", "Configuring Coil image loader...")

            // Initialize Supabase (this will trigger the init block)
            Log.d("BaiturrahmanApp", "Initializing Supabase...")
            SupabaseClient.client // This will trigger initialization
            Log.d("BaiturrahmanApp", "Supabase initialized successfully")

            // Run diagnostic checks in background
            Log.d("BaiturrahmanApp", "Starting diagnostic checks...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Diagnostic 1: Verify database setup
                    val postgresRepo = get<SupabasePostgresRepository>()
                    postgresRepo.verifyDatabaseSetup()

                    // Diagnostic 2: Verify bucket access
                    val imageRepo = get<ImageRepository>()
                    imageRepo.verifyBucketAccess()

                    Log.d("BaiturrahmanApp", "Diagnostic checks completed - review logs above")
                } catch (e: Exception) {
                    Log.e("BaiturrahmanApp", "Error during diagnostic checks", e)
                }
            }

            // Initialize and start SupabaseSyncService
            syncService = get<SupabaseSyncService>()
            syncService.startSync()
            Log.d("BaiturrahmanApp", "SupabaseSyncService started successfully")
        } catch (e: Exception) {
            Log.e("BaiturrahmanApp", "Error initializing Supabase or Sync Service", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::syncService.isInitialized) {
            syncService.stopSync()
        }
    }

    // Coil 3 SingletonImageLoader.Factory implementation
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .logger(DebugLogger())  // Enable Coil debug logging
            .build()
    }
}
