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

    // Flag to prevent infinite sync loops
    private var isLocalUpdate = false

    // Collection names in Firestore
    private val SETTINGS_COLLECTION = "mosque_settings"
    private val IMAGES_COLLECTION = "mosque_images"
    private val SYNC_LOG_COLLECTION = "sync_logs"

    // Document ID for settings (since we only have one settings object)
    private val SETTINGS_DOCUMENT_ID = "main_settings"

    fun startSync() {
        if (!devicePreferences.syncEnabled) {
            Log.d(TAG, "Sync is disabled for this device")
            return
        }

        Log.d(TAG, "Starting Firestore sync. Master device: ${devicePreferences.isMasterDevice}")

        // Always listen for remote changes
        listenForRemoteChanges()

        // Only master devices can push changes to Firestore
        if (devicePreferences.isMasterDevice) {
            // Initial upload of local data to Firestore
            uploadLocalDataToFirestore()

            // Listen for local changes and sync to Firestore
            listenForLocalChanges()

            // Log that this device is now the master
            logSyncEvent("Device ${devicePreferences.deviceName} set as master")
        }
    }

    fun stopSync() {
        settingsListener?.remove()
        imagesListener?.remove()
        Log.d(TAG, "Firestore sync stopped")
    }

    private fun uploadLocalDataToFirestore() {
        if (!devicePreferences.isMasterDevice) return

        coroutineScope.launch {
            try {
                // Upload settings
                val settings = repository.mosqueSettings.first()
                if (settings != null) {
                    isLocalUpdate = true
                    firestore.collection(SETTINGS_COLLECTION)
                        .document(SETTINGS_DOCUMENT_ID)
                        .set(settings)
                        .await()
                    Log.d(TAG, "Settings uploaded to Firestore")

                    // Log the sync event
                    logSyncEvent("Settings uploaded by ${devicePreferences.deviceName}")
                }

                // Upload images
                val images = repository.mosqueImages.first()
                if (images.isNotEmpty()) {
                    // First clear existing images to avoid duplicates
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
                    Log.d(TAG, "${images.size} images uploaded to Firestore")

                    // Log the sync event
                    logSyncEvent("${images.size} images uploaded by ${devicePreferences.deviceName}")
                }
                isLocalUpdate = false
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading local data to Firestore", e)
                isLocalUpdate = false

                // Log the error
                logSyncEvent("Error uploading data: ${e.message}", isError = true)
            }
        }
    }

    private fun listenForRemoteChanges() {
        try {
            // Listen for settings changes
            settingsListener = firestore.collection(SETTINGS_COLLECTION)
                .document(SETTINGS_DOCUMENT_ID)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening for settings changes", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists() && !isLocalUpdate) {
                        try {
                            val remoteSettings = snapshot.toObject(MosqueSettings::class.java)
                            if (remoteSettings != null) {
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
                                        Log.d(TAG, "Settings updated from Firestore")

                                        // Only log if not the master device (to avoid duplicate logs)
                                        if (!devicePreferences.isMasterDevice) {
                                            logSyncEvent("Settings received by ${devicePreferences.deviceName}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error updating local settings", e)
                                        logSyncEvent("Error updating settings: ${e.message}", isError = true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing settings", e)
                        }
                    }
                }

            // Listen for images changes
            imagesListener = firestore.collection(IMAGES_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening for images changes", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !isLocalUpdate) {
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
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error deserializing image: ${doc.id}", e)
                                    }
                                }
                                Log.d(TAG, "Images updated from Firestore: ${snapshot.size()} images")

                                // Only log if not the master device (to avoid duplicate logs)
                                if (!devicePreferences.isMasterDevice) {
                                    logSyncEvent("${snapshot.size()} images received by ${devicePreferences.deviceName}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating local images", e)
                                logSyncEvent("Error updating images: ${e.message}", isError = true)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore listeners", e)
        }
    }

    private fun listenForLocalChanges() {
        if (!devicePreferences.isMasterDevice) return

        coroutineScope.launch {
            // Listen for settings changes
            repository.mosqueSettings.collect { settings ->
                if (!isLocalUpdate && settings != null) {
                    try {
                        isLocalUpdate = true
                        firestore.collection(SETTINGS_COLLECTION)
                            .document(SETTINGS_DOCUMENT_ID)
                            .set(settings)
                            .await()
                        Log.d(TAG, "Settings synced to Firestore")
                        logSyncEvent("Settings updated by ${devicePreferences.deviceName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing settings to Firestore", e)
                        logSyncEvent("Error syncing settings: ${e.message}", isError = true)
                    } finally {
                        isLocalUpdate = false
                    }
                }
            }
        }

        coroutineScope.launch {
            // Listen for images changes
            repository.mosqueImages.collect { images ->
                if (!isLocalUpdate && images.isNotEmpty()) {
                    try {
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
                        Log.d(TAG, "${images.size} images synced to Firestore")
                        logSyncEvent("${images.size} images updated by ${devicePreferences.deviceName}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing images to Firestore", e)
                        logSyncEvent("Error syncing images: ${e.message}", isError = true)
                    } finally {
                        isLocalUpdate = false
                    }
                }
            }
        }
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
            } catch (e: Exception) {
                Log.e(TAG, "Error logging sync event", e)
            }
        }
    }

    fun setMasterDevice(isMaster: Boolean) {
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
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
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
        }
    }

    fun setDeviceName(name: String) {
        val oldName = devicePreferences.deviceName
        devicePreferences.deviceName = name
        logSyncEvent("Device renamed from $oldName to $name")
    }
}
