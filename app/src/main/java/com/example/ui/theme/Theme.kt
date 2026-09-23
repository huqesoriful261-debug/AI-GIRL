package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZoyaDarkColorScheme = darkColorScheme(
    primary = ZoyaMagenta,
    onPrimary = Color.White,
    primaryContainer = ZoyaPurpleGlow,
    onPrimaryContainer = Color.White,
    secondary = ZoyaCyan,
    onSecondary = Color(0xFF001B24),
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = ZoyaCyan,
    tertiary = ZoyaViolet,
    onTertiary = Color.White,
    background = ZoyaDarkBg,
    onBackground = ZoyaTextWhite,
    surface = ZoyaSurface,
    onSurface = ZoyaTextWhite,
    surfaceVariant = ZoyaSurfaceVariant,
    onSurfaceVariant = ZoyaTextMuted,
    error = ZoyaRed,
    onError = Color.White
)

@Composable
fun ZoyaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZoyaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
