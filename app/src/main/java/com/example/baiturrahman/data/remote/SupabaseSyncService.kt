package com.example.baiturrahman.data.remote

import android.util.Log
import androidx.room.withTransaction
import com.example.baiturrahman.data.local.AppDatabase
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service to sync data between Supabase PostgreSQL and local Room database
 * Uses polling mechanism to check for updates.
 *
 * Design: Master device = push only (never pulls from remote).
 *         Non-master devices = pull only.
 */
class SupabaseSyncService(
    private val database: AppDatabase,
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

    // Mutex to prevent sync from racing with user operations
    private val syncMutex = Mutex()

    /**
     * Execute a block while holding the sync lock.
     * Use this to wrap user mutations (upload, delete, save) so sync doesn't race.
     */
    suspend fun <T> withSyncLock(block: suspend () -> T): T = syncMutex.withLock { block() }

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
        Log.d(TAG, "Master device: ${devicePreferences.isMasterDevice}")

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
     * Poll PostgreSQL for image changes and sync to Room.
     * Master device skips pulling from remote (push only).
     * Uses atomic snapshot-replace: if remote differs from local,
     * replaces all local images in a single Room transaction.
     */
    private suspend fun syncImages() {
        Log.d(TAG, "Started images sync loop")
        val mosqueImageDao = database.mosqueImageDao()

        while (coroutineScope.isActive && isRunning) {
            // Master device = push only, skip pulling from remote
            if (devicePreferences.isMasterDevice) {
                delay(syncIntervalMs)
                continue
            }

            try {
                syncMutex.withLock {
                    // Read deviceName fresh each iteration
                    val deviceName = devicePreferences.deviceName

                    // Fetch images from PostgreSQL (only completed ones for this device)
                    val remoteImages = postgresRepository.getCompletedImages(deviceName)

                    // Filter out incomplete uploads (images with null URI)
                    val completedRemoteImages = remoteImages.filter { it.imageUri != null }

                    // Get local images from Room
                    val localImages = localRepository.mosqueImages.first()

                    // Compare URIs
                    val remoteUris = completedRemoteImages.mapNotNull { it.imageUri }.toSet()
                    val localUris = localImages.map { it.imageUri }.toSet()

                    if (remoteUris != localUris) {
                        Log.d(TAG, "🔄 Images changed — performing atomic snapshot-replace")
                        Log.d(TAG, "   Remote: ${completedRemoteImages.size}, Local: ${localImages.size}")

                        // Atomic snapshot-replace in a single Room transaction
                        database.withTransaction {
                            mosqueImageDao.deleteAllImages()
                            completedRemoteImages.forEach { remote ->
                                localRepository.addMosqueImageWithId(
                                    id = 0, // Let Room auto-generate
                                    imageUri = remote.imageUri!!,
                                    displayOrder = remote.displayOrder,
                                    supabaseId = remote.id
                                )
                            }
                        }

                        Log.d(TAG, "✅ Images synced atomically (${completedRemoteImages.size} images)")
                    } else {
                        Log.v(TAG, "No image changes detected (${remoteImages.size} remote, ${localImages.size} local)")
                    }
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
     * Poll PostgreSQL for settings changes and sync to Room.
     * Master device skips pulling from remote (push only).
     */
    private suspend fun syncSettings() {
        Log.d(TAG, "Started settings sync loop")

        while (coroutineScope.isActive && isRunning) {
            // Master device = push only, skip pulling from remote
            if (devicePreferences.isMasterDevice) {
                delay(syncIntervalMs)
                continue
            }

            try {
                syncMutex.withLock {
                    // Read deviceName fresh each iteration
                    val deviceName = devicePreferences.deviceName

                    // Fetch settings from PostgreSQL for this device
                    val remoteSettings = postgresRepository.getSettings(deviceName)

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
                            Log.d(TAG, "🔄 Settings changed for device $deviceName, updating local database")

                            localRepository.saveSettings(
                                deviceName = deviceName,
                                mosqueName = remoteSettings.mosqueName,
                                mosqueLocation = remoteSettings.mosqueLocation,
                                logoImage = remoteSettings.logoImage,
                                prayerAddress = remoteSettings.prayerAddress,
                                prayerTimezone = remoteSettings.prayerTimezone,
                                quoteText = remoteSettings.quoteText,
                                marqueeText = remoteSettings.marqueeText,
                                pushToRemote = false  // Prevent infinite loop - already from remote
                            )

                            Log.d(TAG, "✅ Settings synced successfully for device: $deviceName")
                        } else {
                            Log.v(TAG, "No settings changes detected for device: $deviceName")
                        }
                    } else {
                        Log.w(TAG, "⚠️ No settings found in PostgreSQL for device: ${devicePreferences.deviceName}")
                    }
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
     * Force immediate sync (for manual refresh).
     * Syncs both settings AND images atomically with the sync lock.
     */
    suspend fun forceSyncNow() {
        Log.d(TAG, "🔄 Force sync triggered")

        syncMutex.withLock {
            val deviceName = devicePreferences.deviceName
            Log.d(TAG, "Force sync for device: $deviceName")

            try {
                // Sync settings
                val remoteSettings = postgresRepository.getSettings(deviceName)
                if (remoteSettings != null) {
                    localRepository.saveSettings(
                        deviceName = deviceName,
                        mosqueName = remoteSettings.mosqueName,
                        mosqueLocation = remoteSettings.mosqueLocation,
                        logoImage = remoteSettings.logoImage,
                        prayerAddress = remoteSettings.prayerAddress,
                        prayerTimezone = remoteSettings.prayerTimezone,
                        quoteText = remoteSettings.quoteText,
                        marqueeText = remoteSettings.marqueeText,
                        pushToRemote = false
                    )
                    Log.d(TAG, "✅ Settings force-synced")
                }

                // Sync images — atomic snapshot-replace
                val remoteImages = postgresRepository.getCompletedImages(deviceName)
                val completedRemoteImages = remoteImages.filter { it.imageUri != null }
                val mosqueImageDao = database.mosqueImageDao()

                database.withTransaction {
                    mosqueImageDao.deleteAllImages()
                    completedRemoteImages.forEach { remote ->
                        localRepository.addMosqueImageWithId(
                            id = 0,
                            imageUri = remote.imageUri!!,
                            displayOrder = remote.displayOrder,
                            supabaseId = remote.id
                        )
                    }
                }

                Log.d(TAG, "✅ Images force-synced (${completedRemoteImages.size} images)")
                Log.d(TAG, "✅ Force sync completed for device: $deviceName")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during force sync", e)
            }
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
