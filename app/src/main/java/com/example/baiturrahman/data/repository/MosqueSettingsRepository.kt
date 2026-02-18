package com.example.baiturrahman.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.baiturrahman.data.local.AppDatabase
import com.example.baiturrahman.data.local.dao.MosqueImageDao
import com.example.baiturrahman.data.local.dao.MosqueSettingsDao
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings
import com.example.baiturrahman.data.model.UpdateMosqueSettingsRequest
import kotlinx.coroutines.flow.Flow

class MosqueSettingsRepository(
    private val database: AppDatabase,
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
     * @param deviceName Device name for device-specific settings
     * @param pushToRemote If true, also pushes changes to PostgreSQL. Set to false when syncing from remote to avoid loops.
     */
    suspend fun saveSettings(
        deviceName: String,
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
        Log.d(TAG, "Settings saved to local database for device: $deviceName")

        // Push to Supabase PostgreSQL (only if pushToRemote is true)
        if (pushToRemote) {
            try {
                val request = UpdateMosqueSettingsRequest(
                    deviceName = deviceName,
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
                    Log.d(TAG, "✅ Settings pushed to PostgreSQL for device: $deviceName")
                } else {
                    Log.w(TAG, "⚠️ Failed to push settings to PostgreSQL for device: $deviceName")
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

    suspend fun addMosqueImage(imageUri: String, supabaseId: String? = null) {
        val currentCount = mosqueImageDao.getImageCount()
        if (currentCount < 5) { // Maximum 5 images
            val image = MosqueImage(
                imageUri = imageUri,
                displayOrder = currentCount,
                supabaseId = supabaseId
            )
            mosqueImageDao.insertImage(image)
        }
    }

    /**
     * Fetch all device names from remote Supabase.
     */
    suspend fun getAllDeviceNames(): List<String> {
        return postgresRepository.getAllDeviceNames()
    }

    /**
     * Rename device atomically in remote PostgreSQL (both tables).
     */
    suspend fun renameDevice(oldName: String, newName: String): Boolean {
        return postgresRepository.renameDevice(oldName, newName)
    }

    suspend fun removeMosqueImage(imageId: Int) {
        database.withTransaction {
            mosqueImageDao.deleteImage(imageId)
            val images = mosqueImageDao.getAllImagesSnapshot()
            images.forEachIndexed { index, image ->
                mosqueImageDao.updateImageOrder(image.id, index)
            }
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            mosqueSettingsDao.deleteAllSettings()
            mosqueImageDao.deleteAllImages()
        }
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
