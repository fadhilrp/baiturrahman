package com.example.baiturrahman.data.remote

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    // 🚨 REPLACE THESE WITH YOUR ACTUAL SUPABASE CREDENTIALS 🚨
    // Get these from your Supabase Dashboard > Settings > API
    private const val SUPABASE_URL = "https://zdcoximugrvbajyqoslj.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkY294aW11Z3J2YmFqeXFvc2xqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQ0MDU4NTcsImV4cCI6MjA2OTk4MTg1N30.niF1n6ZwqWgJyLQEBD0Q1yEwq2jJTznChZnNphoGKV0"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }

    init {
        Log.d(TAG, "🔧 Supabase client initialized")
        Log.d(TAG, "📍 URL: $SUPABASE_URL")
        Log.d(TAG, "🔑 Key: ${SUPABASE_ANON_KEY.take(20)}...${SUPABASE_ANON_KEY.takeLast(4)}")

        // Validate credentials format
        if (SUPABASE_URL.contains("your-project-ref") || SUPABASE_URL.contains("your-actual-project-id") ||
            SUPABASE_ANON_KEY.contains("your-anon-key") || SUPABASE_ANON_KEY.contains("your-actual-anon-key")) {
            Log.e(TAG, "❌ PLACEHOLDER CREDENTIALS DETECTED! Please update with real Supabase credentials")
        } else {
            Log.d(TAG, "✅ Credentials format looks valid")
        }

        // Test that storage is accessible
        try {
            val storage = client.storage
            Log.d(TAG, "✅ Storage client accessible: $storage")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Storage client not accessible", e)
        }
    }
}
