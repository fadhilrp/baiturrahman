package com.example.baiturrahman.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_settings")
data class MosqueSettings(
    @PrimaryKey
    val id: Int = 1, // Single row for settings
    val mosqueName: String,
    val mosqueLocation: String,
    val logoImage: String?,
    val prayerAddress: String,
    val prayerTimezone: String,
    val quoteText: String,
    val marqueeText: String
)

