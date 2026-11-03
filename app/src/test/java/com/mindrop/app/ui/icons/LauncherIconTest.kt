package com.mindrop.app.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconTest {
    @Test fun iconoAdaptativoTieneFondoTransparenteYVersionTematica() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertNotNull(icon.monochrome)
        val bitmap = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        try {
            icon.setBounds(0, 0, 216, 216)
            icon.draw(Canvas(bitmap))
            assertEquals(0, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            assertTrue(android.graphics.Color.alpha(bitmap.getPixel(108, 100)) > 200)
            val file = File("build/reports/visual-qa/launcher-md-fluido.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }

    @Test
    @Config(sdk = [23])
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun android6YElWidgetCarganElPngAprobado() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf(R.mipmap.ic_launcher).forEach { id ->
            val icon = context.getDrawable(id)
            assertTrue(icon is BitmapDrawable)
            assertTrue(icon!!.intrinsicWidth > 0)
            val png = context.resources.openRawResource(id).use { it.readBytes() }
            assertEquals("El PNG conserva canal alfa", 6, png[25].toInt())
        }
    }
}
