package com.mindrop.app.ui.proyectos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindrop.app.R

@Composable
internal fun ProyectoCard(
    nombre: String, paginas: Int, abrir: () -> Unit, editar: () -> Unit,
    eliminar: () -> Unit, exportar: (() -> Unit)?, habilitado: Boolean = true,
    exportando: Boolean = false,
) {
    var menu by remember { mutableStateOf(false) }
    Card(onClick = abrir, enabled = habilitado && !exportando, shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PortadaCuaderno()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(nombre, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (paginas == 1) "1 página" else "$paginas páginas",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                if (exportar != null) IconButton(onClick = exportar, enabled = habilitado && !exportando) {
                    Icon(painterResource(R.drawable.ic_pdf_export), "Exportar $nombre a PDF")
                }
                Box {
                    IconButton(enabled = habilitado, onClick = { menu = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), "Opciones de $nombre")
                    }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Editar nombre") }, onClick = { menu = false; editar() })
                        DropdownMenuItem(text = { Text("Eliminar proyecto") }, onClick = { menu = false; eliminar() })
                    }
                }
            }
        }
    }
}
