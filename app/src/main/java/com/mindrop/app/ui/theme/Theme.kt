package com.mindrop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF356B88),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E9F7),
    onPrimaryContainer = Color(0xFF0A354A),
    secondary = Color(0xFF8A6500),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8A3),
    onSecondaryContainer = Color(0xFF2B2100),
    tertiary = Color(0xFF7255A6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECDDFF),
    onTertiaryContainer = Color(0xFF2A1255),
    background = Color(0xFFFFFCFA),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFCFA),
    onSurface = Color(0xFF201A18),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF8F4),
    surfaceContainer = Color(0xFFF8F2EE),
    surfaceVariant = Color(0xFFF1EAE6),
    onSurfaceVariant = Color(0xFF5B5652),
    outlineVariant = Color(0xFFDED7D2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DCCE5),
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF174D65),
    onPrimaryContainer = Color(0xFFC9E9F9),
    secondary = Color(0xFFE9C349),
    onSecondary = Color(0xFF473600),
    secondaryContainer = Color(0xFF574500),
    onSecondaryContainer = Color(0xFFFFE28B),
    tertiary = Color(0xFFD4B9FF),
    onTertiary = Color(0xFF40226D),
    tertiaryContainer = Color(0xFF573A84),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF171311),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF171311),
    onSurface = Color(0xFFEDE0DA),
    surfaceContainerLowest = Color(0xFF1C1816),
    surfaceContainerLow = Color(0xFF211D1B),
    surfaceContainer = Color(0xFF282321),
    surfaceVariant = Color(0xFF322D2A),
    onSurfaceVariant = Color(0xFFD2C5BF),
    outlineVariant = Color(0xFF4A4541),
)

private val MindropTypography = Typography().let { defaults ->
    Typography(
        headlineMedium = defaults.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp,
        ),
        headlineSmall = defaults.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            lineHeight = 31.sp,
        ),
        titleMedium = defaults.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
        ),
        bodyLarge = defaults.bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = defaults.bodyMedium.copy(lineHeight = 20.sp),
    )
}

@Composable
fun MindropTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MindropTypography,
        content = content,
    )
}
