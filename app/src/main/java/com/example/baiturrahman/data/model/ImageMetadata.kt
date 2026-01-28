package com.example.baiturrahman.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for mosque images stored in Supabase PostgreSQL
 * Maps to the 'mosque_images' table
 */
@Serializable
data class ImageMetadata(
    @SerialName("id")
    val id: String = "",

    @SerialName("image_uri")
    val imageUri: String? = null,

    @SerialName("display_order")
    val displayOrder: Int = 0,

    @SerialName("upload_date")
    val uploadDate: String = "", // ISO 8601 timestamp

    @SerialName("file_size")
    val fileSize: Long = 0,

    @SerialName("mime_type")
    val mimeType: String = "image/jpeg",

    @SerialName("upload_status")
    val uploadStatus: String = "uploading", // uploading, completed, failed

    @SerialName("created_at")
    val createdAt: String = "", // ISO 8601 timestamp

    @SerialName("updated_at")
    val updatedAt: String = "" // ISO 8601 timestamp
)

/**
 * Enum for upload status
 */
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

/**
 * Request model for creating a new image record in PostgreSQL
 */
@Serializable
data class CreateImageRequest(
    @SerialName("id")
    val id: String,

    @SerialName("display_order")
    val displayOrder: Int,

    @SerialName("file_size")
    val fileSize: Long,

    @SerialName("mime_type")
    val mimeType: String,

    @SerialName("upload_status")
    val uploadStatus: String = "uploading"
)

/**
 * Request model for updating image with URL after upload
 */
@Serializable
data class UpdateImageUrlRequest(
    @SerialName("image_uri")
    val imageUri: String,

    @SerialName("upload_status")
    val uploadStatus: String
)
