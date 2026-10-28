package com.mindrop.app.ui.proyectos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.PaginaProyecto
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val PapelCuaderno = Color(0xFFF5F1E9)
private val TintaCuaderno = Color(0xFF292B3E)

@Composable
fun CuadernoRoute(vm: CuadernoViewModel, salir: () -> Unit) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val propietario = LocalLifecycleOwner.current
    LaunchedEffect(vm) { vm.salidas.collect { salir() } }
    DisposableEffect(propietario, vm) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) vm.guardarAhora()
        }
        propietario.lifecycle.addObserver(observador)
        onDispose { propietario.lifecycle.removeObserver(observador) }
    }
    BackHandler { vm.salir() }
    CuadernoScreen(estado, vm::seleccionar, vm::cambiarTitulo, vm::cambiarTexto, vm::editar,
        vm::anadirPagina, vm::moverPagina, vm::eliminarPagina, vm::guardarAhora, vm::salir)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuadernoScreen(
    estado: EstadoCuaderno,
    seleccionar: (Long) -> Unit,
    cambiarTitulo: (Long, String) -> Unit,
    cambiarTexto: (Long, String) -> Unit,
    editar: (Boolean) -> Unit,
    anadir: () -> Unit,
    mover: (Long, Int) -> Unit,
    eliminar: (Long) -> Unit,
    reintentar: () -> Unit,
    salir: () -> Unit,
) {
    var indiceAbierto by rememberSaveable { mutableStateOf(false) }
    var borrarId by rememberSaveable { mutableStateOf<Long?>(null) }
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    fun terminarEdicion() { foco.clearFocus(); teclado?.hide(); editar(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(estado.proyecto?.nombre ?: "Cuaderno", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(enabled = !estado.operando, onClick = salir) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver a proyectos")
            } }, actions = {
                TextButton(enabled = !estado.cargando && estado.proyecto != null && !estado.operando, onClick = {
                    terminarEdicion(); indiceAbierto = true
                }) { Text("Índice") }
            }) },
    ) { espacio ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(espacio), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            estado.proyecto == null -> Box(Modifier.fillMaxSize().padding(espacio), contentAlignment = Alignment.Center) { Text("El proyecto ya no existe.") }
            estado.paginas.isEmpty() -> Box(Modifier.fillMaxSize().padding(espacio), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> {
                val posicion = estado.paginas.indexOfFirst { it.id == estado.paginaActualId }.coerceAtLeast(0)
                val pager = rememberPagerState(initialPage = posicion, pageCount = { estado.paginas.size })
                val ambito = rememberCoroutineScope()
                LaunchedEffect(estado.paginaActualId, estado.paginas.map { it.id }) {
                    val destino = estado.paginas.indexOfFirst { it.id == estado.paginaActualId }
                    if (destino >= 0 && !pager.isScrollInProgress && pager.currentPage != destino) pager.scrollToPage(destino)
                }
                LaunchedEffect(pager, estado.paginas.map { it.id }) {
                    snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect { pagina ->
                        estado.paginas.getOrNull(pagina)?.let { seleccionar(it.id) }
                    }
                }
                Column(Modifier.fillMaxSize().padding(espacio).imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cuaderno personal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Página ${pager.currentPage + 1} de ${estado.paginas.size}", style = MaterialTheme.typography.labelLarge)
                    }
                    HorizontalPager(state = pager, key = { estado.paginas[it].id },
                        userScrollEnabled = !estado.editando && !estado.operando,
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("paginas_cuaderno"), beyondViewportPageCount = 0) { numero ->
                        val pagina = estado.paginas[numero]
                        val borrador = estado.borrador?.takeIf { it.id == pagina.id }
                        HojaCuaderno(pagina.copy(titulo = borrador?.titulo ?: pagina.titulo, texto = borrador?.texto ?: pagina.texto),
                            numero + 1, estado.editando && pagina.id == estado.paginaActualId, !estado.operando,
                            cambiarTitulo, cambiarTexto)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(enabled = pager.currentPage > 0 && !estado.editando && !estado.operando,
                            onClick = { ambito.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Text("‹ Anterior") }
                        Text("${pager.currentPage + 1} / ${estado.paginas.size}", style = MaterialTheme.typography.labelMedium)
                        TextButton(enabled = pager.currentPage < estado.paginas.lastIndex && !estado.editando && !estado.operando,
                            onClick = { ambito.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("Siguiente ›") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = !estado.operando, onClick = {
                            if (estado.editando) terminarEdicion() else editar(true)
                        }) { Text(if (estado.editando) "Terminar edición" else "Editar página") }
                        TextButton(enabled = !estado.operando && !estado.editando, onClick = anadir) { Text("＋ Añadir página") }
                    }
                    if (estado.error != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(estado.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = reintentar) { Text("Reintentar") }
                    } else Text(if (estado.guardando || estado.pendiente) "Guardando…" else "Guardado automáticamente",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (indiceAbierto) ModalBottomSheet(onDismissRequest = { indiceAbierto = false }) {
        LazyColumn(contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { Text("Índice", style = MaterialTheme.typography.titleLarge) }
            itemsIndexed(estado.paginas, key = { _, p -> p.id }) { numero, pagina ->
                Column {
                    TextButton(enabled = !estado.operando, onClick = { seleccionar(pagina.id); indiceAbierto = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("${numero + 1}. ${pagina.titulo.ifBlank { "Página ${numero + 1}" }}", modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(enabled = numero > 0 && !estado.operando, onClick = { mover(pagina.id, -1) }) { Text("Subir") }
                        TextButton(enabled = numero < estado.paginas.lastIndex && !estado.operando, onClick = { mover(pagina.id, 1) }) { Text("Bajar") }
                        TextButton(enabled = !estado.operando, onClick = { borrarId = pagina.id }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    borrarId?.let { id ->
        AlertDialog(onDismissRequest = { borrarId = null }, title = { Text("¿Eliminar esta página?") },
            text = { Text("Su texto se borrará y no se podrá recuperar. Si es la última página, el cuaderno conservará una hoja vacía.") },
            confirmButton = { TextButton(enabled = !estado.operando, onClick = { borrarId = null; eliminar(id) }) { Text("Eliminar página") } },
            dismissButton = { TextButton(onClick = { borrarId = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun HojaCuaderno(pagina: PaginaProyecto, numero: Int, editando: Boolean, habilitada: Boolean,
    titulo: (Long, String) -> Unit, texto: (Long, String) -> Unit) {
    val campos = OutlinedTextFieldDefaults.colors(focusedTextColor = TintaCuaderno, unfocusedTextColor = TintaCuaderno,
        disabledTextColor = TintaCuaderno, cursorColor = TintaCuaderno, focusedBorderColor = Color(0xFF86728E),
        unfocusedBorderColor = Color(0xFFBEB1C2), focusedLabelColor = TintaCuaderno, unfocusedLabelColor = TintaCuaderno)
    LazyColumn(modifier = Modifier.fillMaxSize().background(PapelCuaderno, RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp))
        .border(1.dp, Color(0xFFD5C9BA), RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)),
        contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("PÁGINA $numero", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B5C6B)) }
        if (editando) {
            item { OutlinedTextField(pagina.titulo, { titulo(pagina.id, it) }, modifier = Modifier.fillMaxWidth(), enabled = habilitada,
                label = { Text("Título (opcional)") }, colors = campos, textStyle = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(pagina.texto, { texto(pagina.id, it) }, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 480.dp), enabled = habilitada,
                label = { Text("Escribe aquí…") }, minLines = 8, colors = campos) }
        } else {
            item { Text(pagina.titulo.ifBlank { "Página $numero" }, style = MaterialTheme.typography.headlineSmall, color = TintaCuaderno) }
            item { Text(pagina.texto.ifBlank { "Esta página está lista para tus ideas." }, style = MaterialTheme.typography.bodyLarge, color = TintaCuaderno) }
        }
    }
}
