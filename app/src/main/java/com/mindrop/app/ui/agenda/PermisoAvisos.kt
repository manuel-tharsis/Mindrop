package com.mindrop.app.ui.agenda

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mindrop.app.notifications.puedeNotificar

internal data class PermisoAvisos(val disponibles: Boolean, val solicitar: (guardarDespues: Boolean) -> Unit)

/** Compartido por el editor completo y la entrada rápida del escritorio. */
@Composable
internal fun rememberPermisoAvisos(alGuardar: () -> Unit): PermisoAvisos {
    val contexto = LocalContext.current
    var activos by remember { mutableStateOf(puedeNotificar(contexto)) }
    var guardarTrasPermiso by rememberSaveable { mutableStateOf(false) }
    var permisoRechazado by rememberSaveable { mutableStateOf(false) }
    val guardarActual by rememberUpdatedState(alGuardar)
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        activos = puedeNotificar(contexto)
        permisoRechazado = !concedido
        if (concedido && activos && guardarTrasPermiso) guardarActual()
        guardarTrasPermiso = false
    }
    LifecycleResumeEffect(contexto) {
        activos = puedeNotificar(contexto)
        onPauseOrDispose { }
    }
    return PermisoAvisos(activos) { alGuardar ->
        if (!permisoRechazado && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(contexto,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            guardarTrasPermiso = alGuardar
            permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val ajustes = if (Build.VERSION.SDK_INT >= 26) {
                val canalBloqueado = contexto.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel("mindrop_local")?.importance == NotificationManager.IMPORTANCE_NONE
                if (canalBloqueado) Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, "mindrop_local")
                else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
            } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${contexto.packageName}".toUri())
            contexto.startActivity(ajustes)
        }
    }
}
