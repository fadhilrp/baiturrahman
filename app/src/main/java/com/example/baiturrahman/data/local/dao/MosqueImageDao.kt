package com.example.baiturrahman.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.baiturrahman.data.local.entity.MosqueImage
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueImageDao {
    @Query("SELECT * FROM mosque_images ORDER BY displayOrder ASC")
    fun getAllImages(): Flow<List<MosqueImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: MosqueImage)

    @Query("DELETE FROM mosque_images WHERE id = :imageId")
    suspend fun deleteImage(imageId: Int)

    @Query("DELETE FROM mosque_images")
    suspend fun deleteAllImages()

    @Query("SELECT COUNT(*) FROM mosque_images")
    suspend fun getImageCount(): Int

    @Query("UPDATE mosque_images SET displayOrder = :newOrder WHERE id = :imageId")
    suspend fun updateImageOrder(imageId: Int, newOrder: Int)
}

