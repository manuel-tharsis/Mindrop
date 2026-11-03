package com.mindrop.app.ui.proyectos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

/** Decoración de presentación; no representa datos ni modifica las imágenes del usuario. */
@Composable
internal fun PortadaCuaderno(modifier: Modifier = Modifier) {
    val azul = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(58.dp, 82.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFFEDE5D6), topLeft = Offset(3.dp.toPx(), 2.dp.toPx()),
            size = Size(w - 3.dp.toPx(), h - 2.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx()))
        drawRoundRect(Color(0xFF365AC5), size = Size(w - 3.dp.toPx(), h - 3.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx()))
        val onda = Path().apply {
            moveTo(w * .9f, h * .12f)
            cubicTo(w * .92f, h * .55f, w * .18f, h * .35f, w * .16f, h * .94f)
            lineTo(w * .9f, h * .94f); close()
        }
        drawPath(onda, azul)
        drawLine(Color.Black.copy(alpha = .18f), Offset(w * .1f, 2.dp.toPx()), Offset(w * .1f, h - 5.dp.toPx()), 1.dp.toPx())
    }
}

@Composable
internal fun Modifier.acentoPapel(): Modifier {
    val azul = MaterialTheme.colorScheme.primary
    return drawBehind {
        val lado = minOf(60.dp.toPx(), size.width * .22f)
        val onda = Path().apply {
            moveTo(size.width - lado, 0f); lineTo(size.width, 0f); lineTo(size.width, lado)
            cubicTo(size.width - lado * .6f, lado * .9f, size.width - lado * .3f, lado * .15f, size.width - lado, 0f)
            close()
        }
        drawPath(onda, azul.copy(alpha = .22f))
        drawRoundRect(azul.copy(alpha = .14f), topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size((size.width-8.dp.toPx()).coerceAtLeast(0f), (size.height-8.dp.toPx()).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(21.dp.toPx()), style = Stroke(1.dp.toPx()))
    }
}
