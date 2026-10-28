package com.mindrop.app.ui.proyectos

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.RepositorioProyectos
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class CuadernoViewModelTest {
    private lateinit var db: MindropDatabase
    private lateinit var repo: RepositorioProyectos
    private val stores = mutableListOf<ViewModelStore>()
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).build()
        repo = RepositorioProyectos(db)
    }
    @After fun cerrar() { stores.forEach { it.clear() }; db.close() }

    private fun vm(id: Long, estado: SavedStateHandle = SavedStateHandle()): CuadernoViewModel {
        val store = ViewModelStore().also { stores += it }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CuadernoViewModel(id, repo, estado) as T
        }
        return ViewModelProvider(store, factory)[CuadernoViewModel::class.java]
    }
    private fun esperar(condicion: () -> Boolean) {
        val limite = System.nanoTime() + 5_000_000_000
        while (System.nanoTime() < limite) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            if (condicion()) return
            Thread.sleep(10)
        }
        fail("La operación del cuaderno no terminó.")
    }

    @Test fun autoguardadoYAnadirPaginaNoPierdenElUltimoTexto() {
        val id = runBlocking { repo.crear("BioGestor") }
        val modelo = vm(id)
        esperar { !modelo.uiState.value.cargando }
        val pagina = modelo.uiState.value.paginaActualId!!
        modelo.editar(true)
        modelo.cambiarTitulo(pagina, "Primera idea")
        modelo.cambiarTexto(pagina, "Versión inicial")
        modelo.cambiarTexto(pagina, "Versión final\n\nCon saltos")
        modelo.anadirPagina() // Fuerza el guardado aunque aún no hayan pasado los 400 ms.
        esperar { !modelo.uiState.value.operando && modelo.uiState.value.paginas.size == 2 }
        assertEquals("Versión final\n\nCon saltos", runBlocking { repo.pagina(pagina) }!!.texto)
        val segunda = modelo.uiState.value.paginaActualId!!
        assertNotEquals(pagina, segunda)
        modelo.cambiarTexto(segunda, "Guardado automático")
        esperar { !modelo.uiState.value.pendiente && !modelo.uiState.value.guardando }
        assertEquals("Guardado automático", runBlocking { repo.pagina(segunda) }!!.texto)
    }

    @Test fun guardarAlSalirYRecrearRecuperaLaUltimaPagina() {
        val id = runBlocking { repo.crear("Cuaderno") }
        val segunda = runBlocking { repo.anadirPagina(id) }
        val modelo = vm(id)
        esperar { modelo.uiState.value.paginaActualId == segunda }
        modelo.editar(true)
        modelo.cambiarTexto(segunda, "Escrito justo antes de salir")
        modelo.salir()
        esperar { !modelo.uiState.value.operando && !modelo.uiState.value.pendiente }
        val nuevo = vm(id)
        esperar { nuevo.uiState.value.paginaActualId == segunda }
        assertEquals("Escrito justo antes de salir", nuevo.uiState.value.paginas.single { it.id == segunda }.texto)
    }
}
