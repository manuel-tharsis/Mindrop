package com.mindrop.app.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.MainActivity
import com.mindrop.app.MindropApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import androidx.work.testing.WorkManagerTestInitHelper
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgendaWidgetFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: MindropApplication
    private var controller: ActivityController<MainActivity>? = null

    @Before fun preparar() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
    }

    @After fun cerrar() { controller?.pause()?.stop()?.destroy() }

    private fun intentWidget() = Intent(app, MainActivity::class.java).apply {
        data = "mindrop://widget/agenda/nuevo".toUri()
        putExtra("destino_local", AgendaWidget.DESTINO_NUEVO_EVENTO)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun abrir() {
        controller = Robolectric.buildActivity(MainActivity::class.java, intentWidget()).setup()
        compose.onNodeWithText("Nuevo evento").assertIsDisplayed()
    }

    private fun pulsarConAppAbierta() {
        controller!!.newIntent(intentWidget())
    }

    @Test fun crearDesdeAppCerradaGuardaEventoYAvisoYVuelveAlDiaElegido() {
        abrir()
        compose.onNodeWithText("Título").performTextInput("Revisar BioGestor")
        compose.onNodeWithText("Guardar").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { app.agenda.observarEventos().first().isNotEmpty() } }
        compose.onNodeWithText("Revisar BioGestor").assertIsDisplayed()
        runBlocking {
            val evento = app.agenda.observarEventos().first().single()
            assertEquals(0, evento.minutosAntes)
            assertNotNull(app.database.daoRpg().recordatorioEvento(evento.id))
        }
    }

    @Test fun recrearYVolverAPulsarElWidgetConservaUnSoloBorrador() {
        abrir()
        compose.onNodeWithText("Título").performTextInput("Borrador del widget")
        controller!!.recreate()
        compose.onNodeWithText("Borrador del widget").assertExists()
        pulsarConAppAbierta()
        compose.onNodeWithText("Borrador del widget").assertExists()
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithText("¿Descartar cambios?").assertIsDisplayed()
        compose.onNodeWithText("Descartar", substring = false).performClick()
        compose.onNodeWithText("Nuevo evento").assertDoesNotExist()
        assertTrue(runBlocking { app.agenda.observarEventos().first().isEmpty() })
    }

    @Test fun permisoBloqueadoSeExplicaYPermiteGuardarSinAviso() {
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("mindrop_local", "Recordatorios de Mindrop", NotificationManager.IMPORTANCE_NONE))
        abrir()
        compose.onNodeWithText("Título").performTextInput("Evento sin aviso")
        compose.onNodeWithText("Activar notificaciones").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("A la hora del evento").performScrollTo().performClick()
        compose.onNodeWithText("Sin recordatorio").performClick()
        compose.onNodeWithText("Guardar").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { app.agenda.observarEventos().first().isNotEmpty() } }
        runBlocking {
            val evento = app.agenda.observarEventos().first().single()
            assertNull(evento.minutosAntes)
            assertNull(app.database.daoRpg().recordatorioEvento(evento.id))
        }
    }

    @Test fun widgetAbreFormularioConLaAppYaAbierta() {
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        compose.onNodeWithText("Mis ideas").assertExists()
        pulsarConAppAbierta()
        compose.onNodeWithText("Nuevo evento").assertIsDisplayed()
        // Captura de la UI real de Compose para revisar el formulario.
        compose.waitForIdle()
        val vista = ShadowDialog.getLatestDialog().window!!.decorView
        val imagen = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        vista.draw(Canvas(imagen))
        val archivo = File("build/reports/visual-qa/widget-formulario.png").apply { parentFile!!.mkdirs() }
        archivo.outputStream().use { salida ->
            assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, salida))
        }
        imagen.recycle()
    }
}
