package com.mindrop.app.widget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.ui.agenda.*
import java.time.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventoRapidoScreen(vm: AgendaViewModel, peticionFoco: Int, finalizar: () -> Unit) {
    val ahora = rememberSaveable { LocalDateTime.now().toString() }
    val inicial = remember(ahora) { nuevoEventoConRecordatorio(LocalDateTime.parse(ahora)) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(inicial.fecha) }
    var minuto by rememberSaveable { mutableStateOf(inicial.minutoDelDia) }
    var antes by rememberSaveable { mutableStateOf(inicial.minutosAntes) }
    var descartar by rememberSaveable { mutableStateOf(false) }
    val guardando by vm.guardando.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actual = inicial.copy(titulo = titulo, fecha = fecha, minutoDelDia = minuto, minutosAntes = antes)
    val avisos = rememberPermisoAvisos { vm.guardar(actual) }
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    val alFinalizar by rememberUpdatedState(finalizar)
    LaunchedEffect(peticionFoco) { foco.requestFocus(); teclado?.show() }
    LaunchedEffect(vm) { vm.guardados.collect { teclado?.hide(); alFinalizar() } }
    fun salir() {
        if (guardando) return
        if (actual != inicial) descartar = true else alFinalizar()
    }
    fun guardar() {
        if (titulo.isNotBlank() && antes != null && !avisos.disponibles) avisos.solicitar(true)
        else vm.guardar(actual)
    }
    BackHandler(onBack = ::salir)
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Box(Modifier.matchParentSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::salir))
        Column(Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = Color(0xFF232323)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AndroidView(factory = { contexto -> ImageView(contexto).apply {
                        setImageResource(R.mipmap.ic_launcher_round); contentDescription = "Mindrop"
                    } }, modifier = Modifier.size(34.dp))
                    BasicTextField(titulo, { titulo = it }, enabled = !guardando, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFF5F5F5)),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { guardar() }),
                        modifier = Modifier.weight(1f).focusRequester(foco).testTag("nombre_evento_rapido"),
                        decorationBox = { campo ->
                            Box { if (titulo.isEmpty()) Text("Nombre del evento…", color = Color(0xFFBDC1C6)); campo() }
                        })
                }
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoFecha(fecha, { fecha = it }, !guardando)
                        CampoHora(minuto, { minuto = it; if (it == null) antes = null }, !guardando)
                    }
                    SelectorSimple("Recordatorio", etiquetaRecordatorio(antes),
                        listOf(null, 0, 10, 30, 60, 1440).map { it to etiquetaRecordatorio(it) }, !guardando && minuto != null) { antes = it }
                    if (antes != null && !avisos.disponibles) {
                        Text("Activa las notificaciones para recibir el aviso o elige Sin recordatorio.", style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !guardando, onClick = { avisos.solicitar(false) }) { Text("Activar notificaciones") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(enabled = !guardando, onClick = ::salir) { Text("Cancelar") }
                        Button(enabled = !guardando, onClick = ::guardar) { Text("Guardar") }
                    }
                }
            }
        }
    }
    if (descartar) AlertDialog(onDismissRequest = { descartar = false },
        title = { Text("¿Descartar cambios?") },
        confirmButton = { TextButton(onClick = { alFinalizar() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Seguir escribiendo") } })
}
