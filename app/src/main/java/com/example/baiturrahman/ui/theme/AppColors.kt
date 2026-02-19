package com.example.baiturrahman.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val secondary: Color,
    val border: Color,
    val glassWhite: Color,
    val glassBorder: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = DarkBackground,
    surface = DarkSurface,
    secondary = Secondary,
    border = Border,
    glassWhite = GlassWhite,
    glassBorder = GlassBorder,
    foreground = Foreground,
    mutedForeground = MutedForeground,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    secondary = Color(0xFFE2EBF0),
    border = Color(0xFFBEC9D4),
    glassWhite = Color(0x1A000000),
    glassBorder = Color(0x33000000),
    foreground = Color(0xFF0F172A),
    mutedForeground = Color(0xFF64748B),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    isDark = false
)

val LocalAppColors = compositionLocalOf { DarkAppColors }
