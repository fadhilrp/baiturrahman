package com.example.baiturrahman.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.example.baiturrahman.BuildConfig
import retrofit2.converter.gson.GsonConverterFactory

// Create Retrofit instance
object RetrofitClient {
    private const val BASE_URL = "https://api.aladhan.com/"
    private val logger = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    val prayerTimeService: PrayerTimeService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerTimeService::class.java)
    }
}

