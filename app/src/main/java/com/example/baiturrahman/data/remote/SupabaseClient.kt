package com.example.baiturrahman.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseClient {
    // Replace with your Supabase URL and anon key
    private const val SUPABASE_URL = "https://zdcoximugrvbajyqoslj.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkY294aW11Z3J2YmFqeXFvc2xqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQ0MDU4NTcsImV4cCI6MjA2OTk4MTg1N30.niF1n6ZwqWgJyLQEBD0Q1yEwq2jJTznChZnNphoGKV0"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Storage)
    }

    val storage = client.storage
}
