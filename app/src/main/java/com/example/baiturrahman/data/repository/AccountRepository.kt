package com.example.baiturrahman.data.repository

import android.util.Log
import com.example.baiturrahman.data.model.DeviceSession
import com.example.baiturrahman.utils.AccountPreferences

class AccountRepository(
    private val postgresRepository: SupabasePostgresRepository,
    private val accountPreferences: AccountPreferences
) {
    companion object {
        private const val TAG = "AccountRepository"
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return postgresRepository.checkUsernameAvailable(username)
    }

    /**
     * Register a new account and save the session token to prefs.
     * @return null on success, error message string on failure
     */
    suspend fun register(username: String, password: String): String? {
        return try {
            val token = postgresRepository.register(
                username = username,
                password = password,
                deviceId = accountPreferences.deviceIdentifier,
                deviceLabel = accountPreferences.deviceLabel
            ) ?: return "Gagal mendaftar — token tidak diterima"

            accountPreferences.sessionToken = token
            accountPreferences.username = username
            Log.d(TAG, "register succeeded, token saved")
            null
        } catch (e: Exception) {
            Log.e(TAG, "register failed", e)
            when {
                e.message?.contains("USERNAME_TAKEN") == true ->
                    "Nama pengguna sudah digunakan"
                else -> "Gagal mendaftar: ${e.message}"
            }
        }
    }

    /**
     * Login with username/password and save the session token.
     * @return null on success, error message string on failure
     */
    suspend fun login(username: String, password: String): String? {
        return try {
            val token = postgresRepository.login(
                username = username,
                password = password,
                deviceId = accountPreferences.deviceIdentifier,
                deviceLabel = accountPreferences.deviceLabel
            ) ?: return "Gagal masuk — token tidak diterima"

            accountPreferences.sessionToken = token
            accountPreferences.username = username
            Log.d(TAG, "login succeeded, token saved")
            null
        } catch (e: Exception) {
            Log.e(TAG, "login failed", e)
            when {
                e.message?.contains("INVALID_CREDENTIALS") == true ->
                    "Nama pengguna atau kata sandi salah"
                else -> "Gagal masuk: ${e.message}"
            }
        }
    }

    /**
     * Logout the current device — deletes the session server-side and clears prefs.
     */
    suspend fun logout() {
        val token = accountPreferences.sessionToken
        if (token != null) {
            postgresRepository.logout(token)
        }
        accountPreferences.clearSession()
        Log.d(TAG, "logout complete")
    }

    /**
     * Validate the stored session token against the server.
     * Clears prefs and returns false if invalid.
     */
    suspend fun validateAndClearIfInvalid(): Boolean {
        val token = accountPreferences.sessionToken ?: return false
        val result = postgresRepository.validateSession(token)
        return if (result != null) {
            val (accountId, username) = result
            accountPreferences.accountId = accountId
            accountPreferences.username = username
            true
        } else {
            Log.w(TAG, "Session invalid — clearing prefs")
            accountPreferences.clearSession()
            false
        }
    }

    suspend fun getActiveSessions(): List<DeviceSession> {
        val token = accountPreferences.sessionToken ?: return emptyList()
        return postgresRepository.getActiveSessions(token)
    }

    suspend fun forceLogoutDevice(targetSessionId: String) {
        val token = accountPreferences.sessionToken ?: return
        postgresRepository.logoutOtherDevice(token, targetSessionId)
    }

    /**
     * Change password. Returns null on success, error message on failure.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): String? {
        val token = accountPreferences.sessionToken ?: return "Tidak ada sesi aktif"
        val success = postgresRepository.changePassword(token, oldPassword, newPassword)
        return if (success) null else "Kata sandi lama salah"
    }

}
