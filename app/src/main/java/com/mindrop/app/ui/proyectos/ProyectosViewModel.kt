package com.mindrop.app.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProyectosViewModel(private val repositorio: RepositorioProyectos) : ViewModel() {
    val proyectos = repositorio.observarProyectos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val error = MutableStateFlow<String?>(null)
    val operando = MutableStateFlow(false)
    private val canal = Channel<Long?>(Channel.BUFFERED)
    val resultados = canal.receiveAsFlow()

    fun guardar(id: Long?, nombre: String) = ejecutar {
        if (id == null) repositorio.crear(nombre) else { repositorio.renombrar(id, nombre); null }
    }
    fun eliminar(id: Long) = ejecutar { repositorio.eliminar(id); null }

    private fun ejecutar(accion: suspend () -> Long?) {
        if (operando.value) return
        operando.value = true
        error.value = null
        viewModelScope.launch {
            try { canal.send(accion()) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                error.value = e.message ?: "No se pudo guardar el proyecto."
            } finally { operando.value = false }
        }
    }
}
