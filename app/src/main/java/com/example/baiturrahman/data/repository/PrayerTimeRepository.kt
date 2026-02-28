package com.example.baiturrahman.data.repository

import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.remote.RetrofitClient
import com.example.baiturrahman.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrayerTimeRepository {
    private val api = RetrofitClient.prayerTimeService

    suspend fun getPrayerTimes(
        address: String = "Lebak Bulus, Jakarta, ID",
        timezone: String = "Asia/Jakarta"
    ): Result<PrayerData> = withContext(Dispatchers.IO) {
        try {
            val today = DateTimeUtils.formatDateForApi()
            Result.success(api.getPrayerTimesByAddress(date = today, address = address, timezone = timezone).data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrayerTimesByCoords(
        latitude: Double,
        longitude: Double,
        timezone: String = "Asia/Jakarta"
    ): Result<PrayerData> = withContext(Dispatchers.IO) {
        try {
            val today = DateTimeUtils.formatDateForApi()
            Result.success(api.getPrayerTimesByCoords(date = today, latitude = latitude, longitude = longitude, timezone = timezone).data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

