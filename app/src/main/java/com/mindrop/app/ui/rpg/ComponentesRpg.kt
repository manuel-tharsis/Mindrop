package com.mindrop.app.ui.rpg

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.mindrop.app.R
import com.mindrop.app.ui.icons.ThemedIcon

val LocalMovimiento = staticCompositionLocalOf { true }
val LocalEfecto = staticCompositionLocalOf { "chispa" }
val LocalDiseno = staticCompositionLocalOf { "clasico" }
val FormaPixel = RoundedCornerShape(24.dp)

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
fun IconoRpg(tipo: String, modifier: Modifier = Modifier, abierto: Boolean = false) {
    ThemedIcon(when (tipo) {
        "carpeta" -> R.drawable.ic_folder
        "proyecto" -> R.drawable.ic_project
        "idea" -> R.drawable.ic_idea
        "codigo" -> R.drawable.ic_code
        "ajustes" -> R.drawable.ic_settings
        else -> R.drawable.ic_trophy
    }, null, modifier.size(42.dp).graphicsLayer { translationY = if (abierto) -1.dp.toPx() else 0f })
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
    val progreso = remember(clave) { Animatable(0f) }
    LaunchedEffect(clave) {
        if (permitido) progreso.animateTo(1f, tween(650)) else delay(200)
        alTerminar()
    }
    if (permitido) DibujoDestello(progreso.value, efecto, modifier.fillMaxSize())
}

@Composable
fun DibujoDestello(progreso: Float, efecto: String, modifier: Modifier = Modifier) {
    val color = if (efecto == "dorado") Color(0xFFFFD56A) else MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val cantidad = when (efecto) { "dorado" -> 16; "magia" -> 12; else -> 6 }
        repeat(cantidad) { indice ->
            val angulo = indice * kotlin.math.PI * 2 / cantidad
            val distancia = size.minDimension * .44f * progreso
            val x = center.x + kotlin.math.cos(angulo).toFloat() * distancia
            val y = center.y + kotlin.math.sin(angulo).toFloat() * distancia
            val lado = if (efecto == "magia") (3 + indice % 3).dp.toPx() else 4.dp.toPx()
            drawRect(color.copy(alpha = 1f - progreso), Offset(x, y), Size(lado, lado))
        }
    }
}

fun Modifier.marcoRpg(color: Color, intensidad: Float = 0f) =
    if (intensidad <= 0f) this else border(1.dp, color.copy(alpha = intensidad), FormaPixel)
