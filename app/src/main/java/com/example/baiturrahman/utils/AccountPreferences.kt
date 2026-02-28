package com.example.baiturrahman.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AccountPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "account_settings", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_DEVICE_IDENTIFIER = "device_identifier"
        private const val KEY_DEVICE_LABEL = "device_label"
        private const val KEY_USERNAME = "username"
        private const val KEY_DARK_THEME = "dark_theme"
        // GPS coordinates — device-local, not synced to Supabase.
        // Stored as Long bits for full Double precision.
        private const val KEY_PRAYER_LATITUDE = "prayer_latitude"
        private const val KEY_PRAYER_LONGITUDE = "prayer_longitude"
    }

    /** Last GPS latitude used for prayer time calculation. Null if not set or cleared. */
    var prayerLatitude: Double?
        get() = if (prefs.contains(KEY_PRAYER_LATITUDE))
            Double.fromBits(prefs.getLong(KEY_PRAYER_LATITUDE, 0L)) else null
        set(value) = if (value != null) prefs.edit { putLong(KEY_PRAYER_LATITUDE, value.toBits()) }
                     else prefs.edit { remove(KEY_PRAYER_LATITUDE) }

    /** Last GPS longitude used for prayer time calculation. Null if not set or cleared. */
    var prayerLongitude: Double?
        get() = if (prefs.contains(KEY_PRAYER_LONGITUDE))
            Double.fromBits(prefs.getLong(KEY_PRAYER_LONGITUDE, 0L)) else null
        set(value) = if (value != null) prefs.edit { putLong(KEY_PRAYER_LONGITUDE, value.toBits()) }
                     else prefs.edit { remove(KEY_PRAYER_LONGITUDE) }

    private val _isDarkThemeFlow = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, true))
    val isDarkThemeFlow: StateFlow<Boolean> = _isDarkThemeFlow.asStateFlow()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) {
            prefs.edit { putBoolean(KEY_DARK_THEME, value) }
            _isDarkThemeFlow.value = value
        }

    // Reactive session token flow — AuthViewModel observes this to update isLoggedIn
    private val _sessionTokenFlow = MutableStateFlow(prefs.getString(KEY_SESSION_TOKEN, null))
    val sessionTokenFlow: StateFlow<String?> = _sessionTokenFlow.asStateFlow()

    private val _usernameFlow = MutableStateFlow(prefs.getString(KEY_USERNAME, null))
    val usernameFlow: StateFlow<String?> = _usernameFlow.asStateFlow()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) {
            prefs.edit { putString(KEY_USERNAME, value) }
            _usernameFlow.value = value
        }

    var sessionToken: String?
        get() = prefs.getString(KEY_SESSION_TOKEN, null)
        set(value) {
            prefs.edit { putString(KEY_SESSION_TOKEN, value) }
            _sessionTokenFlow.value = value
        }

    var accountId: String?
        get() = prefs.getString(KEY_ACCOUNT_ID, null)
        set(value) = prefs.edit { putString(KEY_ACCOUNT_ID, value) }

    /**
     * Device identifier — generated once on first install, persists across sessions.
     */
    val deviceIdentifier: String
        get() {
            var id = prefs.getString(KEY_DEVICE_IDENTIFIER, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit { putString(KEY_DEVICE_IDENTIFIER, id) }
            }
            return id
        }

    /**
     * Human-readable device label from Build info.
     */
    val deviceLabel: String
        get() = prefs.getString(KEY_DEVICE_LABEL, null)
            ?: "${Build.MANUFACTURER} ${Build.MODEL}".also {
                prefs.edit { putString(KEY_DEVICE_LABEL, it) }
            }

    fun clearSession() {
        prefs.edit {
            remove(KEY_SESSION_TOKEN)
            remove(KEY_ACCOUNT_ID)
            remove(KEY_USERNAME)
        }
        _sessionTokenFlow.value = null
        _usernameFlow.value = null
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_SESSION_TOKEN, null) != null
}
