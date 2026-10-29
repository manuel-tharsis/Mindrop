package com.mindrop.app.ui.rpg

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.data.rpg.*
import com.mindrop.app.ui.agenda.SelectorSimple
import com.mindrop.app.notifications.puedeNotificar
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PantallaLogros(vm: RpgViewModel) {
    val logros by vm.logros.collectAsStateWithLifecycle()
    val metricas by vm.metricas.collectAsStateWithLifecycle()
    val desbloqueados = remember(logros) { logros.map { it.clave }.toSet() }
    val valores = remember(metricas) { metricas.associate { it.clave to it.valor } }
    var filtro by rememberSaveable { mutableStateOf("Todos") }
    val visibles = catalogoLogros.filter { when (filtro) {
        "Desbloqueados" -> it.clave in desbloqueados
        "Bloqueados" -> it.clave !in desbloqueados
        else -> true
    } }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Logros", style = MaterialTheme.typography.headlineMedium) }
        item { SelectorSimple("Mostrar", filtro, listOf("Todos", "Desbloqueados", "Bloqueados").map { it to it }) { filtro = it } }
        items(visibles, key = { it.clave }) { logro ->
            val progreso = (valores[logro.metrica] ?: 0).coerceAtMost(logro.objetivo)
            val conseguido = logro.clave in desbloqueados
            Card(shape = FormaPixel, modifier = Modifier.fillMaxWidth().marcoRpg(
                if (logro.dificil) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconoRpg("estrella")
                        Column { Text(logro.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(if (conseguido) "Desbloqueado" else if (logro.dificil) "TRYHARD · Bloqueado" else "Bloqueado") }
                    }
                    Text(logro.descripcion)
                    LinearProgressIndicator(progress = { if (conseguido) 1f else progreso.toFloat() / logro.objetivo }, modifier = Modifier.fillMaxWidth())
                    Text("${if (conseguido) logro.objetivo else progreso} / ${logro.objetivo}")
                    recompensas.filter { it.logro == logro.clave }.forEach { Text("Recompensa: ${it.nombre}", color = MaterialTheme.colorScheme.secondary) }
                }
            }
        }
        item { Text("La racha cuenta días con acciones reales sobre tus ideas, sugerencias o agenda. Abrir la app no cuenta. Los logros obtenidos se conservan.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun PantallaAjustes(vm: RpgViewModel, copias: @Composable () -> Unit = {}) {
    val p by vm.preferencias.collectAsStateWithLifecycle()
    val logros by vm.logros.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var permiso by remember { mutableStateOf(puedeNotificar(contexto)) }
    val propietario = LocalLifecycleOwner.current
    DisposableEffect(propietario, contexto) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) permiso = puedeNotificar(contexto)
        }
        propietario.lifecycle.addObserver(observador)
        onDispose { propietario.lifecycle.removeObserver(observador) }
    }
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = puedeNotificar(contexto) }
    val disponibles = remember(logros) { logros.map { it.clave }.toSet() }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium) }
        item { PanelAjustes { copias() } }
        item { Text("Lectura del cuaderno", style = MaterialTheme.typography.titleLarge) }
        item { PanelAjustes { SelectorSimple("Tamaño del texto", "${p.tamanoLectura}", listOf(16, 18, 20, 22, 24).map { it to "$it" }) {
            vm.lectura(it, p.papelOscuro)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Papel oscuro", style = MaterialTheme.typography.titleMedium)
                Text("Una lectura más suave con poca luz", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(p.papelOscuro, { vm.lectura(p.tamanoLectura, it) })
        } } }
        item { Text("Notificaciones", style = MaterialTheme.typography.titleLarge) }
        item { PanelAjustes { Text(if (permiso) "Notificaciones permitidas" else "Notificaciones desactivadas")
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && !permiso) solicitar.launch(Manifest.permission.POST_NOTIFICATIONS)
                else contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${contexto.packageName}".toUri()))
            }) { Text("Permitir / configurar notificaciones") }
        SelectorSimple("Redescubrir ideas antiguas", when(p.diasRedescubrimiento) { 0 -> "Desactivado"; 1 -> "Diario"; else -> "Cada ${p.diasRedescubrimiento} días" },
            listOf(0 to "Desactivado", 1 to "Diario", 3 to "Cada 3 días", 7 to "Semanal", 14 to "Cada 2 semanas")) {
            vm.redescubrir(it, p.silencioDesde, p.silencioHasta)
        }
        SelectorSimple("Silencio desde", "${p.silencioDesde}:00", (0..23).filter { it != p.silencioHasta }.map { it to "$it:00" }) {
            vm.redescubrir(p.diasRedescubrimiento, it, p.silencioHasta)
        }
        SelectorSimple("Silencio hasta", "${p.silencioHasta}:00", (0..23).filter { it != p.silencioDesde }.map { it to "$it:00" }) {
            vm.redescubrir(p.diasRedescubrimiento, p.silencioDesde, it)
        } } }
        item { PanelAjustes { Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reducir movimiento", Modifier.weight(1f)); Switch(checked = p.reducirMovimiento, onCheckedChange = vm::movimiento)
        } } }
        item { Text("Personalización", style = MaterialTheme.typography.titleLarge) }
        item { OpcionesAspecto(p.tema, p.diseno, p.efecto, disponibles, vm::aplicar) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Text("Mindrop 2.3.1 · datos privados en este dispositivo. Los avisos pueden retrasarse por el ahorro de batería. No hay sincronización.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun CelebracionLogro(vm: RpgViewModel) {
    val logros by vm.logros.collectAsStateWithLifecycle()
    var activa by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(logros, activa) {
        if (activa == null) logros.firstOrNull { !it.celebrado }?.let {
            activa = it.clave
            vm.celebrar(it.clave)
        }
    }
    val definicion = catalogoLogros.firstOrNull { it.clave == activa } ?: return
    AlertDialog(onDismissRequest = { activa = null },
        icon = { Box(Modifier.size(80.dp)) { IconoRpg("estrella", Modifier.size(64.dp)); DestelloPixel(definicion.clave) } },
        title = { Text("¡Logro desbloqueado!\n${definicion.nombre}") },
        text = { Column { Text(definicion.descripcion); recompensas.filter { it.logro == definicion.clave }.forEach { Text("Recompensa: ${it.nombre}") } } },
        confirmButton = { TextButton(onClick = { activa = null }) { Text("Continuar") } })
}
