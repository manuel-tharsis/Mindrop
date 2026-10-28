package com.mindrop.app.ui.busqueda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.dao.PaginaEncontrada
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.RepositorioProyectos
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.ui.home.buildFolderBreadcrumbs
import com.mindrop.app.ui.home.components.EmptyState
import com.mindrop.app.ui.home.components.IdeaCard
import com.mindrop.app.ui.rpg.FormaPixel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Locale

internal fun patronBusqueda(texto: String) = "%" + texto.trim().lowercase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

data class ResultadoIdea(val idea: IdeaEntity, val ubicacion: String)
data class EstadoBusqueda(val ideas: List<ResultadoIdea> = emptyList(), val paginas: List<PaginaEncontrada> = emptyList(),
    val buscando: Boolean = false, val error: String? = null)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BusquedaViewModel(carpetas: FolderRepository, ideas: IdeaRepository, proyectos: RepositorioProyectos, private val guardado: SavedStateHandle) : ViewModel() {
    val consulta = guardado.getStateFlow("consulta", "")
    fun cambiar(texto: String) { guardado["consulta"] = texto }
    val resultados = consulta.debounce(250).flatMapLatest { texto ->
        if (texto.isBlank()) flowOf(EstadoBusqueda())
        else combine(ideas.buscar(patronBusqueda(texto)), proyectos.buscar(patronBusqueda(texto)),
            carpetas.observeAll()) { encontradas, paginas, carpetas ->
            EstadoBusqueda(encontradas.map { idea ->
                val ubicacion = buildFolderBreadcrumbs(carpetas, idea.folderId).joinToString(" › ") { it.name }.ifBlank { "Mis ideas" }
                ResultadoIdea(idea, if (idea.isCompleted) "Completadas · $ubicacion" else ubicacion)
            }, paginas)
        }.onStart { emit(EstadoBusqueda(buscando = true)) }
            .catch { emit(EstadoBusqueda(error = "No se pudo realizar la búsqueda. Vuelve a intentarlo.")) }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EstadoBusqueda())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusquedaScreen(vm: BusquedaViewModel, volver: () -> Unit, abrirIdea: (Long) -> Unit, abrirPagina: (Long, Long) -> Unit) {
    val consulta by vm.consulta.collectAsStateWithLifecycle()
    val estado by vm.resultados.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Buscar") }, navigationIcon = {
        IconButton(onClick = volver) { Icon(painterResource(R.drawable.ic_arrow_back), "Volver") }
    }) }) { margen ->
        LazyColumn(Modifier.fillMaxSize().padding(margen).imePadding(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(consulta, vm::cambiar, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Ideas y páginas de proyectos") }, leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    trailingIcon = { if (consulta.isNotEmpty()) TextButton(onClick = { vm.cambiar("") }) { Text("Borrar") } })
            }
            if (consulta.isBlank()) item { EmptyState("Todo a mano", "Busca por el título o por cualquier parte del texto.", compact = true) }
            else if (estado.buscando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (estado.error != null) item { Text(estado.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            else if (estado.ideas.isEmpty() && estado.paginas.isEmpty()) item { EmptyState("Sin resultados", "Prueba con otras palabras.", compact = true) }
            if (estado.ideas.isNotEmpty()) item { Text("Ideas", style = MaterialTheme.typography.titleLarge) }
            items(estado.ideas, key = { "idea:${it.idea.id}" }) { resultado ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(resultado.ubicacion, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                    IdeaCard(resultado.idea, onClick = { abrirIdea(resultado.idea.id) })
                }
            }
            if (estado.paginas.isNotEmpty()) item { Text("Páginas", style = MaterialTheme.typography.titleLarge) }
            items(estado.paginas, key = { "pagina:${it.id}" }) { pagina ->
                Card(onClick = { abrirPagina(pagina.proyectoId, pagina.id) }, shape = FormaPixel) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(pagina.titulo.ifBlank { "Página ${pagina.numero}" }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${pagina.nombreProyecto} › Página ${pagina.numero}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (estado.ideas.size == 100 || estado.paginas.size == 100) item {
                Text("Se muestran hasta 100 resultados de cada tipo. Concreta la búsqueda para encontrar más.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
