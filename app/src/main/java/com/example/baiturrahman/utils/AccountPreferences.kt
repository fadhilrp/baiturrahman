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
    }

    // Reactive session token flow — AuthViewModel observes this to update isLoggedIn
    private val _sessionTokenFlow = MutableStateFlow(prefs.getString(KEY_SESSION_TOKEN, null))
    val sessionTokenFlow: StateFlow<String?> = _sessionTokenFlow.asStateFlow()

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
        }
        _sessionTokenFlow.value = null
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_SESSION_TOKEN, null) != null
}
