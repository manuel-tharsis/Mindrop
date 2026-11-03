package com.mindrop.app.widget

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.RepositorioAgenda
import com.mindrop.app.ui.agenda.nuevoEventoConRecordatorio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaWidgetPersistenceTest {
    @Test fun eventoYAvisoDelWidgetSeConservanAlCerrarYAbrirRoom() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nombre = "agenda-widget-prueba.db"
        var db = Room.databaseBuilder(context, MindropDatabase::class.java, nombre).build()
        try {
            val evento = nuevoEventoConRecordatorio(LocalDateTime.parse("2099-12-31T23:59:59"))
                .copy(titulo = "Revisar BioGestor")
            val id = RepositorioAgenda(db).guardar(evento)
            val aviso = db.daoRpg().recordatorioEvento(id)!!
            db.close()
            db = Room.databaseBuilder(context, MindropDatabase::class.java, nombre).build()
            assertEquals(evento.copy(id = id), RepositorioAgenda(db).observarEventos().first().single())
            assertEquals(aviso, db.daoRpg().recordatorioEvento(id))
            assertEquals("2100-01-01T00:00", aviso.fechaHora)
        } finally {
            db.close()
            context.deleteDatabase(nombre)
        }
    }
}
