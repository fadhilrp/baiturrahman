package com.example.baiturrahman.data.repository

import com.example.baiturrahman.data.local.dao.MosqueImageDao
import com.example.baiturrahman.data.local.dao.MosqueSettingsDao
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MosqueSettingsRepository(
    private val mosqueSettingsDao: MosqueSettingsDao,
    private val mosqueImageDao: MosqueImageDao
) {
    // Settings
    val mosqueSettings: Flow<MosqueSettings?> = mosqueSettingsDao.getSettings()

    suspend fun saveSettings(
        mosqueName: String,
        mosqueLocation: String,
        logoImage: String?,
        prayerAddress: String,
        prayerTimezone: String,
        quoteText: String,
        marqueeText: String
    ) {
        val settings = MosqueSettings(
            mosqueName = mosqueName,
            mosqueLocation = mosqueLocation,
            logoImage = logoImage,
            prayerAddress = prayerAddress,
            prayerTimezone = prayerTimezone,
            quoteText = quoteText,
            marqueeText = marqueeText
        )
        mosqueSettingsDao.insertSettings(settings)
    }

    // Images
    val mosqueImages: Flow<List<MosqueImage>> = mosqueImageDao.getAllImages()

    suspend fun addMosqueImage(imageUri: String) {
        val currentCount = mosqueImageDao.getImageCount()
        if (currentCount < 5) { // Maximum 5 images
            val image = MosqueImage(
                imageUri = imageUri,
                displayOrder = currentCount
            )
            mosqueImageDao.insertImage(image)
        }
    }

    suspend fun removeMosqueImage(imageId: Int) {
        mosqueImageDao.deleteImage(imageId)
        // Reorder remaining images
        val images = mosqueImageDao.getAllImages().firstOrNull() ?: return
        images.forEachIndexed { index, image ->
            mosqueImageDao.updateImageOrder(image.id, index)
        }
    }

    suspend fun clearAllData() {
        mosqueSettingsDao.deleteAllSettings()
        mosqueImageDao.deleteAllImages()
    }
}

