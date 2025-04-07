package com.example.baiturrahman.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MosqueDashboardViewModel(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val settingsRepository: MosqueSettingsRepository,
    private val application: Application
) : ViewModel() {

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

    // Current image index for slider
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

    init {
        loadSavedSettings()
        fetchPrayerTimes()
        startImageSlider()
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
            // Load images
            settingsRepository.mosqueImages.collectLatest { images ->
                val imageUris = images.sortedBy { it.displayOrder }.map { it.imageUri }
                _mosqueImages.value = imageUris

                // Update image ID map
                imageIdMap.clear()
                images.forEach { image ->
                    imageIdMap[image.imageUri] = image.id
                }

                // Reset current index if needed
                if (_currentImageIndex.value >= imageUris.size && imageUris.isNotEmpty()) {
                    _currentImageIndex.value = 0
                }
            }
        }
    }

    private fun startImageSlider() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Change image every 5 seconds
                if (_mosqueImages.value.isNotEmpty()) {
                    _currentImageIndex.value = (_currentImageIndex.value + 1) % _mosqueImages.value.size
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

    fun updateLogoImage(uri: String) {
        _logoImage.value = uri
    }

    // Functions for mosque image slider
    fun addMosqueImage(uri: String) {
        if (_mosqueImages.value.size < 5) {
            viewModelScope.launch {
                settingsRepository.addMosqueImage(uri)
            }
        }
    }

    fun removeMosqueImage(index: Int) {
        if (index in _mosqueImages.value.indices) {
            val imageUri = _mosqueImages.value[index]
            val imageId = imageIdMap[imageUri] ?: return

            viewModelScope.launch {
                settingsRepository.removeMosqueImage(imageId)
            }
        }
    }

    fun setCurrentImageIndex(index: Int) {
        if (index in _mosqueImages.value.indices) {
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

    // UI state for the MosqueDashboard
    data class MosqueDashboardUiState(
        val prayerData: PrayerData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )
}

