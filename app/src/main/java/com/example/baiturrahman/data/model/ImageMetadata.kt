package com.example.baiturrahman.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for mosque images stored in Supabase PostgreSQL.
 * Maps to the 'mosque_images' table (account_id replaces device_name).
 */
@Serializable
data class ImageMetadata(
    @SerialName("id")
    val id: String = "",

    @SerialName("account_id")
    val accountId: String? = null,

    @SerialName("image_uri")
    val imageUri: String? = null,

    @SerialName("display_order")
    val displayOrder: Int = 0,

    @SerialName("upload_date")
    val uploadDate: String = "",

    @SerialName("file_size")
    val fileSize: Long = 0,

    @SerialName("mime_type")
    val mimeType: String = "image/jpeg",

    @SerialName("upload_status")
    val uploadStatus: String = "uploading",

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("updated_at")
    val updatedAt: String = ""
)

enum class UploadStatus(val value: String) {
    UPLOADING("uploading"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        fun fromString(value: String): UploadStatus {
            return values().find { it.value == value } ?: UPLOADING
        }
    }
}
