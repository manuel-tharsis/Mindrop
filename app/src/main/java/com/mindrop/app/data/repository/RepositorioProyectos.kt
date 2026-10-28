package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import com.mindrop.app.data.local.entity.ImagenPagina
import com.mindrop.app.data.cuaderno.AlmacenImagenesCuaderno
import com.mindrop.app.data.cuaderno.TrazoDibujo
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine

data class PaginaParaPdf(val pagina: PaginaProyecto, val imagenes: List<ImagenPagina>)
data class CuadernoParaPdf(val proyecto: Proyecto, val paginas: List<PaginaParaPdf>)

class RepositorioProyectos(private val db: MindropDatabase, private val borrados: BorradosRecuperables? = null,
    private val almacenImagenes: AlmacenImagenesCuaderno? = null) {
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
    fun observarImagenes(proyectoId: Long) = dao.observarImagenes(proyectoId)
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

    suspend fun eliminar(id: Long) {
        val imagenes = db.withTransaction {
            val imagenes = dao.imagenesProyecto(id)
            check(dao.borrarProyecto(id) == 1) { "El proyecto ya no existe." }
            imagenes
        }
        limpiarImagenes(imagenes)
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

    suspend fun eliminarPagina(proyectoId: Long, paginaId: Long) {
      val imagenes = db.withTransaction {
        val proyecto = dao.proyecto(proyectoId) ?: error("El proyecto ya no existe.")
        val paginas = dao.paginas(proyectoId)
        val indice = paginas.indexOfFirst { it.id == paginaId }
        check(indice >= 0) { "La página no pertenece al proyecto." }
        val imagenes = dao.imagenesPagina(paginaId)
        dao.borrarPagina(paginaId)
        val restantes = paginas.filterNot { it.id == paginaId }
        // Un cuaderno siempre conserva al menos una hoja; nunca se recrea contenido borrado.
        val sustituta = if (restantes.isEmpty()) dao.insertarPagina(PaginaProyecto(proyectoId = proyectoId))
            else restantes[indice.coerceAtMost(restantes.lastIndex)].id
        if (proyecto.ultimaPaginaId == paginaId || restantes.isEmpty()) dao.recordarPagina(proyectoId, sustituta)
        imagenes
      }
      limpiarImagenes(imagenes)
    }

    suspend fun importarImagen(proyectoId: Long, paginaId: Long, uri: Uri) =
        anadirImagen(proyectoId, paginaId) { checkNotNull(almacenImagenes).importar(uri) }

    suspend fun guardarDibujo(proyectoId: Long, paginaId: Long, trazos: List<TrazoDibujo>) =
        anadirImagen(proyectoId, paginaId) { checkNotNull(almacenImagenes).dibujar(trazos) }

    private suspend fun anadirImagen(proyectoId: Long, paginaId: Long, preparar: suspend () -> String) = withContext(kotlinx.coroutines.NonCancellable) {
        comprobarPagina(proyectoId, paginaId)
        val ruta = preparar()
        var insertada = false
        try {
            db.withTransaction {
                comprobarPagina(proyectoId, paginaId)
                val orden = (dao.imagenesPagina(paginaId).maxOfOrNull { it.orden } ?: -1) + 1
                dao.insertarImagen(ImagenPagina(paginaId = paginaId, ruta = ruta, orden = orden))
            }
            insertada = true
        } finally { if (!insertada) withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { almacenImagenes?.eliminar(ruta) } }
    }

    private suspend fun comprobarPagina(proyectoId: Long, paginaId: Long) {
        check(dao.pagina(paginaId)?.proyectoId == proyectoId && borrados?.pendientes?.value?.none { it.clave == "pagina:$paginaId" } != false) {
            "La página ya no está disponible."
        }
    }

    suspend fun eliminarImagen(proyectoId: Long, imagenId: Long) {
        val imagen = db.withTransaction {
            val imagen = dao.imagen(imagenId) ?: error("La imagen ya no existe.")
            comprobarPagina(proyectoId, imagen.paginaId)
            check(dao.borrarImagen(imagenId) == 1)
            imagen
        }
        limpiarImagenes(listOf(imagen))
    }

    suspend fun contenidoPdf(proyectoId: Long): CuadernoParaPdf = db.withTransaction {
        val proyecto = dao.proyecto(proyectoId) ?: error("El proyecto ya no existe.")
        val paginas = dao.paginas(proyectoId)
        val claves = paginas.map { "pagina:${it.id}" }.toSet()
        check(borrados?.pendientes?.value?.none { it.clave in claves } != false) {
            "Espera a que termine el aviso de borrado o pulsa Deshacer antes de exportar."
        }
        val imagenes = dao.imagenesProyecto(proyectoId).groupBy { it.paginaId }
        CuadernoParaPdf(proyecto, paginas.map { PaginaParaPdf(it, imagenes[it.id].orEmpty()) })
    }

    private suspend fun limpiarImagenes(imagenes: List<ImagenPagina>) = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
        imagenes.forEach { almacenImagenes?.eliminar(it.ruta) }
    }
}
