package com.example.baiturrahman.data.model

import com.google.gson.annotations.SerializedName

// API Models
data class PrayerResponse(
    val code: Int,
    val status: String,
    val data: PrayerData
)

data class PrayerData(
    val timings: PrayerTimings,
    val date: DateInfo
)

data class PrayerTimings(
    @SerializedName("Fajr")     val fajr: String?,
    @SerializedName("Sunrise")  val sunrise: String?,
    @SerializedName("Dhuhr")    val dhuhr: String?,
    @SerializedName("Asr")      val asr: String?,
    @SerializedName("Sunset")   val sunset: String?,
    @SerializedName("Maghrib")  val maghrib: String?,
    @SerializedName("Isha")     val isha: String?,
    @SerializedName("Imsak")    val imsak: String?,
    @SerializedName("Midnight") val midnight: String?
)

data class DateInfo(
    val readable: String,
    val hijri: HijriDate
)

data class HijriDate(
    val date: String,
    val day: String,
    val month: MonthInfo,
    val year: String
)

data class MonthInfo(
    val number: Int,
    val en: String
)

