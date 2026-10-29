package com.mindrop.app.ui.proyectos

import com.mindrop.app.ui.icons.PackIcon as Icon

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.PaginaProyecto
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.mindrop.app.data.preferences.PreferenciasRpg
import com.mindrop.app.data.local.entity.ImagenPagina
import com.mindrop.app.ui.rpg.FormaPixel
import com.mindrop.app.ui.rpg.movimientoPermitido
import androidx.lifecycle.viewmodel.compose.viewModel

private val PapelCuaderno = Color(0xFFF5F1E9)
private val TintaCuaderno = Color(0xFF292B3E)

@Composable
fun CuadernoRoute(vm: CuadernoViewModel, preferencias: PreferenciasRpg = PreferenciasRpg(), salir: () -> Unit) {
    val estado by vm.uiState.collectAsStateWithLifecycle()
    val dibujo: DibujoViewModel = viewModel()
    var paginaFoto by rememberSaveable { mutableStateOf<Long?>(null) }
    val foto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pagina = paginaFoto
        paginaFoto = null
        if (uri != null && pagina != null) vm.importarImagen(pagina, uri)
    }
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
        vm::anadirPagina, vm::moverPagina, vm::eliminarPagina, vm::guardarAhora, vm::salir, preferencias,
        anadirFoto = { id -> paginaFoto = id; foto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        dibujar = { id -> vm.limpiarError(); dibujo.abrir(id) }, eliminarImagen = vm::eliminarImagen)
    DibujoDialog(dibujo, estado.operando, estado.error) { paginaId, trazos -> vm.guardarDibujo(paginaId, trazos, dibujo::cerrar) }
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
    preferencias: PreferenciasRpg = PreferenciasRpg(),
    anadirFoto: (Long) -> Unit = {},
    dibujar: (Long) -> Unit = {},
    eliminarImagen: (Long) -> Unit = {},
) {
    var indiceAbierto by rememberSaveable { mutableStateOf(false) }
    var borrarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var borrarImagenId by rememberSaveable { mutableStateOf<Long?>(null) }
    val movimiento = movimientoPermitido()
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    fun terminarEdicion() { foco.clearFocus(); teclado?.hide(); editar(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(estado.proyecto?.nombre ?: "Cuaderno", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(enabled = !estado.operando, onClick = salir) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver a proyectos")
            } }, actions = {
                IconButton(enabled = !estado.cargando && estado.proyecto != null && !estado.operando, onClick = {
                    terminarEdicion(); indiceAbierto = true
                }) { Icon(painterResource(R.drawable.ic_list), "Índice") }
            }) },
    ) { espacio ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(espacio), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            estado.proyecto == null -> Box(Modifier.fillMaxSize().padding(espacio), contentAlignment = Alignment.Center) { Text("El proyecto ya no existe.") }
            estado.paginas.isEmpty() -> Box(Modifier.fillMaxSize().padding(espacio).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Página eliminada. Puedes deshacer el borrado desde el aviso inferior.")
            }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${pager.currentPage + 1} / ${estado.paginas.size}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                        AccionCuaderno(if (estado.editando) R.drawable.ic_check else R.drawable.ic_edit,
                            if (estado.editando) "Terminar edición" else "Editar página", !estado.operando) {
                            if (estado.editando) terminarEdicion() else editar(true)
                        }
                        AccionCuaderno(R.drawable.ic_page_add, "Añadir página", !estado.operando, MaterialTheme.colorScheme.secondary) {
                            terminarEdicion(); anadir()
                        }
                        AccionCuaderno(R.drawable.ic_image_add, "Añadir imagen", !estado.operando) {
                            terminarEdicion(); estado.paginaActualId?.let(anadirFoto)
                        }
                        AccionCuaderno(R.drawable.ic_draw_note, "Dibujar nota", !estado.operando, MaterialTheme.colorScheme.tertiary) {
                            terminarEdicion(); estado.paginaActualId?.let(dibujar)
                        }
                    }
                    HorizontalPager(state = pager, key = { estado.paginas[it].id },
                        userScrollEnabled = !estado.editando && !estado.operando,
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("paginas_cuaderno"), beyondViewportPageCount = 0) { numero ->
                        val pagina = estado.paginas[numero]
                        val borrador = estado.borrador?.takeIf { it.id == pagina.id }
                        HojaCuaderno(pagina.copy(titulo = borrador?.titulo ?: pagina.titulo, texto = borrador?.texto ?: pagina.texto),
                            numero + 1, estado.editando && pagina.id == estado.paginaActualId, !estado.operando,
                            cambiarTitulo, cambiarTexto, preferencias, estado.imagenes.filter { it.paginaId == pagina.id },
                            eliminarImagen = { borrarImagenId = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(enabled = pager.currentPage > 0 && !estado.editando && !estado.operando, shape = CircleShape,
                            onClick = { ambito.launch { if (movimiento) pager.animateScrollToPage(pager.currentPage - 1) else pager.scrollToPage(pager.currentPage - 1) } }) {
                            Icon(painterResource(R.drawable.ic_arrow_back), "Página anterior")
                        }
                        Text(if (estado.guardando || estado.pendiente || estado.operando) "Guardando…" else "✓ Guardado",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedIconButton(enabled = pager.currentPage < estado.paginas.lastIndex && !estado.editando && !estado.operando, shape = CircleShape,
                            onClick = { ambito.launch { if (movimiento) pager.animateScrollToPage(pager.currentPage + 1) else pager.scrollToPage(pager.currentPage + 1) } }) {
                            Icon(painterResource(R.drawable.ic_arrow_back), "Página siguiente", modifier = Modifier.graphicsLayer { rotationZ = 180f })
                        }
                    }
                    if (estado.error != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(estado.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = reintentar) { Text("Reintentar") }
                    }
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
            text = { Text("Se eliminarán su texto, imágenes y bocetos. Tendrás unos segundos para deshacerlo. Si es la última página, quedará una hoja vacía.") },
            confirmButton = { TextButton(enabled = !estado.operando, onClick = { borrarId = null; eliminar(id) }) { Text("Eliminar página") } },
            dismissButton = { TextButton(onClick = { borrarId = null }) { Text("Cancelar") } })
    }
    borrarImagenId?.let { id ->
        AlertDialog(onDismissRequest = { borrarImagenId = null }, title = { Text("¿Eliminar esta imagen?") },
            text = { Text("La imagen o el boceto se eliminará de esta página definitivamente.") },
            confirmButton = { TextButton(onClick = { borrarImagenId = null; eliminarImagen(id) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { borrarImagenId = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun AccionCuaderno(icono: Int, descripcion: String, habilitada: Boolean,
    tinte: Color = MaterialTheme.colorScheme.primary, accion: () -> Unit) {
    OutlinedIconButton(onClick = accion, enabled = habilitada, shape = FormaPixel, border = BorderStroke(1.dp, tinte.copy(alpha = .3f)),
        modifier = Modifier.size(48.dp)) {
        Icon(painterResource(icono), descripcion, tint = if (habilitada) tinte else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HojaCuaderno(pagina: PaginaProyecto, numero: Int, editando: Boolean, habilitada: Boolean,
    titulo: (Long, String) -> Unit, texto: (Long, String) -> Unit, preferencias: PreferenciasRpg,
    imagenes: List<ImagenPagina>, eliminarImagen: (Long) -> Unit) {
    val tinta = if (preferencias.papelOscuro) Color(0xFFE9E4DB) else TintaCuaderno
    val papel = if (preferencias.papelOscuro) Color(0xFF242638) else PapelCuaderno
    val secundario = if (preferencias.papelOscuro) Color(0xFFBEB7CA) else Color(0xFF6B5C6B)
    val estiloLectura = MaterialTheme.typography.bodyLarge.copy(fontSize = preferencias.tamanoLectura.sp,
        lineHeight = (preferencias.tamanoLectura * 1.6f).sp)
    val campos = OutlinedTextFieldDefaults.colors(focusedTextColor = tinta, unfocusedTextColor = tinta,
        disabledTextColor = tinta, cursorColor = tinta, focusedBorderColor = secundario,
        unfocusedBorderColor = secundario.copy(alpha = .6f), focusedLabelColor = tinta, unfocusedLabelColor = tinta)
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("hoja_cuaderno_$numero").background(papel, RoundedCornerShape(6.dp, 18.dp, 18.dp, 6.dp))
        .border(1.dp, secundario.copy(alpha = .25f), RoundedCornerShape(6.dp, 18.dp, 18.dp, 6.dp)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text("PÁGINA $numero", style = MaterialTheme.typography.labelMedium, color = secundario) }
        if (editando) {
            item { OutlinedTextField(pagina.titulo, { titulo(pagina.id, it) }, modifier = Modifier.fillMaxWidth(), enabled = habilitada,
                label = { Text("Título (opcional)") }, colors = campos, textStyle = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(pagina.texto, { texto(pagina.id, it) }, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 480.dp), enabled = habilitada,
                label = { Text("Escribe aquí…") }, minLines = 8, colors = campos, textStyle = estiloLectura) }
        } else {
            item { Text(pagina.titulo.ifBlank { "Página $numero" }, style = MaterialTheme.typography.headlineSmall, color = tinta) }
            item { Text(pagina.texto.ifBlank { "Esta página está lista para tus ideas." }, style = estiloLectura, color = tinta) }
        }
        items(imagenes, key = { "imagen-${it.id}" }) { imagen -> ImagenCuaderno(imagen, editando, habilitada, tinta, eliminarImagen) }
    }
}
