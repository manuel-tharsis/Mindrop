package com.mindrop.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import com.mindrop.app.ui.idea.BanderaPrioridad
import com.mindrop.app.ui.proyectos.*
import com.mindrop.app.ui.theme.MindropTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CuadernoUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deslizarEnAmbosSentidosYAbrirIndice() {
        var estado by mutableStateOf(EstadoCuaderno(cargando = false,
            proyecto = Proyecto(1, "BioGestor", 1), paginaActualId = 1,
            paginas = listOf(PaginaProyecto(1,1,"Visión general","Primera página"),
                PaginaProyecto(2,1,"Control de horas","Segunda página",1),
                PaginaProyecto(3,1,"APK Jara","Tercera página",2))))
        var eliminada = false
        compose.setContent { MindropTheme {
            CuadernoScreen(estado, seleccionar = { estado = estado.copy(paginaActualId = it) }, cambiarTitulo = { _, _ -> },
                cambiarTexto = { _, _ -> }, editar = { estado = estado.copy(editando = it) }, anadir = {}, mover = { _, _ -> },
                eliminar = { eliminada = true }, reintentar = {}, salir = {})
        } }
        compose.onNodeWithText("Visión general").assertIsDisplayed()
        compose.onNodeWithTag("paginas_cuaderno").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Control de horas").assertIsDisplayed()
        compose.onNodeWithTag("paginas_cuaderno").performTouchInput { swipeRight() }
        compose.onNodeWithText("Visión general").assertIsDisplayed()
        compose.onNodeWithText("Índice").performClick()
        compose.onNodeWithText("3. APK Jara").performClick()
        compose.onNodeWithText("Tercera página").assertIsDisplayed()
        compose.onNodeWithText("Índice").performClick()
        compose.onAllNodesWithText("Eliminar").onFirst().performClick()
        compose.onNodeWithText("¿Eliminar esta página?").assertIsDisplayed()
        assertFalse(eliminada)
        compose.onNodeWithText("Cancelar").performClick()
        assertFalse(eliminada)
    }

    @Test fun prioridadSoloTieneBanderaSinEtiquetaVisible() {
        var importante by mutableStateOf(false)
        compose.setContent { MindropTheme { BanderaPrioridad(importante, { importante = !importante }) } }
        compose.onNodeWithContentDescription("Marcar como importante").performClick()
        compose.onNodeWithContentDescription("Quitar importancia").assertExists()
        compose.onNodeWithText("Importante").assertDoesNotExist()
        assertTrue(importante)
        compose.onNodeWithContentDescription("Quitar importancia").performClick()
        assertFalse(importante)
    }
}
