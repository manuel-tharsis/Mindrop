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
        primaryContainer = androidx.compose.ui.graphics.lerp(Color(0xFF161A21), acento, .13f), onPrimaryContainer = Color(0xFFF0F0F0),
        secondary = Color(0xFFFFD56A), onSecondary = Color(0xFF302300),
        secondaryContainer = androidx.compose.ui.graphics.lerp(Color(0xFF161A21), acento, .13f), onSecondaryContainer = Color(0xFFF0F0F0),
        tertiary = Color(0xFFC9A3FF), onTertiary = Color(0xFF291841),
        tertiaryContainer = Color(0xFF473263), onTertiaryContainer = Color(0xFFF2E6FF),
        background = Color(0xFF0D0F13), onBackground = Color(0xFFEEF1F5),
        surface = Color(0xFF0D0F13), onSurface = Color(0xFFEEF1F5),
        surfaceContainerLowest = Color(0xFF181C22),
        surfaceContainerLow = Color(0xFF181C22), surfaceContainer = Color(0xFF20252D),
        surfaceContainerHigh = Color(0xFF272D36), surfaceContainerHighest = Color(0xFF2E3540),
        surfaceVariant = Color(0xFF272D36), onSurfaceVariant = Color(0xFFA9B2C0),
        outline = Color(0xFF667181), outlineVariant = Color(0xFF343C48), error = Color(0xFFFFB4BA),
    )
}

private val tipografia = Typography().let { base ->
    Typography(
        headlineMedium = base.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp, letterSpacing = 0.sp),
        titleSmall = base.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
        labelLarge = base.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
    )
}

@Composable
fun MindropTheme(tema: String = "clasico", content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = coloresRpg(tema), typography = tipografia,
        shapes = Shapes(extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)), content = content)
}
