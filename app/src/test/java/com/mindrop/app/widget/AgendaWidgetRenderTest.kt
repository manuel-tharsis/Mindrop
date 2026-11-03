package com.mindrop.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgendaWidgetRenderTest {
    @Test fun widgetCompactoYAnchoUsanLosArchivosReales() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(220 to "widget-agenda-estrecho", 300 to "widget-agenda", 380 to "widget-agenda-ancho").forEach { (ancho, nombre) ->
            val view = AgendaWidget.vistas(context).apply(context, FrameLayout(context))
            view.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(64, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, ancho, 64)
            assertEquals(56, view.findViewById<View>(com.mindrop.app.R.id.widget_nuevo_evento).height)
            val bitmap = Bitmap.createBitmap(ancho, 64, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val archivo = File("build/reports/visual-qa/$nombre.png").apply { parentFile!!.mkdirs() }
            archivo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            assertTrue(archivo.length() > 500)
        }
    }
}
