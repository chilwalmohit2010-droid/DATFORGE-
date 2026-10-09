package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LiquidGlassColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = NeonGreenDark,
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = NeonGreenBright,
    secondary = NeonCyan,
    onSecondary = NeonCyanDark,
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonPurple,
    onTertiary = NeonPurpleDark,
    tertiaryContainer = NeonPurpleContainer,
    onTertiaryContainer = NeonPurple,
    background = GlassDarkCanvas,
    onBackground = TextPrimary,
    surface = GlassDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassDarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    onError = Color.White,
    outline = GlassBorderStroke
)

@Composable
fun DisciplineTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LiquidGlassColorScheme,
        typography = Typography,
        content = content
    )
}

