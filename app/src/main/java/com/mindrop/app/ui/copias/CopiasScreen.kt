package com.mindrop.app.ui.copias

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.MindropApplication
import com.mindrop.app.data.rpg.recompensaPermitida
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import androidx.core.net.toUri

data class EstadoCopias(val trabajando: Boolean = false, val mensaje: String? = null, val error: Boolean = false,
    val restauraciones: Int = 0)

class CopiasViewModel(private val app: MindropApplication) : ViewModel() {
    var ultimaRestauracionMostrada = 0
    private val estado = MutableStateFlow(EstadoCopias())
    val uiState = estado.asStateFlow()
    fun exportar(uri: Uri) = ejecutar {
        app.contentResolver.openOutputStream(uri, "wt")?.use { app.copias.exportar(it) }
            ?: error("No se pudo abrir el archivo de destino.")
        estado.update { it.copy(mensaje = "Copia guardada.", error = false) }
    }
    fun restaurar(uri: Uri) = ejecutar {
        val anteriores = app.database.daoRpg().pendientes().map { it.id }
        app.contentResolver.openInputStream(uri)?.use { app.copias.restaurar(it) }
            ?: error("No se pudo abrir la copia.")
        // La restauración de contenido ya se ha confirmado. Un fallo al programar avisos no la revierte.
        estado.update { it.copy(restauraciones = it.restauraciones + 1, mensaje = "Copia restaurada.", error = false) }
        try {
            anteriores.forEach(app.programador::cancelar)
            val p = app.preferencias.preferencias.first()
            val logros = app.database.daoRpg().logros().map { it.clave }.toSet()
            listOf("tema" to p.tema, "diseno" to p.diseno, "efecto" to p.efecto).forEach { (categoria, clave) ->
                if (!recompensaPermitida(categoria, clave, logros)) app.logros.aplicar(categoria, if (categoria == "efecto") "chispa" else "clasico")
            }
            app.programador.reconciliar()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            estado.update { it.copy(mensaje = "Copia restaurada. Los avisos se revisarán al volver a abrir la app.") }
        }
    }
    private fun ejecutar(accion: suspend () -> Unit) {
        if (estado.value.trabajando) return
        estado.update { it.copy(trabajando = true, mensaje = null) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { accion() } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                val mensaje = e.message?.takeIf { texto -> listOf("La copia ", "No es una copia ", "Espera a que ", "El archivo no es ", "Un icono ").any(texto::startsWith) }
                    ?: "No se pudo completar la operación. Comprueba el archivo o elige otra ubicación. Tus datos actuales se conservan."
                estado.update { it.copy(mensaje = mensaje, error = true) }
            } finally { estado.update { it.copy(trabajando = false) } }
        }
    }
}

@Composable
fun OpcionesCopias(vm: CopiasViewModel) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    var seleccionada by rememberSaveable { mutableStateOf<String?>(null) }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let(vm::exportar) }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> seleccionada = uri?.toString() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Copias de seguridad", style = MaterialTheme.typography.titleLarge)
        Text("Guarda tus ideas, proyectos, páginas, agenda, progreso e iconos en un archivo. No requiere conexión.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { exportar.launch("Mindrop-copia-${LocalDate.now()}.zip") }, enabled = !estado.trabajando,
            modifier = Modifier.fillMaxWidth()) { Text("Exportar copia") }
        OutlinedButton(onClick = { importar.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            enabled = !estado.trabajando, modifier = Modifier.fillMaxWidth()) { Text("Restaurar copia") }
        Text("Los ajustes de lectura y notificaciones se mantienen en este dispositivo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        estado.mensaje?.let { Text(it, color = if (estado.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
    }
    seleccionada?.let { ruta ->
        AlertDialog(onDismissRequest = { seleccionada = null }, title = { Text("¿Restaurar esta copia?") },
            text = { Text("Se sustituirán todas las ideas, carpetas, proyectos, páginas, agenda y progreso actuales por los de la copia. Los iconos también se recuperarán. Exporta antes una copia si quieres conservar el contenido actual.") },
            confirmButton = { TextButton(onClick = { seleccionada = null; vm.restaurar(ruta.toUri()) }) { Text("Restaurar y sustituir") } },
            dismissButton = { TextButton(onClick = { seleccionada = null }) { Text("Cancelar") } })
    }
}

@Composable
fun OperacionCopiaEnCurso(trabajando: Boolean) {
    if (trabajando) {
        BackHandler { /* Esperar al final de la operación evita interrumpir una copia. */ }
        AlertDialog(onDismissRequest = {}, title = { Text("Preparando tus datos…") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Espera un momento.") } }, confirmButton = {})
    }
}
