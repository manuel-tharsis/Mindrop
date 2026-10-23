package com.mindrop.app.ui.rpg

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val LocalMovimiento = staticCompositionLocalOf { true }
val LocalEfecto = staticCompositionLocalOf { "chispa" }
val LocalDiseno = staticCompositionLocalOf { "clasico" }
val FormaPixel = CutCornerShape(8.dp)

@Composable
fun movimientoPermitido(): Boolean {
    val contexto = LocalContext.current
    val escala = remember(contexto) { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
    return LocalMovimiento.current && escala > 0f
}

@Composable
fun Modifier.reboteRpg(interacciones: MutableInteractionSource): Modifier {
    val pulsado by interacciones.collectIsPressedAsState()
    val movimiento = movimientoPermitido()
    val escala by animateFloatAsState(if (pulsado && movimiento) .97f else 1f, tween(if (movimiento) 90 else 0), label = "Pulsación")
    return graphicsLayer { scaleX = escala; scaleY = escala }
}

@Composable
fun IconoPixel(tipo: String, modifier: Modifier = Modifier, abierto: Boolean = false) {
    val dorado = MaterialTheme.colorScheme.secondary
    val luz = MaterialTheme.colorScheme.primary
    val diseno = LocalDiseno.current
    Canvas(modifier.size(42.dp)) {
        val p = size.minDimension / 16f
        fun bloque(x: Int, y: Int, ancho: Int, alto: Int, color: Color) {
            drawRect(color, Offset(x * p, y * p), Size(ancho * p, alto * p))
        }
        if (tipo == "carpeta") {
            val color = when (diseno) {
                "runa" -> luz
                "cofre" -> Color(0xFFBE8350)
                else -> dorado
            }
            bloque(1, 4, 6, 2, color)
            bloque(1, 6, 14, 8, color.copy(alpha = .65f))
            bloque(2, if (abierto) 8 else 7, 13, if (abierto) 6 else 7, color)
            bloque(3, 8, 10, 1, Color.White.copy(alpha = .35f))
            if (diseno == "cofre") bloque(7, 9, 2, 3, dorado)
        } else {
            // Estrella original en una cuadrícula de 16×16.
            val filas = listOf(7 to 2, 6 to 4, 2 to 12, 4 to 8, 5 to 6, 4 to 8, 3 to 4, 9 to 4)
            filas.forEachIndexed { y, (x, ancho) -> bloque(x, y + 3, ancho, 1, dorado) }
            bloque(7, 5, 2, 3, Color.White.copy(alpha = .65f))
        }
    }
}

@Composable
fun EntradaPixel(clave: Any, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val permitido = movimientoPermitido()
    val progreso = remember(clave) { Animatable(if (permitido) 0f else 1f) }
    LaunchedEffect(clave, permitido) {
        if (permitido) progreso.animateTo(1f, tween(240)) else progreso.snapTo(1f)
    }
    Box(modifier.graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 12.dp.toPx()
    }) { contenido() }
}

@Composable
fun DestelloPixel(clave: Any, modifier: Modifier = Modifier, alTerminar: () -> Unit = {}) {
    val permitido = movimientoPermitido()
    val efecto = LocalEfecto.current
    val color = if (efecto == "dorado") Color(0xFFFFD56A) else MaterialTheme.colorScheme.primary
    val progreso = remember(clave) { Animatable(0f) }
    LaunchedEffect(clave) {
        if (permitido) progreso.animateTo(1f, tween(650)) else delay(200)
        alTerminar()
    }
    if (permitido) Canvas(modifier.fillMaxSize()) {
        val cantidad = when (efecto) { "dorado" -> 16; "magia" -> 12; else -> 6 }
        repeat(cantidad) { indice ->
            val angulo = indice * kotlin.math.PI * 2 / cantidad
            val distancia = size.minDimension * .35f * progreso.value
            val x = center.x + kotlin.math.cos(angulo).toFloat() * distancia
            val y = center.y + kotlin.math.sin(angulo).toFloat() * distancia
            val lado = if (efecto == "magia") (3 + indice % 3).dp.toPx() else 4.dp.toPx()
            drawRect(color.copy(alpha = 1f - progreso.value), Offset(x, y), Size(lado, lado))
        }
    }
}

fun Modifier.marcoRpg(color: Color) = border(1.dp, color.copy(alpha = .55f), FormaPixel)

fun Modifier.fondoRpg(tema: String, color: Color) = drawBehind {
    // Decoración estática y escasa: no mantiene ningún bucle de animación.
    repeat(14) { i ->
        val x = ((i * 137 + 37) % 997) / 997f * size.width
        val y = ((i * 73 + 19) % 751) / 751f * size.height
        drawRect(color.copy(alpha = .12f), Offset(x, y), Size(3.dp.toPx(), 3.dp.toPx()))
    }
    if (tema == "nocturno") drawCircle(color.copy(alpha = .12f), 28.dp.toPx(), Offset(size.width - 50.dp.toPx(), 70.dp.toPx()))
    if (tema == "bosque") repeat(3) { i ->
        val x = (i + 1) * size.width / 4
        drawRect(color.copy(alpha = .08f), Offset(x - 15.dp.toPx(), size.height - 80.dp.toPx()), Size(30.dp.toPx(), 40.dp.toPx()))
        drawRect(color.copy(alpha = .08f), Offset(x - 3.dp.toPx(), size.height - 40.dp.toPx()), Size(6.dp.toPx(), 25.dp.toPx()))
    }
}
