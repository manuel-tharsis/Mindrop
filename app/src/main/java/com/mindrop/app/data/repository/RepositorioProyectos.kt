package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto

class RepositorioProyectos(private val db: MindropDatabase) {
    private val dao = db.proyectoDao()
    fun observarProyectos() = dao.observarProyectos()
    fun observarProyecto(id: Long) = dao.observarProyecto(id)
    fun observarPaginas(id: Long) = dao.observarPaginas(id)
    suspend fun pagina(id: Long) = dao.pagina(id)
    suspend fun paginas(id: Long) = dao.paginas(id)

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
        val paginas = dao.paginas(proyectoId)
        val origen = paginas.indexOfFirst { it.id == paginaId }
        check(origen >= 0) { "La página ya no existe." }
        val destino = origen + desplazamiento
        if (destino !in paginas.indices) return@withTransaction
        val ids = paginas.map { it.id }.toMutableList()
        ids.add(destino, ids.removeAt(origen))
        ids.forEachIndexed { orden, id -> dao.ordenar(id, orden.toLong()) }
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
