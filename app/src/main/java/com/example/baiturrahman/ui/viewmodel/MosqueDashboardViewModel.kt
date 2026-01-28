package com.example.baiturrahman.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.data.repository.ImageRepository
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
    private val application: Application
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

    // Database image IDs (to keep track for deletion)
    private val imageIdMap = mutableMapOf<String, Int>()
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

                // Update image ID map
                imageIdMap.clear()
                completedImages.forEach { image ->
                    imageIdMap[image.imageUri] = image.id
                }

                // Reset current index if needed, but preserve valid indices
                val currentIndex = _currentImageIndex.value
                if (imageUris.isNotEmpty()) {
                    if (currentIndex >= imageUris.size) {
                        _currentImageIndex.value = 0
                    }
                    // If images list changed but current index is still valid, keep it
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

    // Save all settings to database
    fun saveAllSettings() {
        viewModelScope.launch {
            settingsRepository.saveSettings(
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

    // Update the logo image function with extensive debugging
    fun updateLogoImage(uri: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== UPDATING LOGO IMAGE ===")
                Log.d(TAG, "URI: $uri")

                // Upload to Supabase (displayOrder = 0 for logos, not used in slider)
                val publicUrl = imageRepository.uploadImage(Uri.parse(uri), "logos", displayOrder = 0)
                if (publicUrl != null) {
                    Log.d(TAG, "✅ Logo uploaded successfully: $publicUrl")

                    // Delete old logo if it exists
                    val oldLogoUrl = _logoImage.value
                    if (oldLogoUrl != null && oldLogoUrl != publicUrl) {
                        Log.d(TAG, "🗑️ Deleting old logo: $oldLogoUrl")
                        imageRepository.deleteImage(oldLogoUrl, null)
                    }

                    _logoImage.value = publicUrl
                    Log.d(TAG, "✅ Logo image updated in ViewModel")
                } else {
                    Log.e(TAG, "❌ Failed to upload logo image")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating logo", e)
            }
        }
    }

    // Update the mosque image function with extensive debugging
    fun addMosqueImage(uri: String) {
        if (_mosqueImages.value.size < 5) {
            viewModelScope.launch {
                try {
                    Log.d(TAG, "=== ADDING MOSQUE IMAGE ===")
                    Log.d(TAG, "URI: $uri")
                    Log.d(TAG, "Current images count: ${_mosqueImages.value.size}")

                    // Get current count for display order
                    val currentCount = _mosqueImages.value.size

                    // Upload to Supabase with displayOrder
                    val publicUrl = imageRepository.uploadImage(
                        Uri.parse(uri),
                        "mosque-images",
                        displayOrder = currentCount
                    )
                    if (publicUrl != null) {
                        Log.d(TAG, "✅ Mosque image uploaded successfully: $publicUrl")
                        settingsRepository.addMosqueImage(publicUrl)
                        Log.d(TAG, "✅ Mosque image added to repository")
                    } else {
                        Log.e(TAG, "❌ Failed to upload mosque image")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error adding mosque image", e)
                }
            }
        } else {
            Log.w(TAG, "⚠️ Cannot add more images, limit reached (5/5)")
        }
    }

    // Update remove function to delete from Supabase with debugging
    fun removeMosqueImage(index: Int) {
        if (index in _mosqueImages.value.indices) {
            val imageUrl = _mosqueImages.value[index]
            val imageId = imageIdMap[imageUrl] ?: return

            viewModelScope.launch {
                try {
                    Log.d(TAG, "=== REMOVING MOSQUE IMAGE ===")
                    Log.d(TAG, "Index: $index")
                    Log.d(TAG, "Image URL: $imageUrl")
                    Log.d(TAG, "Image ID (Room): $imageId")

                    // Note: supabaseId should be extracted from the URL or stored in Room
                    // For now, delete with URL only and Room ID
                    // The sync service will handle PostgreSQL cleanup
                    val deleteSuccess = imageRepository.deleteImage(imageUrl, null)
                    if (deleteSuccess) {
                        Log.d(TAG, "✅ Image deleted from Supabase Storage")
                        // Remove from local database
                        settingsRepository.removeMosqueImage(imageId)
                        Log.d(TAG, "✅ Image removed from local database")
                    } else {
                        Log.e(TAG, "❌ Failed to delete image from Supabase")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error removing image", e)
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

    // UI state for the MosqueDashboard
    data class MosqueDashboardUiState(
        val prayerData: PrayerData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )
}
