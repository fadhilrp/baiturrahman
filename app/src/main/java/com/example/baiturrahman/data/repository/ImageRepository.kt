package com.example.baiturrahman.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.baiturrahman.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.time.ExperimentalTime

/**
 * Result of a successful image upload containing both the public URL and the Supabase UUID.
 */
data class UploadResult(val publicUrl: String, val supabaseId: String)

/**
 * Repository for handling image uploads to Supabase
 * NEW ARCHITECTURE: Creates PostgreSQL record -> Uploads to Storage -> Updates record with URL
 */
class ImageRepository(
    private val context: Context,
    private val postgresRepository: SupabasePostgresRepository
) {
    private val bucketName = "mosque-images"

    companion object {
        private const val TAG = "ImageRepository"
    }

    /**
     * Upload image with new architecture:
     * 1. Create PostgreSQL record (status: uploading)
     * 2. Upload to Supabase Storage
     * 3. Update PostgreSQL with URL (status: completed)
     * 4. Return public URL
     *
     * @param uri Local URI of the image
     * @param folder Folder in storage (e.g., "logos" or "mosque-images")
     * @param displayOrder Order for slider (0 if logo)
     * @param deviceName Device name for device-specific data
     * @return Public URL if successful, null otherwise
     */
    @OptIn(ExperimentalTime::class)
    suspend fun uploadImage(uri: Uri, folder: String, displayOrder: Int = 0, deviceName: String = ""): UploadResult? {
        return withContext(Dispatchers.IO) {
            var imageId: String? = null

            try {
                Log.d(TAG, "=== NEW UPLOAD FLOW STARTED ===")
                Log.d(TAG, "URI: $uri")
                Log.d(TAG, "Folder: $folder")
                Log.d(TAG, "Display order: $displayOrder")

                // Validate URI
                if (uri == Uri.EMPTY) {
                    Log.e(TAG, "❌ URI is empty!")
                    return@withContext null
                }

                // Get file info
                val fileExtension = getFileExtension(uri)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                // Read image data
                Log.d(TAG, "📖 Reading image data...")
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "❌ Failed to open input stream")
                    return@withContext null
                }

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                if (imageBytes.isEmpty()) {
                    Log.e(TAG, "❌ Image bytes are empty!")
                    return@withContext null
                }

                Log.d(TAG, "✅ Image read: ${imageBytes.size} bytes (${imageBytes.size / 1024} KB)")

                // STEP 1: Upload to Supabase Storage (idempotent with upsert=true)
                imageId = UUID.randomUUID().toString()
                val fileName = "$imageId.$fileExtension"
                val filePath = "$folder/$fileName"

                Log.d(TAG, "⬆️ Step 1: Uploading to Supabase Storage (ID: $imageId)")
                Log.d(TAG, "File path: $filePath")

                val client = SupabaseClient.client
                client.storage.from(bucketName).upload(filePath, imageBytes) {
                    upsert = true
                }

                Log.d(TAG, "✅ Upload to Storage completed")

                // STEP 2: Get public URL
                val publicUrl = client.storage.from(bucketName).publicUrl(filePath)
                Log.d(TAG, "🔗 Public URL: $publicUrl")

                // STEP 3: Create completed PostgreSQL record atomically via RPC
                Log.d(TAG, "📝 Step 3: Creating PostgreSQL record via uploadImageAtomic RPC")

                val atomicResult = postgresRepository.uploadImageAtomic(
                    id = imageId,
                    deviceName = deviceName,
                    displayOrder = displayOrder,
                    fileSize = imageBytes.size.toLong(),
                    mimeType = mimeType,
                    imageUri = publicUrl
                )

                if (atomicResult != null) {
                    Log.d(TAG, "✅ Atomic upload RPC succeeded: ${atomicResult.id}")
                } else {
                    // Fallback: two-step createImageRecord + updateImageUrl
                    Log.w(TAG, "⚠️ Atomic RPC failed, falling back to two-step flow")

                    val createdRecord = postgresRepository.createImageRecord(
                        id = imageId,
                        deviceName = deviceName,
                        displayOrder = displayOrder,
                        fileSize = imageBytes.size.toLong(),
                        mimeType = mimeType
                    )

                    if (createdRecord != null) {
                        val updateSuccess = postgresRepository.updateImageUrl(imageId, publicUrl)
                        if (!updateSuccess) {
                            Log.w(TAG, "⚠️ Failed to update PostgreSQL record with URL")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Failed to create PostgreSQL record via fallback")
                    }
                }

                Log.d(TAG, "=== UPLOAD FLOW COMPLETED SUCCESSFULLY ===")

                return@withContext UploadResult(publicUrl = publicUrl, supabaseId = imageId)

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR DURING UPLOAD", e)

                // Mark as failed in PostgreSQL if record was created
                if (imageId != null) {
                    try {
                        postgresRepository.markImageFailed(imageId)
                        Log.d(TAG, "PostgreSQL record marked as failed")
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed to mark record as failed", ex)
                    }
                }

                return@withContext null
            }
        }
    }

    /**
     * Delete image from both PostgreSQL (source of truth) and Storage.
     * Remote-first: deletes from PostgreSQL first, then Storage.
     * Uses deleteImageAndReorder RPC for atomic delete + reorder when deviceName is available.
     */
    suspend fun deleteImage(imageUrl: String, supabaseId: String? = null, deviceName: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== DELETING IMAGE ===")
                Log.d(TAG, "URL: $imageUrl")
                Log.d(TAG, "Supabase ID: $supabaseId")
                Log.d(TAG, "Device: $deviceName")

                var success = true

                // STEP 1: Delete from PostgreSQL first (source of truth)
                val postgresId = supabaseId ?: extractIdFromUrl(imageUrl)
                if (postgresId != null) {
                    if (deviceName.isNotEmpty()) {
                        // Try atomic delete + reorder RPC
                        Log.d(TAG, "🗑️ Deleting from PostgreSQL via deleteImageAndReorder RPC")
                        val remaining = postgresRepository.deleteImageAndReorder(postgresId, deviceName)
                        if (remaining != null) {
                            Log.d(TAG, "✅ Atomic delete + reorder succeeded: ${remaining.size} images remaining")
                        } else {
                            // Fallback to simple delete
                            Log.w(TAG, "⚠️ RPC failed, falling back to simple delete")
                            val deleted = postgresRepository.deleteImage(postgresId)
                            if (!deleted) {
                                Log.w(TAG, "⚠️ Failed to delete from PostgreSQL")
                                success = false
                            }
                        }
                    } else {
                        // No deviceName, use simple delete
                        Log.d(TAG, "🗑️ Deleting from PostgreSQL: $postgresId")
                        val deleted = postgresRepository.deleteImage(postgresId)
                        if (!deleted) {
                            Log.w(TAG, "⚠️ Failed to delete from PostgreSQL")
                            success = false
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Could not determine PostgreSQL ID for deletion")
                }

                // STEP 2: Delete from Storage (orphaned file is harmless)
                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath != null) {
                    try {
                        Log.d(TAG, "🗑️ Deleting from Storage: $filePath")
                        val client = SupabaseClient.client
                        client.storage.from(bucketName).delete(listOf(filePath))
                        Log.d(TAG, "✅ Deleted from Storage")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to delete from Storage (orphaned file is harmless)", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ Could not extract file path from URL")
                }

                Log.d(TAG, "=== DELETE COMPLETED (success: $success) ===")
                return@withContext success

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR DURING DELETE", e)
                return@withContext false
            }
        }
    }

    /**
     * Upload a logo to Supabase Storage only — no PostgreSQL record created.
     * Logos are stored in the "logos/" folder.
     * @param uri Local URI of the logo image
     * @return Public URL if successful, null otherwise
     */
    @OptIn(ExperimentalTime::class)
    suspend fun uploadLogoToStorage(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== UPLOADING LOGO TO STORAGE ONLY ===")
                Log.d(TAG, "URI: $uri")

                if (uri == Uri.EMPTY) {
                    Log.e(TAG, "❌ URI is empty!")
                    return@withContext null
                }

                val fileExtension = getFileExtension(uri)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "❌ Failed to open input stream")
                    return@withContext null
                }

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                if (imageBytes.isEmpty()) {
                    Log.e(TAG, "❌ Image bytes are empty!")
                    return@withContext null
                }

                val logoId = UUID.randomUUID().toString()
                val fileName = "$logoId.$fileExtension"
                val filePath = "logos/$fileName"

                Log.d(TAG, "⬆️ Uploading logo to Storage: $filePath")

                val client = SupabaseClient.client
                client.storage.from(bucketName).upload(filePath, imageBytes) {
                    upsert = true
                }

                val publicUrl = client.storage.from(bucketName).publicUrl(filePath)
                Log.d(TAG, "✅ Logo uploaded (storage only): $publicUrl")
                return@withContext publicUrl

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error uploading logo to storage", e)
                return@withContext null
            }
        }
    }

    /**
     * Delete a logo from Supabase Storage only — no PostgreSQL record involved.
     * @param imageUrl Public URL of the logo to delete
     * @return true if successful
     */
    suspend fun deleteLogoFromStorage(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== DELETING LOGO FROM STORAGE ONLY ===")
                Log.d(TAG, "URL: $imageUrl")

                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath == null) {
                    Log.e(TAG, "❌ Could not extract file path from URL: $imageUrl")
                    return@withContext false
                }

                val client = SupabaseClient.client
                client.storage.from(bucketName).delete(listOf(filePath))
                Log.d(TAG, "✅ Logo deleted from storage: $filePath")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting logo from storage", e)
                return@withContext false
            }
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
            // Remove extension to get UUID
            val id = filename.substringBeforeLast(".")
            Log.d(TAG, "Extracted PostgreSQL ID from URL: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ID from URL: $url", e)
            null
        }
    }

    /**
     * Test Supabase connection and bucket access
     */
    @OptIn(ExperimentalTime::class)
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== TESTING SUPABASE CONNECTION ===")

                // Test 1: Basic client validation
                Log.d(TAG, "🧪 Test 1: Validating client configuration...")
                val client = SupabaseClient.client
                Log.d(TAG, "✅ Client created successfully")

                // Test 2: List buckets
                Log.d(TAG, "🧪 Test 2: Listing buckets...")
                val buckets = client.storage.retrieveBuckets()
                Log.d(TAG, "✅ Buckets retrieved: ${buckets.size}")

                if (buckets.isEmpty()) {
                    Log.w(TAG, "⚠️ No buckets found! This might indicate:")
                    Log.w(TAG, "   - Wrong credentials")
                    Log.w(TAG, "   - No buckets created yet")
                    Log.w(TAG, "   - Permission issues")
                } else {
                    Log.d(TAG, "📋 Available buckets:")
                    buckets.forEach { bucket ->
                        Log.d(TAG, "  - ${bucket.name} (ID: ${bucket.id})")
                        Log.d(TAG, "    Public: ${bucket.public}")
                        Log.d(TAG, "    Created: ${bucket.createdAt}")
                    }
                }

                // Test 3: Check if our bucket exists
                Log.d(TAG, "🧪 Test 3: Checking bucket '$bucketName'...")
                val bucketExists = buckets.any { it.name == bucketName }
                if (bucketExists) {
                    Log.d(TAG, "✅ Bucket '$bucketName' exists")

                    val bucket = buckets.find { it.name == bucketName }
                    Log.d(TAG, "📊 Bucket details:")
                    Log.d(TAG, "  - Name: ${bucket?.name}")
                    Log.d(TAG, "  - Public: ${bucket?.public}")
                    Log.d(TAG, "  - ID: ${bucket?.id}")
                } else {
                    Log.e(TAG, "❌ Bucket '$bucketName' does not exist!")
                    Log.e(TAG, "💡 To fix this:")
                    Log.e(TAG, "   1. Go to your Supabase dashboard")
                    Log.e(TAG, "   2. Click 'Storage' in the sidebar")
                    Log.e(TAG, "   3. Click 'Create a new bucket'")
                    Log.e(TAG, "   4. Name it: '$bucketName'")
                    Log.e(TAG, "   5. Make it PUBLIC")
                    Log.e(TAG, "   6. Click 'Create bucket'")
                    return@withContext false
                }

                // Test 4: Try to list files in bucket (if bucket exists)
                Log.d(TAG, "🧪 Test 4: Testing bucket access...")
                try {
                    val files = client.storage.from(bucketName).list()
                    Log.d(TAG, "✅ Bucket access successful! Files in bucket: ${files.size}")
                    if (files.isNotEmpty()) {
                        files.take(3).forEach { file ->
                            Log.d(TAG, "  - ${file.name} (${file.metadata?.size} bytes)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Bucket access failed: ${e.message}")
                    Log.e(TAG, "💡 This might indicate permission issues")
                    Log.e(TAG, "   Check your bucket policies in Supabase dashboard")
                    return@withContext false
                }

                Log.d(TAG, "=== CONNECTION TEST PASSED ===")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "❌ CONNECTION TEST FAILED", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")

                // Provide specific guidance based on error type
                when {
                    e.message?.contains("401") == true -> {
                        Log.e(TAG, "💡 401 Error suggests invalid credentials")
                        Log.e(TAG, "   Check your SUPABASE_URL and SUPABASE_ANON_KEY")
                    }
                    e.message?.contains("404") == true -> {
                        Log.e(TAG, "💡 404 Error suggests wrong URL or project doesn't exist")
                        Log.e(TAG, "   Verify your SUPABASE_URL is correct")
                    }
                    e.message?.contains("network") == true || e.message?.contains("timeout") == true -> {
                        Log.e(TAG, "💡 Network error - check internet connection")
                    }
                    else -> {
                        Log.e(TAG, "💡 Unknown error - check credentials and network")
                    }
                }

                return@withContext false
            }
        }
    }


    /**
     * Download an image and cache it locally (with debugging)
     */
    suspend fun downloadAndCacheImage(imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== DOWNLOADING AND CACHING IMAGE ===")
                Log.d(TAG, "Image URL: $imageUrl")

                // Extract filename from URL
                val fileName = imageUrl.substringAfterLast("/")
                Log.d(TAG, "File name: $fileName")

                val cacheDir = File(context.cacheDir, "images")
                if (!cacheDir.exists()) {
                    Log.d(TAG, "📁 Creating cache directory...")
                    cacheDir.mkdirs()
                }

                val localFile = File(cacheDir, fileName)
                Log.d(TAG, "Local file path: ${localFile.absolutePath}")

                // Check if already cached
                if (localFile.exists()) {
                    Log.d(TAG, "✅ Image already cached: ${localFile.absolutePath}")
                    return@withContext localFile.absolutePath
                }

                // Download from Supabase
                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath == null) {
                    Log.e(TAG, "❌ Could not extract file path from URL: $imageUrl")
                    return@withContext null
                }

                Log.d(TAG, "⬇️ Downloading from Supabase...")
                val client = SupabaseClient.client
                val imageBytes = client.storage.from(bucketName).downloadAuthenticated(filePath)
                Log.d(TAG, "✅ Downloaded ${imageBytes.size} bytes")

                // Save to cache
                Log.d(TAG, "💾 Saving to cache...")
                FileOutputStream(localFile).use { output ->
                    output.write(imageBytes)
                }

                Log.d(TAG, "✅ Image cached successfully: ${localFile.absolutePath}")
                Log.d(TAG, "=== DOWNLOAD AND CACHE COMPLETED ===")
                return@withContext localFile.absolutePath

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR DURING DOWNLOAD AND CACHE", e)
                return@withContext null
            }
        }
    }

    /**
     * Get file extension from URI with debugging
     */
    private fun getFileExtension(uri: Uri): String {
        Log.d(TAG, "🔍 Getting file extension for URI: $uri")

        val mimeType = context.contentResolver.getType(uri)
        Log.d(TAG, "📄 MIME type: $mimeType")

        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/bmp" -> "bmp"
            else -> {
                Log.w(TAG, "⚠️ Unknown MIME type: $mimeType, defaulting to jpg")
                "jpg"
            }
        }

        Log.d(TAG, "📄 File extension: $extension")
        return extension
    }

    /**
     * Extract file path from Supabase public URL with debugging
     */
    private fun extractFilePathFromUrl(url: String): String? {
        return try {
            Log.d(TAG, "🔍 Extracting file path from URL: $url")

            val searchPattern = "/storage/v1/object/public/$bucketName/"
            val parts = url.split(searchPattern)

            Log.d(TAG, "🔍 URL parts: ${parts.size}")
            parts.forEachIndexed { index, part ->
                Log.d(TAG, "  Part $index: $part")
            }

            if (parts.size == 2) {
                val filePath = parts[1]
                Log.d(TAG, "✅ Extracted file path: $filePath")
                return filePath
            } else {
                Log.e(TAG, "❌ Could not split URL properly")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting file path from URL: $url", e)
            null
        }
    }

    /**
     * Comprehensive bucket verification for debugging
     * Checks bucket existence, permissions, and file access
     * @return true if bucket is properly configured
     */
    @OptIn(ExperimentalTime::class)
    suspend fun verifyBucketAccess(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "🗄️ VERIFYING STORAGE BUCKET ACCESS")
                Log.d(TAG, "═══════════════════════════════════════")

                var allChecksPass = true
                val client = SupabaseClient.client

                // Check 1: List all buckets
                Log.d(TAG, "\n📋 CHECK 1: Bucket Existence")
                try {
                    val buckets = client.storage.retrieveBuckets()
                    Log.d(TAG, "✅ Successfully listed buckets: ${buckets.size} total")

                    val targetBucket = buckets.find { it.name == bucketName }
                    if (targetBucket != null) {
                        Log.d(TAG, "✅ Bucket '$bucketName' found")
                        Log.d(TAG, "   ID: ${targetBucket.id}")
                        Log.d(TAG, "   Public: ${targetBucket.public}")
                        Log.d(TAG, "   Created: ${targetBucket.createdAt}")

                        if (!targetBucket.public) {
                            Log.e(TAG, "❌ CRITICAL: Bucket is NOT PUBLIC!")
                            Log.e(TAG, "   Images won't load without authentication")
                            Log.e(TAG, "   Fix: Supabase Dashboard → Storage → $bucketName → Settings → Make Public")
                            allChecksPass = false
                        } else {
                            Log.d(TAG, "   ✅ Bucket is PUBLIC (anonymous access allowed)")
                        }
                    } else {
                        Log.e(TAG, "❌ Bucket '$bucketName' NOT FOUND")
                        Log.e(TAG, "   Available buckets:")
                        buckets.forEach { bucket ->
                            Log.e(TAG, "     - ${bucket.name}")
                        }
                        Log.e(TAG, "   Fix: Create bucket '$bucketName' in Supabase Dashboard")
                        allChecksPass = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to list buckets", e)
                    Log.e(TAG, "   Error: ${e.message}")
                    allChecksPass = false
                }

                // Check 2: Test bucket read access
                Log.d(TAG, "\n📋 CHECK 2: Bucket Read Access")
                try {
                    val files = client.storage.from(bucketName).list()
                    Log.d(TAG, "✅ Successfully accessed bucket")
                    Log.d(TAG, "   Files/folders in root: ${files.size}")

                    if (files.isNotEmpty()) {
                        Log.d(TAG, "   Sample items:")
                        files.take(5).forEach { file ->
                            val size = file.metadata?.size?.let { "${it / 1024} KB" } ?: "unknown"
                            Log.d(TAG, "     - ${file.name} ($size)")
                        }
                    } else {
                        Log.w(TAG, "   ⚠️ Bucket is empty (no files uploaded yet)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to access bucket contents", e)
                    Log.e(TAG, "   Error: ${e.message}")
                    Log.e(TAG, "   This might indicate permission issues")
                    allChecksPass = false
                }

                // Check 3: Test public URL generation
                Log.d(TAG, "\n📋 CHECK 3: Public URL Generation")
                try {
                    val testPath = "mosque-images/test.jpg"
                    val publicUrl = client.storage.from(bucketName).publicUrl(testPath)
                    Log.d(TAG, "✅ Can generate public URLs")
                    Log.d(TAG, "   Sample URL format:")
                    Log.d(TAG, "   ${publicUrl.take(80)}...")

                    // Validate URL format
                    if (publicUrl.contains("/storage/v1/object/public/$bucketName/")) {
                        Log.d(TAG, "   ✅ URL format is correct")
                    } else {
                        Log.w(TAG, "   ⚠️ Unexpected URL format")
                        Log.w(TAG, "   Expected pattern: /storage/v1/object/public/$bucketName/")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to generate public URL", e)
                    allChecksPass = false
                }

                // Check 4: List files in subfolders
                Log.d(TAG, "\n📋 CHECK 4: Subfolder Access")
                val subfolders = listOf("logos", "mosque-images")
                for (folder in subfolders) {
                    try {
                        val folderFiles = client.storage.from(bucketName).list(folder)
                        Log.d(TAG, "   ✅ $folder/: ${folderFiles.size} files")

                        if (folderFiles.isNotEmpty()) {
                            // Show first file URL
                            val firstFile = folderFiles.first()
                            val filePath = "$folder/${firstFile.name}"
                            val publicUrl = client.storage.from(bucketName).publicUrl(filePath)
                            val preview = if (publicUrl.length > 80) {
                                publicUrl.take(50) + "..." + publicUrl.takeLast(20)
                            } else {
                                publicUrl
                            }
                            Log.d(TAG, "      Sample: $preview")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "   ⚠️ $folder/: ${e.message}")
                    }
                }

                // Summary
                Log.d(TAG, "\n═══════════════════════════════════════")
                if (allChecksPass) {
                    Log.d(TAG, "✅ STORAGE BUCKET VERIFICATION PASSED")
                    Log.d(TAG, "   Bucket is properly configured")
                } else {
                    Log.e(TAG, "❌ STORAGE BUCKET VERIFICATION FAILED")
                    Log.e(TAG, "   Review errors above and fix issues")
                    Log.e(TAG, "   Common fixes:")
                    Log.e(TAG, "     1. Create bucket '$bucketName' if missing")
                    Log.e(TAG, "     2. Make bucket PUBLIC")
                    Log.e(TAG, "     3. Update storage RLS policies for anon role")
                }
                Log.d(TAG, "═══════════════════════════════════════\n")

                return@withContext allChecksPass
            } catch (e: Exception) {
                Log.e(TAG, "❌ Bucket verification crashed", e)
                return@withContext false
            }
        }
    }
}
