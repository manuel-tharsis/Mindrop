package com.mindrop.app.ui.proyectos

import com.mindrop.app.ui.icons.PackIcon as Icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import com.mindrop.app.R
import com.mindrop.app.data.cuaderno.PuntoDibujo
import com.mindrop.app.data.cuaderno.TrazoDibujo

class DibujoViewModel : ViewModel() {
    var paginaId by mutableStateOf<Long?>(null); private set
    val trazos = mutableStateListOf<TrazoDibujo>()
    val actual = mutableStateListOf<PuntoDibujo>()
    var confirmarDescarte by mutableStateOf(false); private set
    var limiteAlcanzado by mutableStateOf(false); private set
    private var total = 0
    fun abrir(id: Long) { cerrar(); paginaId = id }
    fun punto(punto: PuntoDibujo) {
        if (total + actual.size >= 50_000) { limiteAlcanzado = true; return }
        actual += PuntoDibujo(punto.x.coerceIn(0f, 1f), punto.y.coerceIn(0f, 1f))
    }
    fun terminarTrazo() { if (actual.isNotEmpty()) { trazos += actual.toList(); total += actual.size; actual.clear() } }
    fun deshacer() { if (trazos.isNotEmpty()) { total -= trazos.removeAt(trazos.lastIndex).size }; limiteAlcanzado = false }
    fun limpiar() { trazos.clear(); actual.clear(); total = 0; limiteAlcanzado = false }
    fun cancelar() { terminarTrazo(); if (trazos.isEmpty()) cerrar() else confirmarDescarte = true }
    fun continuar() { confirmarDescarte = false }
    fun cerrar() { limpiar(); confirmarDescarte = false; paginaId = null }
    fun copia(): List<TrazoDibujo> { terminarTrazo(); return trazos.map { it.toList() } }
}

@Composable
fun DibujoDialog(vm: DibujoViewModel, guardando: Boolean, error: String?, guardar: (Long, List<TrazoDibujo>) -> Unit) {
    val paginaId = vm.paginaId ?: return
    Dialog(onDismissRequest = { if (!guardando) vm.cancelar() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Dibujar nota", style = MaterialTheme.typography.titleLarge)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val ancho = minOf(maxWidth, maxHeight * 1.6f)
                    Canvas(Modifier.width(ancho).aspectRatio(1.6f).background(Color(0xFFF5F1E9), RoundedCornerShape(12.dp))
                        .testTag("lienzo_nota").pointerInput(guardando) {
                            if (guardando) return@pointerInput
                            awaitEachGesture {
                                val inicio = awaitFirstDown(); inicio.consume()
                                fun normalizar(p: Offset) = PuntoDibujo(p.x / size.width.coerceAtLeast(1), p.y / size.height.coerceAtLeast(1))
                                vm.punto(normalizar(inicio.position))
                                try {
                                    do {
                                        val evento = awaitPointerEvent()
                                        val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                                        if (cambio.pressed) vm.punto(normalizar(cambio.position))
                                        cambio.consume()
                                    } while (cambio.pressed)
                                } finally { vm.terminarTrazo() }
                            }
                        }) {
                        fun dibujar(puntos: TrazoDibujo) {
                            if (puntos.isEmpty()) return
                            val tinta = Color(0xFF292B3E)
                            if (puntos.size == 1) drawCircle(tinta, 1.5.dp.toPx(), Offset(puntos[0].x * size.width, puntos[0].y * size.height))
                            else {
                                val camino = Path().apply {
                                    moveTo(puntos[0].x * size.width, puntos[0].y * size.height)
                                    puntos.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                                }
                                drawPath(camino, tinta, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                        vm.trazos.forEach(::dibujar); dibujar(vm.actual)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = vm::deshacer, enabled = !guardando && vm.trazos.isNotEmpty()) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Deshacer último trazo")
                    }
                    IconButton(onClick = vm::limpiar, enabled = !guardando && (vm.trazos.isNotEmpty() || vm.actual.isNotEmpty())) {
                        Icon(painterResource(R.drawable.ic_delete), "Limpiar dibujo")
                    }
                }
                if (vm.limiteAlcanzado) Text("Esta nota ha alcanzado el límite de trazos. Guárdala y continúa en otra.", color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = vm::cancelar, enabled = !guardando) { Text("Cancelar") }
                    Button(onClick = { guardar(paginaId, vm.copia()) }, enabled = !guardando && (vm.trazos.isNotEmpty() || vm.actual.isNotEmpty())) {
                        Text(if (guardando) "Guardando…" else "Guardar nota")
                    }
                }
            }
        }
    }
    if (vm.confirmarDescarte) AlertDialog(onDismissRequest = vm::continuar, title = { Text("¿Descartar el dibujo?") },
        text = { Text("La nota todavía no se ha guardado en la página.") },
        confirmButton = { TextButton(onClick = vm::cerrar) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = vm::continuar) { Text("Seguir dibujando") } })
}
