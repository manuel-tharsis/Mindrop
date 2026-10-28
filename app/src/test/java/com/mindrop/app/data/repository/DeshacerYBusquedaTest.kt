package com.mindrop.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.ui.busqueda.patronBusqueda
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeshacerYBusquedaTest {
    private lateinit var db: MindropDatabase
    private lateinit var borrados: BorradosRecuperables
    private lateinit var ideas: IdeaRepository
    private lateinit var proyectos: RepositorioProyectos
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).build()
        borrados = BorradosRecuperables()
        ideas = IdeaRepository(db, db.ideaDao(), borrados = borrados)
        proyectos = RepositorioProyectos(db, borrados)
    }
    @After fun cerrar() { db.close() }

    @Test fun deshacerConservaIconoSugerenciasYRelacionesSinReconstruirLaIdea() = runBlocking {
        val padre = ideas.insert(IdeaEntity(title = "Padre", shortDescription = "", fullDescription = "", icon = "idea", customIconPath = "/privado/icono.png"))
        val hija = ideas.insert(IdeaEntity(title = "Hija", shortDescription = "", fullDescription = "", icon = "idea", parentIdeaId = padre))
        db.ideaSuggestionDao().insert(IdeaSuggestionEntity(ideaId = padre, text = "Sugerencia", createdAt = 1))
        val original = db.ideaDao().findById(padre)
        assertTrue(ideas.eliminarConDeshacer(padre))
        assertEquals(listOf(hija), ideas.observeAll().first().map { it.id })
        assertNotNull(db.ideaDao().findById(padre))
        borrados.resolver("idea:$padre", deshacer = true)
        assertEquals(original, db.ideaDao().findById(padre))
        assertEquals(padre, db.ideaDao().findById(hija)!!.parentIdeaId)
        assertEquals(1, db.ideaSuggestionDao().observePending(padre).first().size)
        assertEquals(2, ideas.observeAll().first().size)
    }

    @Test fun confirmarBorradoConservaSubideasYNoSeRepite() = runBlocking {
        val padre = ideas.insert(IdeaEntity(title = "Padre", shortDescription = "", fullDescription = "", icon = "idea"))
        val hija = ideas.insert(IdeaEntity(title = "Hija", shortDescription = "", fullDescription = "", icon = "idea", parentIdeaId = padre))
        ideas.eliminarConDeshacer(padre)
        ideas.eliminarConDeshacer(padre)
        assertEquals(1, borrados.pendientes.value.size)
        borrados.resolver("idea:$padre", false)
        borrados.resolver("idea:$padre", false)
        assertNull(db.ideaDao().findById(padre))
        assertNull(db.ideaDao().findById(hija)!!.parentIdeaId)
    }

    @Test fun ultimaPaginaPuedeRecuperarseYConfirmarlaDejaHojaVacia() = runBlocking {
        val proyecto = proyectos.crear("Cuaderno")
        val pagina = proyectos.paginas(proyecto).single()
        proyectos.guardar(pagina.id, "Título", "Texto valioso\nSegundo párrafo")
        proyectos.eliminarPaginaConDeshacer(proyecto, pagina.id)
        assertTrue(proyectos.observarPaginas(proyecto).first().isEmpty())
        borrados.resolver("pagina:${pagina.id}", true)
        assertEquals("Texto valioso\nSegundo párrafo", proyectos.observarPaginas(proyecto).first().single().texto)
        proyectos.eliminarPaginaConDeshacer(proyecto, pagina.id)
        borrados.resolver("pagina:${pagina.id}", false)
        assertEquals("", proyectos.observarPaginas(proyecto).first().single().texto)
        assertNotEquals(pagina.id, proyectos.paginas(proyecto).single().id)
    }

    @Test fun cierreAntesDeConfirmarElBorradoConservaLosDatos() = runBlocking {
        val id = ideas.insert(IdeaEntity(title = "Segura", shortDescription = "", fullDescription = "", icon = "idea"))
        ideas.eliminarConDeshacer(id)
        val nuevaSesion = IdeaRepository(db, db.ideaDao(), borrados = BorradosRecuperables())
        assertEquals(id, nuevaSesion.observeAll().first().single().id)
    }

    @Test fun busquedaEncuentraDescripcionesCompletadasYCarpetasDistintas() = runBlocking {
        val carpeta = db.folderDao().insert(FolderEntity(name = "Otra"))
        val id = ideas.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "Control de disolventes", icon = "idea", folderId = carpeta))
        ideas.setCompleted(id, true)
        assertEquals(id, ideas.buscar(patronBusqueda("DISOLVENTES")).first().single().id)
        assertTrue(ideas.observeInFolder(null).first().isEmpty())
    }

    @Test fun busquedaTrataPorcentajeYGuionBajoComoTextoLiteral() = runBlocking {
        val id = ideas.insert(IdeaEntity(title = "100%_offline", shortDescription = "", fullDescription = "", icon = "idea"))
        ideas.insert(IdeaEntity(title = "100Xoffline", shortDescription = "", fullDescription = "", icon = "idea"))
        assertEquals(listOf(id), ideas.buscar(patronBusqueda("%_")).first().map { it.id })
        assertTrue(ideas.buscar(patronBusqueda("\\")).first().isEmpty())
    }

    @Test fun paginasEncontradasConservanProyectoYNumeroDespuesDeOrdenar() = runBlocking {
        val proyecto = proyectos.crear("BioGestor")
        val segunda = proyectos.anadirPagina(proyecto)
        proyectos.guardar(segunda, "Horas", "Control de disolventes")
        assertEquals(2, db.proyectoDao().buscar(patronBusqueda("disolventes")).first().single().numero)
        proyectos.moverPagina(proyecto, segunda, -1)
        val resultado = db.proyectoDao().buscar(patronBusqueda("HORAS")).first().single()
        assertEquals(1, resultado.numero)
        assertEquals(proyecto, resultado.proyectoId)
        assertEquals("BioGestor", resultado.nombreProyecto)
    }

    @Test fun paginaPendienteDeBorradoNoApareceEnBusquedaYSePuedeOrdenarElResto() = runBlocking {
        val proyecto = proyectos.crear("Cuaderno")
        val primera = proyectos.paginas(proyecto).single().id
        val segunda = proyectos.anadirPagina(proyecto)
        val tercera = proyectos.anadirPagina(proyecto)
        proyectos.guardar(segunda, "Disolventes", "")
        proyectos.eliminarPaginaConDeshacer(proyecto, segunda)
        assertTrue(proyectos.buscar(patronBusqueda("Disolventes")).first().isEmpty())
        proyectos.moverPagina(proyecto, tercera, -1)
        assertEquals(listOf(tercera, primera), proyectos.paginas(proyecto).map { it.id })
        borrados.resolver("pagina:$segunda", true)
        assertEquals(listOf(tercera, segunda, primera), proyectos.paginas(proyecto).map { it.id })
        assertEquals(segunda, proyectos.buscar(patronBusqueda("Disolventes")).first().single().id)
    }

    @Test fun busquedaReconoceMayusculasCastellanasSinConfundirPorcentajes() = runBlocking {
        val id = ideas.insert(IdeaEntity(title = "Árbol de programación", shortDescription = "", fullDescription = "", icon = "idea"))
        assertEquals(id, ideas.buscar(patronBusqueda("ÁRBOL DE PROGRAMACIÓN")).first().single().id)
        val proyecto = proyectos.crear("Cuaderno")
        val pagina = proyectos.paginas(proyecto).single().id
        proyectos.guardar(pagina, "MÓDULO DE AÑO", "")
        assertEquals(pagina, proyectos.buscar(patronBusqueda("módulo de año")).first().single().id)
    }
}
