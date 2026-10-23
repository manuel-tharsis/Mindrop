package com.mindrop.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.*
import com.mindrop.app.MindropApplication
import com.mindrop.app.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecordatoriosTest {
    private lateinit var app: MindropApplication
    @Before fun preparar() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
    }
    @After fun cerrar() { WorkManager.getInstance(app).cancelAllWork(); app.database.close() }

    @Test fun avisoRepetidoSoloNotificaUnaVezYAbreLaIdea() = runBlocking {
        val idIdea = app.ideaRepository.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea"))
        val id = app.database.daoRpg().insertarRecordatorio(RecordatorioLocal(ideaId = idIdea,
            fechaHora = LocalDateTime.now().minusMinutes(1).toString()))
        val datos = workDataOf("id" to id, "revision" to 1L)
        repeat(2) { TestListenableWorkerBuilder<TrabajadorAviso>(app).setInputData(datos).build().doWork() }
        val gestor = Shadows.shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertEquals(1, gestor.allNotifications.size)
        val intent = Shadows.shadowOf(gestor.allNotifications.single().contentIntent).savedIntent
        assertEquals("idea/$idIdea", intent.getStringExtra("destino_local"))
        assertTrue(app.database.daoRpg().recordatorio(id)!!.enviado)
    }

    @Test fun cancelarYCompletarInvalidanRecordatorios() = runBlocking {
        val idIdea = app.ideaRepository.insert(IdeaEntity(title = "Idea", shortDescription = "", fullDescription = "", icon = "idea"))
        val futuro = LocalDateTime.now().plusDays(1).toString()
        app.programador.programarIdea(idIdea, futuro)
        val primero = app.database.daoRpg().recordatorioIdea(idIdea)!!
        app.programador.programarIdea(idIdea, futuro)
        assertEquals(primero.id, app.database.daoRpg().recordatorioIdea(idIdea)!!.id)
        app.ideaRepository.setCompleted(idIdea, true)
        assertNull(app.database.daoRpg().recordatorioIdea(idIdea))
        TestListenableWorkerBuilder<TrabajadorAviso>(app).setInputData(workDataOf("id" to primero.id, "revision" to primero.revision)).build().doWork()
        val gestor = Shadows.shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertTrue(gestor.allNotifications.isEmpty())
    }

    @Test fun silencioYConversionLocalSonDeterministas() {
        assertTrue(enSilencio(23, 22, 9)); assertTrue(enSilencio(8, 22, 9)); assertFalse(enSilencio(12, 22, 9))
        assertEquals(Instant.parse("2026-11-15T17:30:00Z").toEpochMilli(),
            instanteRecordatorio("2026-11-15T18:30", ZoneId.of("Europe/Madrid")))
    }

    @Test fun redescubrimientoExcluyeCompletadasRevisadasYRepeticionesRecientes() = runBlocking {
        val dao = app.database.daoRpg()
        val antigua = app.database.ideaDao().insert(IdeaEntity(title = "Antigua", shortDescription = "", fullDescription = "", icon = "idea", updatedAt = 10))
        val otra = app.database.ideaDao().insert(IdeaEntity(title = "Otra", shortDescription = "", fullDescription = "", icon = "idea", updatedAt = 20))
        app.database.ideaDao().insert(IdeaEntity(title = "Completada", shortDescription = "", fullDescription = "", icon = "idea", isCompleted = true, updatedAt = 1))
        assertEquals(antigua, dao.ideaOlvidada(100, 100)?.id)
        dao.guardarRevision(RevisionIdea(antigua, revisada = 200))
        assertEquals(otra, dao.ideaOlvidada(100, 100)?.id)
        dao.guardarRevision(RevisionIdea(otra, sugerida = 200))
        assertNull(dao.ideaOlvidada(100, 100))
    }
}
