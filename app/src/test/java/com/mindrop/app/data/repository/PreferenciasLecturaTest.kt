package com.mindrop.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.preferences.RepositorioPreferencias
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreferenciasLecturaTest {
    @Test fun lecturaPersisteAlReabrirElAlmacenYRechazaTamanosInvalidos() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val archivo = File(contexto.cacheDir, "lectura-${UUID.randomUUID()}.preferences_pb")
        var trabajo = SupervisorJob()
        var almacen = PreferenceDataStoreFactory.create(scope = CoroutineScope(trabajo + Dispatchers.IO), produceFile = { archivo })
        try {
            val repo = RepositorioPreferencias(contexto, almacen)
            assertEquals(18, repo.preferencias.first().tamanoLectura)
            assertFalse(repo.preferencias.first().papelOscuro)
            repo.lectura(22, true)
            assertTrue(runCatching { repo.lectura(100, false) }.isFailure)
            trabajo.cancelAndJoin()
            trabajo = SupervisorJob()
            almacen = PreferenceDataStoreFactory.create(scope = CoroutineScope(trabajo + Dispatchers.IO), produceFile = { archivo })
            val p = RepositorioPreferencias(contexto, almacen).preferencias.first()
            assertEquals(22, p.tamanoLectura)
            assertTrue(p.papelOscuro)
        } finally { trabajo.cancelAndJoin(); archivo.delete() }
    }
}
