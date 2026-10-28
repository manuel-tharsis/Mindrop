package com.mindrop.app.data.cuaderno

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withClip
import com.mindrop.app.data.repository.CuadernoParaPdf
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ArchivoCuadernoPdf(val archivo: File, val nombre: String, val paginas: Int, val imagenesAusentes: Int)

internal fun nombrePdf(nombre: String): String = nombre.trim().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "-")
    .trim('.', ' ').take(100).ifBlank { "Cuaderno" } + ".pdf"

class ExportadorCuadernoPdf(private val contexto: Context, private val repositorio: RepositorioProyectos,
    private val imagenes: AlmacenImagenesCuaderno) {
    val directorio: File get() = File(contexto.cacheDir, "pdf_cuadernos").apply { mkdirs() }

    suspend fun preparar(proyectoId: Long): ArchivoCuadernoPdf = withContext(Dispatchers.IO) {
        val contenido = repositorio.contenidoPdf(proyectoId)
        // Solo se limpian archivos temporales propios; los PDF elegidos por el usuario quedan fuera de la app.
        directorio.listFiles()?.filter { it.name.startsWith("mindrop-") && it.extension == "pdf" &&
            it.lastModified() < System.currentTimeMillis() - 86_400_000L }?.forEach { it.delete() }
        val destino = File.createTempFile("mindrop-", ".pdf", directorio)
        try { renderizar(contenido, destino) }
        catch (e: Throwable) { destino.delete(); throw e }
    }

    internal fun renderizar(contenido: CuadernoParaPdf, destino: File): ArchivoCuadernoPdf {
        var numeroPaginas = 0
        var ausentes = 0
        val documento = PdfDocument()
        var paginaActual: PdfDocument.Page? = null
        try {
            val lienzo = PaginadorPdf(contenido.proyecto.nombre,
                iniciar = { numero -> documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numero).create()).also { paginaActual = it }.canvas },
                finalizar = { paginaActual?.let(documento::finishPage); paginaActual = null })
            try {
                ausentes = representar(contenido, lienzo)
                numeroPaginas = lienzo.numero
            } finally { lienzo.terminarPagina() }
            destino.outputStream().buffered().use(documento::writeTo)
        } finally { documento.close() }
        return ArchivoCuadernoPdf(destino, nombrePdf(contenido.proyecto.nombre), numeroPaginas, ausentes)
    }

    internal fun representar(contenido: CuadernoParaPdf, lienzo: PaginadorPdf): Int {
        var ausentes = 0
        contenido.paginas.forEachIndexed { indice, hoja ->
            lienzo.nuevaPagina()
            lienzo.texto(hoja.pagina.titulo.ifBlank { "Página ${indice + 1}" }, titulo = true)
            lienzo.texto(hoja.pagina.texto)
            hoja.imagenes.forEach { imagen ->
                val archivo = imagenes.archivo(imagen.ruta)
                val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                if (archivo != null) BitmapFactory.decodeFile(archivo.absolutePath, limites)
                val bitmap = if (archivo != null && limites.outWidth in 1..1600 && limites.outHeight in 1..1600)
                    BitmapFactory.decodeFile(archivo.absolutePath) else null
                if (bitmap == null) {
                    ausentes++; lienzo.texto("[Imagen no disponible]")
                } else try { lienzo.imagen(bitmap) } finally { bitmap.recycle() }
            }
        }
        if (contenido.paginas.isEmpty()) { lienzo.nuevaPagina(); lienzo.texto("Cuaderno vacío") }
        return ausentes
    }
}

/** El texto se divide por líneas de StaticLayout; nunca se reduce hasta hacerlo ilegible ni se recorta. */
internal class PaginadorPdf(private val proyecto: String, private val iniciar: (Int) -> Canvas, private val finalizar: () -> Unit) {
    var numero = 0; private set
    private var pagina: Canvas? = null
    private var posicion = INICIO
    private val cuerpo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(34, 36, 49); textSize = 11.5f }
    private val titulo = TextPaint(cuerpo).apply { textSize = 20f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    private val cabecera = TextPaint(cuerpo).apply { textSize = 9f; color = Color.rgb(100, 103, 117) }
    private val canvas: Canvas get() = checkNotNull(pagina)

    fun nuevaPagina() {
        terminarPagina()
        check(numero < 1000) { "El cuaderno supera el límite de 1000 hojas PDF. No se ha exportado parcialmente." }
        numero++
        pagina = iniciar(numero)
        canvas.drawColor(Color.WHITE)
        if (numero == 1) canvas.drawText(TextUtils.ellipsize(proyecto, cabecera, UTIL.toFloat(), TextUtils.TruncateAt.END).toString(), MARGEN, 32f, cabecera)
        canvas.drawText(numero.toString(), ANCHO / 2f, ALTO - 23f, cabecera)
        posicion = INICIO
    }

    fun terminarPagina() { if (pagina != null) finalizar(); pagina = null }

    fun texto(texto: String, titulo: Boolean = false) {
        if (texto.isEmpty()) return
        val pintura = if (titulo) this.titulo else cuerpo
        val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, pintura, UTIL)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
            .setLineSpacing(3f, 1f).build()
        var primera = 0
        while (primera < layout.lineCount) {
            val origen = layout.getLineTop(primera)
            var siguiente = primera
            while (siguiente < layout.lineCount && layout.getLineBottom(siguiente) - origen <= FIN - posicion) siguiente++
            if (siguiente == primera) { nuevaPagina(); continue }
            val altura = layout.getLineBottom(siguiente - 1) - origen
            canvas.withClip(MARGEN, posicion, ANCHO - MARGEN, posicion + altura) {
                translate(MARGEN, posicion - origen)
                layout.draw(this)
            }
            posicion += altura
            primera = siguiente
            if (primera < layout.lineCount) nuevaPagina()
        }
        posicion += if (titulo) 20f else 15f
    }

    fun imagen(bitmap: android.graphics.Bitmap) {
        val escala = minOf(UTIL.toFloat() / bitmap.width, (FIN - INICIO) / bitmap.height, 1f)
        val ancho = bitmap.width * escala; val alto = bitmap.height * escala
        if (posicion + alto > FIN) nuevaPagina()
        val izquierda = (ANCHO - ancho) / 2f
        canvas.drawBitmap(bitmap, null, RectF(izquierda, posicion, izquierda + ancho, posicion + alto), Paint(Paint.FILTER_BITMAP_FLAG))
        posicion += alto + 16f
    }

    companion object {
        const val ANCHO = 595; const val ALTO = 842; const val UTIL = 503
        const val MARGEN = 46f; const val INICIO = 62f; const val FIN = 796f
    }
}
