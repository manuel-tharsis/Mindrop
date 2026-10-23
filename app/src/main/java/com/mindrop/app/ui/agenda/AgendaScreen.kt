package com.mindrop.app.ui.agenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.ui.rpg.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mindrop.app.notifications.puedeNotificar

@Composable
fun CampoFecha(fecha: String, cambiar: (String) -> Unit, habilitado: Boolean = true) {
    val contexto = LocalContext.current
    val valor = LocalDate.parse(fecha)
    OutlinedButton(enabled = habilitado, onClick = {
        DatePickerDialog(contexto, { _, anio, mes, dia -> cambiar(LocalDate.of(anio, mes + 1, dia).toString()) },
            valor.year, valor.monthValue - 1, valor.dayOfMonth).show()
    }) { Text("Fecha: ${valor.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}") }
}

@Composable
fun CampoHora(minuto: Int?, cambiar: (Int?) -> Unit, habilitado: Boolean = true) {
    val contexto = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = habilitado, onClick = {
            TimePickerDialog(contexto, { _, h, m -> cambiar(h * 60 + m) },
                (minuto ?: 540) / 60, (minuto ?: 540) % 60, true).show()
        }) { Text(if (minuto == null) "Añadir hora" else "%02d:%02d".format(minuto / 60, minuto % 60)) }
        if (minuto != null) TextButton(enabled = habilitado, onClick = { cambiar(null) }) { Text("Sin hora") }
    }
}

@Composable
fun AgendaScreen(vm: AgendaViewModel, ideas: List<IdeaEntity>, abrirIdea: (Long) -> Unit) {
    val eventos by vm.eventos.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val guardando by vm.guardando.collectAsStateWithLifecycle()
    var fecha by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mes by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var editarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var borrarId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(vm) { vm.guardados.collect { editarId = null; borrarId = null } }
    val mesActual = YearMonth.parse(mes)
    val eventosDia = remember(eventos, fecha) { eventos.filter { it.fecha == fecha } }
    val diasConEventos = remember(eventos, mes) { eventos.filter { it.fecha.startsWith(mes) }.map { LocalDate.parse(it.fecha).dayOfMonth }.toSet() }
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { vm.error.value = null; editarId = 0 }, shape = FormaPixel) { Text("+ Evento") }
    }) { espacio ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(espacio),
            contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Agenda", style = MaterialTheme.typography.headlineMedium) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { mes = mesActual.minusMonths(1).toString() }) { Text("‹ Anterior") }
                    Text(mesActual.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"))))
                    TextButton(onClick = { mes = mesActual.plusMonths(1).toString() }) { Text("Siguiente ›") }
                }
            }
            item {
                Column {
                    Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, Modifier.weight(1f)) } }
                    val desplazamiento = mesActual.atDay(1).dayOfWeek.value - 1
                    val total = ((desplazamiento + mesActual.lengthOfMonth() + 6) / 7) * 7
                    (0 until total step 7).forEach { fila ->
                        Row {
                            repeat(7) { columna ->
                                val dia = fila + columna - desplazamiento + 1
                                Box(Modifier.weight(1f)) {
                                    if (dia in 1..mesActual.lengthOfMonth()) {
                                        val valor = mesActual.atDay(dia).toString()
                                        TextButton(onClick = { fecha = valor }, contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.fillMaxWidth().then(if (fecha == valor) Modifier.marcoRpg(MaterialTheme.colorScheme.primary) else Modifier)) {
                                            Column {
                                                Text(dia.toString(), color = if (valor == LocalDate.now().toString()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                                                if (dia in diasConEventos) Text("•", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Text(LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es"))), style = MaterialTheme.typography.titleMedium) }
            if (eventosDia.isEmpty()) item { Text("Sin eventos este día.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(eventosDia, key = { it.id }) { evento ->
                Card(shape = FormaPixel, modifier = Modifier.fillMaxWidth().marcoRpg(MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(evento.titulo, style = MaterialTheme.typography.titleMedium)
                        evento.minutoDelDia?.let { Text("%02d:%02d".format(it / 60, it % 60)) }
                        if (evento.descripcion.isNotBlank()) Text(evento.descripcion)
                        Row {
                            evento.ideaId?.let { id -> TextButton(onClick = { abrirIdea(id) }) { Text("Abrir idea") } }
                            TextButton(onClick = { vm.error.value = null; editarId = evento.id }) { Text("Editar") }
                            TextButton(onClick = { vm.error.value = null; borrarId = evento.id }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
    editarId?.let { id ->
        EditorEvento(id, eventos.firstOrNull { it.id == id }, fecha, ideas, error, guardando,
            guardar = vm::guardar, cancelar = { editarId = null })
    }
    borrarId?.let { id ->
        AlertDialog(onDismissRequest = { if (!guardando) borrarId = null },
            title = { Text("¿Eliminar evento?") }, text = { Text(error ?: "También se cancelará su recordatorio.") },
            confirmButton = { TextButton(enabled = !guardando, onClick = { vm.eliminar(id) }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !guardando, onClick = { borrarId = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun EditorEvento(id: Long, original: EventoAgenda?, fechaInicial: String, ideas: List<IdeaEntity>, error: String?,
    guardando: Boolean, guardar: (EventoAgenda) -> Unit, cancelar: () -> Unit) {
    var titulo by rememberSaveable(id) { mutableStateOf(original?.titulo.orEmpty()) }
    var descripcion by rememberSaveable(id) { mutableStateOf(original?.descripcion.orEmpty()) }
    var fecha by rememberSaveable(id) { mutableStateOf(original?.fecha ?: fechaInicial) }
    var minuto by rememberSaveable(id) { mutableStateOf(original?.minutoDelDia) }
    var ideaId by rememberSaveable(id) { mutableStateOf(original?.ideaId) }
    var antes by rememberSaveable(id) { mutableStateOf(original?.minutosAntes) }
    var descartar by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun salir() {
        if (guardando) return
        val actual = EventoAgenda(id, titulo, descripcion, fecha, minuto, ideaId, antes)
        if (actual != (original ?: EventoAgenda(0, "", fecha = fechaInicial))) descartar = true else cancelar()
    }
    Dialog(onDismissRequest = ::salir, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            BackHandler(onBack = ::salir)
            LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text(if (id == 0L) "Nuevo evento" else "Editar evento", style = MaterialTheme.typography.headlineSmall) }
                item { OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, enabled = !guardando, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción (opcional)") }, minLines = 3, enabled = !guardando, modifier = Modifier.fillMaxWidth()) }
                item { CampoFecha(fecha, { fecha = it }, !guardando) }
                item { CampoHora(minuto, { minuto = it; if (it == null) antes = null }, !guardando) }
                item { SelectorSimple("Idea relacionada", ideas.firstOrNull { it.id == ideaId }?.title ?: "Ninguna",
                    listOf(null to "Ninguna") + ideas.map { it.id to it.title }, !guardando) { ideaId = it } }
                item { SelectorSimple("Recordatorio", etiquetaRecordatorio(antes),
                    listOf(null, 0, 10, 30, 60, 1440).map { it to etiquetaRecordatorio(it) }, !guardando && minuto != null) {
                        antes = it
                        if (it != null && Build.VERSION.SDK_INT >= 33 && !puedeNotificar(contexto)) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } }
                item { Text("Los avisos son aproximados y requieren permitir notificaciones.", style = MaterialTheme.typography.bodySmall) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !guardando, onClick = { guardar(EventoAgenda(id, titulo, descripcion, fecha, minuto, ideaId, antes)) }) { Text("Guardar") }
                    OutlinedButton(enabled = !guardando, onClick = ::salir) { Text("Cancelar") }
                } }
            }
        }
    }
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("¿Descartar cambios?") },
        confirmButton = { TextButton(onClick = cancelar) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Seguir editando") } })
}

fun etiquetaRecordatorio(minutos: Int?) = when (minutos) {
    null -> "Sin recordatorio"; 0 -> "A la hora del evento"; 60 -> "1 hora antes"; 1440 -> "1 día antes"; else -> "$minutos minutos antes"
}

@Composable
fun <T> SelectorSimple(titulo: String, seleccionado: String, opciones: List<Pair<T, String>>, habilitado: Boolean = true, elegir: (T) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = habilitado, onClick = { abierto = true }) { Text(seleccionado) }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { (clave, nombre) -> DropdownMenuItem(text = { Text(nombre) }, onClick = { elegir(clave); abierto = false }) }
            }
        }
    }
}
