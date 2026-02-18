package com.example.baiturrahman.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// === Backgrounds (matched to web: HSL 220 40% 7%) ===
val DarkBackground = Color(0xFF0F1820)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)

// === Web design tokens ===
val CardBackground = Color(0xFF172030)   // HSL 220 35% 10%
val Secondary = Color(0xFF1A2332)        // HSL 220 30% 15%
val Border = Color(0xFF2A3548)           // HSL 220 25% 18%
val MutedForeground = Color(0xFF7A8A9E)  // HSL 215 15% 55%
val Foreground = Color(0xFFF0F4F8)       // HSL 210 40% 96%
val SecondaryForeground = Color(0xFFDDE4ED)  // HSL 210 40% 90%

// === Glassmorphism ===
val GlassWhite = Color(0x1AFFFFFF)      // 10% white
val GlassBorder = Color(0x33FFFFFF)     // 20% white

// === Primary Accent — Emerald ===
val EmeraldGreen = Color(0xFF34D399)
val EmeraldLight = Color(0xFF6EE7B7)
val EmeraldDark = Color(0xFF059669)
val EmeraldMuted = Color(0x3334D399)    // 20% emerald

// === Glow utility colors ===
val EmeraldGlow40 = Color(0x6634D399)   // 40% alpha
val EmeraldGlow15 = Color(0x2634D399)   // 15% alpha
val EmeraldGlow20 = Color(0x3334D399)   // 20% alpha
val ActivePrayerBg = Color(0x2634D399)  // 15% alpha for active prayer
val NextPrayerBg = Secondary            // solid secondary

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
