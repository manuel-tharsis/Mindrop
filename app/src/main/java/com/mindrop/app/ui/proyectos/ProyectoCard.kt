package com.mindrop.app.ui.proyectos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.icons.PackIcon
import com.mindrop.app.ui.icons.packPainterResource
import com.mindrop.app.ui.rpg.IconoRpg

@Composable
internal fun ProyectoCard(
    nombre: String,
    paginas: Int,
    abrir: () -> Unit,
    editar: () -> Unit,
    eliminar: () -> Unit,
    exportar: (() -> Unit)?,
    habilitado: Boolean = true,
    exportando: Boolean = false,
) {
    var menu by remember { mutableStateOf(false) }
    Card(onClick = { if (!exportando) abrir() }, shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconoRpg("proyecto", Modifier.size(40.dp))
                Text(nombre, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(enabled = habilitado, onClick = { menu = true }) {
                        PackIcon(packPainterResource(R.drawable.ic_more_vert), "Opciones de $nombre")
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Editar nombre") }, onClick = { menu = false; editar() })
                        DropdownMenuItem(text = { Text("Eliminar proyecto") }, onClick = { menu = false; eliminar() })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 52.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (paginas == 1) "1 página" else "$paginas páginas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (exportar != null) TextButton(onClick = exportar, enabled = habilitado && !exportando) {
                    PackIcon(packPainterResource(R.drawable.ic_pdf_export), "Exportar $nombre a PDF", Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
