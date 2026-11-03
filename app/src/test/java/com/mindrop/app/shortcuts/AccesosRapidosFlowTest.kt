package com.mindrop.app.shortcuts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.mindrop.app.MainActivity
import com.mindrop.app.MindropApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccesosRapidosFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: MindropApplication
    private var controller: ActivityController<MainActivity>? = null
    @Before fun preparar() {
        app = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
    }
    @After fun cerrar() { controller?.pause()?.stop()?.destroy() }
    private fun abrir(destino: String) {
        controller = Robolectric.buildActivity(MainActivity::class.java, AccesosRapidos.intent(app, destino)).setup()
    }

    @Test fun nuevaIdeaGuardaEnRoomYRegresaAlInicio() {
        abrir(AccesosRapidos.NUEVA_IDEA)
        compose.onNodeWithText("Nombre").performTextInput("Idea desde el escritorio")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Guardar"))
        compose.onNodeWithText("Guardar").performClick()
        compose.waitUntil(10_000) { runBlocking { app.ideaRepository.observeInFolder(null).first().isNotEmpty() } }
        compose.onNodeWithText("Mis ideas").assertIsDisplayed()
        compose.onNodeWithText("Idea desde el escritorio").performClick()
        compose.onNodeWithContentDescription("Editar idea").assertIsDisplayed()
        controller!!.recreate()
        compose.onNodeWithText("Idea desde el escritorio").assertIsDisplayed()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("Mis ideas").assertIsDisplayed()
    }

    @Test fun repetirAccesoYRecrearConservanUnSoloBorrador() {
        abrir(AccesosRapidos.NUEVA_IDEA)
        compose.onNodeWithText("Nombre").performTextInput("Borrador privado")
        controller!!.recreate()
        controller!!.newIntent(AccesosRapidos.intent(app, AccesosRapidos.NUEVA_IDEA))
        compose.onNodeWithText("Borrador privado").assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("Descartar", substring = false).performClick()
        compose.onNodeWithText("Mis ideas").assertIsDisplayed()
        assertTrue(runBlocking { app.ideaRepository.observeInFolder(null).first().isEmpty() })
    }

    @Test fun nuevoEventoAbreFormularioExistenteYConservaBorrador() {
        abrir(AccesosRapidos.NUEVO_EVENTO)
        compose.onNodeWithText("Nuevo evento").assertIsDisplayed()
        compose.onNodeWithText("Título").performTextInput("Evento del acceso")
        controller!!.newIntent(AccesosRapidos.intent(app, AccesosRapidos.NUEVO_EVENTO))
        compose.onNodeWithText("Evento del acceso").assertExists()
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithText("Descartar", substring = false).performClick()
        assertTrue(runBlocking { app.agenda.observarEventos().first().isEmpty() })
    }
}
