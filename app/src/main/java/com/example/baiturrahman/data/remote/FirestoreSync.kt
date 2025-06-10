package com.example.baiturrahman.data.remote

import android.util.Log
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.utils.DevicePreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class FirestoreSync(
    private val repository: MosqueSettingsRepository,
    private val devicePreferences: DevicePreferences,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "FirestoreSync"
    private var settingsListener: ListenerRegistration? = null
    private var imagesListener: ListenerRegistration? = null
    private var localSettingsListener: kotlinx.coroutines.Job? = null
    private var localImagesListener: kotlinx.coroutines.Job? = null

    // Flag to prevent infinite sync loops
    private var isLocalUpdate = false

    // Collection names in Firestore
    private val SETTINGS_COLLECTION = "mosque_settings"
    private val IMAGES_COLLECTION = "mosque_images"
    private val SYNC_LOG_COLLECTION = "sync_logs"

    // Document ID for settings (since we only have one settings object)
    private val SETTINGS_DOCUMENT_ID = "main_settings"

    fun startSync() {
        Log.d(TAG, "=== STARTING SYNC ===")
        Log.d(TAG, "Sync enabled: ${devicePreferences.syncEnabled}")
        Log.d(TAG, "Is master device: ${devicePreferences.isMasterDevice}")
        Log.d(TAG, "Device name: ${devicePreferences.deviceName}")

        if (!devicePreferences.syncEnabled) {
            Log.d(TAG, "Sync is disabled for this device")
            return
        }

        // Always listen for remote changes (pulling)
        listenForRemoteChanges()
        Log.d(TAG, "Started listening for remote changes (PULLING)")

        // Only master devices can push changes to Firestore
        if (devicePreferences.isMasterDevice) {
            Log.d(TAG, "This is a master device - enabling PUSHING")

            // Initial upload of local data to Firestore
            uploadLocalDataToFirestore()

            // Listen for local changes and sync to Firestore
            listenForLocalChanges()

            // Log that this device is now the master
            logSyncEvent("Device ${devicePreferences.deviceName} set as master")
        } else {
            Log.d(TAG, "This is NOT a master device - PUSHING disabled")
        }
    }

    fun stopSync() {
        settingsListener?.remove()
        imagesListener?.remove()
        localSettingsListener?.cancel()
        localImagesListener?.cancel()
        Log.d(TAG, "Firestore sync stopped")
    }

    private fun uploadLocalDataToFirestore() {
        Log.d(TAG, "=== UPLOADING LOCAL DATA TO FIRESTORE ===")
        if (!devicePreferences.isMasterDevice) {
            Log.d(TAG, "Not a master device, skipping upload")
            return
        }

        coroutineScope.launch {
            try {
                // Upload settings
                Log.d(TAG, "Getting local settings...")
                val settings = repository.mosqueSettings.first()
                Log.d(TAG, "Local settings: $settings")

                if (settings != null) {
                    Log.d(TAG, "Uploading settings to Firestore...")
                    isLocalUpdate = true
                    firestore.collection(SETTINGS_COLLECTION)
                        .document(SETTINGS_DOCUMENT_ID)
                        .set(settings)
                        .await()
                    Log.d(TAG, "✅ Settings uploaded to Firestore successfully")

                    // Log the sync event
                    logSyncEvent("Settings uploaded by ${devicePreferences.deviceName}")
                } else {
                    Log.d(TAG, "No local settings found to upload")
                }

                // Upload images
                Log.d(TAG, "Getting local images...")
                val images = repository.mosqueImages.first()
                Log.d(TAG, "Local images count: ${images.size}")

                if (images.isNotEmpty()) {
                    Log.d(TAG, "Clearing existing images in Firestore...")
                    // First clear existing images to avoid duplicates
                    val batch = firestore.batch()
                    val existingImages = firestore.collection(IMAGES_COLLECTION).get().await()
                    for (doc in existingImages.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()

                    Log.d(TAG, "Uploading ${images.size} images to Firestore...")
                    // Upload current images
                    for (image in images) {
                        firestore.collection(IMAGES_COLLECTION)
                            .document(image.id.toString())
                            .set(image)
                            .await()
                        Log.d(TAG, "Uploaded image: ${image.id}")
                    }
                    Log.d(TAG, "✅ ${images.size} images uploaded to Firestore successfully")

                    // Log the sync event
                    logSyncEvent("${images.size} images uploaded by ${devicePreferences.deviceName}")
                } else {
                    Log.d(TAG, "No local images found to upload")
                }
                isLocalUpdate = false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error uploading local data to Firestore", e)
                isLocalUpdate = false

                // Log the error
                logSyncEvent("Error uploading data: ${e.message}", isError = true)
            }
        }
    }

    private fun listenForRemoteChanges() {
        Log.d(TAG, "=== SETTING UP REMOTE LISTENERS (PULLING) ===")
        try {
            // Listen for settings changes
            settingsListener = firestore.collection(SETTINGS_COLLECTION)
                .document(SETTINGS_DOCUMENT_ID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening for settings changes", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists() && !isLocalUpdate) {
                        Log.d(TAG, "📥 Received settings update from Firestore")
                        try {
                            val remoteSettings = snapshot.toObject(MosqueSettings::class.java)
                            if (remoteSettings != null) {
                                Log.d(TAG, "Remote settings: $remoteSettings")
                                coroutineScope.launch {
                                    try {
                                        repository.saveSettings(
                                            mosqueName = remoteSettings.mosqueName,
                                            mosqueLocation = remoteSettings.mosqueLocation,
                                            logoImage = remoteSettings.logoImage,
                                            prayerAddress = remoteSettings.prayerAddress,
                                            prayerTimezone = remoteSettings.prayerTimezone,
                                            quoteText = remoteSettings.quoteText,
                                            marqueeText = remoteSettings.marqueeText
                                        )
                                        Log.d(TAG, "✅ Settings updated from Firestore")

                                        // Only log if not the master device (to avoid duplicate logs)
                                        if (!devicePreferences.isMasterDevice) {
                                            logSyncEvent("Settings received by ${devicePreferences.deviceName}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error updating local settings", e)
                                        logSyncEvent("Error updating settings: ${e.message}", isError = true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error deserializing settings", e)
                        }
                    } else {
                        Log.d(TAG, "Ignoring settings update (local update or no data)")
                    }
                }

            // Listen for images changes
            imagesListener = firestore.collection(IMAGES_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening for images changes", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !isLocalUpdate) {
                        Log.d(TAG, "📥 Received images update from Firestore (${snapshot.size()} images)")
                        coroutineScope.launch {
                            try {
                                // Clear existing images
                                repository.clearAllImages()

                                // Add new images from Firestore
                                for (doc in snapshot.documents) {
                                    try {
                                        val remoteImage = doc.toObject(MosqueImage::class.java)
                                        if (remoteImage != null) {
                                            repository.addMosqueImageWithId(
                                                id = remoteImage.id,
                                                imageUri = remoteImage.imageUri,
                                                displayOrder = remoteImage.displayOrder
                                            )
                                            Log.d(TAG, "Added image: ${remoteImage.id}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Error deserializing image: ${doc.id}", e)
                                    }
                                }
                                Log.d(TAG, "✅ Images updated from Firestore: ${snapshot.size()} images")

                                // Only log if not the master device (to avoid duplicate logs)
                                if (!devicePreferences.isMasterDevice) {
                                    logSyncEvent("${snapshot.size()} images received by ${devicePreferences.deviceName}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error updating local images", e)
                                logSyncEvent("Error updating images: ${e.message}", isError = true)
                            }
                        }
                    } else {
                        Log.d(TAG, "Ignoring images update (local update)")
                    }
                }

            Log.d(TAG, "✅ Remote listeners set up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up Firestore listeners", e)
        }
    }

    private fun listenForLocalChanges() {
        Log.d(TAG, "=== SETTING UP LOCAL LISTENERS (PUSHING) ===")
        if (!devicePreferences.isMasterDevice) {
            Log.d(TAG, "Not a master device, skipping local listeners")
            return
        }

        // Listen for settings changes
        localSettingsListener = coroutineScope.launch {
            Log.d(TAG, "Starting to listen for local settings changes...")
            repository.mosqueSettings.collect { settings ->
                Log.d(TAG, "🔄 Local settings changed: $settings")
                Log.d(TAG, "isLocalUpdate flag: $isLocalUpdate")

                if (!isLocalUpdate && settings != null) {
                    try {
                        Log.d(TAG, "📤 Pushing settings to Firestore...")
                        isLocalUpdate = true
                        firestore.collection(SETTINGS_COLLECTION)
                            .document(SETTINGS_DOCUMENT_ID)
                            .set(settings)
                            .await()
                        Log.d(TAG, "✅ Settings synced to Firestore successfully")
                        logSyncEvent("Settings updated by ${devicePreferences.deviceName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error syncing settings to Firestore", e)
                        logSyncEvent("Error syncing settings: ${e.message}", isError = true)
                    } finally {
                        isLocalUpdate = false
                    }
                } else {
                    Log.d(TAG, "Skipping settings sync (isLocalUpdate=$isLocalUpdate, settings=$settings)")
                }
            }
        }

        // Listen for images changes
        localImagesListener = coroutineScope.launch {
            Log.d(TAG, "Starting to listen for local images changes...")
            repository.mosqueImages.collect { images ->
                Log.d(TAG, "🔄 Local images changed: ${images.size} images")
                Log.d(TAG, "isLocalUpdate flag: $isLocalUpdate")

                if (!isLocalUpdate) {
                    try {
                        Log.d(TAG, "📤 Pushing ${images.size} images to Firestore...")
                        isLocalUpdate = true

                        // Clear existing images in Firestore
                        val batch = firestore.batch()
                        val existingImages = firestore.collection(IMAGES_COLLECTION).get().await()
                        for (doc in existingImages.documents) {
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()

                        // Upload current images
                        for (image in images) {
                            firestore.collection(IMAGES_COLLECTION)
                                .document(image.id.toString())
                                .set(image)
                                .await()
                        }
                        Log.d(TAG, "✅ ${images.size} images synced to Firestore successfully")
                        logSyncEvent("${images.size} images updated by ${devicePreferences.deviceName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error syncing images to Firestore", e)
                        logSyncEvent("Error syncing images: ${e.message}", isError = true)
                    } finally {
                        isLocalUpdate = false
                    }
                } else {
                    Log.d(TAG, "Skipping images sync (isLocalUpdate=$isLocalUpdate)")
                }
            }
        }

        Log.d(TAG, "✅ Local listeners set up successfully")
    }

    private fun logSyncEvent(message: String, isError: Boolean = false) {
        coroutineScope.launch {
            try {
                val logEntry = hashMapOf(
                    "timestamp" to Date(),
                    "device" to devicePreferences.deviceName,
                    "message" to message,
                    "isError" to isError
                )

                firestore.collection(SYNC_LOG_COLLECTION)
                    .add(logEntry)
                    .await()
                Log.d(TAG, "📝 Logged sync event: $message")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error logging sync event", e)
            }
        }
    }

    fun setMasterDevice(isMaster: Boolean) {
        Log.d(TAG, "=== CHANGING MASTER DEVICE STATUS ===")
        Log.d(TAG, "Current master status: ${devicePreferences.isMasterDevice}")
        Log.d(TAG, "New master status: $isMaster")

        // Only change if different from current setting
        if (devicePreferences.isMasterDevice != isMaster) {
            devicePreferences.isMasterDevice = isMaster

            // Restart sync with new master setting
            stopSync()
            startSync()

            // Log the change
            logSyncEvent(
                if (isMaster) "Device ${devicePreferences.deviceName} set as master"
                else "Device ${devicePreferences.deviceName} set as non-master"
            )
        } else {
            Log.d(TAG, "Master status unchanged")
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        Log.d(TAG, "=== CHANGING SYNC ENABLED STATUS ===")
        Log.d(TAG, "Current sync status: ${devicePreferences.syncEnabled}")
        Log.d(TAG, "New sync status: $enabled")

        // Only change if different from current setting
        if (devicePreferences.syncEnabled != enabled) {
            devicePreferences.syncEnabled = enabled

            if (enabled) {
                startSync()
                logSyncEvent("Sync enabled on ${devicePreferences.deviceName}")
            } else {
                stopSync()
                logSyncEvent("Sync disabled on ${devicePreferences.deviceName}")
            }
        } else {
            Log.d(TAG, "Sync status unchanged")
        }
    }

    fun setDeviceName(name: String) {
        val oldName = devicePreferences.deviceName
        devicePreferences.deviceName = name
        logSyncEvent("Device renamed from $oldName to $name")
    }

    // Add this method to manually trigger a push
    fun forcePush() {
        Log.d(TAG, "=== FORCE PUSH TRIGGERED ===")
        if (devicePreferences.isMasterDevice) {
            uploadLocalDataToFirestore()
        } else {
            Log.d(TAG, "Cannot force push - not a master device")
        }
    }
}
