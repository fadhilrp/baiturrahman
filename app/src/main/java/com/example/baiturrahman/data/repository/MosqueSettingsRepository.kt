package com.example.baiturrahman.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.baiturrahman.data.local.AppDatabase
import com.example.baiturrahman.data.local.dao.MosqueImageDao
import com.example.baiturrahman.data.local.dao.MosqueSettingsDao
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings
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

    val mosqueSettings: Flow<MosqueSettings?> = mosqueSettingsDao.getSettings()
    val mosqueImages: Flow<List<MosqueImage>> = mosqueImageDao.getAllImages()

    /**
     * Save settings to local Room AND push to Supabase via RPC.
     * @param sessionToken Auth token for the RPC call
     * @param pushToRemote Set to false when syncing from remote to avoid loops
     */
    suspend fun saveSettings(
        sessionToken: String,
        mosqueName: String,
        mosqueLocation: String,
        logoImage: String?,
        prayerAddress: String,
        prayerTimezone: String,
        quoteText: String,
        marqueeText: String,
        iqomahDurationMinutes: Int = 10,
        pushToRemote: Boolean = true
    ) {
        val settings = MosqueSettings(
            mosqueName = mosqueName,
            mosqueLocation = mosqueLocation,
            logoImage = logoImage,
            prayerAddress = prayerAddress,
            prayerTimezone = prayerTimezone,
            quoteText = quoteText,
            marqueeText = marqueeText,
            iqomahDurationMinutes = iqomahDurationMinutes
        )
        mosqueSettingsDao.insertSettings(settings)
        Log.d(TAG, "Settings saved to local DB")

        if (pushToRemote) {
            try {
                postgresRepository.upsertSettingsByToken(
                    token = sessionToken,
                    mosqueName = mosqueName,
                    mosqueLocation = mosqueLocation,
                    logoImage = logoImage,
                    prayerAddress = prayerAddress,
                    prayerTimezone = prayerTimezone,
                    quoteText = quoteText,
                    marqueeText = marqueeText,
                    iqomahDurationMinutes = iqomahDurationMinutes
                )
                Log.d(TAG, "Settings pushed to remote")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push settings to remote", e)
            }
        }
    }

    suspend fun addMosqueImage(imageUri: String, supabaseId: String? = null) {
        val currentCount = mosqueImageDao.getImageCount()
        if (currentCount < 5) {
            mosqueImageDao.insertImage(
                MosqueImage(imageUri = imageUri, displayOrder = currentCount, supabaseId = supabaseId)
            )
        }
    }

    suspend fun addMosqueImageWithId(
        id: Int,
        imageUri: String,
        displayOrder: Int,
        supabaseId: String? = null
    ) {
        mosqueImageDao.insertImage(
            MosqueImage(id = id, imageUri = imageUri, displayOrder = displayOrder, supabaseId = supabaseId)
        )
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

}
