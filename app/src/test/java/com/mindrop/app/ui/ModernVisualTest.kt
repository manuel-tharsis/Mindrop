package com.mindrop.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.components.MindropAddButton
import com.mindrop.app.ui.idea.IdeaDetailScreen
import com.mindrop.app.ui.idea.IdeaDetailUiState
import com.mindrop.app.ui.navigation.MindropNavigationBar
import com.mindrop.app.ui.proyectos.ProyectoCard
import com.mindrop.app.ui.rpg.LocalMovimiento
import com.mindrop.app.ui.theme.MindropTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ModernVisualTest {
    @Test fun proyectosConNombresLargos() { proyectos(390, 844, 1f, "proyectos-minimal") }
    @Test fun proyectosEnPantallaEstrechaConTextoGrande() { proyectos(320, 740, 1.3f, "proyectos-minimal-estrecho") }
    @Test fun detalleDeIdeaConDescripcionLarga() { detalle(390, 844, 1f, "detalle-minimal") }
    @Test fun detalleEstrechoConTextoGrande() { detalle(320, 740, 1.3f, "detalle-minimal-estrecho") }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun proyectos(ancho: Int, alto: Int, escala: Float, nombre: String) = capturar(ancho, alto, escala, nombre) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                Scaffold(topBar = { TopAppBar(title = { Text("Proyectos", style = MaterialTheme.typography.headlineMedium) }) },
                    floatingActionButton = { MindropAddButton("Nuevo proyecto") { } }) { espacio ->
                    Column(Modifier.padding(espacio).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Un cuaderno para desarrollar cada proyecto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ProyectoCard("BioGestor", 8, {}, {}, {}, {})
                        ProyectoCard("APK Montones de Jara y sistema de pesaje", 3, {}, {}, {}, {})
                    }
                }
            }
            MindropNavigationBar("proyectos") { }
        }
    }

    private fun detalle(ancho: Int, alto: Int, escala: Float, nombre: String) = capturar(ancho, alto, escala, nombre) {
        IdeaDetailScreen(IdeaDetailUiState(isLoading = false, idea = IdeaEntity(id = 1, title = "BioGestor",
            shortDescription = "Una herramienta para organizar el trabajo del laboratorio.",
            fullDescription = "Reunir en un mismo lugar las ideas para el laboratorio.\n\n" +
                "Registrar materiales, preparar las revisiones y desarrollar cada módulo con calma.\n".repeat(30), icon = "brain")),
            onBack = {}, onEdit = {}, onDelete = {}, onToggleCompleted = {}, onValidateSuggestion = {}, onDeleteSuggestion = {})
    }

    private fun capturar(ancho: Int, alto: Int, escala: Float, nombre: String, content: @Composable () -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            controller.get().setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, escala), LocalMovimiento provides false) {
                    MindropTheme { Surface(color = MaterialTheme.colorScheme.background) { content() } }
                }
            }
            val vista = controller.get().window.decorView
            repeat(12) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                vista.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(alto, View.MeasureSpec.EXACTLY))
                vista.layout(0, 0, ancho, alto)
            }
            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
            try {
                vista.draw(Canvas(bitmap))
                val archivo = File("build/reports/visual-qa/$nombre.png").apply { parentFile!!.mkdirs() }
                archivo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                assertTrue(archivo.length() > 5000)
            } finally { bitmap.recycle() }
        } finally { controller.pause().stop().destroy() }
    }
}
