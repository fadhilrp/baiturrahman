package com.example.baiturrahman.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.data.repository.ImageRepository
import com.example.baiturrahman.utils.DevicePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MosqueDashboardViewModel(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val settingsRepository: MosqueSettingsRepository,
    private val imageRepository: ImageRepository,
    private val devicePreferences: DevicePreferences,
    private val application: Application,
    private val syncService: SupabaseSyncService
) : ViewModel() {

    companion object {
        private const val TAG = "MosqueDashboardViewModel"
    }

    // Default prayer timings to show before API loads
    private val defaultPrayerTimings = PrayerTimings(
        Fajr = "XX:XX",
        Sunrise = "XX:XX",
        Dhuhr = "XX:XX",
        Asr = "XX:XX",
        Sunset = "XX:XX",
        Maghrib = "XX:XX",
        Isha = "XX:XX",
        Imsak = "XX:XX",
        Midnight = "XX:XX"
    )

    private val _uiState = MutableStateFlow(MosqueDashboardUiState())
    val uiState: StateFlow<MosqueDashboardUiState> = _uiState.asStateFlow()

    // Editable content state
    private val _quoteText = MutableStateFlow("\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vel egestas dolor, nec dignissim metus.\"")
    val quoteText: StateFlow<String> = _quoteText

    private val _mosqueName = MutableStateFlow("Masjid Baiturrahman")
    val mosqueName: StateFlow<String> = _mosqueName

    private val _mosqueLocation = MutableStateFlow("Pondok Pinang")
    val mosqueLocation: StateFlow<String> = _mosqueLocation

    private val _marqueeText = MutableStateFlow("Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text")
    val marqueeText: StateFlow<String> = _marqueeText

    private val _logoImage = MutableStateFlow<String?>(null)
    val logoImage: StateFlow<String?> = _logoImage

    // Multiple mosque images for slider
    private val _mosqueImages = MutableStateFlow<List<String>>(emptyList())
    val mosqueImages: StateFlow<List<String>> = _mosqueImages

    // Current image index for slider - with proper synchronization
    private val _currentImageIndex = MutableStateFlow(0)
    val currentImageIndex: StateFlow<Int> = _currentImageIndex

    // Prayer API settings
    private val _prayerAddress = MutableStateFlow("Lebak Bulus, Jakarta, ID")
    val prayerAddress: StateFlow<String> = _prayerAddress

    private val _prayerTimezone = MutableStateFlow("Asia/Jakarta")
    val prayerTimezone: StateFlow<String> = _prayerTimezone

    // Available timezones
    val availableTimezones = listOf(
        "Asia/Jakarta",
        "Asia/Pontianak",
        "Asia/Makassar",
        "Asia/Jayapura"
    )

    // Loading states for UI
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _isUploadingLogo = MutableStateFlow(false)
    val isUploadingLogo: StateFlow<Boolean> = _isUploadingLogo.asStateFlow()

    private val _isDeletingImage = MutableStateFlow(false)
    val isDeletingImage: StateFlow<Boolean> = _isDeletingImage.asStateFlow()

    // Database image IDs — stores both Room ID and Supabase ID
    private data class ImageIds(val roomId: Int, val supabaseId: String?)
    private val imageIdMap = mutableMapOf<String, ImageIds>()
    private var isSliderSyncing = false

    init {
        loadSavedSettings()
        fetchPrayerTimes()
        startImageSlider()

        // Test Supabase connection on startup
        testSupabaseConnection()
    }

    private fun testSupabaseConnection() {
        viewModelScope.launch {
            Log.d(TAG, "🧪 Testing Supabase connection on startup...")
            val connectionSuccess = imageRepository.testConnection()
            if (connectionSuccess) {
                Log.d(TAG, "✅ Supabase connection test passed")
            } else {
                Log.e(TAG, "❌ Supabase connection test failed")
            }
        }
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            // Load settings
            settingsRepository.mosqueSettings.collectLatest { settings ->
                settings?.let {
                    _mosqueName.value = it.mosqueName
                    _mosqueLocation.value = it.mosqueLocation
                    _logoImage.value = it.logoImage
                    _prayerAddress.value = it.prayerAddress
                    _prayerTimezone.value = it.prayerTimezone
                    _quoteText.value = it.quoteText
                    _marqueeText.value = it.marqueeText
                }
            }
        }

        viewModelScope.launch {
            // Load images with proper synchronization
            settingsRepository.mosqueImages.collectLatest { images ->
                // Filter out images with null URIs (incomplete uploads)
                val completedImages = images.filter { !it.imageUri.isNullOrBlank() }
                val imageUris = completedImages.sortedBy { it.displayOrder }.map { it.imageUri }

                // Set syncing flag to prevent conflicts
                isSliderSyncing = true

                _mosqueImages.value = imageUris

                // Update image ID map with both Room ID and Supabase ID
                imageIdMap.clear()
                completedImages.forEach { image ->
                    imageIdMap[image.imageUri] = ImageIds(
                        roomId = image.id,
                        supabaseId = image.supabaseId
                    )
                }

                // Reset current index if needed, but preserve valid indices
                val currentIndex = _currentImageIndex.value
                if (imageUris.isNotEmpty()) {
                    if (currentIndex >= imageUris.size) {
                        _currentImageIndex.value = 0
                    }
                } else {
                    _currentImageIndex.value = 0
                }

                // Clear syncing flag after a short delay
                delay(100)
                isSliderSyncing = false
            }
        }
    }

    private fun startImageSlider() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Change image every 5 seconds

                // Only auto-advance if not currently syncing and we have images
                if (!isSliderSyncing && _mosqueImages.value.isNotEmpty()) {
                    val currentImages = _mosqueImages.value
                    val currentIndex = _currentImageIndex.value

                    // Calculate next index safely
                    val nextIndex = if (currentIndex + 1 >= currentImages.size) 0 else currentIndex + 1
                    _currentImageIndex.value = nextIndex
                }
            }
        }
    }

    fun fetchPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            prayerTimeRepository.getPrayerTimes(
                address = _prayerAddress.value,
                timezone = _prayerTimezone.value
            ).fold(
                onSuccess = { prayerData ->
                    _uiState.value = _uiState.value.copy(
                        prayerData = prayerData,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch prayer times: ${error.message}"
                    )
                }
            )
        }
    }

    // Internal save logic — can be called from coroutines without launching nested ones
    private suspend fun saveAllSettingsInternal() {
        syncService.withSyncLock {
            settingsRepository.saveSettings(
                deviceName = devicePreferences.deviceName,
                mosqueName = _mosqueName.value,
                mosqueLocation = _mosqueLocation.value,
                logoImage = _logoImage.value,
                prayerAddress = _prayerAddress.value,
                prayerTimezone = _prayerTimezone.value,
                quoteText = _quoteText.value,
                marqueeText = _marqueeText.value
            )
        }
    }

    // Save all settings to database, wrapped in sync lock
    fun saveAllSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                saveAllSettingsInternal()
            } finally {
                _isSaving.value = false
            }
        }
    }

    // Update functions for editable content
    fun updateQuoteText(text: String) {
        _quoteText.value = text
    }

    fun updateMosqueName(name: String) {
        _mosqueName.value = name
    }

    fun updateMosqueLocation(location: String) {
        _mosqueLocation.value = location
    }

    fun updateMarqueeText(text: String) {
        _marqueeText.value = text
    }

    // Update the logo image — uses storage-only methods (no mosque_images record)
    fun updateLogoImage(uri: String) {
        viewModelScope.launch {
            _isUploadingLogo.value = true
            try {
                Log.d(TAG, "=== UPDATING LOGO IMAGE ===")
                Log.d(TAG, "URI: $uri")

                val publicUrl = imageRepository.uploadLogoToStorage(Uri.parse(uri))
                if (publicUrl != null) {
                    Log.d(TAG, "✅ Logo uploaded successfully: $publicUrl")

                    // Delete old logo from storage if it exists
                    val oldLogoUrl = _logoImage.value
                    if (oldLogoUrl != null && oldLogoUrl != publicUrl) {
                        Log.d(TAG, "🗑️ Deleting old logo: $oldLogoUrl")
                        imageRepository.deleteLogoFromStorage(oldLogoUrl)
                    }

                    _logoImage.value = publicUrl
                    Log.d(TAG, "✅ Logo image updated in ViewModel")
                    // Auto-save settings after logo upload
                    saveAllSettingsInternal()
                } else {
                    Log.e(TAG, "❌ Failed to upload logo image")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating logo", e)
            } finally {
                _isUploadingLogo.value = false
            }
        }
    }

    // Add mosque image — stores supabaseId in Room, wrapped in sync lock
    fun addMosqueImage(uri: String) {
        if (_mosqueImages.value.size < 5) {
            viewModelScope.launch {
                _isUploadingImage.value = true
                try {
                    Log.d(TAG, "=== ADDING MOSQUE IMAGE ===")
                    Log.d(TAG, "URI: $uri")
                    Log.d(TAG, "Device: ${devicePreferences.deviceName}")
                    Log.d(TAG, "Current images count: ${_mosqueImages.value.size}")

                    val currentCount = _mosqueImages.value.size

                    syncService.withSyncLock {
                        val result = imageRepository.uploadImage(
                            Uri.parse(uri),
                            "mosque-images",
                            displayOrder = currentCount,
                            deviceName = devicePreferences.deviceName
                        )
                        if (result != null) {
                            Log.d(TAG, "✅ Mosque image uploaded: ${result.publicUrl} (id: ${result.supabaseId})")
                            settingsRepository.addMosqueImage(result.publicUrl, result.supabaseId)
                            Log.d(TAG, "✅ Mosque image added to repository with supabaseId")
                        } else {
                            Log.e(TAG, "❌ Failed to upload mosque image")
                        }
                    }
                    // Auto-save settings after image upload
                    saveAllSettingsInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error adding mosque image", e)
                } finally {
                    _isUploadingImage.value = false
                }
            }
        } else {
            Log.w(TAG, "⚠️ Cannot add more images, limit reached (5/5)")
        }
    }

    // Remove mosque image — passes supabaseId + deviceName, wrapped in sync lock
    fun removeMosqueImage(index: Int) {
        if (index in _mosqueImages.value.indices) {
            val imageUrl = _mosqueImages.value[index]
            val ids = imageIdMap[imageUrl] ?: return

            viewModelScope.launch {
                _isDeletingImage.value = true
                try {
                    Log.d(TAG, "=== REMOVING MOSQUE IMAGE ===")
                    Log.d(TAG, "Index: $index")
                    Log.d(TAG, "Image URL: $imageUrl")
                    Log.d(TAG, "Room ID: ${ids.roomId}, Supabase ID: ${ids.supabaseId}")

                    syncService.withSyncLock {
                        val deleteSuccess = imageRepository.deleteImage(
                            imageUrl,
                            ids.supabaseId,
                            devicePreferences.deviceName
                        )
                        if (deleteSuccess) {
                            Log.d(TAG, "✅ Image deleted from Supabase")
                            settingsRepository.removeMosqueImage(ids.roomId)
                            Log.d(TAG, "✅ Image removed from local database")
                        } else {
                            Log.e(TAG, "❌ Failed to delete image from Supabase")
                        }
                    }
                    // Auto-save settings after image delete
                    saveAllSettingsInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error removing image", e)
                } finally {
                    _isDeletingImage.value = false
                }
            }
        } else {
            Log.w(TAG, "⚠️ Invalid image index: $index")
        }
    }

    // Functions for mosque image slider
    fun setCurrentImageIndex(index: Int) {
        val currentImages = _mosqueImages.value
        if (!isSliderSyncing && index in currentImages.indices) {
            _currentImageIndex.value = index
        }
    }

    // Update prayer API settings
    fun updatePrayerAddress(address: String) {
        _prayerAddress.value = address
        fetchPrayerTimes() // Refresh prayer times with new address
    }

    fun updatePrayerTimezone(timezone: String) {
        if (timezone in availableTimezones) {
            _prayerTimezone.value = timezone
            fetchPrayerTimes() // Refresh prayer times with new timezone
        }
    }

    // Debug function to test Supabase connection
    fun debugSupabaseConnection() {
        viewModelScope.launch {
            Log.d(TAG, "🧪 Manual Supabase connection test triggered")
            val success = imageRepository.testConnection()
            Log.d(TAG, if (success) "✅ Connection test passed" else "❌ Connection test failed")
        }
    }

    /**
     * Rename device atomically in remote PostgreSQL, wrapped in sync lock.
     */
    suspend fun renameDevice(oldName: String, newName: String): Boolean {
        return syncService.withSyncLock {
            settingsRepository.renameDevice(oldName, newName)
        }
    }

    // UI state for the MosqueDashboard
    data class MosqueDashboardUiState(
        val prayerData: PrayerData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )
}
