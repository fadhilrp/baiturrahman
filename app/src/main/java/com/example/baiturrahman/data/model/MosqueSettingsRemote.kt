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
    val quoteText: String = "\"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar.\" (QS. Al-Ankabut: 45)",

    @SerialName("marquee_text")
    val marqueeText: String = "Lurus dan rapatkan shaf, mohon untuk mematikan alat komunikasi demi menjaga kesempurnaan sholat.",

    @SerialName("iqomah_duration_minutes")
    val iqomahDurationMinutes: Int = 10,

    @SerialName("iqomah_subuh_minutes")
    val iqomahSubuhMinutes: Int = 10,

    @SerialName("iqomah_dzuhur_minutes")
    val iqomahDzuhurMinutes: Int = 10,

    @SerialName("iqomah_ashar_minutes")
    val iqomahAsharMinutes: Int = 10,

    @SerialName("iqomah_maghrib_minutes")
    val iqomahMaghribMinutes: Int = 10,

    @SerialName("iqomah_isya_minutes")
    val iqomahIsyaMinutes: Int = 10,

    @SerialName("adzan_offset_minutes")
    val adzanOffsetMinutes: Int = 0,

    @SerialName("is_dark_mode")
    val isDarkMode: Boolean = true,

    @SerialName("updated_at")
    val updatedAt: String = ""
)
