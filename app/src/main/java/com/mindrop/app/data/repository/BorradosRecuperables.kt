package com.mindrop.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BorradoPendiente(val clave: String, val mensaje: String)

/** El borrado físico se aplaza hasta que termina el aviso. Si muere el proceso, se conserva el contenido. */
class BorradosRecuperables {
    private val acciones = mutableMapOf<String, suspend () -> Unit>()
    private val cerrojo = Mutex()
    private val estado = MutableStateFlow<List<BorradoPendiente>>(emptyList())
    val pendientes = estado.asStateFlow()
    val error = MutableStateFlow<String?>(null)

    suspend fun proponer(clave: String, mensaje: String, accion: suspend () -> Unit) = cerrojo.withLock {
        if (clave !in acciones) {
            acciones[clave] = accion
            estado.update { it + BorradoPendiente(clave, mensaje) }
        }
    }

    suspend fun resolver(clave: String, deshacer: Boolean) = cerrojo.withLock {
        val accion = acciones.remove(clave) ?: return@withLock
        try { if (!deshacer) accion() }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error.value = "No se pudo completar el borrado. El contenido se ha conservado."
        } finally { estado.update { lista -> lista.filterNot { it.clave == clave } } }
    }
}
