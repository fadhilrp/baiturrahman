package com.example.baiturrahman.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_images")
data class MosqueImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val imageUri: String = "",
    val displayOrder: Int = 0 // To maintain the order of images
) {
    // No-argument constructor required by Firestore
    constructor() : this(0, "", 0)
}
