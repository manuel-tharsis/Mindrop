package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.cuaderno.AlmacenImagenesCuaderno
import com.mindrop.app.data.cuaderno.PuntoDibujo
import com.mindrop.app.data.local.MindropDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CopiasCuadernoTest {
    private lateinit var db: MindropDatabase
    private lateinit var almacen: AlmacenImagenesCuaderno
    private lateinit var repo: RepositorioProyectos
    private lateinit var copias: RepositorioCopias
    @Before fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
        almacen = AlmacenImagenesCuaderno(contexto); repo = RepositorioProyectos(db, almacenImagenes = almacen)
        copias = RepositorioCopias(contexto, db)
    }
    @After fun cerrar() { db.close() }
    private suspend fun exportar() = ByteArrayOutputStream().also { copias.exportar(it) }.toByteArray()
    private suspend fun conDibujo(): Pair<Long, Long> {
        val proyecto = repo.crear("BioGestor"); val pagina = repo.paginas(proyecto).single().id
        repo.guardarDibujo(proyecto, pagina, listOf(listOf(PuntoDibujo(.2f,.3f), PuntoDibujo(.8f,.7f))))
        return proyecto to pagina
    }
    private fun cambiar(bytes: ByteArray, editar: (JSONObject) -> Unit = {}, romperImagen: Boolean = false): ByteArray {
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { zip -> ZipInputStream(bytes.inputStream()).use { entrada ->
            while (true) {
                val archivo = entrada.nextEntry ?: break
                val datos = entrada.readBytes()
                zip.putNextEntry(ZipEntry(archivo.name))
                zip.write(when {
                    archivo.name == "mindrop.json" -> JSONObject(String(datos, Charsets.UTF_8)).also(editar).toString().toByteArray(Charsets.UTF_8)
                    romperImagen && archivo.name.startsWith("imagenes/") -> "imagen rota".toByteArray()
                    else -> datos
                }); zip.closeEntry()
            }
        } }
        return salida.toByteArray()
    }

    @Test fun fotosYBocetosSeRestauranConSusPaginasYSinElArchivoAnterior() = runBlocking {
        val (proyecto, pagina) = conDibujo()
        val antigua = db.proyectoDao().imagenesPagina(pagina).single()
        val bytes = File(antigua.ruta).readBytes(); val copia = exportar()
        repo.eliminar(proyecto); assertFalse(File(antigua.ruta).exists())
        copias.restaurar(copia.inputStream())
        val nueva = db.proyectoDao().imagenesPagina(pagina).single()
        assertNotEquals(antigua.ruta, nueva.ruta)
        assertArrayEquals(bytes, File(nueva.ruta).readBytes())
        assertEquals(almacen.directorio, File(nueva.ruta).canonicalFile.parentFile)
        assertEquals(proyecto, db.proyectoDao().pagina(pagina)!!.proyectoId)
    }

    @Test fun copiasRoom7SeAceptanSinInventarImagenesNiCambiarElTexto() = runBlocking {
        val proyecto = repo.crear("Anterior"); val pagina = repo.paginas(proyecto).single().id
        repo.guardar(pagina, "Datos", "Contenido de v2.1\n\nConservado")
        val copia = cambiar(exportar(), editar = {
            it.put("versionRoom", 7); it.getJSONObject("tablas").remove("imagenes_pagina"); it.getJSONObject("secuencias").remove("imagenes_pagina")
        })
        repo.guardar(pagina, "Temporal", "otro")
        copias.restaurar(copia.inputStream())
        assertEquals("Contenido de v2.1\n\nConservado", repo.pagina(pagina)!!.texto)
        assertTrue(db.proyectoDao().imagenesProyecto(proyecto).isEmpty())
    }

    @Test fun imagenCorruptaEnLaCopiaNoAlteraNiBorraLaInstalacion() = runBlocking {
        val (_, pagina) = conDibujo(); val original = db.proyectoDao().imagenesPagina(pagina).single()
        val copia = cambiar(exportar(), romperImagen = true)
        assertTrue(runCatching { copias.restaurar(copia.inputStream()) }.isFailure)
        assertEquals(original, db.proyectoDao().imagenesPagina(pagina).single())
        assertTrue(File(original.ruta).isFile)
    }

    @Test fun referenciasExternasORelacionesRotasSeRechazanSinDejarArchivosNuevos() = runBlocking {
        val (_, pagina) = conDibujo(); val original = db.proyectoDao().imagenesPagina(pagina).single()
        val iniciales = almacen.directorio.listFiles()!!.map { it.name }.toSet()
        val copia = exportar()
        val externa = cambiar(copia, editar = { it.getJSONObject("tablas").getJSONArray("imagenes_pagina").getJSONObject(0).put("ruta", "../externa.png") })
        assertTrue(runCatching { copias.restaurar(externa.inputStream()) }.isFailure)
        val rota = cambiar(copia, editar = { it.getJSONObject("tablas").getJSONArray("imagenes_pagina").getJSONObject(0).put("paginaId", 99999) })
        assertTrue(runCatching { copias.restaurar(rota.inputStream()) }.isFailure)
        assertEquals(original, db.proyectoDao().imagenesPagina(pagina).single())
        assertEquals(iniciales, almacen.directorio.listFiles()!!.map { it.name }.toSet())
    }
}
