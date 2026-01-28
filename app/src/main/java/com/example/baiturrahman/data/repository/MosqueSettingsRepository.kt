package com.example.baiturrahman.data.repository

import android.util.Log
import com.example.baiturrahman.data.local.dao.MosqueImageDao
import com.example.baiturrahman.data.local.dao.MosqueSettingsDao
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings
import com.example.baiturrahman.data.model.UpdateMosqueSettingsRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MosqueSettingsRepository(
    private val mosqueSettingsDao: MosqueSettingsDao,
    private val mosqueImageDao: MosqueImageDao,
    private val postgresRepository: SupabasePostgresRepository
) {
    companion object {
        private const val TAG = "MosqueSettingsRepo"
    }

    // Settings
    val mosqueSettings: Flow<MosqueSettings?> = mosqueSettingsDao.getSettings()

    /**
     * Save settings to local Room database AND push to Supabase PostgreSQL
     * @param pushToRemote If true, also pushes changes to PostgreSQL. Set to false when syncing from remote to avoid loops.
     */
    suspend fun saveSettings(
        mosqueName: String,
        mosqueLocation: String,
        logoImage: String?,
        prayerAddress: String,
        prayerTimezone: String,
        quoteText: String,
        marqueeText: String,
        pushToRemote: Boolean = true
    ) {
        // Save to local Room database
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
        Log.d(TAG, "Settings saved to local database")

        // Push to Supabase PostgreSQL (only if pushToRemote is true)
        if (pushToRemote) {
            try {
                val request = UpdateMosqueSettingsRequest(
                    mosqueName = mosqueName,
                    mosqueLocation = mosqueLocation,
                    logoImage = logoImage,
                    prayerAddress = prayerAddress,
                    prayerTimezone = prayerTimezone,
                    quoteText = quoteText,
                    marqueeText = marqueeText
                )

                val success = postgresRepository.updateSettings(request)
                if (success) {
                    Log.d(TAG, "✅ Settings pushed to PostgreSQL")
                } else {
                    Log.w(TAG, "⚠️ Failed to push settings to PostgreSQL")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error pushing settings to PostgreSQL", e)
                // Don't fail the operation - local save succeeded
            }
        } else {
            Log.d(TAG, "Skipping remote push (syncing from remote)")
        }
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

    suspend fun addMosqueImageWithId(
        id: Int,
        imageUri: String,
        displayOrder: Int,
        supabaseId: String? = null
    ) {
        val image = MosqueImage(
            id = id,
            imageUri = imageUri,
            displayOrder = displayOrder,
            supabaseId = supabaseId
        )
        mosqueImageDao.insertImage(image)
    }

    suspend fun clearAllImages() {
        mosqueImageDao.deleteAllImages()
    }
}
