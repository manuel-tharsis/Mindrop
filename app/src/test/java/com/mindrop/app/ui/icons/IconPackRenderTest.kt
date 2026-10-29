package com.mindrop.app.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.ui.home.HomeScreen
import com.mindrop.app.ui.home.HomeUiState
import com.mindrop.app.ui.rpg.LocalDiseno
import com.mindrop.app.ui.rpg.LocalMovimiento
import com.mindrop.app.ui.rpg.MuestraEfecto
import com.mindrop.app.ui.rpg.OpcionesAspecto
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
class IconPackRenderTest {
    @Test fun cincoPacksRenderizanCategoriasYAccionesDistintas() {
        val hashes = IconPack.entries.map { pack ->
            capturar("pack-${pack.key}") {
                CompositionLocalProvider(LocalDiseno provides pack.key) {
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Iconos 2D", style = MaterialTheme.typography.titleLarge)
                        val ids = mindropIconOptions.map { it.drawableRes } + listOf(R.drawable.ic_settings, R.drawable.ic_trophy,
                            R.drawable.ic_edit, R.drawable.ic_delete, R.drawable.ic_move, R.drawable.ic_search, R.drawable.ic_add,
                            R.drawable.ic_save, R.drawable.ic_flag, R.drawable.ic_completed, R.drawable.ic_page_add,
                            R.drawable.ic_image_add, R.drawable.ic_pdf_export, R.drawable.ic_arrow_back, R.drawable.ic_list, R.drawable.ic_check)
                        ids.chunked(5).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { ThemedIcon(it, null, Modifier.size(48.dp)) }
                            }
                        }
                    }
                }
            }
        }
        assertEquals("Los cinco packs deben verse diferentes", 5, hashes.toSet().size)
    }

    @Test fun inicioRealConJerarquiaYPrioridad() { inicio(390, 844, 1f, "inicio-basico") }
    @Test fun inicioRealEstrechoConTextoGrande() { inicio(320, 740, 1.3f, "inicio-estrecho") }

    @Test fun materialNoRepintaLosColoresDelArchivoAprobado() {
        capturar("fidelidad-icono", verificar = { pixels ->
            assertTrue("Debe verse el amarillo original incluso con otro tinte de contenido", pixels.any {
                val red = (it ushr 16) and 255; val green = (it ushr 8) and 255; val blue = it and 255
                red > 220 && green > 140 && blue < 120
            })
        }) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { ThemedIcon(R.drawable.ic_folder, "Carpeta", Modifier.size(96.dp), tint = Color.Magenta) }
        }
    }

    @Test fun personalizacionConPanelesDeLaReferencia() { aspecto(390, 1f, "aspecto-referencia") }
    @Test fun personalizacionEstrechaConTextoGrande() { aspecto(320, 1.3f, "aspecto-estrecho") }

    private fun aspecto(width: Int, fontScale: Float, name: String) {
        capturar(name, width = width, height = 1100, fontScale = fontScale) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Personalización", style = MaterialTheme.typography.headlineMedium)
                OpcionesAspecto("clasico", "clasico", "chispa", setOf("carpetas_5", "subideas_10")) { _, _ -> }
            }
        }
    }

    @Test fun efectosSonVisiblesSinReproducirAnimacion() {
        capturar("efectos") {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("chispa", "magia", "dorado").forEach { Text(it); MuestraEfecto(it) }
            }
        }
    }

    @Test fun accionesSobrePapelClaroConservanLaTintaOscura() {
        val ink = Color(0xFF292B3E)
        capturar("accion-papel", verificar = { pixels -> assertTrue("El botón debe conservar el contraste del papel", pixels.count { it == ink.toArgb() } > 30) }) {
            Surface(color = Color(0xFFF5F1E9), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.padding(24.dp)) { ThemedIcon(R.drawable.ic_delete, "Eliminar imagen", Modifier.size(48.dp), tint = ink) }
            }
        }
    }

    private fun inicio(width: Int, height: Int, fontScale: Float, name: String) {
        capturar(name, width, height, fontScale) {
            HomeScreen(HomeUiState(hasAnyContent = true, completedIdeaCount = 2,
                folders = listOf(FolderSummary(FolderEntity(1, "Programación", "folder"), 7, 1), FolderSummary(FolderEntity(2, "Inventos", "folder"), 4, 0)),
                ideas = listOf(IdeaEntity(1, "BioGestor", "Organizar el trabajo del laboratorio", "", "brain", esImportante = true),
                    IdeaEntity(2, "APK Montones de Jara", "Registro de materiales", "", "mobile", parentIdeaId = 1),
                    IdeaEntity(3, "Mantenimiento del coche", "Revisiones y pequeñas mejoras", "", "car"))),
                onSearchQueryChange = {}, onFolderClick = {}, onIdeaClick = {}, onEditFolder = {},
                onMoveFolder = {}, onDeleteFolder = {}, onMoveIdea = {}, onBack = {},
                onBreadcrumbClick = {}, onAddClick = {}, onCompletedClick = {})
        }
    }

    private fun capturar(name: String, width: Int = 390, height: Int = 844, fontScale: Float = 1f, verificar: (IntArray) -> Unit = {}, content: @Composable () -> Unit): Int {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            controller.get().setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale), LocalMovimiento provides false) {
                    MindropTheme { Surface(color = MaterialTheme.colorScheme.background, content = content) }
                }
            }
            val view = controller.get().window.decorView
            repeat(12) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                val pixels = IntArray(width * height); bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                verificar(pixels)
                val file = File("build/reports/visual-qa/$name.png").absoluteFile.apply { parentFile!!.mkdirs() }
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                assertTrue("La captura debe contener contenido dibujado", pixels.any { it != pixels[0] })
                return pixels.contentHashCode()
            } finally { bitmap.recycle() }
        } finally { controller.pause().stop().destroy() }
    }
}
