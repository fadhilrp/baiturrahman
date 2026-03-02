package com.example.baiturrahman.data.repository

import android.util.Log
import com.example.baiturrahman.data.model.DeviceSession
import com.example.baiturrahman.data.model.ImageMetadata
import com.example.baiturrahman.data.model.MosqueSettingsRemote
import com.example.baiturrahman.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Repository for all Supabase PostgreSQL RPC operations.
 * All data access goes through SECURITY DEFINER RPCs — no direct table access.
 */
class SupabasePostgresRepository {
    private val TAG = "SupabasePostgresRepo"
    private val client = SupabaseClient.client

    // ========== AUTH RPCs ==========

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return try {
            val params = buildJsonObject { put("p_username", username) }
            val result = client.postgrest.rpc("check_username_available", params)
                .decodeAs<Boolean>()
            result
        } catch (e: Exception) {
            Log.e(TAG, "checkUsernameAvailable failed", e)
            true // optimistic: assume available on error; actual register will catch real conflicts
        }
    }

    /**
     * Register a new account. Returns session token on success, null on failure.
     * Throws with message "USERNAME_TAKEN" if username is already used.
     */
    suspend fun register(
        username: String,
        password: String,
        deviceId: String,
        deviceLabel: String
    ): String? {
        return try {
            val params = buildJsonObject {
                put("p_username", username)
                put("p_password", password)
                put("p_device_id", deviceId)
                put("p_device_label", deviceLabel)
            }
            val result = client.postgrest.rpc("register_account", params)
                .decodeAs<JsonObject>()
            result["session_token"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "register failed: ${e.message}")
            throw e
        }
    }

    /**
     * Login to existing account. Returns session token on success, null on failure.
     * Throws with message "INVALID_CREDENTIALS" on bad username/password.
     */
    suspend fun login(
        username: String,
        password: String,
        deviceId: String,
        deviceLabel: String
    ): String? {
        return try {
            val params = buildJsonObject {
                put("p_username", username)
                put("p_password", password)
                put("p_device_id", deviceId)
                put("p_device_label", deviceLabel)
            }
            val result = client.postgrest.rpc("login_account", params)
                .decodeAs<JsonObject>()
            result["session_token"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "login failed: ${e.message}")
            throw e
        }
    }

    /**
     * Validate session token. Returns (account_id, username) if valid, null if invalid/expired.
     */
    /**
     * Returns (account_id, username) if session is valid, null if the server says it's invalid.
     * Throws on network/connectivity errors so callers can distinguish "invalid session" from
     * "couldn't reach server" and avoid clearing a valid stored session unnecessarily.
     */
    suspend fun validateSession(token: String): Pair<String, String>? {
        val params = buildJsonObject { put("p_session_token", token) }
        val result = client.postgrest.rpc("validate_session", params)
            .decodeAs<JsonObject>()
        val accountId = result["account_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val username = result["username"]?.jsonPrimitive?.contentOrNull ?: return null
        return accountId to username
    }

    suspend fun logout(token: String) {
        try {
            val params = buildJsonObject { put("p_session_token", token) }
            client.postgrest.rpc("logout_device", params)
        } catch (e: Exception) {
            Log.e(TAG, "logout failed: ${e.message}")
        }
    }

    suspend fun logoutOtherDevice(token: String, targetSessionId: String) {
        try {
            val params = buildJsonObject {
                put("p_session_token", token)
                put("p_target_session_id", targetSessionId)
            }
            client.postgrest.rpc("logout_other_device", params)
        } catch (e: Exception) {
            Log.e(TAG, "logoutOtherDevice failed: ${e.message}")
        }
    }

    /**
     * Change password. Returns true on success, false if old password is wrong.
     */
    suspend fun changePassword(token: String, oldPassword: String, newPassword: String): Boolean {
        return try {
            val params = buildJsonObject {
                put("p_session_token", token)
                put("p_old_password", oldPassword)
                put("p_new_password", newPassword)
            }
            val result = client.postgrest.rpc("change_password", params)
                .decodeAs<JsonObject>()
            result["success"]?.jsonPrimitive?.content == "true"
        } catch (e: Exception) {
            Log.e(TAG, "changePassword failed: ${e.message}")
            false
        }
    }

    suspend fun updateSessionLastSeen(token: String) {
        try {
            val params = buildJsonObject { put("p_session_token", token) }
            client.postgrest.rpc("update_session_last_seen", params)
        } catch (e: Exception) {
            Log.e(TAG, "updateSessionLastSeen failed: ${e.message}")
            throw e
        }
    }

    suspend fun getActiveSessions(token: String): List<DeviceSession> {
        return try {
            val params = buildJsonObject { put("p_session_token", token) }
            client.postgrest.rpc("get_active_sessions", params)
                .decodeAs<List<DeviceSession>>()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveSessions failed: ${e.message}")
            emptyList()
        }
    }

    // ========== SETTINGS RPCs ==========

    suspend fun getSettingsByToken(token: String): MosqueSettingsRemote? {
        return try {
            val params = buildJsonObject { put("p_session_token", token) }
            client.postgrest.rpc("get_settings_by_token", params)
                .decodeAs<MosqueSettingsRemote>()
        } catch (e: Exception) {
            Log.e(TAG, "getSettingsByToken failed: ${e.message}")
            null
        }
    }

    suspend fun upsertSettingsByToken(
        token: String,
        mosqueName: String,
        mosqueLocation: String,
        logoImage: String?,
        prayerAddress: String,
        prayerTimezone: String,
        quoteText: String,
        marqueeText: String,
        iqomahDurationMinutes: Int = 10
    ) {
        try {
            val params = buildJsonObject {
                put("p_session_token", token)
                put("p_mosque_name", mosqueName)
                put("p_mosque_location", mosqueLocation)
                put("p_logo_image", logoImage)
                put("p_prayer_address", prayerAddress)
                put("p_prayer_timezone", prayerTimezone)
                put("p_quote_text", quoteText)
                put("p_marquee_text", marqueeText)
                put("p_iqomah_duration_minutes", iqomahDurationMinutes)
            }
            client.postgrest.rpc("upsert_settings_by_token", params)
            Log.d(TAG, "upsertSettingsByToken succeeded")
        } catch (e: Exception) {
            Log.e(TAG, "upsertSettingsByToken failed: ${e.message}")
            throw e
        }
    }

    // ========== IMAGE RPCs ==========

    suspend fun getImagesByToken(token: String): List<ImageMetadata> {
        return try {
            val params = buildJsonObject { put("p_session_token", token) }
            client.postgrest.rpc("get_images_by_token", params)
                .decodeAs<List<ImageMetadata>>()
        } catch (e: Exception) {
            Log.e(TAG, "getImagesByToken failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadImageAtomic(
        sessionToken: String,
        id: String,
        displayOrder: Int,
        fileSize: Long,
        mimeType: String,
        imageUri: String
    ): ImageMetadata? {
        return try {
            Log.d(TAG, "upload_image_atomic RPC: id=$id, order=$displayOrder")
            val params = buildJsonObject {
                put("p_session_token", sessionToken)
                put("p_id", id)
                put("p_display_order", displayOrder)
                put("p_file_size", fileSize)
                put("p_mime_type", mimeType)
                put("p_image_uri", imageUri)
                put("p_upload_status", "completed")
            }
            val metadata = client.postgrest.rpc("upload_image_atomic", params)
                .decodeAs<ImageMetadata>()
            Log.d(TAG, "upload_image_atomic succeeded: ${metadata.id}")
            metadata
        } catch (e: Exception) {
            Log.e(TAG, "upload_image_atomic failed", e)
            null
        }
    }

    suspend fun deleteImageAndReorder(imageId: String, sessionToken: String): List<ImageMetadata>? {
        return try {
            Log.d(TAG, "delete_image_and_reorder RPC: id=$imageId")
            val params = buildJsonObject {
                put("p_session_token", sessionToken)
                put("p_image_id", imageId)
            }
            val images = client.postgrest.rpc("delete_image_and_reorder", params)
                .decodeAs<List<ImageMetadata>>()
            Log.d(TAG, "delete_image_and_reorder succeeded: ${images.size} remaining")
            images
        } catch (e: Exception) {
            Log.e(TAG, "delete_image_and_reorder failed", e)
            null
        }
    }
}

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null
