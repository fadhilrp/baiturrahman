package com.example.baiturrahman.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.baiturrahman.data.local.dao.MosqueImageDao
import com.example.baiturrahman.data.local.dao.MosqueSettingsDao
import com.example.baiturrahman.data.local.entity.MosqueImage
import com.example.baiturrahman.data.local.entity.MosqueSettings

@Database(
    entities = [MosqueSettings::class, MosqueImage::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mosqueSettingsDao(): MosqueSettingsDao
    abstract fun mosqueImageDao(): MosqueImageDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 - add new fields to mosque_images
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                Log.d(TAG, "Migrating database from version 1 to 2")

                // Add new columns to mosque_images table
                database.execSQL("ALTER TABLE mosque_images ADD COLUMN uploadDate INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE mosque_images ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE mosque_images ADD COLUMN mimeType TEXT NOT NULL DEFAULT 'image/jpeg'")
                database.execSQL("ALTER TABLE mosque_images ADD COLUMN uploadStatus TEXT NOT NULL DEFAULT 'completed'")
                database.execSQL("ALTER TABLE mosque_images ADD COLUMN supabaseId TEXT")

                Log.d(TAG, "Migration completed successfully")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baiturrahman_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.d(TAG, "Database instance created successfully")
                instance
            }
        }
    }
}

