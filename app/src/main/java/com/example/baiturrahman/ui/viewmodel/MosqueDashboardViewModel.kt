package com.example.baiturrahman.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.model.DeviceSession
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.data.remote.SupabaseSyncService
import com.example.baiturrahman.data.repository.AccountRepository
import com.example.baiturrahman.data.repository.ImageRepository
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import com.example.baiturrahman.utils.AccountPreferences
import com.example.baiturrahman.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MosqueDashboardViewModel(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val settingsRepository: MosqueSettingsRepository,
    private val imageRepository: ImageRepository,
    private val accountPreferences: AccountPreferences,
    private val application: Application,
    private val syncService: SupabaseSyncService,
    private val accountRepository: AccountRepository,
    private val connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "MosqueDashboardViewModel"
    }

    private val defaultPrayerTimings = PrayerTimings(
        Fajr = "XX:XX", Sunrise = "XX:XX", Dhuhr = "XX:XX", Asr = "XX:XX",
        Sunset = "XX:XX", Maghrib = "XX:XX", Isha = "XX:XX",
        Imsak = "XX:XX", Midnight = "XX:XX"
    )

    private val _uiState = MutableStateFlow(MosqueDashboardUiState())
    val uiState: StateFlow<MosqueDashboardUiState> = _uiState.asStateFlow()

    private val _quoteText = MutableStateFlow("\"Lorem ipsum dolor sit amet, consectetur adipiscing elit.\"")
    val quoteText: StateFlow<String> = _quoteText

    private val _mosqueName = MutableStateFlow("Masjid Baiturrahman")
    val mosqueName: StateFlow<String> = _mosqueName

    private val _mosqueLocation = MutableStateFlow("Pondok Pinang")
    val mosqueLocation: StateFlow<String> = _mosqueLocation

    private val _marqueeText = MutableStateFlow("Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text Rolling Text")
    val marqueeText: StateFlow<String> = _marqueeText

    private val _logoImage = MutableStateFlow<String?>(null)
    val logoImage: StateFlow<String?> = _logoImage

    private val _mosqueImages = MutableStateFlow<List<String>>(emptyList())
    val mosqueImages: StateFlow<List<String>> = _mosqueImages

    private val _currentImageIndex = MutableStateFlow(0)
    val currentImageIndex: StateFlow<Int> = _currentImageIndex

    private val _prayerAddress = MutableStateFlow("Lebak Bulus, Jakarta, ID")
    val prayerAddress: StateFlow<String> = _prayerAddress

    private val _prayerTimezone = MutableStateFlow("Asia/Jakarta")
    val prayerTimezone: StateFlow<String> = _prayerTimezone

    val availableTimezones = listOf(
        "Asia/Jakarta", "Asia/Pontianak", "Asia/Makassar", "Asia/Jayapura"
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _isUploadingLogo = MutableStateFlow(false)
    val isUploadingLogo: StateFlow<Boolean> = _isUploadingLogo.asStateFlow()

    private val _isDeletingImage = MutableStateFlow(false)
    val isDeletingImage: StateFlow<Boolean> = _isDeletingImage.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<DeviceSession>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceSession>> = _connectedDevices.asStateFlow()

    val isOffline: StateFlow<Boolean> = connectivityObserver.isConnected
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Image ID map: URI → (roomId, supabaseId)
    private data class ImageIds(val roomId: Int, val supabaseId: String?)
    private val imageIdMap = mutableMapOf<String, ImageIds>()
    private var isSliderSyncing = false

    init {
        loadSavedSettings()
        fetchPrayerTimes()
        startImageSlider()
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
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
            settingsRepository.mosqueImages.collectLatest { images ->
                val completedImages = images.filter { !it.imageUri.isNullOrBlank() }
                val imageUris = completedImages.sortedBy { it.displayOrder }.map { it.imageUri }

                isSliderSyncing = true
                _mosqueImages.value = imageUris

                imageIdMap.clear()
                completedImages.forEach { image ->
                    imageIdMap[image.imageUri] = ImageIds(image.id, image.supabaseId)
                }

                val currentIndex = _currentImageIndex.value
                if (imageUris.isNotEmpty() && currentIndex >= imageUris.size) {
                    _currentImageIndex.value = 0
                } else if (imageUris.isEmpty()) {
                    _currentImageIndex.value = 0
                }

                delay(100)
                isSliderSyncing = false
            }
        }
    }

    private fun startImageSlider() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                if (!isSliderSyncing && _mosqueImages.value.isNotEmpty()) {
                    val currentImages = _mosqueImages.value
                    val currentIndex = _currentImageIndex.value
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
                    _uiState.value = _uiState.value.copy(prayerData = prayerData, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat waktu sholat: ${error.message}"
                    )
                }
            )
        }
    }

    private suspend fun saveAllSettingsInternal() {
        val token = accountPreferences.sessionToken ?: return
        syncService.withSyncLock {
            settingsRepository.saveSettings(
                sessionToken = token,
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

    fun updateQuoteText(text: String) { _quoteText.value = text }
    fun updateMosqueName(name: String) { _mosqueName.value = name }
    fun updateMosqueLocation(location: String) { _mosqueLocation.value = location }
    fun updateMarqueeText(text: String) { _marqueeText.value = text }

    fun updateLogoImage(uri: String) {
        viewModelScope.launch {
            _isUploadingLogo.value = true
            try {
                val publicUrl = imageRepository.uploadLogoToStorage(Uri.parse(uri))
                if (publicUrl != null) {
                    val oldLogoUrl = _logoImage.value
                    if (oldLogoUrl != null && oldLogoUrl != publicUrl) {
                        imageRepository.deleteLogoFromStorage(oldLogoUrl)
                    }
                    _logoImage.value = publicUrl
                    saveAllSettingsInternal()
                } else {
                    Log.e(TAG, "Logo upload failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating logo", e)
            } finally {
                _isUploadingLogo.value = false
            }
        }
    }

    fun addMosqueImage(uri: String) {
        if (_mosqueImages.value.size >= 5) {
            Log.w(TAG, "Max images reached")
            return
        }
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val token = accountPreferences.sessionToken ?: return@launch
                val currentCount = _mosqueImages.value.size
                syncService.withSyncLock {
                    val result = imageRepository.uploadImage(
                        Uri.parse(uri),
                        "mosque-images",
                        displayOrder = currentCount,
                        sessionToken = token
                    )
                    if (result != null) {
                        settingsRepository.addMosqueImage(result.publicUrl, result.supabaseId)
                    } else {
                        Log.e(TAG, "Image upload failed")
                    }
                }
                saveAllSettingsInternal()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding image", e)
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun removeMosqueImage(index: Int) {
        if (index !in _mosqueImages.value.indices) return
        val imageUrl = _mosqueImages.value[index]
        val ids = imageIdMap[imageUrl] ?: return

        viewModelScope.launch {
            _isDeletingImage.value = true
            try {
                val token = accountPreferences.sessionToken ?: return@launch
                syncService.withSyncLock {
                    val deleteSuccess = imageRepository.deleteImage(imageUrl, ids.supabaseId, token)
                    if (deleteSuccess) {
                        settingsRepository.removeMosqueImage(ids.roomId)
                    } else {
                        Log.e(TAG, "Image delete failed")
                    }
                }
                saveAllSettingsInternal()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing image", e)
            } finally {
                _isDeletingImage.value = false
            }
        }
    }

    fun setCurrentImageIndex(index: Int) {
        if (!isSliderSyncing && index in _mosqueImages.value.indices) {
            _currentImageIndex.value = index
        }
    }

    fun updatePrayerAddress(address: String) { _prayerAddress.value = address }

    fun updatePrayerTimezone(timezone: String) {
        if (timezone in availableTimezones) _prayerTimezone.value = timezone
    }

    fun loadConnectedDevices() {
        viewModelScope.launch {
            _connectedDevices.value = accountRepository.getActiveSessions()
        }
    }

    fun forceLogoutDevice(sessionId: String) {
        viewModelScope.launch {
            accountRepository.forceLogoutDevice(sessionId)
            loadConnectedDevices()
        }
    }

    /**
     * Change password. Returns null on success, error message on failure.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): String? {
        return accountRepository.changePassword(oldPassword, newPassword)
    }

    data class MosqueDashboardUiState(
        val prayerData: PrayerData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )
}
