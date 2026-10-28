package com.mindrop.app.ui.proyectos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mindrop.app.data.local.entity.PaginaProyecto
import com.mindrop.app.data.local.entity.Proyecto
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
class CuadernoRenderTest {
    @Test fun cuadernoConControlesDeSimbolosSeRenderiza() { capturar(390, 844, 1f, "cuaderno") }
    @Test fun cuadernoEnPantallaEstrechaConTextoGrandeSeRenderiza() { capturar(320, 740, 1.3f, "cuaderno-estrecho") }

    private fun capturar(ancho: Int, alto: Int, escala: Float, nombre: String) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            controller.get().setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, escala), LocalMovimiento provides false) {
                    MindropTheme {
                        CuadernoScreen(EstadoCuaderno(cargando = false, proyecto = Proyecto(1, "BioGestor", 1), paginaActualId = 1,
                            paginas = listOf(PaginaProyecto(1,1,"Visión general","Un lugar para desarrollar BioGestor con calma.\n\nOrganizar el trabajo del laboratorio y reunir el control de recursos."),
                                PaginaProyecto(2,1,"Disolventes","Otra página",1))),
                            seleccionar = {}, cambiarTitulo = { _, _ -> }, cambiarTexto = { _, _ -> }, editar = {},
                            anadir = {}, mover = { _, _ -> }, eliminar = {}, reintentar = {}, salir = {})
                    }
                }
            }
            val vista = controller.get().window.decorView
            repeat(12) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                vista.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(alto, View.MeasureSpec.EXACTLY))
                vista.layout(0, 0, ancho, alto)
            }
            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
            try {
                vista.draw(Canvas(bitmap))
                val archivo = File("build/reports/visual-qa/$nombre.png").absoluteFile.apply { parentFile!!.mkdirs() }
                archivo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                assertTrue(archivo.length() > 5000)
            } finally { bitmap.recycle() }
        } finally { controller.pause().stop().destroy() }
    }
}
