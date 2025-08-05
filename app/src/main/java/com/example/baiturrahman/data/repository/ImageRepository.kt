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
    private val storage = SupabaseClient.storage
    private val bucketName = "mosque-images" // Create this bucket in Supabase

    companion object {
        private const val TAG = "ImageRepository"
    }

    /**
     * Upload an image to Supabase Storage
     * @param uri Local URI of the image
     * @param folder Folder name in the bucket (e.g., "logos", "mosque-images")
     * @return Public URL of the uploaded image, or null if failed
     */
    suspend fun uploadImage(uri: Uri, folder: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting image upload for URI: $uri")

                // Generate unique filename
                val fileExtension = getFileExtension(uri)
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val filePath = "$folder/$fileName"

                // Read the image data
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes == null) {
                    Log.e(TAG, "Failed to read image data from URI: $uri")
                    return@withContext null
                }

                Log.d(TAG, "Image size: ${imageBytes.size} bytes")

                // Upload to Supabase Storage with correct syntax
                val uploadResponse = storage.from(bucketName).upload(filePath, imageBytes) {
                    upsert = true
                }

                Log.d(TAG, "Upload response: $uploadResponse")

                // Get public URL
                val publicUrl = storage.from(bucketName).publicUrl(filePath)

                Log.d(TAG, "Image uploaded successfully. Public URL: $publicUrl")
                return@withContext publicUrl

            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                return@withContext null
            }
        }
    }

    /**
     * Delete an image from Supabase Storage
     * @param imageUrl The public URL of the image
     * @return true if successful, false otherwise
     */
    suspend fun deleteImage(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Extract file path from URL
                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath == null) {
                    Log.e(TAG, "Could not extract file path from URL: $imageUrl")
                    return@withContext false
                }

                Log.d(TAG, "Deleting image: $filePath")

                // Delete from Supabase Storage
                val deleteResponse = storage.from(bucketName).delete(listOf(filePath))

                Log.d(TAG, "Delete response: $deleteResponse")
                Log.d(TAG, "Image deleted successfully: $filePath")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image", e)
                return@withContext false
            }
        }
    }

    /**
     * Download an image and cache it locally (optional, for offline access)
     */
    suspend fun downloadAndCacheImage(imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Extract filename from URL
                val fileName = imageUrl.substringAfterLast("/")
                val cacheDir = File(context.cacheDir, "images")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val localFile = File(cacheDir, fileName)

                // Check if already cached
                if (localFile.exists()) {
                    Log.d(TAG, "Image already cached: ${localFile.absolutePath}")
                    return@withContext localFile.absolutePath
                }

                // Download from Supabase
                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath == null) {
                    Log.e(TAG, "Could not extract file path from URL: $imageUrl")
                    return@withContext null
                }

                val imageBytes = storage.from(bucketName).downloadAuthenticated(filePath)

                // Save to cache
                FileOutputStream(localFile).use { output ->
                    output.write(imageBytes)
                }

                Log.d(TAG, "Image downloaded and cached: ${localFile.absolutePath}")
                return@withContext localFile.absolutePath

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image", e)
                return@withContext null
            }
        }
    }

    /**
     * Get file extension from URI
     */
    private fun getFileExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg" // default
        }
    }

    /**
     * Extract file path from Supabase public URL
     */
    private fun extractFilePathFromUrl(url: String): String? {
        return try {
            // Supabase storage URLs typically look like:
            // https://your-project.supabase.co/storage/v1/object/public/bucket-name/folder/filename.jpg
            val parts = url.split("/storage/v1/object/public/$bucketName/")
            if (parts.size == 2) {
                parts[1]
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting file path from URL: $url", e)
            null
        }
    }

    /**
     * List all files in a folder (useful for debugging)
     */
    suspend fun listFiles(folder: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val files = storage.from(bucketName).list(folder)
                files.map { it.name }
            } catch (e: Exception) {
                Log.e(TAG, "Error listing files in folder: $folder", e)
                emptyList()
            }
        }
    }

    /**
     * Get file info (useful for debugging)
     */
    @OptIn(ExperimentalTime::class)
    suspend fun getFileInfo(filePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileInfo = storage.from(bucketName).info(filePath)
                "File: ${fileInfo.name}, Size: ${fileInfo.metadata?.size}, Updated: ${fileInfo.updatedAt}"
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file info for: $filePath", e)
                null
            }
        }
    }
}
