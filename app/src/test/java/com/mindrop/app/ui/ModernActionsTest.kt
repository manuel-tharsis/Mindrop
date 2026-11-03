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
        compose.onNodeWithContentDescription("Exportar BioGestor a PDF").performClick()
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

    @Test fun detalleAgrupaAccionesSinPerderSusCallbacks() {
        var editado = false; var eliminado = false; var recordatorio = false
        controller.get().setContent { MindropTheme {
            com.mindrop.app.ui.idea.IdeaDetailScreen(
                com.mindrop.app.ui.idea.IdeaDetailUiState(isLoading = false,
                    idea = com.mindrop.app.data.local.entity.IdeaEntity(id = 3, title = "BioGestor", shortDescription = "", fullDescription = "Texto", icon = "idea")),
                onBack = {}, onEdit = { editado = true }, onDelete = { eliminado = true },
                onToggleCompleted = {}, onValidateSuggestion = {}, onDeleteSuggestion = {},
                onRecordatorio = { recordatorio = true })
        } }
        compose.onNodeWithContentDescription("Editar idea").performClick()
        assertTrue(editado)
        compose.onNodeWithText("Recordatorio").assertDoesNotExist()
        compose.onNodeWithContentDescription("Opciones de la idea").performClick()
        compose.onNodeWithText("Recordatorio").performClick()
        assertTrue(recordatorio)
        compose.onNodeWithContentDescription("Opciones de la idea").performClick()
        compose.onNodeWithText("Eliminar idea").performClick()
        assertTrue(eliminado)
        compose.onNodeWithText("Descripción", substring = false).assertIsDisplayed()
    }

    @Test fun tarjetaSinBanderaGrisConservaLaAccionDePrioridadEnMenu() {
        var importante = false
        controller.get().setContent { MindropTheme {
            com.mindrop.app.ui.home.components.IdeaCard(
                com.mindrop.app.data.local.entity.IdeaEntity(id = 3, title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea"),
                onClick = {}, onMoveClick = {}, onToggleImportant = { importante = true })
        } }
        compose.onNodeWithContentDescription("Marcar como importante").assertDoesNotExist()
        compose.onNodeWithContentDescription("Más acciones").performClick()
        compose.onNodeWithText("Marcar como importante").performClick()
        assertTrue(importante)
    }

    @Test fun inicioNoRecortaLaListaYPermiteLeerElFinalPorEncimaDelBoton() {
        val cantidad = mutableStateOf(1)
        controller.get().setContent { MindropTheme { Surface {
            com.mindrop.app.ui.home.HomeScreen(
                com.mindrop.app.ui.home.HomeUiState(hasAnyContent = true,
                    ideas = (1..cantidad.value).map { com.mindrop.app.data.local.entity.IdeaEntity(
                        id = it.toLong(), title = "Idea $it", shortDescription = "Descripción", fullDescription = "", icon = "idea") }),
                onSearchQueryChange = {}, onFolderClick = {}, onIdeaClick = {}, onEditFolder = {},
                onMoveFolder = {}, onDeleteFolder = {}, onMoveIdea = {}, onBack = {},
                onBreadcrumbClick = {}, onAddClick = {}, onCompletedClick = {})
        } } }
        for (numero in listOf(1, 40)) {
            compose.runOnIdle { cantidad.value = numero }
            val lista = compose.onNode(hasScrollToIndexAction())
            val boton = compose.onNodeWithContentDescription("Añadir")
            assertTrue("La lista debe usar también la zona detrás del FAB",
                lista.getUnclippedBoundsInRoot().bottom > boton.getUnclippedBoundsInRoot().let { (it.top + it.bottom) / 2 })
            lista.performScrollToNode(hasText("Completadas"))
            lista.performTouchInput { swipeUp() }
            compose.onNodeWithText("Completadas").assertIsDisplayed()
            assertTrue("El último elemento debe poder quedar por encima del FAB",
                compose.onNodeWithText("Completadas").getUnclippedBoundsInRoot().bottom < boton.getUnclippedBoundsInRoot().top)
        }
    }
}
