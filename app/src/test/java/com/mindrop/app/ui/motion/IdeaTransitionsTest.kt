package com.mindrop.app.ui.motion

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.home.components.IdeaCard
import com.mindrop.app.ui.idea.IdeaDetailScreen
import com.mindrop.app.ui.idea.IdeaDetailUiState
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

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IdeaTransitionsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var controller: ActivityController<ComponentActivity>
    private lateinit var shared: SharedTransitionScope
    private val abierto = mutableStateOf(false)
    private val origenVisible = mutableStateOf(true)
    private var idea = IdeaEntity(id = 12, title = "BioGestor", shortDescription = "Gestión local", fullDescription = "Texto completo\nSegunda línea", icon = "idea")
    @Before fun abrir() { controller = Robolectric.buildActivity(ComponentActivity::class.java).setup() }
    @After fun cerrar() { controller.pause().stop().destroy() }

    private fun contenido(movimiento: Boolean = true) {
        controller.get().setContent {
            MindropTheme { Surface(color = MaterialTheme.colorScheme.background) {
                CompositionLocalProvider(LocalMovimiento provides movimiento) {
                    SharedTransitionLayout {
                        shared = this
                        CompositionLocalProvider(LocalIdeaSharedScope provides this) {
                            AnimatedContent(abierto.value, modifier = Modifier.fillMaxSize(), transitionSpec = {
                                if (movimiento) fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                                else EnterTransition.None togetherWith ExitTransition.None
                            }, label = "Detalle") { detalle ->
                                CompositionLocalProvider(LocalIdeaVisibilityScope provides this) {
                                    if (detalle) IdeaDetailScreen(
                                        IdeaDetailUiState(isLoading = false, idea = idea),
                                        onBack = { abierto.value = false }, onEdit = {}, onDelete = {},
                                        onToggleCompleted = {}, onValidateSuggestion = {}, onDeleteSuggestion = {},
                                    ) else Column(Modifier.fillMaxSize().padding(16.dp)) {
                                        Spacer(Modifier.height(160.dp))
                                        if (origenVisible.value) IdeaCard(idea, { abierto.value = true })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    @Test fun iconoYTituloSeCompartenAlAbrirYVolver() {
        contenido()
        capturar("idea-lista")
        compose.onNodeWithText("BioGestor").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()
        assertTrue("Debe existir una transición compartida real", shared.isTransitionActive)
        capturar("idea-transicion")
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        assertFalse(shared.isTransitionActive)
        compose.onNodeWithText("Texto completo\nSegunda línea").assertIsDisplayed()
        capturar("idea-abierta")
        compose.onNodeWithContentDescription("Atrás").performClick()
        assertFalse("El botón Atrás debe abrir la lista", abierto.value)
        val estados = (1..12).map {
            compose.mainClock.advanceTimeBy(16)
            compose.waitForIdle()
            shared.isTransitionActive
        }
        assertTrue("Transición de regreso: $estados", estados.any { it })
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithText("BioGestor").assertIsDisplayed()
    }

    @Test fun reducirMovimientoDesactivaLaTransicion() {
        contenido(movimiento = false)
        compose.onNodeWithText("BioGestor").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.waitForIdle()
        assertFalse(shared.isTransitionActive)
        compose.onNodeWithText("Texto completo\nSegunda línea").assertIsDisplayed()
    }

    @Test fun iconoPersonalizadoSigueDibujandoseAlDesaparecerLaTarjeta() {
        val archivo = File(controller.get().filesDir, "icono-transicion.png")
        val imagen = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        imagen.eraseColor(android.graphics.Color.MAGENTA)
        archivo.outputStream().use { assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        imagen.recycle()
        try {
            idea = idea.copy(customIconPath = archivo.absolutePath)
            contenido()
            compose.onNodeWithText("BioGestor").performClick()
            compose.mainClock.advanceTimeBy(64)
            compose.runOnUiThread { origenVisible.value = false }
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
            capturar("idea-icono-personalizado")
            compose.onNodeWithText("Texto completo\nSegunda línea").assertIsDisplayed()
        } finally { archivo.delete() }
    }

    private fun capturar(nombre: String) {
        compose.runOnUiThread {
            val vista = controller.get().window.decorView
            val imagen = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
            try {
                vista.draw(Canvas(imagen))
                val archivo = File("build/reports/visual-qa/$nombre.png").apply { parentFile!!.mkdirs() }
                archivo.outputStream().use { assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { imagen.recycle() }
        }
    }

    @Test fun desaparecerElOrigenDuranteLaTransicionConservaElDetalle() {
        contenido()
        capturar("idea-lista")
        compose.onNodeWithText("BioGestor").performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.runOnUiThread { origenVisible.value = false }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        assertFalse(shared.isTransitionActive)
        compose.onNodeWithText("Texto completo\nSegunda línea").assertIsDisplayed()
    }
}
