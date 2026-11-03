package com.mindrop.app.ui.proyectos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.PaginaProyecto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IndiceCuaderno(
    paginas: List<PaginaProyecto>, actual: Long?, operando: Boolean,
    seleccionar: (Long) -> Unit, mover: (Long, Int) -> Unit,
    eliminar: (Long) -> Unit, anadir: () -> Unit, cerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = cerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Índice", style = MaterialTheme.typography.headlineSmall)
                    Text(if (paginas.size == 1) "1 página" else "${paginas.size} páginas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = cerrar) { Text("Cerrar") }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(paginas, key = { _, p -> p.id }) { numero, pagina ->
                    var menu by remember { mutableStateOf(false) }
                    val seleccionada = pagina.id == actual
                    Card(onClick = { seleccionar(pagina.id) }, enabled = !operando,
                        colors = CardDefaults.cardColors(containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Abrir página ${numero+1}" }) {
                        Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${numero+1}. ${pagina.titulo.ifBlank { "Página ${numero+1}" }}",
                                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (seleccionada) Text("Actual", Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Box {
                                IconButton(enabled = !operando, onClick = { menu = true }) {
                                    Icon(painterResource(R.drawable.ic_more_vert), "Opciones de la página ${numero+1}")
                                }
                                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Subir") }, enabled = numero > 0 && !operando,
                                        onClick = { menu = false; mover(pagina.id, -1) })
                                    DropdownMenuItem(text = { Text("Bajar") }, enabled = numero < paginas.lastIndex && !operando,
                                        onClick = { menu = false; mover(pagina.id, 1) })
                                    DropdownMenuItem(text = { Text("Eliminar") }, enabled = !operando,
                                        onClick = { menu = false; eliminar(pagina.id) })
                                }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = anadir, enabled = !operando, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Icon(painterResource(R.drawable.ic_page_add), null)
                Spacer(Modifier.width(8.dp)); Text("Añadir página")
            }
        }
    }
}
