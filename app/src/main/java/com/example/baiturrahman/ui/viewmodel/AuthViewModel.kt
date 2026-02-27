package com.example.baiturrahman.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baiturrahman.data.repository.AccountRepository
import com.example.baiturrahman.utils.AccountPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    accountPreferences: AccountPreferences,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Derived from the live sessionTokenFlow — auto-updates when token is set/cleared
    val isLoggedIn: StateFlow<Boolean> = accountPreferences.sessionTokenFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, accountPreferences.isLoggedIn())

    val currentUsername: StateFlow<String?> = accountPreferences.usernameFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val error = accountRepository.login(username.trim(), password)
                if (error != null) _errorMessage.value = error
                // isLoggedIn updates automatically via sessionTokenFlow
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val error = accountRepository.register(username.trim(), password)
                if (error != null) _errorMessage.value = error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkUsernameAvailable(username: String) {
        if (username.length < 3) {
            _usernameAvailable.value = null
            return
        }
        viewModelScope.launch {
            _usernameAvailable.value = null
            val available = accountRepository.checkUsernameAvailable(username.trim())
            _usernameAvailable.value = available
        }
    }

    fun resetUsernameAvailability() {
        _usernameAvailable.value = null
    }

    fun logout() {
        viewModelScope.launch {
            accountRepository.logout()
            // accountPreferences.clearSession() is called inside logout()
            // which updates sessionTokenFlow → isLoggedIn becomes false automatically
        }
    }
}
