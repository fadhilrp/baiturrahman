package com.example.baiturrahman.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// === Backgrounds ===
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)

// === Glassmorphism ===
val GlassWhite = Color(0x1AFFFFFF)      // 10% white
val GlassBorder = Color(0x33FFFFFF)     // 20% white

// === Primary Accent — Emerald ===
val EmeraldGreen = Color(0xFF34D399)
val EmeraldLight = Color(0xFF6EE7B7)
val EmeraldDark = Color(0xFF059669)
val EmeraldMuted = Color(0x3334D399)    // 20% emerald

// === Secondary Accent — Gold ===
val GoldAccent = Color(0xFFFBBF24)
val GoldLight = Color(0xFFFDE68A)
val GoldMuted = Color(0x33FBBF24)       // 20% gold

// === Text ===
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)
val TextOnAccent = Color(0xFF0F172A)

// === Gradient Brushes ===
val EmeraldGradient = Brush.horizontalGradient(
    colors = listOf(EmeraldDark, EmeraldGreen, EmeraldLight)
)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFF59E0B), GoldAccent, GoldLight)
)

val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(DarkBackground, DarkSurface)
)

// === Legacy aliases for compilation safety ===
val emeraldGreen: Color
    @JvmName("getLegacyEmeraldGreen") get() = EmeraldGreen
val white: Color
    @JvmName("getLegacyWhite") get() = Color.White
val black: Color
    @JvmName("getLegacyBlack") get() = Color.Black
