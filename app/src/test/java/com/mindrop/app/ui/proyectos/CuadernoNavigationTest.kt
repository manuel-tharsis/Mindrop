package com.mindrop.app.ui.proyectos

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
import com.mindrop.app.ui.rpg.LocalMovimiento
import com.mindrop.app.ui.theme.MindropTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CuadernoNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var controller: ActivityController<ComponentActivity>
    private val estado = mutableStateOf(EstadoCuaderno(cargando = false,
        proyecto = Proyecto(1, "BioGestor", 1), paginaActualId = 1,
        paginas = listOf(PaginaProyecto(1,1,"Visión general","Primera página"),
            PaginaProyecto(2,1,"Control de horas","Segunda página",1),
            PaginaProyecto(3,1,"APK Jara","Tercera página",2))))
    private val movimientos = mutableListOf<Pair<Long, Int>>()
    private val eliminadas = mutableListOf<Long>()
    private var nuevas = 0
    @Before fun abrir() {
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.get().setContent { MindropTheme {
            CompositionLocalProvider(LocalMovimiento provides false) {
                CuadernoScreen(estado.value, seleccionar = { estado.value = estado.value.copy(paginaActualId = it) },
                    cambiarTitulo = { _, _ -> }, cambiarTexto = { _, _ -> }, editar = { estado.value = estado.value.copy(editando = it) },
                    anadir = { nuevas++ }, mover = { id, direccion -> movimientos += id to direccion },
                    eliminar = { eliminadas += it }, reintentar = {}, salir = {})
            }
        } }
    }
    @After fun cerrar() { controller.pause().stop().destroy() }

    @Test fun indiceSeleccionaPaginaYMantieneLasFlechasSinCambiarElContenido() {
        compose.onNodeWithContentDescription("Índice").performClick()
        compose.onNodeWithText("Actual").assertIsDisplayed()
        compose.onNodeWithContentDescription("Abrir página 3").performClick()
        compose.onNodeWithText("Tercera página").assertIsDisplayed()
        compose.onNodeWithText("3 de 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Página siguiente").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Página anterior").performClick()
        compose.onNodeWithText("Segunda página").assertIsDisplayed()
        compose.onNodeWithText("2 de 3").assertIsDisplayed()
    }

    @Test fun menuMueveYConfirmaElBorradoSinSeleccionarLaPagina() {
        compose.onNodeWithContentDescription("Índice").performClick()
        compose.waitForIdle()
        val vista = ShadowDialog.getLatestDialog().window!!.decorView
        val imagen = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        try {
            vista.draw(Canvas(imagen))
            val archivo = File("build/reports/visual-qa/indice-artistico.png").apply { parentFile!!.mkdirs() }
            archivo.outputStream().use { assertTrue(imagen.compress(Bitmap.CompressFormat.PNG,100,it)) }
        } finally { imagen.recycle() }
        compose.onNodeWithContentDescription("Opciones de la página 1").performClick()
        compose.onNodeWithText("Subir").assertIsNotEnabled()
        compose.onNodeWithText("Bajar").performClick()
        assertEquals(listOf(1L to 1), movimientos)
        assertEquals(1L, estado.value.paginaActualId)
        compose.onNodeWithContentDescription("Opciones de la página 2").performClick()
        compose.onNodeWithText("Eliminar").performClick()
        compose.onNodeWithText("¿Eliminar esta página?").assertIsDisplayed()
        assertTrue(eliminadas.isEmpty())
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(eliminadas.isEmpty())
    }

    @Test fun anadirDesdeIndiceConservaLaAccionExistente() {
        compose.onNodeWithContentDescription("Índice").performClick()
        compose.onNodeWithText("Añadir página").performClick()
        assertEquals(1, nuevas)
        compose.onNodeWithText("Cerrar").assertDoesNotExist()
    }
}
