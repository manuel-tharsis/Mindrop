package com.mindrop.app.ui.idea

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindrop.app.MindropApplication
import com.mindrop.app.ui.agenda.*
import com.mindrop.app.notifications.puedeNotificar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.*

@Composable
fun DialogoRecordatorio(id: Long, app: MindropApplication, salir: () -> Unit) {
    var fecha by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var minuto by rememberSaveable { mutableStateOf<Int?>(LocalTime.now().plusHours(1).let { it.hour * 60 + it.minute }) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var hayAviso by remember { mutableStateOf(false) }
    val ambito = rememberCoroutineScope()
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(id) {
        try {
            app.database.daoRpg().recordatorioIdea(id)?.let {
                val valor = LocalDateTime.parse(it.fechaHora)
                fecha = valor.toLocalDate().toString(); minuto = valor.hour * 60 + valor.minute; hayAviso = !it.enviado
            }
        } finally { cargando = false }
    }
    fun guardar(cancelar: Boolean) {
        if (guardando || cargando) return
        guardando = true
        ambito.launch {
            try {
                app.programador.programarIdea(id, if (cancelar) null else LocalDate.parse(fecha).atStartOfDay().plusMinutes((minuto ?: 540).toLong()).toString())
                salir()
            } catch (e: Exception) { if (e is CancellationException) throw e; error = e.message }
            finally { guardando = false }
        }
    }
    AlertDialog(onDismissRequest = { if (!guardando) salir() }, title = { Text("Recordatorio de idea") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoFecha(fecha, { fecha = it }, !guardando && !cargando)
            CampoHora(minuto, { minuto = it }, !guardando && !cargando)
            Text("Aviso aproximado. Al tocarlo se abrirá esta idea.")
            if (!puedeNotificar(app)) TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Permitir notificaciones") }
            if (hayAviso) TextButton(enabled = !guardando, onClick = { guardar(true) }) { Text("Cancelar recordatorio") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !guardando && !cargando && minuto != null, onClick = { guardar(false) }) { Text("Guardar") } },
        dismissButton = { TextButton(enabled = !guardando, onClick = salir) { Text("Volver") } })
}
