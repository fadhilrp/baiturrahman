package com.example.baiturrahman.data.remote

import com.example.baiturrahman.data.model.PrayerResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit API Service
interface PrayerTimeService {

    /** Address-based lookup — Aladhan passes this to Google Geocoding internally. */
    @GET("v1/timingsByAddress/{date}")
    suspend fun getPrayerTimesByAddress(
        @Path("date") date: String,
        @Query("address") address: String,
        @Query("method") method: Int = 20,
        @Query("timezonestring") timezone: String,
        @Query("calendarMethod") calendarMethod: String = "UAQ",
        // +2 min ihtiyat on every prayer time (Kemenag standard precautionary buffer).
        // Order: Imsak,Fajr,Sunrise,Dhuhr,Asr,Maghrib,Sunset,Isha,Midnight
        @Query("tune") tune: String = "2,2,0,2,2,2,0,2,0"
    ): PrayerResponse

    /** Coordinate-based lookup — skips Aladhan's internal geocoding, always accurate. */
    @GET("v1/timings/{date}")
    suspend fun getPrayerTimesByCoords(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 20,
        @Query("timezonestring") timezone: String,
        @Query("calendarMethod") calendarMethod: String = "UAQ",
        @Query("tune") tune: String = "2,2,0,2,2,2,0,2,0"
    ): PrayerResponse
}

