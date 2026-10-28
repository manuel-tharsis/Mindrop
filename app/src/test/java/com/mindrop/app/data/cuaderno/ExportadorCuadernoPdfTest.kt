package com.mindrop.app.data.cuaderno

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.ImagenPagina
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExportadorCuadernoPdfTest {
    private lateinit var contexto: Context
    private lateinit var db: MindropDatabase
    private lateinit var repo: RepositorioProyectos
    private lateinit var almacen: AlmacenImagenesCuaderno
    private lateinit var exportador: ExportadorCuadernoPdf
    @Before fun preparar() {
        contexto = ApplicationProvider.getApplicationContext(); db = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
        almacen = AlmacenImagenesCuaderno(contexto); repo = RepositorioProyectos(db, almacenImagenes = almacen)
        exportador = ExportadorCuadernoPdf(contexto, repo, almacen)
    }
    @After fun cerrar() { db.close() }

    @Test fun pdfIncluyeTextoLargoSaltosDeLineaFotosBocetosYHojaVaciaEnOrden() = runBlocking {
        val id = repo.crear("BioGestor")
        val primera = repo.paginas(id).single().id
        repo.guardar(primera, "Visión general", "Inicio del cuaderno.\n\nOrganizar el trabajo del laboratorio y conservar las imágenes de referencia.")
        val fuente = File(contexto.cacheDir, "referencia.png")
        val bitmap = Bitmap.createBitmap(800, 360, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(145, 175, 195)) }
        fuente.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        repo.importarImagen(id, primera, fuente.toUri())
        val segunda = repo.anadirPagina(id)
        repo.guardar(segunda, "Control de disolventes", (1..65).joinToString("\n") { "Línea $it: revisión de muestras, horas y disolventes." } + "\n\nÚltimo párrafo completo.")
        repo.guardarDibujo(id, segunda, listOf(listOf(PuntoDibujo(.1f,.2f),PuntoDibujo(.5f,.8f),PuntoDibujo(.9f,.1f))))
        repo.anadirPagina(id)
        val directorio = File("build/reports/pdf-qa").absoluteFile.apply { mkdirs() }
        var actual: Bitmap? = null; var numero = 0
        val lienzo = PaginadorPdf("BioGestor", iniciar = { n ->
            numero = n; Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888).also { actual = it }.let(::Canvas)
        }, finalizar = {
            File(directorio, "pagina-$numero.png").outputStream().use { actual!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
            actual!!.recycle(); actual = null
        })
        val ausentes = try { exportador.representar(repo.contenidoPdf(id), lienzo) } finally { lienzo.terminarPagina() }
        assertTrue(lienzo.numero >= 4); assertEquals(0, ausentes)
        assertEquals(lienzo.numero, directorio.listFiles()!!.count { it.name.matches(Regex("pagina-[0-9]+.png")) })
    }

    @Test fun imagenInexistenteNoImpideExportarYSeIndicaEnElResultado() = runBlocking {
        val id = repo.crear("Cuaderno"); val pagina = repo.paginas(id).single().id
        db.proyectoDao().insertarImagen(ImagenPagina(paginaId = pagina, ruta = "/no-existe.png"))
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val lienzo = PaginadorPdf("Cuaderno", iniciar = { Canvas(bitmap) }, finalizar = {})
        try { assertEquals(1, exportador.representar(repo.contenidoPdf(id), lienzo)) }
        finally { lienzo.terminarPagina(); bitmap.recycle() }
    }

    @Test fun nombreDeArchivoEsSeguroYNoDependeDeRutasDelUsuario() {
        assertEquals("BioGestor.pdf", nombrePdf(" BioGestor "))
        assertEquals("Cuaderno.pdf", nombrePdf(" . "))
        assertFalse(nombrePdf("../Bio:/Gestor?\n").removeSuffix(".pdf").contains('/'))
        assertTrue(nombrePdf("A".repeat(500)).length <= 104)
    }
}
