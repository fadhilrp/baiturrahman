package com.example.baiturrahman.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.baiturrahman.data.local.entity.MosqueSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueSettingsDao {
    @Query("SELECT * FROM mosque_settings WHERE id = 1")
    fun getSettings(): Flow<MosqueSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: MosqueSettings)

    @Query("DELETE FROM mosque_settings")
    suspend fun deleteAllSettings()
}

