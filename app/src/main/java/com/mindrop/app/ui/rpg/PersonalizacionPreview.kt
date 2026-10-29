package com.mindrop.app.ui.rpg

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.icons.ThemedIcon
import kotlinx.coroutines.launch

@Composable
fun MuestraPackIconos(clave: String, compacto: Boolean = false) {
    CompositionLocalProvider(LocalDiseno provides clave) {
        if (compacto) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(R.drawable.ic_folder, R.drawable.ic_idea, R.drawable.ic_project, R.drawable.ic_settings).chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { fila.forEach { ThemedIcon(it, null, Modifier.size(28.dp)) } }
                }
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(R.drawable.ic_folder, R.drawable.ic_idea, R.drawable.ic_project,
                R.drawable.ic_mobile, R.drawable.ic_code, R.drawable.ic_settings).forEach {
                ThemedIcon(it, null, Modifier.size(30.dp))
            }
        }
    }
}

/** Fotograma visible al entrar; reproducción finita y lenta únicamente en la muestra. */
@Composable
fun MuestraEfecto(clave: String) {
    val fase = remember(clave) { Animatable(.65f) }
    val scope = rememberCoroutineScope()
    val movimiento = movimientoPermitido()
    var reproduciendo by remember(clave) { mutableStateOf(false) }
    LaunchedEffect(movimiento) { if (!movimiento) { fase.stop(); fase.snapTo(.65f); reproduciendo = false } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            IconoRpg("carpeta", Modifier.size(32.dp))
            DibujoDestello(fase.value, clave, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f)) {
            Text("Vista ampliada", style = MaterialTheme.typography.labelMedium)
            Text("Reproducción lenta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(enabled = movimiento && !reproduciendo, onClick = {
                reproduciendo = true
                scope.launch {
                    try { fase.snapTo(0f); fase.animateTo(1f, tween(2400)); fase.snapTo(.65f) }
                    finally { reproduciendo = false }
                }
            }) { Text(if (reproduciendo) "Reproduciendo…" else "Probar efecto") }
        }
    }
}
