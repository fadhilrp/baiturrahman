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
    val quoteText: String = "\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel egestas dolor, nec dignissim metus.\"",
    val marqueeText: String = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text"
) {
    // No-argument constructor required by Firestore
    constructor() : this(
        id = 1,
        mosqueName = "Masjid Baiturrahman",
        mosqueLocation = "Pondok Pinang",
        logoImage = null,
        prayerAddress = "Lebak Bulus, Jakarta, ID",
        prayerTimezone = "Asia/Jakarta",
        quoteText = "\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel egestas dolor, nec dignissim metus.\"",
        marqueeText = "Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text"
    )
}
