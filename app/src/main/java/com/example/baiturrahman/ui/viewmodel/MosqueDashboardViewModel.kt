package com.example.baiturrahman.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.data.model.PrayerTimings
import com.example.baiturrahman.data.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MosqueDashboardViewModel(
    private val repository: PrayerTimeRepository
) : ViewModel() {

    // Default prayer timings to show before API loads
    private val defaultPrayerTimings = PrayerTimings(
        Fajr = "04:40",
        Sunrise = "05:58",
        Dhuhr = "12:06",
        Asr = "15:14",
        Sunset = "18:14",
        Maghrib = "18:14",
        Isha = "19:25",
        Imsak = "04:30",
        Midnight = "00:06"
    )

    private val _uiState = MutableStateFlow(MosqueDashboardUiState())
    val uiState: StateFlow<MosqueDashboardUiState> = _uiState.asStateFlow()

    init {
        fetchPrayerTimes()
    }

    fun fetchPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getPrayerTimes().fold(
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

    // UI state for the MosqueDashboard
    data class MosqueDashboardUiState(
        val prayerData: PrayerData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )
}

