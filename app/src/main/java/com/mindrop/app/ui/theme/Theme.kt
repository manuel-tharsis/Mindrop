package com.mindrop.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun coloresRpg(tema: String): ColorScheme {
    val acento = when (tema) {
        "bosque" -> Color(0xFF8DE4A8)
        "nocturno" -> Color(0xFFC7BEFF)
        "cyberpunk" -> Color(0xFF55F4E6)
        "dorado" -> Color(0xFFFFD56A)
        else -> Color(0xFF92BEFF)
    }
    val fondo = when (tema) {
        "bosque" -> Color(0xFF0D201C)
        "cyberpunk" -> Color(0xFF180E29)
        "dorado" -> Color(0xFF201A16)
        else -> Color(0xFF101329)
    }
    return darkColorScheme(
        primary = acento, onPrimary = Color(0xFF101329),
        primaryContainer = Color(0xFF303D68), onPrimaryContainer = Color(0xFFE3E9FF),
        secondary = Color(0xFFFFD56A), onSecondary = Color(0xFF302300),
        secondaryContainer = Color(0xFF4D3C23), onSecondaryContainer = Color(0xFFFFE8AE),
        tertiary = Color(0xFFC9A3FF), onTertiary = Color(0xFF291841),
        tertiaryContainer = Color(0xFF473263), onTertiaryContainer = Color(0xFFF2E6FF),
        background = fondo, onBackground = Color(0xFFF1EEFF),
        surface = fondo, onSurface = Color(0xFFF1EEFF),
        surfaceContainerLowest = fondo.copy(red = (fondo.red + .04f).coerceAtMost(1f)),
        surfaceContainerLow = Color(0xFF20243C), surfaceContainer = Color(0xFF282D49),
        surfaceVariant = Color(0xFF343A58), onSurfaceVariant = Color(0xFFCACCE0),
        outlineVariant = Color(0xFF555E83), error = Color(0xFFFFB4BA),
    )
}

private val tipografia = Typography().let { base ->
    Typography(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 36.sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 31.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
        bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
}

@Composable
fun MindropTheme(tema: String = "clasico", content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = coloresRpg(tema), typography = tipografia, content = content)
}
