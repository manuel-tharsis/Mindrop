package com.mindrop.app.ui.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
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
class PackAssetIntegrityTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun todosLosRecursosSeDecodificanYNoTienenUnRecuadroOpaco() {
        packAssetResources.forEach { (key, id) ->
            val bitmap = decodeResource(id)
            try {
                assertTrue(key.toString(), bitmap.width > 20 && bitmap.height > 20)
                listOf(0 to 0, bitmap.width-1 to 0, 0 to bitmap.height-1, bitmap.width-1 to bitmap.height-1).forEach { (x,y) ->
                    assertEquals("$key: esquina opaca", 0, bitmap.getPixel(x,y) ushr 24)
                }
            } finally { bitmap.recycle() }
        }
    }

    @Test fun losPixelesDelOriginalSeConservanEnElRecursoAndroid() {
        listOf(IconPack.BASIC to "folder", IconPack.BASIC to "brain", IconPack.BASIC to "game", IconPack.ADVENTURE to "folder",
            IconPack.FOREST to "idea", IconPack.ARCANE to "project", IconPack.CYBER to "mobile").forEach { (pack, role) ->
            val name = mapOf(IconPack.BASIC to "basic", IconPack.ADVENTURE to "adventure", IconPack.FOREST to "forest", IconPack.ARCANE to "arcane", IconPack.CYBER to "cyber").getValue(pack)
            val bytes = File("../artwork/icon-packs/originals/$name/$role.png").readBytes()
            val png = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val actual = decodeResource(packAssetId(pack, role, Color.Blue)!!)
            try {
                assertEquals(png.width, actual.width); assertEquals(png.height, actual.height)
                var checked = 0
                for (y in 0 until png.height) for (x in 0 until png.width) {
                    val expected = png.getPixel(x,y)
                    if (expected ushr 24 == 255) { assertEquals("$pack/$role ($x,$y)", expected, actual.getPixel(x,y)); checked++ }
                }
                assertTrue(checked > 100)
            } finally { png.recycle(); actual.recycle() }
        }
    }

    private fun decodeResource(id: Int): Bitmap {
        val bytes = context.resources.openRawResource(id).use { it.readBytes() }
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }
}
