package com.mindrop.app.data.rpg

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProgresoRpgTest {
    private lateinit var db: MindropDatabase
    @Before fun preparar() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).allowMainThreadQueries().build() }
    @After fun cerrar() { db.close() }

    @Test fun creacionYCompletarNoSeDuplicanAlRestaurar() = runBlocking {
        val repo = IdeaRepository(db, db.ideaDao())
        val id = repo.insert(idea())
        assertEquals(1L, db.daoRpg().metrica("profundidad"))
        repeat(3) { repo.setCompleted(id, true); repo.setCompleted(id, false) }
        assertEquals(1L, db.daoRpg().metrica("ideas"))
        assertEquals(1L, db.daoRpg().metrica("completadas"))
        assertEquals(1, db.daoRpg().logros().count { it.clave == "completadas_1" })
        repo.deleteById(id)
        assertEquals(1L, db.daoRpg().metrica("completadas"))
        assertTrue(db.daoRpg().logros().any { it.clave == "completadas_1" })
    }

    @Test fun historicoNoInventaRachaNiRepitePrimeraValidacion() = runBlocking {
        val id = db.ideaDao().insert(idea().copy(isCompleted = true))
        val s = db.ideaSuggestionDao().insert(IdeaSuggestionEntity(ideaId = id, text = "Mejora", createdAt = 1, validatedAt = 2, updateNumber = 1))
        ProgresoRpg(db).inicializar()
        assertNull(db.daoRpg().metrica("racha"))
        assertTrue(db.daoRpg().logros().all { it.celebrado })
        IdeaSuggestionRepository(db, db.ideaSuggestionDao()).validate(id, s)
        assertEquals(1L, db.daoRpg().metrica("validadas"))
    }

    @Test fun rachaConsecutivaAlcanzaTryhardYRechazaRelojAdelantado() = runBlocking {
        var pared = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        var mono = 1000L
        val progreso = ProgresoRpg(db, { pared }, { mono })
        repeat(365) {
            progreso.actividad()
            progreso.actividad() // Repetir acciones el mismo día no suma otro día.
            pared += 86_400_000L; mono += 86_400_000L
        }
        assertEquals(365L, db.daoRpg().metrica("racha_maxima"))
        assertTrue(db.daoRpg().logros().any { it.clave == "racha_365" })
        pared += 86_400_000L // El reloj de pared salta, el monotónico no.
        progreso.actividad()
        assertEquals(365L, db.daoRpg().metrica("racha_maxima"))
    }

    @Test fun jerarquiaDeCincoNivelesDesbloqueaRuna() = runBlocking {
        val repo = IdeaRepository(db, db.ideaDao())
        var padre: Long? = null
        repeat(5) { padre = repo.insert(idea().copy(title = "Nivel $it", parentIdeaId = padre)) }
        assertEquals(5L, db.daoRpg().metrica("profundidad"))
        assertTrue(recompensaPermitida("diseno", "runa", db.daoRpg().logros().map { it.clave }.toSet()))
        assertFalse(recompensaPermitida("tema", "dorado", emptySet()))
        assertFalse(recompensaPermitida("tema", "inventado", setOf("completadas_100")))
    }

    private fun idea() = IdeaEntity(title = "Idea", shortDescription = "", fullDescription = "", icon = "idea")
}
