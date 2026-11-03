package com.mindrop.app.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.mindrop.app.MindropApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EventoRapidoFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: MindropApplication
    private lateinit var controller: ActivityController<EventoRapidoActivity>

    @Before fun abrir() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
        controller = Robolectric.buildActivity(EventoRapidoActivity::class.java).setup()
    }
    @After fun cerrar() { controller.pause().stop().destroy() }

    @Test fun entradaTieneElFocoYGuardaElNombreConAvisoSinAbrirLaAgenda() {
        compose.onNodeWithTag("nombre_evento_rapido").assertIsFocused().performTextInput("Revisar BioGestor")
        compose.onNodeWithText("Guardar").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { app.agenda.observarEventos().first().isNotEmpty() } }
        compose.waitUntil(5_000) { compose.waitForIdle(); controller.get().isFinishing }
        runBlocking {
            val evento = app.agenda.observarEventos().first().single()
            assertEquals("Revisar BioGestor", evento.titulo)
            assertNotNull(app.database.daoRpg().recordatorioEvento(evento.id))
        }
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test fun recreacionYSegundaPulsacionMantienenElTexto() {
        compose.onNodeWithTag("nombre_evento_rapido").performTextInput("Borrador de la barra")
        controller.recreate()
        controller.newIntent(Intent(app, EventoRapidoActivity::class.java))
        compose.onNodeWithTag("nombre_evento_rapido").assertTextEquals("Borrador de la barra").assertIsFocused()
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithText("¿Descartar cambios?").assertIsDisplayed()
        compose.onNodeWithText("Seguir escribiendo").performClick()
        assertFalse(controller.get().isFinishing)
        assertTrue(runBlocking { app.agenda.observarEventos().first().isEmpty() })
    }

    @Test fun nombreVacioNoCreaUnEvento() {
        compose.onNodeWithText("Guardar").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Escribe un título.").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { app.agenda.observarEventos().first().isEmpty() })
        assertFalse(controller.get().isFinishing)
    }

    @Test fun canalBloqueadoOfreceGuardarExplicitamenteSinAviso() {
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("mindrop_local", "Avisos", NotificationManager.IMPORTANCE_NONE))
        controller.pause().resume()
        compose.onNodeWithTag("nombre_evento_rapido").performTextInput("Sin aviso")
        compose.onNodeWithText("Activar notificaciones").assertIsDisplayed()
        compose.onNodeWithText("A la hora del evento").performClick()
        compose.onNodeWithText("Sin recordatorio").performClick()
        compose.onNodeWithText("Guardar").performClick()
        compose.waitUntil(10_000) { runBlocking { app.agenda.observarEventos().first().isNotEmpty() } }
        runBlocking {
            val evento = app.agenda.observarEventos().first().single()
            assertNull(evento.minutosAntes)
            assertNull(app.database.daoRpg().recordatorioEvento(evento.id))
        }
    }

    @Test fun capturaDeLaBarraDeEscrituraReal() {
        compose.onNodeWithTag("nombre_evento_rapido").performTextInput("Revisar BioGestor")
        compose.waitForIdle()
        val vista = controller.get().window.decorView
        val bitmap = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        vista.draw(Canvas(bitmap))
        val archivo = File("build/reports/visual-qa/widget-entrada-rapida.png").apply { parentFile!!.mkdirs() }
        archivo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}
