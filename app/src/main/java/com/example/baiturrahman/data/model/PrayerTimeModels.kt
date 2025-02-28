package com.example.baiturrahman.data.model

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
    val Fajr: String?,
    val Sunrise: String?,
    val Dhuhr: String?,
    val Asr: String?,
    val Sunset: String?,
    val Maghrib: String?,
    val Isha: String?,
    val Imsak: String?,
    val Midnight: String?
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

