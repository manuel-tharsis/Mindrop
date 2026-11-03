package com.mindrop.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.mindrop.app.ui.navigation.MindropNavigationBar
import com.mindrop.app.ui.proyectos.ProyectoCard
import com.mindrop.app.ui.theme.MindropTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModernActionsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var controller: ActivityController<ComponentActivity>
    @Before fun abrir() { controller = Robolectric.buildActivity(ComponentActivity::class.java).setup() }
    @After fun cerrar() { controller.pause().stop().destroy() }

    @Test fun exportarYEditarNoAbrenElCuadernoAccidentalmente() {
        var aperturas = 0; var exportaciones = 0; var ediciones = 0
        controller.get().setContent { MindropTheme { Surface {
            ProyectoCard("BioGestor", 8, abrir = { aperturas++ }, editar = { ediciones++ }, eliminar = {}, exportar = { exportaciones++ })
        } } }
        compose.onNodeWithText("PDF").performClick()
        assertEquals(1, exportaciones)
        assertEquals(0, aperturas)
        compose.onNodeWithContentDescription("Opciones de BioGestor").performClick()
        compose.onNodeWithText("Editar nombre").performClick()
        assertEquals(1, ediciones)
        assertEquals(0, aperturas)
        compose.onNodeWithText("BioGestor").performClick()
        assertEquals(1, aperturas)
    }

    @Test fun navegacionConservaLasCincoSeccionesYSuSeleccion() {
        val ruta = mutableStateOf("home")
        controller.get().setContent { MindropTheme { MindropNavigationBar(ruta.value) { ruta.value = it } } }
        compose.onNodeWithText("Inicio").assertIsSelected()
        compose.onNodeWithText("Proyectos").performClick()
        assertEquals("proyectos", ruta.value)
        compose.onNodeWithText("Proyectos").assertIsSelected()
        compose.onNodeWithText("Agenda").performClick()
        assertEquals("agenda", ruta.value)
        compose.onNodeWithText("Logros").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
    }
}
