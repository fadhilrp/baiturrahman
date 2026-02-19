package com.example.baiturrahman.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = TextOnAccent,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = GoldAccent,
    onSecondary = TextOnAccent,
    secondaryContainer = GoldMuted,
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldLight,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassWhite
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = TextOnAccent,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = GoldAccent,
    onSecondary = TextOnAccent,
    secondaryContainer = GoldMuted,
    onSecondaryContainer = GoldLight,
    tertiary = EmeraldLight,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2EBF0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFBEC9D4),
    outlineVariant = Color(0x33000000)
)

@Composable
fun BaiturrahmanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            window.navigationBarColor = appColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
