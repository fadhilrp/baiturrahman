package com.example.baiturrahman.data.repository

import android.util.Log
import com.example.baiturrahman.data.model.CreateImageRequest
import com.example.baiturrahman.data.model.ImageMetadata
import com.example.baiturrahman.data.model.MosqueSettingsRemote
import com.example.baiturrahman.data.model.UpdateImageUrlRequest
import com.example.baiturrahman.data.model.UpdateMosqueSettingsRequest
import com.example.baiturrahman.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * Repository for Supabase PostgreSQL operations
 * Handles all database CRUD operations for mosque images and settings
 */
class SupabasePostgresRepository {
    private val TAG = "SupabasePostgresRepo"
    private val client = SupabaseClient.client

    // Table names
    private companion object {
        const val IMAGES_TABLE = "mosque_images"
        const val SETTINGS_TABLE = "mosque_settings"
    }

    // ========== IMAGE OPERATIONS ==========

    /**
     * Create a new image record in PostgreSQL with "uploading" status
     * @param id UUID for the image
     * @param displayOrder Order in the slider
     * @param fileSize Size in bytes
     * @param mimeType MIME type (e.g., "image/jpeg")
     * @return ImageMetadata if successful, null otherwise
     */
    suspend fun createImageRecord(
        id: String,
        displayOrder: Int,
        fileSize: Long,
        mimeType: String
    ): ImageMetadata? {
        return try {
            Log.d(TAG, "Creating image record: id=$id, order=$displayOrder, size=$fileSize")

            val request = CreateImageRequest(
                id = id,
                displayOrder = displayOrder,
                fileSize = fileSize,
                mimeType = mimeType,
                uploadStatus = "uploading"
            )

            val result = client.from(IMAGES_TABLE)
                .insert(request) {
                    select(Columns.ALL)
                }
                .decodeSingle<ImageMetadata>()

            Log.d(TAG, "✅ Image record created: ${result.id}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating image record", e)
            null
        }
    }

    /**
     * Update image record with public URL and set status to "completed"
     * @param id UUID of the image
     * @param imageUri Public URL from Supabase Storage
     * @return true if successful
     */
    suspend fun updateImageUrl(id: String, imageUri: String): Boolean {
        return try {
            Log.d(TAG, "Updating image URL: id=$id")

            val request = UpdateImageUrlRequest(
                imageUri = imageUri,
                uploadStatus = "completed"
            )

            val result = client.from(IMAGES_TABLE)
                .update(request) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingleOrNull<ImageMetadata>()

            Log.d(TAG, "✅ Image URL updated: $id, result: ${result?.uploadStatus}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating image URL", e)
            false
        }
    }

    /**
     * Mark image upload as failed
     * @param id UUID of the image
     * @return true if successful
     */
    suspend fun markImageFailed(id: String): Boolean {
        return try {
            Log.d(TAG, "Marking image as failed: $id")

            client.from(IMAGES_TABLE)
                .update(mapOf("upload_status" to "failed")) {
                    filter {
                        eq("id", id)
                    }
                    select()
                }
                .decodeSingleOrNull<ImageMetadata>()

            Log.d(TAG, "✅ Image marked as failed: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking image as failed", e)
            false
        }
    }

    /**
     * Get all images from PostgreSQL, ordered by display_order
     * @return List of ImageMetadata
     */
    suspend fun getAllImages(): List<ImageMetadata> {
        return try {
            Log.d(TAG, "Fetching all images from PostgreSQL")

            val result = client.from(IMAGES_TABLE)
                .select {
                    order(column = "display_order", order = Order.ASCENDING)
                }
                .decodeList<ImageMetadata>()

            Log.d(TAG, "✅ Fetched ${result.size} images from PostgreSQL")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching images", e)
            emptyList()
        }
    }

    /**
     * Get images with status "completed" only
     * @return List of ImageMetadata
     */
    suspend fun getCompletedImages(): List<ImageMetadata> {
        return try {
            Log.d(TAG, "Fetching completed images from PostgreSQL")

            val result = client.from(IMAGES_TABLE)
                .select {
                    filter {
                        eq("upload_status", "completed")
                    }
                    order(column = "display_order", order = Order.ASCENDING)
                }
                .decodeList<ImageMetadata>()

            Log.d(TAG, "✅ Fetched ${result.size} completed images")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching completed images", e)
            emptyList()
        }
    }

    /**
     * Delete an image record from PostgreSQL
     * @param id UUID of the image
     * @return true if successful
     */
    suspend fun deleteImage(id: String): Boolean {
        return try {
            Log.d(TAG, "Deleting image from PostgreSQL: $id")

            client.from(IMAGES_TABLE)
                .delete {
                    filter {
                        eq("id", id)
                    }
                }

            Log.d(TAG, "✅ Image deleted from PostgreSQL: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting image", e)
            false
        }
    }

    // ========== SETTINGS OPERATIONS ==========

    /**
     * Get mosque settings from PostgreSQL
     * @return MosqueSettingsRemote if found, null otherwise
     */
    suspend fun getSettings(): MosqueSettingsRemote? {
        return try {
            Log.d(TAG, "Fetching mosque settings from PostgreSQL")

            val result = client.from(SETTINGS_TABLE)
                .select {
                    filter {
                        eq("id", 1)
                    }
                }
                .decodeSingleOrNull<MosqueSettingsRemote>()

            if (result != null) {
                Log.d(TAG, "✅ Settings fetched: ${result.mosqueName}")
            } else {
                Log.d(TAG, "⚠️ No settings found in PostgreSQL")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching settings", e)
            null
        }
    }

    /**
     * Update mosque settings in PostgreSQL
     * @param settings The settings to update
     * @return true if successful
     */
    suspend fun updateSettings(settings: UpdateMosqueSettingsRequest): Boolean {
        return try {
            Log.d(TAG, "Updating mosque settings in PostgreSQL")

            client.from(SETTINGS_TABLE)
                .update(settings) {
                    filter {
                        eq("id", 1)
                    }
                    select()
                }
                .decodeSingleOrNull<MosqueSettingsRemote>()

            Log.d(TAG, "✅ Settings updated in PostgreSQL")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating settings", e)
            false
        }
    }

    /**
     * Update only the logo image URL in settings
     * @param logoUrl Public URL of the logo
     * @return true if successful
     */
    suspend fun updateLogoImage(logoUrl: String?): Boolean {
        return try {
            Log.d(TAG, "Updating logo image in PostgreSQL: $logoUrl")

            client.from(SETTINGS_TABLE)
                .update(mapOf("logo_image" to logoUrl)) {
                    filter {
                        eq("id", 1)
                    }
                    select()
                }
                .decodeSingleOrNull<MosqueSettingsRemote>()

            Log.d(TAG, "✅ Logo image updated in PostgreSQL")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating logo image", e)
            false
        }
    }

    /**
     * Test connection to PostgreSQL
     * @return true if connection is successful
     */
    suspend fun testConnection(): Boolean {
        return try {
            Log.d(TAG, "🧪 Testing PostgreSQL connection...")

            val result = client.from(SETTINGS_TABLE)
                .select {
                    filter {
                        eq("id", 1)
                    }
                }
                .decodeSingleOrNull<MosqueSettingsRemote>()

            val success = result != null
            Log.d(TAG, if (success) "✅ PostgreSQL connection successful" else "⚠️ PostgreSQL connection failed - no data")
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ PostgreSQL connection test failed", e)
            false
        }
    }

    /**
     * Comprehensive database verification for debugging
     * Checks tables, data, and access permissions
     * @return true if everything is properly configured
     */
    suspend fun verifyDatabaseSetup(): Boolean {
        return try {
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🔍 VERIFYING DATABASE SETUP")
            Log.d(TAG, "═══════════════════════════════════════")

            var allChecksPass = true

            // Check 1: Test basic connection
            Log.d(TAG, "\n📋 CHECK 1: Basic Connection")
            try {
                val settingsResult = client.from(SETTINGS_TABLE)
                    .select {
                        filter {
                            eq("id", 1)
                        }
                    }
                    .decodeSingleOrNull<MosqueSettingsRemote>()

                if (settingsResult != null) {
                    Log.d(TAG, "✅ Connected to PostgreSQL successfully")
                    Log.d(TAG, "   Mosque: ${settingsResult.mosqueName}")
                } else {
                    Log.w(TAG, "⚠️ Connected but no settings found in database")
                    Log.w(TAG, "   This is OK if it's a fresh setup")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to connect to PostgreSQL", e)
                Log.e(TAG, "   Check your SUPABASE_URL and SUPABASE_ANON_KEY")
                allChecksPass = false
            }

            // Check 2: Verify mosque_images table access
            Log.d(TAG, "\n📋 CHECK 2: Images Table Access")
            try {
                val allImages = client.from(IMAGES_TABLE)
                    .select {
                        order(column = "display_order", order = Order.ASCENDING)
                    }
                    .decodeList<ImageMetadata>()

                Log.d(TAG, "✅ Successfully queried mosque_images table")
                Log.d(TAG, "   Total images in database: ${allImages.size}")

                // Count by status
                val completed = allImages.count { it.uploadStatus == "completed" }
                val uploading = allImages.count { it.uploadStatus == "uploading" }
                val failed = allImages.count { it.uploadStatus == "failed" }

                Log.d(TAG, "   Status breakdown:")
                Log.d(TAG, "     - Completed: $completed")
                Log.d(TAG, "     - Uploading: $uploading")
                Log.d(TAG, "     - Failed: $failed")

                // Show sample URLs
                if (completed > 0) {
                    Log.d(TAG, "\n   Sample image URLs:")
                    allImages.filter { it.uploadStatus == "completed" && it.imageUri != null }
                        .take(3)
                        .forEachIndexed { index, image ->
                            val url = image.imageUri ?: "null"
                            val preview = if (url.length > 80) {
                                url.take(50) + "..." + url.takeLast(20)
                            } else {
                                url
                            }
                            Log.d(TAG, "     ${index + 1}. $preview")
                        }
                } else if (allImages.isNotEmpty()) {
                    Log.w(TAG, "   ⚠️ Images exist but none are completed")
                    Log.w(TAG, "   Check ImageRepository upload logs")
                } else {
                    Log.w(TAG, "   ⚠️ No images in database yet")
                    Log.w(TAG, "   Upload an image to test the flow")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to query mosque_images table", e)
                Log.e(TAG, "   Error: ${e.message}")
                Log.e(TAG, "   Possible causes:")
                Log.e(TAG, "     1. Table doesn't exist (run SUPABASE_SETUP.md SQL)")
                Log.e(TAG, "     2. RLS policy blocks anon role SELECT")
                Log.e(TAG, "     3. Column names mismatch (check snake_case)")
                allChecksPass = false
            }

            // Check 3: Test INSERT permission (without actually inserting)
            Log.d(TAG, "\n📋 CHECK 3: RLS Policies")
            Log.d(TAG, "   Note: Actual INSERT test requires creating a record")
            Log.d(TAG, "   If uploads fail, check RLS policies in Supabase Dashboard:")
            Log.d(TAG, "     - Table Editor → mosque_images → RLS Policies")
            Log.d(TAG, "     - Must allow 'anon' role for SELECT, INSERT, UPDATE, DELETE")

            // Check 4: Verify completed images have valid URLs
            Log.d(TAG, "\n📋 CHECK 4: Image URL Validation")
            try {
                val completedImages = client.from(IMAGES_TABLE)
                    .select {
                        filter {
                            eq("upload_status", "completed")
                        }
                    }
                    .decodeList<ImageMetadata>()

                val imagesWithNullUrl = completedImages.count { it.imageUri == null || it.imageUri.isBlank() }
                val imagesWithValidUrl = completedImages.count {
                    it.imageUri != null && it.imageUri.startsWith("https://")
                }

                if (completedImages.isNotEmpty()) {
                    Log.d(TAG, "   Completed images: ${completedImages.size}")
                    Log.d(TAG, "   With valid URLs: $imagesWithValidUrl")

                    if (imagesWithNullUrl > 0) {
                        Log.w(TAG, "   ⚠️ ${imagesWithNullUrl} completed images have null/blank URLs")
                        Log.w(TAG, "   This indicates upload flow issues")
                        allChecksPass = false
                    } else {
                        Log.d(TAG, "   ✅ All completed images have URLs")
                    }
                } else {
                    Log.d(TAG, "   No completed images to validate")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed URL validation", e)
                allChecksPass = false
            }

            // Summary
            Log.d(TAG, "\n═══════════════════════════════════════")
            if (allChecksPass) {
                Log.d(TAG, "✅ DATABASE SETUP VERIFICATION PASSED")
                Log.d(TAG, "   All checks completed successfully")
            } else {
                Log.e(TAG, "❌ DATABASE SETUP VERIFICATION FAILED")
                Log.e(TAG, "   Review errors above and fix issues")
                Log.e(TAG, "   Common fixes:")
                Log.e(TAG, "     1. Run SQL from SUPABASE_SETUP.md")
                Log.e(TAG, "     2. Update RLS policies for anon role")
                Log.e(TAG, "     3. Verify credentials in SupabaseClient.kt")
            }
            Log.d(TAG, "═══════════════════════════════════════\n")

            allChecksPass
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database verification crashed", e)
            false
        }
    }
}
