package com.mindrop.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaTest {
    private lateinit var db: MindropDatabase
    private lateinit var repo: RepositorioAgenda
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).allowMainThreadQueries().build()
        repo = RepositorioAgenda(db)
    }
    @After fun cerrar() { db.close() }

    @Test fun crearEditarConsultarEliminar() = runBlocking {
        val id = repo.guardar(EventoAgenda(titulo = "Revisar BioGestor", fecha = "2026-11-15"))
        repo.guardar(db.daoRpg().evento(id)!!.copy(titulo = "Revisión", fecha = "2026-11-16"))
        val eventos = repo.observarEventos().first()
        assertEquals("Revisión", eventos.single().titulo)
        assertEquals("2026-11-16", eventos.single().fecha)
        repo.eliminar(id)
        assertTrue(repo.observarEventos().first().isEmpty())
    }

    @Test fun borrarIdeaConservaEventoYDesvincula() = runBlocking {
        val idIdea = db.ideaDao().insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea"))
        val id = repo.guardar(EventoAgenda(titulo = "Revisar", fecha = "2026-11-15", ideaId = idIdea))
        assertEquals(idIdea, db.daoRpg().evento(id)!!.ideaId)
        db.ideaDao().deleteById(idIdea)
        assertNull(db.daoRpg().evento(id)!!.ideaId)
    }

    @Test fun reprogramarNoDuplicaYEliminarCancela() = runBlocking {
        val id = repo.guardar(EventoAgenda(titulo = "Revisar", fecha = "2099-11-15", minutoDelDia = 1080, minutosAntes = 30))
        val primero = db.daoRpg().recordatorioEvento(id)!!
        repo.guardar(db.daoRpg().evento(id)!!.copy(minutosAntes = 60))
        val segundo = db.daoRpg().recordatorioEvento(id)!!
        assertEquals(primero.id, segundo.id)
        assertEquals(primero.revision + 1, segundo.revision)
        assertEquals(1, db.daoRpg().pendientes().size)
        repo.eliminar(id)
        assertTrue(db.daoRpg().pendientes().isEmpty())
    }
}
