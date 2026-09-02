package com.mindrop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF356B88),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E9F7),
    onPrimaryContainer = Color(0xFF0A354A),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF1EAE6),
    onSurfaceVariant = Color(0xFF5B5652),
    outlineVariant = Color(0xFFDED7D2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DCCE5),
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF174D65),
    onPrimaryContainer = Color(0xFFC9E9F9),
    background = Color(0xFF171311),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF171311),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF322D2A),
    onSurfaceVariant = Color(0xFFD2C5BF),
    outlineVariant = Color(0xFF4A4541),
)

@Composable
fun MindropTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
