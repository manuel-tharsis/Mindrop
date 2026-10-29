package com.mindrop.app.ui.proyectos

import com.mindrop.app.ui.icons.PackIcon as Icon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.ui.rpg.FormaPixel
import com.mindrop.app.ui.rpg.marcoRpg
import com.mindrop.app.ui.rpg.IconoRpg
import com.mindrop.app.ui.home.components.EmptyState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProyectosRoute(vm: ProyectosViewModel, pdf: ExportarPdfViewModel? = null, abrir: (Long) -> Unit) {
    val proyectos by vm.proyectos.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val operando by vm.operando.collectAsStateWithLifecycle()
    val exportacion = pdf?.uiState?.collectAsStateWithLifecycle()?.value ?: EstadoExportacionPdf()
    val destinoPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { pdf?.guardar(it) }
    LaunchedEffect(exportacion.archivo?.archivo?.absolutePath, exportacion.destinoSolicitado, exportacion.trabajando) {
        val archivo = exportacion.archivo
        if (archivo != null && !exportacion.destinoSolicitado && !exportacion.trabajando) {
            pdf?.solicitarDestino(); destinoPdf.launch(archivo.nombre)
        }
    }
    var editarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var nombreInicial by rememberSaveable { mutableStateOf("") }
    var descartar by rememberSaveable { mutableStateOf(false) }
    var eliminarId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(vm) {
        vm.resultados.collect { nuevoId ->
            editarId = null; eliminarId = null
            nuevoId?.let(abrir)
        }
    }
    fun cerrarEditor() {
        if (operando) return
        if (nombre != nombreInicial) descartar = true else editarId = null
    }
    Scaffold(
        topBar = { MediumTopAppBar(title = { Text("Proyectos", style = MaterialTheme.typography.headlineMedium) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nombre = ""; nombreInicial = ""; vm.error.value = null; editarId = 0
            }, shape = FormaPixel) { Text("＋ Nuevo proyecto", Modifier.padding(horizontal = 14.dp)) }
        },
    ) { espacio ->
        LazyColumn(Modifier.fillMaxSize().padding(espacio), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Un cuaderno para desarrollar cada proyecto.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (proyectos.isEmpty()) item {
                EmptyState("Tu próximo proyecto empieza aquí", "Crea un cuaderno y desarrolla una idea en cada página.", compact = true)
            }
            items(proyectos, key = { it.proyecto.id }) { resumen ->
                val proyecto = resumen.proyecto
                Card(onClick = { if (!exportacion.trabajando) abrir(proyecto.id) }, shape = FormaPixel,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().marcoRpg(MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconoRpg("proyecto", Modifier.size(38.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(proyecto.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (resumen.numeroPaginas == 1) "1 página" else "${resumen.numeroPaginas} páginas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (pdf != null) OutlinedIconButton(onClick = { pdf.preparar(proyecto.id) }, enabled = !operando && !exportacion.trabajando,
                            shape = FormaPixel, modifier = Modifier.size(width = 48.dp, height = 56.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.ic_pdf_export), "Exportar ${proyecto.nombre} a PDF", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text("PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        var menu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menu = true }, enabled = !operando) {
                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "Opciones de ${proyecto.nombre}")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Editar nombre") }, onClick = {
                                    menu = false; vm.error.value = null; nombre = proyecto.nombre; nombreInicial = nombre; editarId = proyecto.id
                                })
                                DropdownMenuItem(text = { Text("Eliminar proyecto") }, onClick = {
                                    menu = false; vm.error.value = null; eliminarId = proyecto.id
                                })
                            }
                        }
                    }
                }
            }
            if (editarId == null && eliminarId == null) error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            exportacion.mensaje?.let { mensaje -> item {
                Column {
                    Text(mensaje, color = if (exportacion.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    if (exportacion.error && exportacion.archivo != null) TextButton(onClick = { pdf?.reintentar() }) { Text("Elegir otra ubicación") }
                }
            } }
        }
    }
    editarId?.let { id ->
        AlertDialog(onDismissRequest = ::cerrarEditor,
            title = { Text(if (id == 0L) "Nuevo proyecto" else "Editar proyecto") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, enabled = !operando,
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(enabled = !operando, onClick = { vm.guardar(id.takeUnless { it == 0L }, nombre) }) { Text("Guardar") } },
            dismissButton = { TextButton(enabled = !operando, onClick = ::cerrarEditor) { Text("Cancelar") } })
    }
    if (descartar) AlertDialog(onDismissRequest = { descartar = false }, title = { Text("¿Descartar los cambios?") },
        confirmButton = { TextButton(onClick = { descartar = false; editarId = null }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { descartar = false }) { Text("Seguir editando") } })
    eliminarId?.let { id ->
        val resumen = proyectos.firstOrNull { it.proyecto.id == id }
        AlertDialog(onDismissRequest = { if (!operando) eliminarId = null },
            title = { Text("¿Eliminar ${resumen?.proyecto?.nombre ?: "el proyecto"}?") },
            text = { Text(error ?: "Se eliminarán el cuaderno y todas sus páginas (${resumen?.numeroPaginas ?: 0}). No se podrá deshacer. Las ideas de Mindrop no se borrarán.") },
            confirmButton = { TextButton(enabled = !operando, onClick = { vm.eliminar(id) }) { Text("Eliminar proyecto") } },
            dismissButton = { TextButton(enabled = !operando, onClick = { eliminarId = null }) { Text("Cancelar") } })
    }
    if (exportacion.trabajando) AlertDialog(onDismissRequest = {}, title = { Text("Exportando cuaderno…") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparando todas las páginas, imágenes y bocetos.") } }, confirmButton = {})
}
