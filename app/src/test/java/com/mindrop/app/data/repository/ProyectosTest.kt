package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProyectosTest {
    private lateinit var db: MindropDatabase
    private lateinit var repo: RepositorioProyectos
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).build()
        repo = RepositorioProyectos(db)
    }
    @After fun cerrar() { db.close() }

    @Test fun proyectoCreaCuadernoConHojaVaciaYPermiteRenombrarlo() = runBlocking {
        val id = repo.crear(" BioGestor ")
        val resumen = repo.observarProyectos().first().single()
        assertEquals("BioGestor", resumen.proyecto.nombre)
        assertEquals(1, resumen.numeroPaginas)
        val pagina = repo.paginas(id).single()
        assertEquals("", pagina.texto)
        assertEquals(pagina.id, resumen.proyecto.ultimaPaginaId)
        repo.renombrar(id, "Laboratorio")
        assertEquals("Laboratorio", repo.observarProyecto(id).first()!!.nombre)
    }

    @Test fun nombreNecesarioPeroLasPaginasPuedenEstarVacias() = runBlocking {
        assertTrue(runCatching { repo.crear(" ") }.isFailure)
        val id = repo.crear("Proyecto")
        val pagina = repo.paginas(id).single()
        repo.guardar(pagina.id, "", "")
        assertTrue(runCatching { repo.renombrar(id, " ") }.isFailure)
        assertEquals("", repo.pagina(pagina.id)!!.titulo)
    }

    @Test fun paginasConservanTextoLargoSaltosDeLineaYOrdenAlMoverse() = runBlocking {
        val id = repo.crear("BioGestor")
        val primera = repo.paginas(id).single().id
        val segunda = repo.anadirPagina(id)
        val tercera = repo.anadirPagina(id)
        val texto = "Primera línea\n\nSegunda línea\n".repeat(2000)
        repo.guardar(segunda, "Control de horas", texto)
        repo.moverPagina(id, segunda, -1)
        assertEquals(listOf(segunda, primera, tercera), repo.paginas(id).map { it.id })
        repo.moverPagina(id, segunda, 1)
        assertEquals(listOf(primera, segunda, tercera), repo.paginas(id).map { it.id })
        assertEquals(texto, repo.pagina(segunda)!!.texto)
    }

    @Test fun paginasDeOtroProyectoNoPuedenMoverseBorrarseNiRecordarse() = runBlocking {
        val a = repo.crear("A")
        val b = repo.crear("B")
        val pagina = repo.paginas(a).single().id
        assertTrue(runCatching { repo.recordarPagina(b, pagina) }.isFailure)
        assertTrue(runCatching { repo.eliminarPagina(b, pagina) }.isFailure)
        assertTrue(runCatching { repo.moverPagina(b, pagina, 1) }.isFailure)
        assertNotNull(repo.pagina(pagina))
    }

    @Test fun borrarPaginaActualRecuperaOtraYLaUltimaDejaUnaHojaVacia() = runBlocking {
        val id = repo.crear("Cuaderno")
        val primera = repo.paginas(id).single().id
        val segunda = repo.anadirPagina(id)
        repo.eliminarPagina(id, segunda)
        assertEquals(primera, repo.observarProyecto(id).first()!!.ultimaPaginaId)
        repo.guardar(primera, "Anterior", "Texto que se elimina")
        repo.eliminarPagina(id, primera)
        val vacia = repo.paginas(id).single()
        assertNotEquals(primera, vacia.id)
        assertEquals("", vacia.texto)
        assertEquals(vacia.id, repo.observarProyecto(id).first()!!.ultimaPaginaId)
    }

    @Test fun eliminarProyectoBorraSoloSusPaginas() = runBlocking {
        val idea = db.ideaDao().insert(IdeaEntity(title = "Idea independiente", shortDescription = "", fullDescription = "", icon = "idea"))
        val a = repo.crear("A")
        val b = repo.crear("B")
        val pagina = repo.paginas(a).single().id
        repo.eliminar(a)
        assertNull(repo.pagina(pagina))
        assertNotNull(repo.observarProyecto(b).first())
        assertNotNull(db.ideaDao().findById(idea))
    }

    @Test fun reiniciarConservaPaginasYUltimaUbicacion() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "cuaderno-persistencia.db"
        contexto.deleteDatabase(nombre)
        var almacen = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        var r = RepositorioProyectos(almacen)
        val id = r.crear("Persistente")
        val pagina = r.anadirPagina(id)
        r.guardar(pagina, "Mi idea", "Primera línea\nSegunda línea")
        r.recordarPagina(id, pagina)
        almacen.close()
        almacen = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        r = RepositorioProyectos(almacen)
        assertEquals(pagina, r.observarProyecto(id).first()!!.ultimaPaginaId)
        assertEquals("Primera línea\nSegunda línea", r.pagina(pagina)!!.texto)
        almacen.close(); contexto.deleteDatabase(nombre)
        Unit
    }

    @Test fun prioridadNoAlteraFechasOrdenEstadoNiJerarquia() = runBlocking {
        val ideas = IdeaRepository(db, db.ideaDao())
        val padre = ideas.insert(IdeaEntity(title = "Padre", shortDescription = "", fullDescription = "", icon = "idea"))
        val id = ideas.insert(IdeaEntity(title = "Hija", shortDescription = "", fullDescription = "", icon = "idea", parentIdeaId = padre))
        val original = ideas.findById(id)!!
        assertFalse(original.esImportante)
        assertTrue(ideas.cambiarImportancia(id, true))
        assertEquals(original.copy(esImportante = true), ideas.findById(id))
        ideas.setCompleted(id, true); ideas.setCompleted(id, false)
        assertTrue(ideas.findById(id)!!.esImportante)
        assertEquals(padre, ideas.findById(id)!!.parentIdeaId)
        ideas.cambiarImportancia(id, false)
        assertFalse(ideas.findById(id)!!.esImportante)
    }
}
