package com.mindrop.app.ui.proyectos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.ui.rpg.FormaPixel
import com.mindrop.app.ui.rpg.marcoRpg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProyectosRoute(vm: ProyectosViewModel, abrir: (Long) -> Unit) {
    val proyectos by vm.proyectos.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val operando by vm.operando.collectAsStateWithLifecycle()
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
        topBar = { TopAppBar(title = { Text("Proyectos") }) },
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
                Text("Todavía no tienes proyectos. Crea uno para empezar tu primer cuaderno.", Modifier.padding(vertical = 24.dp))
            }
            items(proyectos, key = { it.proyecto.id }) { resumen ->
                val proyecto = resumen.proyecto
                Card(onClick = { abrir(proyecto.id) }, shape = FormaPixel,
                    modifier = Modifier.fillMaxWidth().marcoRpg(MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("▤", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.headlineMedium)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(proyecto.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (resumen.numeroPaginas == 1) "1 página" else "${resumen.numeroPaginas} páginas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Abrir cuaderno →", color = MaterialTheme.colorScheme.primary)
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
}
