package com.example.baiturrahman.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for mosque settings stored in Supabase PostgreSQL
 * Maps to the 'mosque_settings' table
 */
@Serializable
data class MosqueSettingsRemote(
    @SerialName("id")
    val id: Int = 1, // Always 1, single row table

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
    val updatedAt: String = "" // ISO 8601 timestamp
)

/**
 * Request model for updating mosque settings
 */
@Serializable
data class UpdateMosqueSettingsRequest(
    @SerialName("mosque_name")
    val mosqueName: String,

    @SerialName("mosque_location")
    val mosqueLocation: String,

    @SerialName("logo_image")
    val logoImage: String? = null,

    @SerialName("prayer_address")
    val prayerAddress: String,

    @SerialName("prayer_timezone")
    val prayerTimezone: String,

    @SerialName("quote_text")
    val quoteText: String,

    @SerialName("marquee_text")
    val marqueeText: String
)
