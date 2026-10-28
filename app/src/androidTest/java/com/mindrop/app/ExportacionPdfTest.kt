package com.mindrop.app

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindrop.app.data.cuaderno.*
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportacionPdfTest {
    @Test fun pdfRealPuedeAbrirseConTextoLargoBocetosYVariasHojas() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
        try {
            val almacen = AlmacenImagenesCuaderno(contexto)
            val repo = RepositorioProyectos(db, almacenImagenes = almacen)
            val proyecto = repo.crear("BioGestor")
            val pagina = repo.paginas(proyecto).single().id
            repo.guardar(pagina, "Control de disolventes", "Texto largo\n\n".repeat(180))
            repo.guardarDibujo(proyecto, pagina, listOf(listOf(PuntoDibujo(.1f,.1f), PuntoDibujo(.9f,.8f))))
            repo.anadirPagina(proyecto)
            val archivo = ExportadorCuadernoPdf(contexto, repo, almacen).preparar(proyecto)
            try {
                PdfRenderer(ParcelFileDescriptor.open(archivo.archivo, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                    assertTrue(renderer.pageCount >= 3)
                    assertEquals(archivo.paginas, renderer.pageCount)
                    renderer.openPage(0).use { assertEquals(595, it.width); assertEquals(842, it.height) }
                }
            } finally { archivo.archivo.delete() }
        } finally { db.close() }
    }
}
