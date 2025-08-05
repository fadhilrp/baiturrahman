package com.example.baiturrahman

import android.app.Application
import android.util.Log
import com.example.baiturrahman.data.remote.FirestoreSync
import com.example.baiturrahman.data.remote.SupabaseClient
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.di.appModule
import com.example.baiturrahman.utils.DevicePreferences
import com.google.firebase.FirebaseApp
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BaiturrahmanApp : Application() {
    // Make FirestoreSync accessible throughout the app
    lateinit var firestoreSync: FirestoreSync
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection
        startKoin {
            androidContext(this@BaiturrahmanApp)
            modules(appModule)
        }

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Initialize Supabase (this will trigger the init block)
            Log.d("BaiturrahmanApp", "Initializing Supabase...")
            SupabaseClient.client // This will trigger initialization
            Log.d("BaiturrahmanApp", "Supabase initialized successfully")

            // Initialize DevicePreferences
            val devicePreferences = DevicePreferences(this)

            // Initialize and start Firestore sync
            val repository = get<MosqueSettingsRepository>()
            firestoreSync = FirestoreSync(repository, devicePreferences)
            firestoreSync.startSync()
        } catch (e: Exception) {
            Log.e("BaiturrahmanApp", "Error initializing Firebase, Supabase, or Firestore", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::firestoreSync.isInitialized) {
            firestoreSync.stopSync()
        }
    }
}
