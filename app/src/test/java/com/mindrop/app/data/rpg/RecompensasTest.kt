package com.mindrop.app.data.rpg

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.LogroDesbloqueado
import com.mindrop.app.data.preferences.RepositorioPreferencias
import com.mindrop.app.data.repository.RepositorioLogros
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecompensasTest {
    @Test fun temaBloqueadoNoSeAplicaYDesbloqueoYPreferenciasPersisten() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val archivo = File(contexto.cacheDir, "prueba-rpg.preferences_pb")
        val nombre = "recompensas.db"
        contexto.deleteDatabase(nombre); archivo.delete()
        var trabajo = SupervisorJob()
        var preferencias = RepositorioPreferencias(contexto, PreferenceDataStoreFactory.create(scope = CoroutineScope(trabajo + Dispatchers.IO), produceFile = { archivo }))
        var db = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        var repo = RepositorioLogros(db, preferencias)
        assertTrue(runCatching { repo.aplicar("tema", "dorado") }.exceptionOrNull() is IllegalArgumentException)
        db.daoRpg().desbloquear(LogroDesbloqueado("completadas_100", 1, true))
        repo.aplicar("tema", "dorado")
        preferencias.movimiento(true)
        assertEquals("dorado", preferencias.preferencias.first().tema)
        trabajo.cancelAndJoin(); db.close()
        trabajo = SupervisorJob()
        preferencias = RepositorioPreferencias(contexto, PreferenceDataStoreFactory.create(scope = CoroutineScope(trabajo + Dispatchers.IO), produceFile = { archivo }))
        db = Room.databaseBuilder(contexto, MindropDatabase::class.java, nombre).build()
        repo = RepositorioLogros(db, preferencias)
        assertEquals("dorado", preferencias.preferencias.first().tema)
        assertTrue(preferencias.preferencias.first().reducirMovimiento)
        assertEquals("completadas_100", db.daoRpg().logros().single().clave)
        trabajo.cancelAndJoin(); db.close(); archivo.delete(); contexto.deleteDatabase(nombre)
        Unit
    }
}
