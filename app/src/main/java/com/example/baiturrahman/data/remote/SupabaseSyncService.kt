package com.example.baiturrahman.data.remote

import android.util.Log
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import com.example.baiturrahman.utils.DevicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Service to sync data between Supabase PostgreSQL and local Room database
 * Uses polling mechanism to check for updates
 */
class SupabaseSyncService(
    private val postgresRepository: SupabasePostgresRepository,
    private val localRepository: MosqueSettingsRepository,
    private val devicePreferences: DevicePreferences,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "SupabaseSyncService"

    // Polling jobs
    private var imagesSyncJob: Job? = null
    private var settingsSyncJob: Job? = null

    // Sync configuration
    private var syncIntervalMs: Long = 10_000L // Poll every 10 seconds
    private var isRunning = false

    /**
     * Start the sync service
     * Begins polling PostgreSQL for changes
     */
    fun startSync() {
        if (isRunning) {
            Log.w(TAG, "Sync already running")
            return
        }

        Log.d(TAG, "=== STARTING SUPABASE SYNC SERVICE ===")
        Log.d(TAG, "Sync interval: ${syncIntervalMs / 1000}s")
        Log.d(TAG, "Device name: ${devicePreferences.deviceName}")

        isRunning = true

        // Start polling for images
        imagesSyncJob = coroutineScope.launch {
            syncImages()
        }

        // Start polling for settings
        settingsSyncJob = coroutineScope.launch {
            syncSettings()
        }

        Log.d(TAG, "✅ Sync service started")
    }

    /**
     * Stop the sync service
     */
    fun stopSync() {
        Log.d(TAG, "Stopping sync service...")
        isRunning = false
        imagesSyncJob?.cancel()
        settingsSyncJob?.cancel()
        Log.d(TAG, "✅ Sync service stopped")
    }

    /**
     * Poll PostgreSQL for image changes and sync to Room
     */
    private suspend fun syncImages() {
        Log.d(TAG, "Started images sync loop")

        while (coroutineScope.isActive && isRunning) {
            try {
                // Fetch images from PostgreSQL (only completed ones)
                val remoteImages = postgresRepository.getCompletedImages()

                // Get local images from Room
                val localImages = localRepository.mosqueImages.first()

                // Filter out incomplete uploads (images with null URI)
                val completedRemoteImages = remoteImages.filter { it.imageUri != null }

                // Compare and sync
                val remoteUris = completedRemoteImages.mapNotNull { it.imageUri }.toSet()
                val localUris = localImages.map { it.imageUri }.toSet()

                // Find images to add (in remote but not in local)
                val imagesToAdd = completedRemoteImages.filter { it.imageUri !in localUris }

                // Find images to remove (in local but not in remote)
                val imagesToRemove = localImages.filter { it.imageUri !in remoteUris }

                if (imagesToAdd.isNotEmpty() || imagesToRemove.isNotEmpty()) {
                    Log.d(TAG, "🔄 Images changed - Add: ${imagesToAdd.size}, Remove: ${imagesToRemove.size}")
                    Log.d(TAG, "   Remote images: ${remoteImages.size} total, ${completedRemoteImages.size} completed")
                    Log.d(TAG, "   Local images: ${localImages.size}")

                    // Remove deleted images
                    for (image in imagesToRemove) {
                        val urlPreview = image.imageUri.let {
                            if (it.length > 60) it.take(40) + "..." + it.takeLast(15) else it
                        }
                        Log.d(TAG, "🗑️ Removing image: ${image.id}")
                        Log.d(TAG, "   URL: $urlPreview")
                        localRepository.removeMosqueImage(image.id)
                    }

                    // Add new images (all have non-null URIs due to filtering)
                    for (remoteImage in imagesToAdd) {
                        val url = remoteImage.imageUri!!
                        val urlPreview = if (url.length > 60) {
                            url.take(40) + "..." + url.takeLast(15)
                        } else {
                            url
                        }
                        Log.d(TAG, "➕ Adding image: order=${remoteImage.displayOrder}, id=${remoteImage.id}")
                        Log.d(TAG, "   URL: $urlPreview")
                        Log.d(TAG, "   Size: ${remoteImage.fileSize / 1024} KB")
                        localRepository.addMosqueImageWithId(
                            id = 0, // Let Room auto-generate
                            imageUri = url,
                            displayOrder = remoteImage.displayOrder,
                            supabaseId = remoteImage.id // Store PostgreSQL UUID for deletion
                        )
                    }

                    Log.d(TAG, "✅ Images synced successfully")

                    // Log final state
                    val finalLocalImages = localRepository.mosqueImages.first()
                    Log.d(TAG, "   Final local count: ${finalLocalImages.size}")
                } else {
                    Log.v(TAG, "No image changes detected (${remoteImages.size} remote, ${localImages.size} local)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing images", e)
            }

            // Wait before next poll
            delay(syncIntervalMs)
        }

        Log.d(TAG, "Images sync loop stopped")
    }

    /**
     * Poll PostgreSQL for settings changes and sync to Room
     */
    private suspend fun syncSettings() {
        Log.d(TAG, "Started settings sync loop")

        while (coroutineScope.isActive && isRunning) {
            try {
                // Fetch settings from PostgreSQL
                val remoteSettings = postgresRepository.getSettings()

                if (remoteSettings != null) {
                    // Get local settings
                    val localSettings = localRepository.mosqueSettings.first()

                    // Check if settings are different
                    val isDifferent = localSettings == null ||
                        localSettings.mosqueName != remoteSettings.mosqueName ||
                        localSettings.mosqueLocation != remoteSettings.mosqueLocation ||
                        localSettings.logoImage != remoteSettings.logoImage ||
                        localSettings.prayerAddress != remoteSettings.prayerAddress ||
                        localSettings.prayerTimezone != remoteSettings.prayerTimezone ||
                        localSettings.quoteText != remoteSettings.quoteText ||
                        localSettings.marqueeText != remoteSettings.marqueeText

                    if (isDifferent) {
                        Log.d(TAG, "🔄 Settings changed, updating local database")

                        localRepository.saveSettings(
                            mosqueName = remoteSettings.mosqueName,
                            mosqueLocation = remoteSettings.mosqueLocation,
                            logoImage = remoteSettings.logoImage,
                            prayerAddress = remoteSettings.prayerAddress,
                            prayerTimezone = remoteSettings.prayerTimezone,
                            quoteText = remoteSettings.quoteText,
                            marqueeText = remoteSettings.marqueeText,
                            pushToRemote = false  // Prevent infinite loop - already from remote
                        )

                        Log.d(TAG, "✅ Settings synced successfully")
                    } else {
                        Log.v(TAG, "No settings changes detected")
                    }
                } else {
                    Log.w(TAG, "⚠️ No settings found in PostgreSQL")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing settings", e)
            }

            // Wait before next poll
            delay(syncIntervalMs)
        }

        Log.d(TAG, "Settings sync loop stopped")
    }

    /**
     * Force immediate sync (for manual refresh)
     */
    suspend fun forceSyncNow() {
        Log.d(TAG, "🔄 Force sync triggered")
        try {
            // Sync images
            val remoteImages = postgresRepository.getCompletedImages()
            Log.d(TAG, "Fetched ${remoteImages.size} images from PostgreSQL")

            // Sync settings
            val remoteSettings = postgresRepository.getSettings()
            if (remoteSettings != null) {
                localRepository.saveSettings(
                    mosqueName = remoteSettings.mosqueName,
                    mosqueLocation = remoteSettings.mosqueLocation,
                    logoImage = remoteSettings.logoImage,
                    prayerAddress = remoteSettings.prayerAddress,
                    prayerTimezone = remoteSettings.prayerTimezone,
                    quoteText = remoteSettings.quoteText,
                    marqueeText = remoteSettings.marqueeText,
                    pushToRemote = false  // Prevent infinite loop - already from remote
                )
            }

            Log.d(TAG, "✅ Force sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during force sync", e)
        }
    }

    /**
     * Update sync interval
     * @param intervalMs Interval in milliseconds
     */
    fun setSyncInterval(intervalMs: Long) {
        Log.d(TAG, "Updating sync interval: ${intervalMs / 1000}s")
        syncIntervalMs = intervalMs

        // Restart sync with new interval
        if (isRunning) {
            stopSync()
            startSync()
        }
    }

    /**
     * Check if sync is currently running
     */
    fun isSyncRunning(): Boolean = isRunning
}
