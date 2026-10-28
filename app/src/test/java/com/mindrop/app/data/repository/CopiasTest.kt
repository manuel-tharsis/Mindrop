package com.mindrop.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.util.UUID
import java.util.zip.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CopiasTest {
    private lateinit var contexto: Context
    private lateinit var db: MindropDatabase
    private lateinit var copias: RepositorioCopias
    @Before fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, MindropDatabase::class.java).build()
        copias = RepositorioCopias(contexto, db)
    }
    @After fun cerrar() { db.close() }
    private suspend fun exportar(): ByteArray = ByteArrayOutputStream().also { copias.exportar(it) }.toByteArray()
    private fun modificar(original: ByteArray, transformar: (JSONObject) -> Unit): ByteArray {
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { zip ->
            ZipInputStream(original.inputStream()).use { entrada ->
                while (true) {
                    val siguiente = entrada.nextEntry ?: break
                    val bytes = entrada.readBytes()
                    zip.putNextEntry(ZipEntry(siguiente.name))
                    zip.write(if (siguiente.name == "mindrop.json") JSONObject(String(bytes, Charsets.UTF_8)).also(transformar).toString().toByteArray(Charsets.UTF_8) else bytes)
                    zip.closeEntry()
                }
            }
        }
        return salida.toByteArray()
    }
    private suspend fun idea(titulo: String = "BioGestor", padre: Long? = null, carpeta: Long? = null, icono: String? = null) =
        db.ideaDao().insert(IdeaEntity(title = titulo, shortDescription = "", fullDescription = "", icon = "idea",
            parentIdeaId = padre, folderId = carpeta, customIconPath = icono))

    @Test fun copiaRecuperaJerarquiaSugerenciasCompletadasCuadernoYProgreso() = runBlocking {
        val carpeta = db.folderDao().insert(FolderEntity(name = "Laboratorio"))
        val subcarpeta = db.folderDao().insert(FolderEntity(name = "Proyectos", parentFolderId = carpeta))
        val padre = idea(carpeta = subcarpeta)
        val hija = idea("APK Montones de Jara", padre, subcarpeta)
        val evento = db.daoRpg().insertarEvento(EventoAgenda(titulo = "Archivo", fecha = "1960-01-01", ideaId = padre))
        db.ideaDao().cambiarImportancia(padre, true)
        db.ideaDao().setCompleted(padre, true, 4)
        val sugerencia = db.ideaSuggestionDao().insert(IdeaSuggestionEntity(ideaId = padre, text = "Disolventes", createdAt = 1))
        IdeaSuggestionRepository(db, db.ideaSuggestionDao()).validate(padre, sugerencia)
        val proyectos = RepositorioProyectos(db)
        val proyecto = proyectos.crear("Mi cuaderno")
        val pagina = proyectos.anadirPagina(proyecto)
        val texto = "Horas\n\nDisolventes\n".repeat(2000)
        proyectos.guardar(pagina, "Organización", texto)
        db.daoRpg().guardarMetrica(MetricaRpg("personal", 42))
        val copia = exportar()
        db.ideaDao().deleteById(padre)
        proyectos.eliminar(proyecto)
        val intrusa = idea("No pertenece a la copia")
        copias.restaurar(copia.inputStream())
        assertNull(db.ideaDao().findById(intrusa))
        assertEquals(padre, db.ideaDao().findById(hija)!!.parentIdeaId)
        assertEquals(subcarpeta, db.ideaDao().findById(padre)!!.folderId)
        assertTrue(db.ideaDao().findById(padre)!!.isCompleted)
        assertTrue(db.ideaDao().findById(padre)!!.esImportante)
        assertEquals(1, db.ideaSuggestionDao().observeUpdates(padre).first().single().updateNumber)
        assertEquals(texto, proyectos.pagina(pagina)!!.texto)
        assertEquals(pagina, proyectos.observarProyecto(proyecto).first()!!.ultimaPaginaId)
        assertEquals(42L, db.daoRpg().metrica("personal"))
        assertEquals("1960-01-01", db.daoRpg().evento(evento)!!.fecha)
        assertEquals(padre, db.daoRpg().evento(evento)!!.ideaId)
        assertTrue(idea("Nueva") > intrusa)
    }

    @Test fun iconoSeRecuperaAunqueElArchivoOriginalYaNoExista() = runBlocking {
        val archivo = File(contexto.filesDir, "custom_icons/${UUID.randomUUID()}.png").apply { parentFile!!.mkdirs() }
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        val id = idea(icono = archivo.absolutePath)
        val original = archivo.readBytes()
        val copia = exportar()
        archivo.delete()
        copias.restaurar(copia.inputStream())
        val recuperado = File(db.ideaDao().findById(id)!!.customIconPath!!)
        assertNotEquals(archivo.absolutePath, recuperado.absolutePath)
        assertArrayEquals(original, recuperado.readBytes())
        assertTrue(recuperado.delete())
    }

    @Test fun archivoTruncadoNoModificaDatosActuales() = runBlocking {
        val id = idea()
        assertTrue(runCatching { copias.restaurar(byteArrayOf(1, 2, 3).inputStream()) }.isFailure)
        assertEquals("BioGestor", db.ideaDao().findById(id)!!.title)
    }

    @Test fun cicloEnCopiaSeRechazaAntesDeBorrarDatosActuales() = runBlocking {
        val id = idea()
        val copia = modificar(exportar()) { json -> json.getJSONObject("tablas").getJSONArray("ideas").getJSONObject(0).put("parent_idea_id", id) }
        assertTrue(runCatching { copias.restaurar(copia.inputStream()) }.isFailure)
        assertNull(db.ideaDao().findById(id)!!.parentIdeaId)
    }

    @Test fun relacionRotaSeRechazaEnBaseTemporalSinAlterarLaOriginal() = runBlocking {
        val id = idea()
        val copia = modificar(exportar()) { json -> json.getJSONObject("tablas").getJSONArray("ideas").getJSONObject(0).put("folder_id", 9999) }
        assertTrue(runCatching { copias.restaurar(copia.inputStream()) }.isFailure)
        assertNull(db.ideaDao().findById(id)!!.folderId)
    }

    @Test fun noSeExtraenRutasFueraDelDirectorioTemporal() = runBlocking {
        val id = idea()
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { it.putNextEntry(ZipEntry("../intruso.txt")); it.write("mal".toByteArray()); it.closeEntry() }
        assertTrue(runCatching { copias.restaurar(salida.toByteArray().inputStream()) }.isFailure)
        assertNotNull(db.ideaDao().findById(id))
        assertFalse(File(contexto.cacheDir, "intruso.txt").exists())
    }

    @Test fun versionFuturaNoModificaLaInstalacion() = runBlocking {
        val id = idea()
        val copia = modificar(exportar()) { it.put("versionRoom", 999) }
        assertTrue(runCatching { copias.restaurar(copia.inputStream()) }.isFailure)
        assertNotNull(db.ideaDao().findById(id))
    }

    @Test fun recordatorioRestauradoInvalidaTrabajosAnteriores() = runBlocking {
        val id = idea()
        val aviso = db.daoRpg().insertarRecordatorio(RecordatorioLocal(ideaId = id, fechaHora = "2030-01-01T10:00", revision = 2))
        val copia = exportar()
        copias.restaurar(copia.inputStream())
        assertTrue(db.daoRpg().recordatorio(aviso)!!.revision > 2)
        assertEquals(0, db.daoRpg().reclamarEnvio(aviso, 2))
    }

    @Test fun copiaVaciaEsValidaYBorradosPendientesImpidenExportar() = runBlocking {
        val vacia = exportar()
        val id = idea()
        copias.restaurar(vacia.inputStream())
        assertNull(db.ideaDao().findById(id))
        val borrados = BorradosRecuperables()
        borrados.proponer("idea:1", "Pendiente") { }
        assertTrue(runCatching { RepositorioCopias(contexto, db, borrados).exportar(ByteArrayOutputStream()) }.isFailure)
    }

    @Test fun restauracionPersisteDespuesDeCerrarYReabrirRoom() = runBlocking {
        val id = idea()
        val copia = exportar()
        val nombre = "copia-prueba-${UUID.randomUUID()}.db"
        var persistente = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        try {
            RepositorioCopias(contexto, persistente).restaurar(copia.inputStream())
            persistente.close()
            persistente = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
            assertEquals("BioGestor", persistente.ideaDao().findById(id)!!.title)
        } finally { persistente.close(); contexto.deleteDatabase(nombre) }
    }

    @Test fun iconoAusenteEnElArchivoRechazaLaRestauracionCompleta() = runBlocking {
        val id = idea()
        val copia = modificar(exportar()) { json ->
            json.getJSONObject("tablas").getJSONArray("ideas").getJSONObject(0)
                .put("custom_icon_path", "iconos/${UUID.randomUUID()}.png")
        }
        assertTrue(runCatching { copias.restaurar(copia.inputStream()) }.isFailure)
        assertNull(db.ideaDao().findById(id)!!.customIconPath)
    }
}
