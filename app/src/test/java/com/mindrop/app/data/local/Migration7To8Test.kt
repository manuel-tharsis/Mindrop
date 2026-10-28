package com.mindrop.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.entity.ImagenPagina
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration7To8Test {
    @Test fun migracionConservaIdeasPrioridadCuadernoYUltimaPagina() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-imagenes-${UUID.randomUUID()}.db"
        val ruta = contexto.getDatabasePath(nombre).apply { parentFile!!.mkdirs() }
        val esquema = JSONObject(javaClass.classLoader!!.getResourceAsStream("com.mindrop.app.data.local.MindropDatabase/7.json")!!.bufferedReader().readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(ruta, null).use { vieja ->
            val entidades = esquema.getJSONArray("entities")
            repeat(entidades.length()) { indice ->
                val tabla = entidades.getJSONObject(indice); val nombreTabla = tabla.getString("tableName")
                vieja.execSQL(tabla.getString("createSql").replace("\${TABLE_NAME}", nombreTabla))
                val indices = tabla.optJSONArray("indices") ?: JSONArray()
                repeat(indices.length()) { vieja.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", nombreTabla)) }
            }
            vieja.execSQL("INSERT INTO ideas (id,title,short_description,full_description,icon,custom_icon_path,folder_id,parent_idea_id,is_completed,es_importante,sort_order,created_at,updated_at) VALUES (1,'BioGestor','','Datos personales','idea',NULL,NULL,NULL,0,1,0,10,20)")
            vieja.execSQL("INSERT INTO proyectos VALUES (1,'BioGestor',2)")
            vieja.execSQL("INSERT INTO paginas_proyecto VALUES (1,1,'Primera','Texto original',0)")
            vieja.execSQL("INSERT INTO paginas_proyecto VALUES (2,1,'Segunda','Texto con saltos\n\nÚltima línea',1)")
            vieja.version = 7
        }
        val db = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).addMigrations(MIGRATION_7_8).build()
        try {
            assertTrue(db.ideaDao().findById(1)!!.esImportante)
            assertEquals("Datos personales", db.ideaDao().findById(1)!!.fullDescription)
            assertEquals(2L, db.proyectoDao().proyecto(1)!!.ultimaPaginaId)
            assertEquals(listOf(1L, 2L), db.proyectoDao().paginas(1).map { it.id })
            assertEquals("Texto con saltos\n\nÚltima línea", db.proyectoDao().pagina(2)!!.texto)
            assertTrue(db.proyectoDao().imagenesProyecto(1).isEmpty())
            db.proyectoDao().insertarImagen(ImagenPagina(paginaId = 2, ruta = "privada.png"))
            assertEquals(1, db.proyectoDao().imagenesPagina(2).size)
            db.proyectoDao().borrarPagina(2)
            assertTrue(db.proyectoDao().imagenesProyecto(1).isEmpty())
        } finally { db.close(); contexto.deleteDatabase(nombre) }
        Unit
    }
}
