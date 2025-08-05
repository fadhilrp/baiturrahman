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

class ImageRepository(private val context: Context) {
    private val bucketName = "mosque-images"

    companion object {
        private const val TAG = "ImageRepository"
    }

    /**
     * Test Supabase connection and bucket access with enhanced debugging
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
     * Upload an image to Supabase Storage with extensive debugging
     */
    @OptIn(ExperimentalTime::class)
    suspend fun uploadImage(uri: Uri, folder: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== STARTING IMAGE UPLOAD ===")
                Log.d(TAG, "URI: $uri")
                Log.d(TAG, "Folder: $folder")
                Log.d(TAG, "Bucket: $bucketName")

                // Pre-flight check: Test connection first
                Log.d(TAG, "🔍 Pre-flight check: Testing connection...")
                val connectionOk = testConnection()
                if (!connectionOk) {
                    Log.e(TAG, "❌ Pre-flight check failed - aborting upload")
                    return@withContext null
                }
                Log.d(TAG, "✅ Pre-flight check passed")

                // Check if URI is valid
                if (uri == Uri.EMPTY) {
                    Log.e(TAG, "❌ URI is empty!")
                    return@withContext null
                }

                // Generate unique filename
                val fileExtension = getFileExtension(uri)
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val filePath = "$folder/$fileName"

                Log.d(TAG, "Generated filename: $fileName")
                Log.d(TAG, "File extension: $fileExtension")
                Log.d(TAG, "Full path: $filePath")

                // Read the image data
                Log.d(TAG, "📖 Reading image data from URI...")
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

                if (inputStream == null) {
                    Log.e(TAG, "❌ Failed to open input stream from URI: $uri")
                    return@withContext null
                }

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                if (imageBytes.isEmpty()) {
                    Log.e(TAG, "❌ Image bytes are empty!")
                    return@withContext null
                }

                Log.d(TAG, "✅ Image data read successfully")
                Log.d(TAG, "📊 Image size: ${imageBytes.size} bytes (${imageBytes.size / 1024} KB)")

                // Upload to Supabase Storage
                Log.d(TAG, "⬆️ Starting upload to Supabase...")
                Log.d(TAG, "Upload details:")
                Log.d(TAG, "  - Bucket: $bucketName")
                Log.d(TAG, "  - Path: $filePath")
                Log.d(TAG, "  - Size: ${imageBytes.size} bytes")
                Log.d(TAG, "  - Upsert: true")

                val client = SupabaseClient.client
                val uploadResponse = client.storage.from(bucketName).upload(filePath, imageBytes) {
                    upsert = true
                }

                Log.d(TAG, "✅ Upload completed!")
                Log.d(TAG, "📤 Upload response: $uploadResponse")

                // Get public URL
                Log.d(TAG, "🔗 Getting public URL...")
                val publicUrl = client.storage.from(bucketName).publicUrl(filePath)

                Log.d(TAG, "✅ Public URL generated: $publicUrl")

                // Verify the upload by trying to get file info
                try {
                    Log.d(TAG, "🔍 Verifying upload...")
                    val fileList = client.storage.from(bucketName).list(folder)
                    val uploadedFile = fileList.find { it.name == fileName }

                    if (uploadedFile != null) {
                        Log.d(TAG, "✅ Upload verified! File exists in bucket")
                        Log.d(TAG, "📊 Uploaded file info:")
                        Log.d(TAG, "  - Name: ${uploadedFile.name}")
                        Log.d(TAG, "  - Size: ${uploadedFile.metadata?.size}")
                        Log.d(TAG, "  - Updated: ${uploadedFile.updatedAt}")
                    } else {
                        Log.w(TAG, "⚠️ File not found in bucket after upload")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Could not verify upload: ${e.message}")
                }

                Log.d(TAG, "=== UPLOAD COMPLETED SUCCESSFULLY ===")
                return@withContext publicUrl

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR DURING UPLOAD", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Error cause: ${e.cause}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    /**
     * Delete an image from Supabase Storage with debugging
     */
    suspend fun deleteImage(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== STARTING IMAGE DELETE ===")
                Log.d(TAG, "Image URL: $imageUrl")

                // Extract file path from URL
                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath == null) {
                    Log.e(TAG, "❌ Could not extract file path from URL: $imageUrl")
                    return@withContext false
                }

                Log.d(TAG, "Extracted file path: $filePath")

                // Delete from Supabase Storage
                Log.d(TAG, "🗑️ Deleting file: $filePath")
                val client = SupabaseClient.client
                val deleteResponse = client.storage.from(bucketName).delete(listOf(filePath))

                Log.d(TAG, "✅ Delete completed!")
                Log.d(TAG, "🗑️ Delete response: $deleteResponse")
                Log.d(TAG, "=== DELETE COMPLETED SUCCESSFULLY ===")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR DURING DELETE", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                e.printStackTrace()
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
}
