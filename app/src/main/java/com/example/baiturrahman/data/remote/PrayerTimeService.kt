package com.example.baiturrahman.data.remote

import com.example.baiturrahman.data.model.PrayerResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit API Service
interface PrayerTimeService {
    @GET("v1/timingsByAddress/{date}") // Date in path
    suspend fun getPrayerTimes(
        @Path("date") date: String, // Path parameter
        @Query("address") address: String,
        @Query("method") method: Int = 20,
        @Query("shafaq") shafaq: String = "general",
        @Query("timezonestring") timezone: String = "Asia/Jakarta",
        @Query("calendarMethod") calendarMethod: String = "UAQ"
    ): PrayerResponse
}

