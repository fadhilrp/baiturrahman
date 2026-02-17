package com.example.baiturrahman.utils

import android.util.Log
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Helper class to migrate existing data from local Room database to Supabase PostgreSQL
 * This is a one-time migration tool to push existing local data to the cloud
 */
class MigrationHelper(
    private val localRepository: MosqueSettingsRepository,
    private val postgresRepository: SupabasePostgresRepository,
    private val devicePreferences: DevicePreferences
) {
    companion object {
        private const val TAG = "MigrationHelper"
    }

    /**
     * Migrate all existing data from Room to PostgreSQL
     * This should be called once after setting up PostgreSQL tables
     */
    suspend fun migrateAllData(): MigrationResult {
        Log.d(TAG, "=== STARTING DATA MIGRATION ===")

        val result = MigrationResult()

        try {
            // Migrate settings
            migrateSettings(result)

            // Migrate images
            migrateImages(result)

            Log.d(TAG, "=== MIGRATION COMPLETED ===")
            Log.d(TAG, "Settings migrated: ${result.settingsMigrated}")
            Log.d(TAG, "Images migrated: ${result.imagesMigrated}")
            Log.d(TAG, "Errors: ${result.errors.size}")

            if (result.errors.isNotEmpty()) {
                Log.w(TAG, "⚠️ Migration completed with ${result.errors.size} errors:")
                result.errors.forEach { error ->
                    Log.w(TAG, "  - $error")
                }
            } else {
                Log.d(TAG, "✅ Migration completed successfully with no errors!")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error during migration", e)
            result.errors.add("Fatal error: ${e.message}")
        }

        return result
    }

    /**
     * Migrate settings from Room to PostgreSQL
     */
    private suspend fun migrateSettings(result: MigrationResult) {
        Log.d(TAG, "🔄 Migrating settings...")

        try {
            // Get local settings
            val localSettings = localRepository.mosqueSettings.first()

            if (localSettings == null) {
                Log.w(TAG, "⚠️ No local settings found to migrate")
                return
            }

            Log.d(TAG, "Found local settings: ${localSettings.mosqueName}")

            // Push to PostgreSQL
            val request = com.example.baiturrahman.data.model.UpdateMosqueSettingsRequest(
                deviceName = devicePreferences.deviceName,
                mosqueName = localSettings.mosqueName,
                mosqueLocation = localSettings.mosqueLocation,
                logoImage = localSettings.logoImage,
                prayerAddress = localSettings.prayerAddress,
                prayerTimezone = localSettings.prayerTimezone,
                quoteText = localSettings.quoteText,
                marqueeText = localSettings.marqueeText
            )

            val success = postgresRepository.updateSettings(request)

            if (success) {
                Log.d(TAG, "✅ Settings migrated successfully")
                result.settingsMigrated = true
            } else {
                Log.e(TAG, "❌ Failed to migrate settings")
                result.errors.add("Failed to migrate settings to PostgreSQL")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error migrating settings", e)
            result.errors.add("Settings migration error: ${e.message}")
        }
    }

    /**
     * Migrate images from Room to PostgreSQL using batch upsert RPC.
     * Inherently idempotent — safe to run multiple times.
     */
    private suspend fun migrateImages(result: MigrationResult) {
        Log.d(TAG, "🔄 Migrating images...")

        try {
            // Get local images
            val localImages = localRepository.mosqueImages.first()

            if (localImages.isEmpty()) {
                Log.w(TAG, "⚠️ No local images found to migrate")
                return
            }

            val deviceName = devicePreferences.deviceName
            Log.d(TAG, "Found ${localImages.size} local images to migrate for device: $deviceName")

            // Build JSON array for batch upsert
            val imagesJsonArray = buildJsonArray {
                for (localImage in localImages) {
                    val imageId = extractIdFromUrl(localImage.imageUri)
                        ?: java.util.UUID.randomUUID().toString()

                    add(buildJsonObject {
                        put("id", imageId)
                        put("device_name", deviceName)
                        put("display_order", localImage.displayOrder)
                        put("file_size", localImage.fileSize)
                        put("mime_type", localImage.mimeType)
                        put("image_uri", localImage.imageUri)
                        put("upload_status", "completed")
                    })
                }
            }

            // Try batch upsert RPC first
            val upsertCount = postgresRepository.batchUpsertImages(imagesJsonArray.toString())

            if (upsertCount >= 0) {
                Log.d(TAG, "✅ Batch upsert succeeded: $upsertCount images migrated")
                result.imagesMigrated = upsertCount
            } else {
                // Fallback: per-image migration
                Log.w(TAG, "⚠️ Batch upsert RPC failed, falling back to per-image migration")
                migrateImagesFallback(localImages, deviceName, result)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error migrating images", e)
            result.errors.add("Images migration error: ${e.message}")
        }
    }

    /**
     * Fallback per-image migration when batch RPC is unavailable
     */
    private suspend fun migrateImagesFallback(
        localImages: List<com.example.baiturrahman.data.local.entity.MosqueImage>,
        deviceName: String,
        result: MigrationResult
    ) {
        val existingRemoteImages = postgresRepository.getCompletedImages(deviceName)
        val existingUrls = existingRemoteImages.mapNotNull { it.imageUri }.toSet()

        var migratedCount = 0
        var skippedCount = 0

        for (localImage in localImages) {
            try {
                if (localImage.imageUri in existingUrls) {
                    skippedCount++
                    continue
                }

                val imageId = extractIdFromUrl(localImage.imageUri)
                    ?: java.util.UUID.randomUUID().toString()

                val createdRecord = postgresRepository.createImageRecord(
                    id = imageId,
                    deviceName = deviceName,
                    displayOrder = localImage.displayOrder,
                    fileSize = localImage.fileSize,
                    mimeType = localImage.mimeType
                )

                if (createdRecord != null) {
                    val updated = postgresRepository.updateImageUrl(imageId, localImage.imageUri)
                    if (updated) {
                        migratedCount++
                    } else {
                        result.errors.add("Failed to update image ${localImage.id}")
                    }
                } else {
                    result.errors.add("Failed to create record for image ${localImage.id}")
                }
            } catch (e: Exception) {
                result.errors.add("Image ${localImage.id} error: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Fallback migration: $migratedCount migrated, $skippedCount skipped")
        result.imagesMigrated = migratedCount
    }

    /**
     * Extract UUID from Supabase Storage URL
     * Format: https://xxx.supabase.co/storage/v1/object/public/bucket/folder/uuid.ext
     */
    private fun extractIdFromUrl(url: String): String? {
        return try {
            val parts = url.split("/")
            val filename = parts.lastOrNull() ?: return null
            // Remove extension
            filename.substringBeforeLast(".")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ID from URL: $url", e)
            null
        }
    }

    /**
     * Result of migration operation
     */
    data class MigrationResult(
        var settingsMigrated: Boolean = false,
        var imagesMigrated: Int = 0,
        val errors: MutableList<String> = mutableListOf()
    ) {
        fun isSuccess(): Boolean = settingsMigrated && errors.isEmpty()
        fun hasErrors(): Boolean = errors.isNotEmpty()
    }
}
