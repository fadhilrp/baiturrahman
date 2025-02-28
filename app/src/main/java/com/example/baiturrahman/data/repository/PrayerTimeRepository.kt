package com.example.baiturrahman.data.repository

import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrayerTimeRepository {
    private val api = RetrofitClient.prayerTimeService

    suspend fun getPrayerTimes(address: String = "Lebak Bulus, Jakarta, ID"): Result<PrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                val response = api.getPrayerTimes(
                    date = today,
                    address = address
                )
                Result.success(response.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

