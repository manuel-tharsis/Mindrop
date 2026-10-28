package com.mindrop.app.data.local.dao

import androidx.room.*
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import kotlinx.coroutines.flow.Flow

data class ResumenProyecto(
    @Embedded val proyecto: Proyecto,
    val numeroPaginas: Int,
)

@Dao
interface ProyectoDao {
    @Query("""SELECT proyectos.*, (SELECT COUNT(*) FROM paginas_proyecto WHERE proyectoId = proyectos.id) AS numeroPaginas
        FROM proyectos ORDER BY nombre COLLATE NOCASE, id""")
    fun observarProyectos(): Flow<List<ResumenProyecto>>
    @Query("SELECT * FROM proyectos WHERE id = :id") fun observarProyecto(id: Long): Flow<Proyecto?>
    @Query("SELECT * FROM proyectos WHERE id = :id") suspend fun proyecto(id: Long): Proyecto?
    @Query("SELECT * FROM paginas_proyecto WHERE proyectoId = :proyectoId ORDER BY orden, id")
    fun observarPaginas(proyectoId: Long): Flow<List<PaginaProyecto>>
    @Query("SELECT * FROM paginas_proyecto WHERE proyectoId = :proyectoId ORDER BY orden, id")
    suspend fun paginas(proyectoId: Long): List<PaginaProyecto>
    @Query("SELECT * FROM paginas_proyecto WHERE id = :id") suspend fun pagina(id: Long): PaginaProyecto?
    @Insert suspend fun insertarProyecto(proyecto: Proyecto): Long
    @Insert suspend fun insertarPagina(pagina: PaginaProyecto): Long
    @Query("UPDATE proyectos SET nombre = :nombre WHERE id = :id") suspend fun renombrar(id: Long, nombre: String): Int
    @Query("UPDATE proyectos SET ultimaPaginaId = :paginaId WHERE id = :id") suspend fun recordarPagina(id: Long, paginaId: Long?)
    @Query("UPDATE paginas_proyecto SET titulo = :titulo, texto = :texto WHERE id = :id")
    suspend fun guardarContenido(id: Long, titulo: String, texto: String): Int
    @Query("UPDATE paginas_proyecto SET orden = :orden WHERE id = :id") suspend fun ordenar(id: Long, orden: Long)
    @Query("DELETE FROM paginas_proyecto WHERE id = :id") suspend fun borrarPagina(id: Long): Int
    @Query("DELETE FROM proyectos WHERE id = :id") suspend fun borrarProyecto(id: Long): Int
}
