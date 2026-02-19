package com.example.baiturrahman.data.remote

import android.util.Log
import androidx.room.withTransaction
import com.example.baiturrahman.data.local.AppDatabase
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import com.example.baiturrahman.utils.AccountPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service to sync data between Supabase (via RPCs) and local Room database.
 * All devices push AND pull identically — no master/slave distinction.
 *
 * Poll cycle (every 10s):
 *  1. update_session_last_seen (heartbeat)
 *  2. get_settings_by_token → compare with local → update if changed
 *  3. get_images_by_token → compare URIs → atomic replace if changed
 *
 * Session detection:
 *  If the heartbeat RPC throws (session deleted by force-logout), clearSession() is called
 *  which causes AuthViewModel.isLoggedIn to become false → LoginScreen shown.
 */
class SupabaseSyncService(
    private val database: AppDatabase,
    private val postgresRepository: SupabasePostgresRepository,
    private val localRepository: MosqueSettingsRepository,
    private val accountPreferences: AccountPreferences,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "SupabaseSyncService"

    private var imagesSyncJob: Job? = null
    private var settingsSyncJob: Job? = null
    private var syncIntervalMs: Long = 10_000L
    private var isRunning = false

    private val syncMutex = Mutex()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    suspend fun <T> withSyncLock(block: suspend () -> T): T = syncMutex.withLock { block() }

    fun startSync() {
        if (isRunning) {
            Log.w(TAG, "Sync already running")
            return
        }
        Log.d(TAG, "=== STARTING SYNC SERVICE ===")
        isRunning = true

        imagesSyncJob = coroutineScope.launch { syncImages() }
        settingsSyncJob = coroutineScope.launch { syncSettings() }

        Log.d(TAG, "Sync service started")
    }

    fun stopSync() {
        Log.d(TAG, "Stopping sync service")
        isRunning = false
        imagesSyncJob?.cancel()
        settingsSyncJob?.cancel()
    }

    private suspend fun syncImages() {
        Log.d(TAG, "Images sync loop started")
        val mosqueImageDao = database.mosqueImageDao()

        while (coroutineScope.isActive && isRunning) {
            val token = accountPreferences.sessionToken
            if (token == null) {
                delay(syncIntervalMs)
                continue
            }

            try {
                syncMutex.withLock {
                    // Heartbeat — also detects force-logout
                    try {
                        postgresRepository.updateSessionLastSeen(token)
                        _isOffline.value = false
                    } catch (e: Exception) {
                        Log.w(TAG, "Heartbeat failed: ${e.message}")
                        if (isSessionInvalidError(e)) {
                            Log.w(TAG, "Session invalid — clearing prefs")
                            accountPreferences.clearSession()
                            stopSync()
                            return@withLock
                        }
                        _isOffline.value = true
                        return@withLock
                    }

                    val remoteImages = postgresRepository.getImagesByToken(token)
                    val completedRemoteImages = remoteImages.filter { it.imageUri != null }
                    val localImages = localRepository.mosqueImages.first()

                    val remoteUris = completedRemoteImages.mapNotNull { it.imageUri }.toSet()
                    val localUris = localImages.map { it.imageUri }.toSet()

                    if (remoteUris != localUris) {
                        Log.d(TAG, "Images changed — atomic snapshot-replace (remote=${completedRemoteImages.size}, local=${localImages.size})")
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
                        Log.d(TAG, "Images synced: ${completedRemoteImages.size}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Images sync error", e)
                _isOffline.value = true
            }

            delay(syncIntervalMs)
        }
        Log.d(TAG, "Images sync loop stopped")
    }

    private suspend fun syncSettings() {
        Log.d(TAG, "Settings sync loop started")

        while (coroutineScope.isActive && isRunning) {
            val token = accountPreferences.sessionToken
            if (token == null) {
                delay(syncIntervalMs)
                continue
            }

            try {
                syncMutex.withLock {
                    val remoteSettings = postgresRepository.getSettingsByToken(token)

                    if (remoteSettings != null) {
                        val localSettings = localRepository.mosqueSettings.first()

                        val isDifferent = localSettings == null ||
                            localSettings.mosqueName != remoteSettings.mosqueName ||
                            localSettings.mosqueLocation != remoteSettings.mosqueLocation ||
                            localSettings.logoImage != remoteSettings.logoImage ||
                            localSettings.prayerAddress != remoteSettings.prayerAddress ||
                            localSettings.prayerTimezone != remoteSettings.prayerTimezone ||
                            localSettings.quoteText != remoteSettings.quoteText ||
                            localSettings.marqueeText != remoteSettings.marqueeText

                        if (isDifferent) {
                            Log.d(TAG, "Settings changed — updating local DB")
                            localRepository.saveSettings(
                                sessionToken = token,
                                mosqueName = remoteSettings.mosqueName,
                                mosqueLocation = remoteSettings.mosqueLocation,
                                logoImage = remoteSettings.logoImage,
                                prayerAddress = remoteSettings.prayerAddress,
                                prayerTimezone = remoteSettings.prayerTimezone,
                                quoteText = remoteSettings.quoteText,
                                marqueeText = remoteSettings.marqueeText,
                                pushToRemote = false
                            )
                            Log.d(TAG, "Settings synced")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Settings sync error", e)
            }

            delay(syncIntervalMs)
        }
        Log.d(TAG, "Settings sync loop stopped")
    }

    suspend fun forceSyncNow() {
        val token = accountPreferences.sessionToken ?: return
        Log.d(TAG, "Force sync triggered")

        syncMutex.withLock {
            try {
                val remoteSettings = postgresRepository.getSettingsByToken(token)
                if (remoteSettings != null) {
                    localRepository.saveSettings(
                        sessionToken = token,
                        mosqueName = remoteSettings.mosqueName,
                        mosqueLocation = remoteSettings.mosqueLocation,
                        logoImage = remoteSettings.logoImage,
                        prayerAddress = remoteSettings.prayerAddress,
                        prayerTimezone = remoteSettings.prayerTimezone,
                        quoteText = remoteSettings.quoteText,
                        marqueeText = remoteSettings.marqueeText,
                        pushToRemote = false
                    )
                    Log.d(TAG, "Settings force-synced")
                }

                val remoteImages = postgresRepository.getImagesByToken(token)
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
                Log.d(TAG, "Images force-synced: ${completedRemoteImages.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Force sync error", e)
            }
        }
    }

    fun isSyncRunning(): Boolean = isRunning

    private fun isSessionInvalidError(e: Exception): Boolean {
        val msg = e.message ?: return false
        return msg.contains("INVALID_SESSION") ||
            msg.contains("401") ||
            msg.contains("JWT expired") ||
            msg.contains("session_token")
    }
}
