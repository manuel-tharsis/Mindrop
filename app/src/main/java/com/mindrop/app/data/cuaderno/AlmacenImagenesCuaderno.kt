package com.mindrop.app.data.cuaderno

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

data class PuntoDibujo(val x: Float, val y: Float)
typealias TrazoDibujo = List<PuntoDibujo>

class AlmacenImagenesCuaderno(private val contexto: Context) {
    val directorio: File get() = File(contexto.filesDir, "imagenes_cuaderno").apply { mkdirs() }.canonicalFile

    suspend fun importar(uri: Uri): String = withContext(Dispatchers.IO) {
        val temporal = File.createTempFile("foto-cuaderno-", ".tmp", contexto.cacheDir)
        try {
            contexto.contentResolver.openInputStream(uri)?.use { entrada ->
                temporal.outputStream().buffered().use { salida ->
                    val buffer = ByteArray(8192); var total = 0L
                    while (true) {
                        val leidos = entrada.read(buffer); if (leidos < 0) break
                        total += leidos
                        require(total <= 32L * 1024 * 1024) { "La imagen es demasiado grande. Elige una de menos de 32 MB." }
                        salida.write(buffer, 0, leidos)
                    }
                }
            } ?: error("No se pudo abrir la imagen seleccionada.")
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporal.absolutePath, limites)
            require(limites.outWidth > 0 && limites.outHeight > 0 && limites.outWidth.toLong() * limites.outHeight <= 150_000_000L) {
                "La imagen no es válida o tiene una resolución demasiado grande."
            }
            var muestra = 1
            while (max(limites.outWidth, limites.outHeight) / muestra > 2048) muestra *= 2
            val original = BitmapFactory.decodeFile(temporal.absolutePath, BitmapFactory.Options().apply { inSampleSize = muestra })
                ?: error("La imagen no se pudo leer.")
            var orientada: Bitmap? = null; var reducida: Bitmap? = null
            try {
                val orientacion = runCatching { ExifInterface(temporal.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
                val matriz = Matrix().apply {
                    when (orientacion) {
                        2 -> setScale(-1f, 1f)
                        3 -> setRotate(180f)
                        4 -> setScale(1f, -1f)
                        5 -> { setRotate(90f); postScale(-1f, 1f) }
                        6 -> setRotate(90f)
                        7 -> { setRotate(-90f); postScale(-1f, 1f) }
                        8 -> setRotate(270f)
                    }
                }
                orientada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
                val factor = (1600f / max(orientada.width, orientada.height)).coerceAtMost(1f)
                reducida = orientada.scale((orientada.width * factor).toInt().coerceAtLeast(1),
                    (orientada.height * factor).toInt().coerceAtLeast(1), true)
                // JPEG no tiene transparencia; se compone sobre papel blanco.
                val opaca = createBitmap(reducida.width, reducida.height, Bitmap.Config.ARGB_8888)
                try {
                    Canvas(opaca).apply { drawColor(Color.WHITE); drawBitmap(reducida, 0f, 0f, null) }
                    guardar(opaca, "jpg", Bitmap.CompressFormat.JPEG, 85)
                } finally { opaca.recycle() }
            } finally {
                reducida?.takeIf { it !== original && it !== orientada }?.recycle()
                orientada?.takeIf { it !== original }?.recycle()
                original.recycle()
            }
        } finally { temporal.delete() }
    }

    suspend fun dibujar(trazos: List<TrazoDibujo>): String = withContext(Dispatchers.IO) {
        require(trazos.isNotEmpty() && trazos.sumOf { it.size } in 1..50_000) { "Dibuja una nota antes de guardarla." }
        require(trazos.flatten().all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f }) { "El dibujo no es válido." }
        val bitmap = createBitmap(1024, 640, Bitmap.Config.ARGB_8888)
        try {
            val lienzo = Canvas(bitmap); lienzo.drawColor(Color.rgb(245, 241, 233))
            val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(41, 43, 62); strokeWidth = 5f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; style = Paint.Style.STROKE
            }
            trazos.filter { it.isNotEmpty() }.forEach { puntos ->
                if (puntos.size == 1) lienzo.drawPoint(puntos[0].x * 1024, puntos[0].y * 640, tinta)
                else {
                    val camino = Path().apply {
                        moveTo(puntos[0].x * 1024, puntos[0].y * 640)
                        puntos.drop(1).forEach { lineTo(it.x * 1024, it.y * 640) }
                    }
                    lienzo.drawPath(camino, tinta)
                }
            }
            guardar(bitmap, "png", Bitmap.CompressFormat.PNG, 100)
        } finally { bitmap.recycle() }
    }

    fun archivo(ruta: String): File? = runCatching {
        File(ruta).canonicalFile.takeIf { it.parentFile == directorio && it.extension in listOf("jpg", "png") && it.isFile }
    }.getOrNull()

    fun eliminar(ruta: String): Boolean = runCatching {
        val archivo = File(ruta).canonicalFile
        if (archivo.parentFile != directorio || archivo.extension !in listOf("jpg", "png")) false
        else !archivo.exists() || archivo.delete()
    }.getOrDefault(false)

    private fun guardar(bitmap: Bitmap, extension: String, formato: Bitmap.CompressFormat, calidad: Int): String {
        val temporal = File.createTempFile("imagen-", ".tmp", directorio)
        val destino = File(directorio, "${UUID.randomUUID()}.$extension")
        try {
            check(temporal.outputStream().buffered().use { bitmap.compress(formato, calidad, it) } && temporal.length() <= MAX_BYTES) {
                "No se pudo guardar la imagen del cuaderno."
            }
            check(temporal.renameTo(destino)) { "No se pudo guardar la imagen del cuaderno." }
            return destino.absolutePath
        } finally { temporal.delete() }
    }

    companion object { const val MAX_BYTES = 3L * 1024 * 1024 }
}
