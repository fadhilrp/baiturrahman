package com.example.baiturrahman.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_settings")
data class MosqueSettings(
    @PrimaryKey
    val id: Int = 1, // Single row for settings
    val mosqueName: String = "Masjid Baiturrahman",
    val mosqueLocation: String = "Pondok Pinang",
    val logoImage: String? = null,
    val prayerAddress: String = "Lebak Bulus, Jakarta, ID",
    val prayerTimezone: String = "Asia/Jakarta",
    val quoteText: String = "\"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar.\" (QS. Al-Ankabut: 45)",
    val marqueeText: String = "Lurus dan rapatkan shaf, mohon untuk mematikan alat komunikasi demi menjaga kesempurnaan sholat.",
    val iqomahDurationMinutes: Int = 10
)