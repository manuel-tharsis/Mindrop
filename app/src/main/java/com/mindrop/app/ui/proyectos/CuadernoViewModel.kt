package com.mindrop.app.ui.proyectos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import com.mindrop.app.data.repository.RepositorioProyectos
import com.mindrop.app.data.local.entity.ImagenPagina
import com.mindrop.app.data.cuaderno.TrazoDibujo
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BorradorPagina(val id: Long, val titulo: String, val texto: String)
data class EstadoCuaderno(
    val cargando: Boolean = true,
    val proyecto: Proyecto? = null,
    val paginas: List<PaginaProyecto> = emptyList(),
    val imagenes: List<ImagenPagina> = emptyList(),
    val paginaActualId: Long? = null,
    val borrador: BorradorPagina? = null,
    val editando: Boolean = false,
    val pendiente: Boolean = false,
    val guardando: Boolean = false,
    val operando: Boolean = false,
    val error: String? = null,
)

class CuadernoViewModel(
    private val proyectoId: Long,
    private val repositorio: RepositorioProyectos,
    private val estadoGuardado: SavedStateHandle,
) : ViewModel() {
    private val estado = MutableStateFlow(EstadoCuaderno(editando = estadoGuardado["editando"] ?: false))
    val uiState = estado.asStateFlow()
    private val cerrojoGuardado = Mutex()
    private var autoguardado: Job? = null
    private val canalSalida = Channel<Unit>(Channel.BUFFERED)
    val salidas = canalSalida.receiveAsFlow()

    init {
        viewModelScope.launch { repositorio.observarImagenes(proyectoId).collect { imagenes -> estado.update { it.copy(imagenes = imagenes) } } }
        viewModelScope.launch {
            combine(repositorio.observarProyecto(proyectoId), repositorio.observarPaginas(proyectoId)) { p, hojas -> p to hojas }
                .collect { (proyecto, hojas) ->
                    estado.update { anterior ->
                        // La selección explícita prevalece aunque Room entregue una emisión anterior mientras se añade una hoja.
                        val preferida = estadoGuardado.get<Long>("pagina")?.takeIf { it > 0 } ?: anterior.paginaActualId ?: proyecto?.ultimaPaginaId
                        val actual = preferida?.takeIf { id -> hojas.any { it.id == id } } ?: hojas.firstOrNull()?.id
                        anterior.copy(cargando = false, proyecto = proyecto, paginas = hojas, paginaActualId = actual)
                    }
                }
        }
    }

    fun editar(valor: Boolean) {
        if (estado.value.operando) return
        estadoGuardado["editando"] = valor
        estado.update { it.copy(editando = valor) }
        if (!valor) guardarAhora()
    }

    fun cambiarTitulo(id: Long, texto: String) = cambiar(id) { copy(titulo = texto) }
    fun cambiarTexto(id: Long, texto: String) = cambiar(id) { copy(texto = texto) }

    private fun cambiar(id: Long, transformacion: BorradorPagina.() -> BorradorPagina) {
        val actual = estado.value
        if (actual.operando || actual.paginaActualId != id) return
        val hoja = actual.paginas.firstOrNull { it.id == id } ?: return
        val borrador = actual.borrador?.takeIf { it.id == id } ?: BorradorPagina(id, hoja.titulo, hoja.texto)
        estado.update { it.copy(borrador = borrador.transformacion(), pendiente = true, error = null) }
        autoguardado?.cancel()
        autoguardado = viewModelScope.launch {
            delay(400)
            intentarGuardar()
        }
    }

    private suspend fun guardarPendiente() = cerrojoGuardado.withLock {
        val actual = estado.value
        if (!actual.pendiente) return@withLock
        val borrador = actual.borrador ?: return@withLock
        estado.update { it.copy(guardando = true) }
        try {
            repositorio.guardar(borrador.id, borrador.titulo, borrador.texto)
            estado.update {
                it.copy(pendiente = if (it.borrador == borrador) false else it.pendiente, error = null)
            }
        } finally { estado.update { it.copy(guardando = false) } }
    }

    private suspend fun intentarGuardar(): Boolean {
        try { guardarPendiente(); return true }
        catch (e: Exception) {
            if (e is CancellationException) throw e
            estado.update { it.copy(error = "No se pudo guardar. Tu texto sigue en pantalla; vuelve a intentarlo.") }
            return false
        }
    }

    fun guardarAhora() { viewModelScope.launch { intentarGuardar() } }
    fun limpiarError() { estado.update { it.copy(error = null) } }

    fun seleccionar(id: Long) {
        if (estado.value.paginaActualId == id) return
        ejecutar {
            repositorio.recordarPagina(proyectoId, id)
            estadoGuardado["pagina"] = id
            estado.update { it.copy(paginaActualId = id, borrador = null, editando = false) }
            estadoGuardado["editando"] = false
        }
    }

    fun anadirPagina() = ejecutar {
        val id = repositorio.anadirPagina(proyectoId)
        val paginas = repositorio.paginas(proyectoId)
        estadoGuardado["pagina"] = id
        estadoGuardado["editando"] = true
        estado.update { it.copy(paginas = paginas, paginaActualId = id, borrador = null, editando = true) }
    }

    fun moverPagina(id: Long, desplazamiento: Int) = ejecutar { repositorio.moverPagina(proyectoId, id, desplazamiento) }
    fun importarImagen(paginaId: Long, uri: Uri) = ejecutar { repositorio.importarImagen(proyectoId, paginaId, uri) }
    fun guardarDibujo(paginaId: Long, trazos: List<TrazoDibujo>, alGuardar: () -> Unit) = ejecutar {
        repositorio.guardarDibujo(proyectoId, paginaId, trazos)
        alGuardar()
    }
    fun eliminarImagen(imagenId: Long) = ejecutar { repositorio.eliminarImagen(proyectoId, imagenId) }

    fun eliminarPagina(id: Long) = ejecutar {
        repositorio.eliminarPaginaConDeshacer(proyectoId, id)
        val paginas = repositorio.paginas(proyectoId)
        val actual = if (estado.value.paginaActualId == id) paginas.firstOrNull()?.id else estado.value.paginaActualId
        actual?.let { repositorio.recordarPagina(proyectoId, it) }
        estadoGuardado["pagina"] = actual
        estado.update { it.copy(paginas = paginas, paginaActualId = actual, borrador = null, editando = false) }
        estadoGuardado["editando"] = false
    }

    fun salir() = ejecutar {
        estado.value.paginaActualId?.let { repositorio.recordarPagina(proyectoId, it) }
        canalSalida.send(Unit)
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        if (estado.value.operando) return
        estado.update { it.copy(operando = true, error = null) }
        viewModelScope.launch {
            try {
                guardarPendiente()
                accion()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                estado.update { it.copy(error = e.message ?: "No se pudo guardar el cuaderno.") }
            } finally { estado.update { it.copy(operando = false) } }
        }
    }
}
