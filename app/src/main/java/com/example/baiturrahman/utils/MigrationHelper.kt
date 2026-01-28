package com.example.baiturrahman.utils

import android.util.Log
import com.example.baiturrahman.data.repository.MosqueSettingsRepository
import com.example.baiturrahman.data.repository.SupabasePostgresRepository
import kotlinx.coroutines.flow.first

/**
 * Helper class to migrate existing data from local Room database to Supabase PostgreSQL
 * This is a one-time migration tool to push existing local data to the cloud
 */
class MigrationHelper(
    private val localRepository: MosqueSettingsRepository,
    private val postgresRepository: SupabasePostgresRepository
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
     * Migrate images from Room to PostgreSQL
     * Creates PostgreSQL records for existing images that are already in Supabase Storage
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

            Log.d(TAG, "Found ${localImages.size} local images to migrate")

            // First, check if images already exist in PostgreSQL
            val existingRemoteImages = postgresRepository.getCompletedImages()
            val existingUrls = existingRemoteImages.mapNotNull { it.imageUri }.toSet()

            Log.d(TAG, "Found ${existingRemoteImages.size} images already in PostgreSQL")

            var migratedCount = 0
            var skippedCount = 0

            for (localImage in localImages) {
                try {
                    // Skip if already in PostgreSQL
                    if (localImage.imageUri in existingUrls) {
                        Log.d(TAG, "⏭️ Skipping image (already in PostgreSQL): ${localImage.imageUri}")
                        skippedCount++
                        continue
                    }

                    Log.d(TAG, "Creating PostgreSQL record for: ${localImage.imageUri}")

                    // Extract UUID from URL (assuming format: .../folder/uuid.ext)
                    val imageId = extractIdFromUrl(localImage.imageUri) ?: java.util.UUID.randomUUID().toString()

                    // Create record in PostgreSQL with "completed" status since image already exists in Storage
                    val createdRecord = postgresRepository.createImageRecord(
                        id = imageId,
                        displayOrder = localImage.displayOrder,
                        fileSize = localImage.fileSize,
                        mimeType = localImage.mimeType
                    )

                    if (createdRecord != null) {
                        // Update with URL immediately since image already exists
                        val updated = postgresRepository.updateImageUrl(imageId, localImage.imageUri)
                        if (updated) {
                            Log.d(TAG, "✅ Image migrated: ${localImage.displayOrder}")
                            migratedCount++
                        } else {
                            Log.e(TAG, "❌ Failed to update image URL in PostgreSQL")
                            result.errors.add("Failed to update image ${localImage.id}")
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to create image record in PostgreSQL")
                        result.errors.add("Failed to create record for image ${localImage.id}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error migrating image ${localImage.id}", e)
                    result.errors.add("Image ${localImage.id} error: ${e.message}")
                }
            }

            Log.d(TAG, "✅ Images migration completed: $migratedCount migrated, $skippedCount skipped")
            result.imagesMigrated = migratedCount

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error migrating images", e)
            result.errors.add("Images migration error: ${e.message}")
        }
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
