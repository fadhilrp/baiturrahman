# ============================================================
# Baiturrahman App — ProGuard / R8 Rules
# ============================================================

# Keep source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-dontwarn kotlin.**

# Kotlin Serialization — used by Supabase models
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn kotlinx.serialization.**

# ============================================================
# Jetpack Compose
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================
# Koin
# ============================================================
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ============================================================
# Room
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.baiturrahman.data.local.** { *; }
-dontwarn androidx.room.**

# ============================================================
# Retrofit + OkHttp
# ============================================================
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson — model fields used in Retrofit responses
-keepclassmembers class com.example.baiturrahman.data.model.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ============================================================
# Supabase Kotlin SDK + Ktor
# ============================================================
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ============================================================
# EncryptedSharedPreferences / Tink
# ============================================================
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class androidx.security.crypto.** { *; }

# ============================================================
# Coil
# ============================================================
-keep class coil3.** { *; }
-dontwarn coil3.**

# ============================================================
# App data models
# ============================================================
-keep class com.example.baiturrahman.data.model.** { *; }
-keep class com.example.baiturrahman.data.local.entity.** { *; }

# ============================================================
# Coroutines
# ============================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
