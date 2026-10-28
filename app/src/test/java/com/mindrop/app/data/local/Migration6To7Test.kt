package com.mindrop.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration6To7Test {
    @Test fun desdeV2ConservaIdeasAgendaLogrosYPreferenciasDeRecordatorios() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-cuadernos.db"
        contexto.deleteDatabase(nombre)
        val esquema = JSONObject(javaClass.classLoader!!.getResourceAsStream("com.mindrop.app.data.local.MindropDatabase/6.json")!!.bufferedReader().readText()).getJSONObject("database")
        val ruta = contexto.getDatabasePath(nombre)
        ruta.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(ruta, null).use { vieja ->
            val tablas = esquema.getJSONArray("entities")
            repeat(tablas.length()) { indice ->
                val tabla = tablas.getJSONObject(indice)
                vieja.execSQL(tabla.getString("createSql").replace("\${TABLE_NAME}", tabla.getString("tableName")))
                val indices = tabla.optJSONArray("indices") ?: JSONArray()
                repeat(indices.length()) { vieja.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", tabla.getString("tableName"))) }
            }
            vieja.execSQL("INSERT INTO folders (id,name,icon,parent_folder_id,sort_order) VALUES (1,'Proyectos','folder',NULL,0)")
            vieja.execSQL("INSERT INTO ideas (id,title,short_description,full_description,icon,custom_icon_path,folder_id,parent_idea_id,is_completed,sort_order,created_at,updated_at) VALUES (1,'BioGestor','','Contenido personal','idea',NULL,1,NULL,1,0,10,20)")
            vieja.execSQL("INSERT INTO ideas (id,title,short_description,full_description,icon,custom_icon_path,folder_id,parent_idea_id,is_completed,sort_order,created_at,updated_at) VALUES (2,'Jara','','Subidea','idea','icono-privado.png',1,1,0,0,10,20)")
            vieja.execSQL("INSERT INTO idea_suggestions VALUES (1,1,'Actualización',1,2,1)")
            vieja.execSQL("INSERT INTO idea_suggestions VALUES (2,2,'Pendiente',1,NULL,NULL)")
            vieja.execSQL("INSERT INTO eventos VALUES (1,'Revisión','Descripción','2099-11-15',1080,2,30)")
            vieja.execSQL("INSERT INTO recordatorios VALUES (1,NULL,1,'2099-11-15T17:30',1,0)")
            vieja.execSQL("INSERT INTO logros VALUES ('ideas_1',1000,1)")
            vieja.execSQL("INSERT INTO metricas_rpg VALUES ('racha_maxima',7)")
            vieja.version = 6
        }
        val db = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).addMigrations(MIGRATION_6_7, MIGRATION_7_8).build()
        try {
            val padre = db.ideaDao().findById(1)!!
            assertEquals("Contenido personal", padre.fullDescription)
            assertTrue(padre.isCompleted)
            assertFalse(padre.esImportante)
            val hija = db.ideaDao().findById(2)!!
            assertEquals(1L, hija.parentIdeaId)
            assertEquals("icono-privado.png", hija.customIconPath)
            assertFalse(hija.esImportante)
            assertEquals(1, db.ideaSuggestionDao().findAllForIdea(1).single().updateNumber)
            assertNull(db.ideaSuggestionDao().findAllForIdea(2).single().validatedAt)
            assertEquals("Revisión", db.daoRpg().evento(1)!!.titulo)
            assertFalse(db.daoRpg().recordatorio(1)!!.enviado)
            assertEquals("ideas_1", db.daoRpg().logros().single().clave)
            assertEquals(7L, db.daoRpg().metrica("racha_maxima"))
            val proyectos = RepositorioProyectos(db)
            proyectos.crear("Nuevo cuaderno")
            assertEquals(1, proyectos.observarProyectos().first().single().numeroPaginas)
        } finally { db.close(); contexto.deleteDatabase(nombre) }
        Unit
    }
}
