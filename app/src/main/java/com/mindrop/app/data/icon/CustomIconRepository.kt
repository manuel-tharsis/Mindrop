package com.mindrop.app.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.min

class InvalidCustomIconException : IllegalArgumentException(
    "La imagen seleccionada no es válida.",
)

interface CustomIconFileStore {
    fun delete(path: String): Boolean
}

class CustomIconRepository(
    context: Context,
) : CustomIconFileStore {
    private val applicationContext = context.applicationContext
    private val iconDirectory: File
        get() = File(applicationContext.filesDir, ICON_DIRECTORY).apply { mkdirs() }

    suspend fun importImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsInput = resolver.openInputStream(uri) ?: throw InvalidCustomIconException()
        boundsInput.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw InvalidCustomIconException()
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw InvalidCustomIconException()

        val prepared = try {
            prepareSquareIcon(decoded)
        } catch (error: Exception) {
            decoded.recycle()
            throw error
        }

        val temporaryFile = File.createTempFile("icon_", ".tmp", iconDirectory)
        val destination = File(iconDirectory, "${UUID.randomUUID()}.png")
        try {
            val written = temporaryFile.outputStream().buffered().use { output ->
                prepared.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (!written || !temporaryFile.renameTo(destination)) {
                throw IllegalStateException("No se pudo guardar la imagen seleccionada.")
            }
            destination.absolutePath
        } finally {
            temporaryFile.delete()
            if (prepared !== decoded) prepared.recycle()
            decoded.recycle()
        }
    }

    override fun delete(path: String): Boolean {
        val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return false
        val directory = runCatching { iconDirectory.canonicalFile }.getOrNull() ?: return false
        if (file.parentFile != directory || file.extension.lowercase() != "png") return false
        return !file.exists() || file.delete()
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_DECODE_SIZE || height / sampleSize > MAX_DECODE_SIZE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun prepareSquareIcon(source: Bitmap): Bitmap {
        val side = min(source.width, source.height)
        if (side <= 0) throw InvalidCustomIconException()

        val cropped = Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side,
        )
        if (side <= MAX_ICON_SIZE) return cropped

        val scaled = cropped.scale(MAX_ICON_SIZE, MAX_ICON_SIZE)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    private companion object {
        const val ICON_DIRECTORY = "custom_icons"
        const val MAX_DECODE_SIZE = 512
        const val MAX_ICON_SIZE = 256
    }
}
