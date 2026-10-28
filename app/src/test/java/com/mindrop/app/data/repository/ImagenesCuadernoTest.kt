package com.mindrop.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.cuaderno.AlmacenImagenesCuaderno
import com.mindrop.app.data.cuaderno.PuntoDibujo
import com.mindrop.app.data.local.MindropDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImagenesCuadernoTest {
    private lateinit var contexto: Context
    private lateinit var db: MindropDatabase
    private lateinit var almacen: AlmacenImagenesCuaderno
    private lateinit var repo: RepositorioProyectos
    private lateinit var borrados: BorradosRecuperables
    @Before fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
        almacen = AlmacenImagenesCuaderno(contexto); borrados = BorradosRecuperables()
        repo = RepositorioProyectos(db, borrados, almacen)
    }
    @After fun cerrar() { db.close() }
    private fun foto(): File {
        val archivo = File(contexto.cacheDir, "fuente-${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
        archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        return archivo
    }
    private val trazos get() = listOf(listOf(PuntoDibujo(.1f, .2f), PuntoDibujo(.5f, .7f), PuntoDibujo(.9f, .1f)))

    @Test fun imagenSeCopiaSeReduceYConservaSinElOriginal() = runBlocking {
        val proyecto = repo.crear("Cuaderno"); val pagina = repo.paginas(proyecto).single().id
        val fuente = foto()
        repo.importarImagen(proyecto, pagina, fuente.toUri()); fuente.delete()
        val imagen = db.proyectoDao().imagenesPagina(pagina).single()
        val copia = File(imagen.ruta)
        assertTrue(copia.isFile); assertTrue(copia.length() <= AlmacenImagenesCuaderno.MAX_BYTES)
        assertEquals(almacen.directorio, copia.canonicalFile.parentFile)
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(copia.absolutePath, limites)
        assertTrue(limites.outWidth in 1..1600); assertEquals(limites.outWidth / 2, limites.outHeight)
        assertEquals(imagen, repo.observarImagenes(proyecto).first().single())
    }

    @Test fun dibujosPersistenComoImagenYSeLimpianAlEliminarse() = runBlocking {
        val proyecto = repo.crear("Cuaderno"); val pagina = repo.paginas(proyecto).single().id
        repo.guardarDibujo(proyecto, pagina, trazos)
        val imagen = db.proyectoDao().imagenesPagina(pagina).single()
        assertEquals("png", File(imagen.ruta).extension)
        assertTrue(File(imagen.ruta).isFile)
        repo.eliminarImagen(proyecto, imagen.id)
        assertFalse(File(imagen.ruta).exists()); assertTrue(db.proyectoDao().imagenesPagina(pagina).isEmpty())
    }

    @Test fun orientacionExifSeAplicaAntesDeGuardarLaCopia() = runBlocking {
        val proyecto = repo.crear("Foto"); val pagina = repo.paginas(proyecto).single().id
        val fuente = File(contexto.cacheDir, "girada.jpg")
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
        fuente.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }; bitmap.recycle()
        ExifInterface(fuente.absolutePath).apply { setAttribute(ExifInterface.TAG_ORIENTATION, "6"); saveAttributes() }
        repo.importarImagen(proyecto, pagina, fuente.toUri())
        val imagen = db.proyectoDao().imagenesPagina(pagina).single()
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagen.ruta, limites)
        assertEquals(40, limites.outWidth); assertEquals(80, limites.outHeight)
    }

    @Test fun deshacerPaginaConservaSusImagenesYConfirmarLasElimina() = runBlocking {
        val proyecto = repo.crear("Cuaderno"); val pagina = repo.paginas(proyecto).single().id
        repo.guardarDibujo(proyecto, pagina, trazos)
        val imagen = db.proyectoDao().imagenesPagina(pagina).single()
        repo.eliminarPaginaConDeshacer(proyecto, pagina)
        assertTrue(File(imagen.ruta).isFile)
        assertTrue(runCatching { repo.contenidoPdf(proyecto) }.isFailure)
        borrados.resolver("pagina:$pagina", true)
        assertEquals(imagen, db.proyectoDao().imagenesPagina(pagina).single())
        repo.eliminarPaginaConDeshacer(proyecto, pagina); borrados.resolver("pagina:$pagina", false)
        assertFalse(File(imagen.ruta).exists()); assertTrue(db.proyectoDao().imagenesPagina(pagina).isEmpty())
        assertEquals(1, repo.paginas(proyecto).size)
    }

    @Test fun borrarProyectoLimpiaImagenesPeroNoArchivosExternos() = runBlocking {
        val proyecto = repo.crear("Cuaderno"); val pagina = repo.paginas(proyecto).single().id
        val fuente = foto(); repo.importarImagen(proyecto, pagina, fuente.toUri())
        val imagen = db.proyectoDao().imagenesPagina(pagina).single()
        repo.eliminar(proyecto)
        assertFalse(File(imagen.ruta).exists()); assertTrue(fuente.exists())
        assertFalse(almacen.eliminar(fuente.absolutePath)); assertTrue(fuente.exists())
    }

    @Test fun imagenesCorruptasDibujosVaciosYReferenciasAProyectoAjenoSeRechazan() = runBlocking {
        val a = repo.crear("A"); val b = repo.crear("B"); val pagina = repo.paginas(a).single().id
        val mala = File(contexto.cacheDir, "mala.png").apply { writeText("no es una imagen") }
        assertTrue(runCatching { repo.importarImagen(a, pagina, mala.toUri()) }.isFailure)
        assertTrue(runCatching { repo.guardarDibujo(a, pagina, emptyList()) }.isFailure)
        assertTrue(runCatching { repo.guardarDibujo(b, pagina, trazos) }.isFailure)
        assertTrue(runCatching { repo.guardarDibujo(a, pagina, listOf(listOf(PuntoDibujo(Float.NaN, .5f)))) }.isFailure)
        assertTrue(db.proyectoDao().imagenesProyecto(a).isEmpty())
    }

    @Test fun imagenesConservanLaRelacionAlReabrirRoomYReordenarPaginas() = runBlocking {
        val nombre = "imagenes-${UUID.randomUUID()}.db"
        var persistente = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        try {
            var proyectos = RepositorioProyectos(persistente, almacenImagenes = almacen)
            val proyecto = proyectos.crear("Cuaderno"); val primera = proyectos.paginas(proyecto).single().id
            val segunda = proyectos.anadirPagina(proyecto)
            proyectos.guardarDibujo(proyecto, primera, trazos)
            val imagen = persistente.proyectoDao().imagenesPagina(primera).single()
            proyectos.moverPagina(proyecto, segunda, -1)
            persistente.close(); persistente = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
            proyectos = RepositorioProyectos(persistente, almacenImagenes = almacen)
            assertEquals(listOf(segunda, primera), proyectos.paginas(proyecto).map { it.id })
            assertEquals(imagen, persistente.proyectoDao().imagenesPagina(primera).single())
            assertTrue(File(imagen.ruta).isFile)
        } finally { persistente.close(); contexto.deleteDatabase(nombre) }
        Unit
    }
}
