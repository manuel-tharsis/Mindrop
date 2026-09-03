package com.mindrop.app.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.ui.icons.decodeCustomIcon
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomIconRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: CustomIconRepository
    private lateinit var iconDirectory: File
    private lateinit var sourceDirectory: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = CustomIconRepository(context)
        iconDirectory = File(context.filesDir, "custom_icons")
        sourceDirectory = File(context.cacheDir, "custom_icon_test").apply { mkdirs() }
        iconDirectory.deleteRecursively()
    }

    @After
    fun tearDown() {
        iconDirectory.deleteRecursively()
        sourceDirectory.deleteRecursively()
    }

    @Test
    fun importedImageIsCopiedCroppedAndReducedInPrivateStorage() = runBlocking {
        val source = createImage("large.jpg", width = 1_200, height = 600)

        val storedPath = repository.importImage(Uri.fromFile(source))
        source.delete()

        val storedFile = File(storedPath)
        val bitmap = decodeCustomIcon(storedPath)!!
        assertTrue(storedFile.isFile)
        assertEquals(iconDirectory.canonicalFile, storedFile.parentFile?.canonicalFile)
        assertEquals(bitmap.width, bitmap.height)
        assertTrue(bitmap.width <= 256)
        bitmap.recycle()
    }

    @Test
    fun corruptImageIsRejectedAndDoesNotCreateAnIcon() {
        val corrupt = File(sourceDirectory, "corrupt.jpg").apply {
            writeText("Esto no es una imagen")
        }

        assertThrows(InvalidCustomIconException::class.java) {
            runBlocking { repository.importImage(Uri.fromFile(corrupt)) }
        }
        assertTrue(iconDirectory.listFiles().isNullOrEmpty())
    }

    @Test
    fun missingImageIsReportedAsInvalid() {
        val missing = File(iconDirectory, "missing.png")
        val corrupt = File(iconDirectory, "corrupt.png").apply {
            parentFile?.mkdirs()
            writeText("not an image")
        }

        assertNull(decodeCustomIcon(missing.absolutePath))
        assertNull(decodeCustomIcon(corrupt.absolutePath))
    }

    @Test
    fun deletionOnlyRemovesManagedIconFiles() = runBlocking {
        val storedPath = repository.importImage(
            Uri.fromFile(createImage("small.png", width = 64, height = 64)),
        )
        val unrelated = File(sourceDirectory, "keep.png").apply { writeText("keep") }

        assertFalse(repository.delete(unrelated.absolutePath))
        assertTrue(unrelated.exists())
        assertTrue(repository.delete(storedPath))
        assertFalse(File(storedPath).exists())
    }

    private fun createImage(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(55, 110, 170))
        val file = File(sourceDirectory, name)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }
}
