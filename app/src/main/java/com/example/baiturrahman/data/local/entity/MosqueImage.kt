package com.example.baiturrahman.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_images")
data class MosqueImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val imageUri: String = "", // Supabase Storage public URL

    val displayOrder: Int = 0, // Order in slider

    val uploadDate: Long = System.currentTimeMillis(), // Upload timestamp

    val fileSize: Long = 0, // Size in bytes

    val mimeType: String = "image/jpeg", // MIME type

    val uploadStatus: String = "completed", // uploading, completed, failed

    val supabaseId: String? = null // UUID from PostgreSQL (optional)
)
