package com.example.baiturrahman.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class DevicePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "device_settings", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_MASTER = "is_master_device"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
    }

    var isMasterDevice: Boolean
        get() = prefs.getBoolean(KEY_IS_MASTER, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_MASTER, value) }

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, "TV-${android.os.Build.SERIAL.takeLast(4)}") ?:
        "TV-${android.os.Build.SERIAL.takeLast(4)}"
        set(value) = prefs.edit { putString(KEY_DEVICE_NAME, value) }

    var syncEnabled: Boolean
        get() = prefs.getBoolean(KEY_SYNC_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SYNC_ENABLED, value) }
}