package com.mindrop.app.ui.agenda

import androidx.lifecycle.*
import com.mindrop.app.data.local.entity.EventoAgenda
import com.mindrop.app.data.repository.RepositorioAgenda
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgendaViewModel(private val repositorio: RepositorioAgenda) : ViewModel() {
    val eventos = repositorio.observarEventos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val error = MutableStateFlow<String?>(null)
    val guardando = MutableStateFlow(false)
    private val canal = Channel<Unit>(Channel.BUFFERED)
    val guardados = canal.receiveAsFlow()
    fun guardar(evento: EventoAgenda) = ejecutar { repositorio.guardar(evento) }
    fun eliminar(id: Long) = ejecutar { repositorio.eliminar(id) }
    private fun ejecutar(accion: suspend () -> Unit) {
        if (guardando.value) return
        guardando.value = true
        error.value = null
        viewModelScope.launch {
            try { accion(); canal.send(Unit) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                error.value = e.message ?: "No se pudo guardar."
            } finally { guardando.value = false }
        }
    }
}
