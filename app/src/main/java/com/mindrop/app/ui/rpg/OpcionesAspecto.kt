package com.mindrop.app.ui.rpg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.rpg.Recompensa
import com.mindrop.app.data.rpg.catalogoLogros
import com.mindrop.app.data.rpg.recompensaPermitida
import com.mindrop.app.data.rpg.recompensas
import com.mindrop.app.ui.icons.ThemedIcon
import com.mindrop.app.ui.theme.coloresRpg

@Composable
fun PanelAjustes(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = FormaPixel, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
    }
}

/** Las mismas opciones y reglas de desbloqueo, presentadas como selecciones visuales. */
@Composable
fun OpcionesAspecto(tema: String, diseno: String, efecto: String, logros: Set<String>, elegir: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf("tema" to "Temas y colores", "diseno" to "Packs de iconos", "efecto" to "Animaciones").forEach { (categoria, titulo) ->
            Text(titulo, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            val seleccion = when (categoria) { "tema" -> tema; "diseno" -> diseno; else -> efecto }
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recompensas.filter { it.categoria == categoria }.chunked(if (categoria == "efecto") 1 else 2).forEach { fila ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fila.forEach { opcion ->
                            OpcionAspecto(opcion, seleccion == opcion.clave, recompensaPermitida(categoria, opcion.clave, logros),
                                Modifier.weight(1f).fillMaxHeight()) { elegir(categoria, opcion.clave) }
                        }
                        if (fila.size == 1 && categoria != "efecto") Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionAspecto(opcion: Recompensa, seleccionada: Boolean, permitida: Boolean, modifier: Modifier, elegir: () -> Unit) {
    val acento = MaterialTheme.colorScheme.primary
    Card(modifier = modifier.clip(FormaPixel).selectable(selected = seleccionada, enabled = permitida,
        role = Role.RadioButton, onClick = { if (!seleccionada) elegir() }), shape = FormaPixel,
        border = if (seleccionada) BorderStroke(2.dp, acento) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (opcion.categoria) {
                "tema" -> MaterialTheme(colorScheme = coloresRpg(opcion.clave)) {
                    CompositionLocalProvider(LocalDiseno provides "runa") {
                        Row(Modifier.height(64.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            listOf(R.drawable.ic_folder, R.drawable.ic_project, R.drawable.ic_idea).forEach { ThemedIcon(it, null, Modifier.size(24.dp)) }
                        }
                    }
                }
                "diseno" -> MuestraPackIconos(opcion.clave, compacto = true)
                else -> MuestraEfecto(opcion.clave)
            }
            Text(opcion.nombre, style = MaterialTheme.typography.titleMedium,
                color = if (seleccionada) acento else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            if (opcion.categoria == "efecto") Text(opcion.detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(when { seleccionada -> "✓ Seleccionado"; permitida -> "Elegir"; else -> "Bloqueado" },
                style = MaterialTheme.typography.labelMedium, color = if (seleccionada || permitida) acento else MaterialTheme.colorScheme.onSurfaceVariant)
            if (!permitida) {
                val logro = catalogoLogros.first { it.clave == opcion.logro }
                Text("Necesita: ${logro.nombre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
