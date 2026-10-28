package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import kotlinx.coroutines.flow.combine

class RepositorioProyectos(private val db: MindropDatabase, private val borrados: BorradosRecuperables? = null) {
    private val dao = db.proyectoDao()
    fun observarProyectos() = dao.observarProyectos()
    fun observarProyecto(id: Long) = dao.observarProyecto(id)
    fun buscar(patron: String) = borrados?.let { gestor -> combine(dao.buscar(patron), gestor.pendientes) { paginas, pendientes ->
        val ocultas = pendientes.map { it.clave }.toSet()
        paginas.filterNot { "pagina:${it.id}" in ocultas }
    } } ?: dao.buscar(patron)
    fun observarPaginas(id: Long) = borrados?.let { gestor -> combine(dao.observarPaginas(id), gestor.pendientes) { paginas, pendientes ->
        val ocultas = pendientes.map { it.clave }.toSet()
        paginas.filterNot { "pagina:${it.id}" in ocultas }
    } } ?: dao.observarPaginas(id)
    suspend fun pagina(id: Long) = dao.pagina(id)
    suspend fun paginas(id: Long) = dao.paginas(id).filterNot { p -> borrados?.pendientes?.value?.any { it.clave == "pagina:${p.id}" } == true }

    suspend fun eliminarPaginaConDeshacer(proyectoId: Long, paginaId: Long) {
        check(dao.pagina(paginaId)?.proyectoId == proyectoId) { "La página no pertenece al proyecto." }
        val gestor = borrados ?: return eliminarPagina(proyectoId, paginaId)
        gestor.proponer("pagina:$paginaId", "Página eliminada") {
            // Puede haberse eliminado el proyecto durante el aviso.
            if (dao.pagina(paginaId) != null) eliminarPagina(proyectoId, paginaId)
        }
    }

    suspend fun crear(nombre: String): Long = db.withTransaction {
        require(nombre.isNotBlank()) { "Escribe un nombre para el proyecto." }
        val id = dao.insertarProyecto(Proyecto(nombre = nombre.trim()))
        val pagina = dao.insertarPagina(PaginaProyecto(proyectoId = id))
        dao.recordarPagina(id, pagina)
        id
    }

    suspend fun renombrar(id: Long, nombre: String) {
        require(nombre.isNotBlank()) { "Escribe un nombre para el proyecto." }
        check(dao.renombrar(id, nombre.trim()) == 1) { "El proyecto ya no existe." }
    }

    suspend fun eliminar(id: Long) = db.withTransaction {
        check(dao.borrarProyecto(id) == 1) { "El proyecto ya no existe." }
    }

    suspend fun anadirPagina(proyectoId: Long): Long = db.withTransaction {
        check(dao.proyecto(proyectoId) != null) { "El proyecto ya no existe." }
        val siguiente = (dao.paginas(proyectoId).maxOfOrNull { it.orden } ?: -1) + 1
        val id = dao.insertarPagina(PaginaProyecto(proyectoId = proyectoId, orden = siguiente))
        dao.recordarPagina(proyectoId, id)
        id
    }

    suspend fun guardar(id: Long, titulo: String, texto: String) {
        check(dao.guardarContenido(id, titulo, texto) == 1) { "La página ya no existe." }
    }

    suspend fun recordarPagina(proyectoId: Long, paginaId: Long) = db.withTransaction {
        check(dao.pagina(paginaId)?.proyectoId == proyectoId) { "La página no pertenece al proyecto." }
        dao.recordarPagina(proyectoId, paginaId)
    }

    suspend fun moverPagina(proyectoId: Long, paginaId: Long, desplazamiento: Int) = db.withTransaction {
        require(desplazamiento == -1 || desplazamiento == 1)
        val todas = dao.paginas(proyectoId)
        val paginas = paginas(proyectoId)
        val origen = paginas.indexOfFirst { it.id == paginaId }
        check(origen >= 0) { "La página ya no existe." }
        val destino = origen + desplazamiento
        if (destino !in paginas.indices) {
            return@withTransaction
        }
        val ids = paginas.map { it.id }.toMutableList()
        ids.add(destino, ids.removeAt(origen))
        val visibles = paginas.map { it.id }.toSet()
        val posiciones = ids.iterator()
        todas.map { if (it.id in visibles) posiciones.next() else it.id }
            .forEachIndexed { orden, id -> dao.ordenar(id, orden.toLong()) }
    }

    suspend fun eliminarPagina(proyectoId: Long, paginaId: Long) = db.withTransaction {
        val proyecto = dao.proyecto(proyectoId) ?: error("El proyecto ya no existe.")
        val paginas = dao.paginas(proyectoId)
        val indice = paginas.indexOfFirst { it.id == paginaId }
        check(indice >= 0) { "La página no pertenece al proyecto." }
        dao.borrarPagina(paginaId)
        val restantes = paginas.filterNot { it.id == paginaId }
        // Un cuaderno siempre conserva al menos una hoja; nunca se recrea contenido borrado.
        val sustituta = if (restantes.isEmpty()) dao.insertarPagina(PaginaProyecto(proyectoId = proyectoId))
            else restantes[indice.coerceAtMost(restantes.lastIndex)].id
        if (proyecto.ultimaPaginaId == paginaId || restantes.isEmpty()) dao.recordarPagina(proyectoId, sustituta)
    }
}
