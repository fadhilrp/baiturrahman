package com.example.baiturrahman.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.baiturrahman.data.remote.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import kotlin.time.ExperimentalTime

/**
 * Result of a successful image upload containing both the public URL and the Supabase UUID.
 */
data class UploadResult(val publicUrl: String, val supabaseId: String)

/**
 * Repository for handling image uploads to Supabase Storage and PostgreSQL.
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
     * Upload mosque image to Storage and record in PostgreSQL via RPC.
     * @param sessionToken Auth token used by the RPC to associate the image with the account
     */
    @OptIn(ExperimentalTime::class)
    suspend fun uploadImage(
        uri: Uri,
        folder: String,
        displayOrder: Int = 0,
        sessionToken: String = ""
    ): UploadResult? {
        return withContext(Dispatchers.IO) {
            var imageId: String?
            try {
                Log.d(TAG, "=== UPLOAD STARTED: folder=$folder, order=$displayOrder ===")

                if (uri == Uri.EMPTY) {
                    Log.e(TAG, "URI is empty")
                    return@withContext null
                }

                val fileExtension = getFileExtension(uri)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                val imageBytes = inputStream.readBytes()
                inputStream.close()

                if (imageBytes.isEmpty()) {
                    Log.e(TAG, "Image bytes are empty")
                    return@withContext null
                }

                imageId = UUID.randomUUID().toString()
                val filePath = "$folder/$imageId.$fileExtension"

                Log.d(TAG, "Uploading to Storage: $filePath")
                val client = SupabaseClient.client
                client.storage.from(bucketName).upload(filePath, imageBytes) { upsert = true }

                val publicUrl = client.storage.from(bucketName).publicUrl(filePath)
                Log.d(TAG, "Storage upload done: $publicUrl")

                val atomicResult = postgresRepository.uploadImageAtomic(
                    sessionToken = sessionToken,
                    id = imageId,
                    displayOrder = displayOrder,
                    fileSize = imageBytes.size.toLong(),
                    mimeType = mimeType,
                    imageUri = publicUrl
                )

                if (atomicResult != null) {
                    Log.d(TAG, "uploadImageAtomic succeeded: ${atomicResult.id}")
                } else {
                    Log.w(TAG, "uploadImageAtomic returned null — image stored in Storage but not DB")
                }

                Log.d(TAG, "=== UPLOAD COMPLETE ===")
                UploadResult(publicUrl = publicUrl, supabaseId = imageId)

            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                null
            }
        }
    }

    /**
     * Delete image from Storage and PostgreSQL using the atomic RPC.
     */
    suspend fun deleteImage(
        imageUrl: String,
        supabaseId: String? = null,
        sessionToken: String = ""
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== DELETE: $imageUrl (id=$supabaseId) ===")

                val postgresId = supabaseId ?: extractIdFromUrl(imageUrl)
                if (postgresId != null) {
                    val remaining = postgresRepository.deleteImageAndReorder(postgresId, sessionToken)
                    if (remaining != null) {
                        Log.d(TAG, "deleteImageAndReorder OK: ${remaining.size} remaining")
                    } else {
                        Log.w(TAG, "deleteImageAndReorder returned null")
                    }
                }

                val filePath = extractFilePathFromUrl(imageUrl)
                if (filePath != null) {
                    try {
                        SupabaseClient.client.storage.from(bucketName).delete(listOf(filePath))
                        Log.d(TAG, "Storage delete OK: $filePath")
                    } catch (e: Exception) {
                        Log.w(TAG, "Storage delete failed (harmless orphan)", e)
                    }
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Delete error", e)
                false
            }
        }
    }

    /**
     * Upload a logo to Storage only — no PostgreSQL record.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun uploadLogoToStorage(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (uri == Uri.EMPTY) return@withContext null

                val fileExtension = getFileExtension(uri)
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                val imageBytes = inputStream.readBytes()
                inputStream.close()

                if (imageBytes.isEmpty()) return@withContext null

                val logoId = UUID.randomUUID().toString()
                val filePath = "logos/$logoId.$fileExtension"

                val client = SupabaseClient.client
                client.storage.from(bucketName).upload(filePath, imageBytes) { upsert = true }

                val publicUrl = client.storage.from(bucketName).publicUrl(filePath)
                Log.d(TAG, "Logo uploaded: $publicUrl")
                publicUrl

            } catch (e: Exception) {
                Log.e(TAG, "Logo upload error", e)
                null
            }
        }
    }

    /**
     * Delete a logo from Storage only.
     */
    suspend fun deleteLogoFromStorage(imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = extractFilePathFromUrl(imageUrl) ?: return@withContext false
                SupabaseClient.client.storage.from(bucketName).delete(listOf(filePath))
                Log.d(TAG, "Logo deleted: $filePath")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Logo delete error", e)
                false
            }
        }
    }

    private fun extractIdFromUrl(url: String): String? {
        return try {
            val filename = url.split("/").lastOrNull() ?: return null
            filename.substringBeforeLast(".")
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return when (context.contentResolver.getType(uri)) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    }

    private fun extractFilePathFromUrl(url: String): String? {
        return try {
            val parts = url.split("/storage/v1/object/public/$bucketName/")
            if (parts.size == 2) parts[1] else null
        } catch (_: Exception) {
            null
        }
    }
}
