package com.mindrop.app.ui.rpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindrop.app.MindropApplication
import com.mindrop.app.data.preferences.PreferenciasRpg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RpgViewModel(private val app: MindropApplication) : ViewModel() {
    val preferencias = app.preferencias.preferencias.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenciasRpg())
    val logros = app.logros.observarLogros().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val metricas = app.logros.observarMetricas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ideas = app.ideaRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val error = MutableStateFlow<String?>(null)
    fun aplicar(categoria: String, clave: String) = ejecutar { app.logros.aplicar(categoria, clave) }
    fun movimiento(reducir: Boolean) = ejecutar { app.preferencias.movimiento(reducir) }
    fun lectura(tamano: Int, oscuro: Boolean) = ejecutar { app.preferencias.lectura(tamano, oscuro) }
    fun redescubrir(dias: Int, desde: Int, hasta: Int) = ejecutar {
        app.preferencias.redescubrimiento(dias, desde, hasta)
        if (dias == 0) app.programador.desactivarRedescubrimiento() else app.programador.activarRedescubrimiento()
    }
    fun celebrar(clave: String) = ejecutar { app.logros.celebrar(clave) }
    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try { accion(); error.value = null }
            catch (e: Exception) { if (e is CancellationException) throw e; error.value = e.message ?: "No se pudo aplicar el cambio." }
        }
    }
}
