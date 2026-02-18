package com.example.baiturrahman.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for mosque settings stored in Supabase PostgreSQL.
 * Maps to the 'mosque_settings' table (account_id replaces device_name).
 */
@Serializable
data class MosqueSettingsRemote(
    @SerialName("id")
    val id: Int = 0,

    @SerialName("account_id")
    val accountId: String? = null,

    @SerialName("mosque_name")
    val mosqueName: String = "Masjid Baiturrahman",

    @SerialName("mosque_location")
    val mosqueLocation: String = "Pondok Pinang",

    @SerialName("logo_image")
    val logoImage: String? = null,

    @SerialName("prayer_address")
    val prayerAddress: String = "Lebak Bulus, Jakarta, ID",

    @SerialName("prayer_timezone")
    val prayerTimezone: String = "Asia/Jakarta",

    @SerialName("quote_text")
    val quoteText: String = "Lorem ipsum dolor sit amet",

    @SerialName("marquee_text")
    val marqueeText: String = "Rolling Text",

    @SerialName("updated_at")
    val updatedAt: String = ""
)
