package com.mindrop.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun coloresRpg(tema: String): ColorScheme {
    val acento = when (tema) {
        "bosque" -> Color(0xFF8DE4A8)
        "nocturno" -> Color(0xFFC7BEFF)
        "cyberpunk" -> Color(0xFF55F4E6)
        "dorado" -> Color(0xFFFFD56A)
        else -> Color(0xFF6B98FF)
    }
    return darkColorScheme(
        primary = acento, onPrimary = Color(0xFF081327),
        primaryContainer = androidx.compose.ui.graphics.lerp(Color(0xFF161616), acento, .16f), onPrimaryContainer = Color(0xFFF0F0F0),
        secondary = Color(0xFFFFD56A), onSecondary = Color(0xFF302300),
        secondaryContainer = androidx.compose.ui.graphics.lerp(Color(0xFF161616), acento, .16f), onSecondaryContainer = Color(0xFFF0F0F0),
        tertiary = Color(0xFFC9A3FF), onTertiary = Color(0xFF291841),
        tertiaryContainer = Color(0xFF473263), onTertiaryContainer = Color(0xFFF2E6FF),
        background = Color.Black, onBackground = Color(0xFFF4F4F4),
        surface = Color.Black, onSurface = Color(0xFFF4F4F4),
        surfaceContainerLowest = Color(0xFF232323),
        surfaceContainerLow = Color(0xFF232323), surfaceContainer = Color(0xFF282828),
        surfaceContainerHigh = Color(0xFF2C2C2C), surfaceContainerHighest = Color(0xFF303030),
        surfaceVariant = Color(0xFF303030), onSurfaceVariant = Color(0xFFB3B3B3),
        outline = Color(0xFF777777), outlineVariant = Color(0xFF424242), error = Color(0xFFFFB4BA),
    )
}

private val tipografia = Typography().let { base ->
    Typography(
        headlineMedium = base.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Normal, lineHeight = 38.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.Normal, lineHeight = 34.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 28.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 23.sp),
        bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
}

@Composable
fun MindropTheme(tema: String = "clasico", content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = coloresRpg(tema), typography = tipografia,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)), content = content)
}
