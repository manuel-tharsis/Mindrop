package com.mindrop.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.entity.EventoAgenda
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration5To6Test {
    @Test fun actualizarV111ConservaIdeasRelacionesYContenidoYPermiteAgenda() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "migracion-v2.db"
        contexto.deleteDatabase(nombre)
        val esquema = JSONObject(javaClass.classLoader!!.getResourceAsStream("com.mindrop.app.data.local.MindropDatabase/5.json")!!.bufferedReader().readText()).getJSONObject("database")
        val ruta = contexto.getDatabasePath(nombre)
        ruta.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(ruta, null).use { vieja ->
            val tablas = esquema.getJSONArray("entities")
            repeat(tablas.length()) { indice ->
                val tabla = tablas.getJSONObject(indice)
                vieja.execSQL(tabla.getString("createSql").replace("\${TABLE_NAME}", tabla.getString("tableName")))
                val indices = tabla.getJSONArray("indices")
                repeat(indices.length()) { vieja.execSQL(indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", tabla.getString("tableName"))) }
            }
            vieja.execSQL("INSERT INTO folders VALUES (1, 'Proyectos', 'folder', NULL, 0)")
            vieja.execSQL("INSERT INTO ideas (id,title,short_description,full_description,icon,custom_icon_path,folder_id,parent_idea_id,is_completed,sort_order,created_at,updated_at) VALUES (1,'BioGestor','','Contenido personal','idea',NULL,1,NULL,1,0,1,1)")
            vieja.execSQL("INSERT INTO ideas (id,title,short_description,full_description,icon,custom_icon_path,folder_id,parent_idea_id,is_completed,sort_order,created_at,updated_at) VALUES (2,'Jara','','Subidea','idea',NULL,1,1,0,0,1,1)")
            vieja.execSQL("INSERT INTO idea_suggestions VALUES (1,1,'Actualización conservada',1,2,1)")
            vieja.version = 5
        }
        val db = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).allowMainThreadQueries().addMigrations(MIGRATION_5_6, MIGRATION_6_7).build()
        try {
            assertEquals("Contenido personal", db.ideaDao().findById(1)!!.fullDescription)
            assertTrue(db.ideaDao().findById(1)!!.isCompleted)
            assertEquals(1L, db.ideaDao().findById(2)!!.parentIdeaId)
            assertEquals(1, db.ideaSuggestionDao().findAllForIdea(1).single().updateNumber)
            db.daoRpg().insertarEvento(EventoAgenda(titulo = "Revisar", fecha = "2026-11-15", ideaId = 1))
        } finally { db.close(); contexto.deleteDatabase(nombre) }
        Unit
    }
}
