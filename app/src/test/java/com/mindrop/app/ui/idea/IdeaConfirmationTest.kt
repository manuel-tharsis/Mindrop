package com.mindrop.app.ui.idea

import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.IdeaSuggestionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
class IdeaConfirmationTest {
    private lateinit var db: MindropDatabase
    private lateinit var ideas: IdeaRepository
    private lateinit var sugerencias: IdeaSuggestionRepository
    private val store = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val eventos = mutableListOf<IdeaDetailEvent>()
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MindropDatabase::class.java).build()
        ideas = IdeaRepository(db, db.ideaDao())
        sugerencias = IdeaSuggestionRepository(db, db.ideaSuggestionDao())
    }
    @After fun cerrar() { scope.cancel(); store.clear(); db.close() }
    private fun modelo(id: Long): IdeaDetailViewModel {
        val vm = ViewModelProvider(store, IdeaDetailViewModel.factory(id, ideas, sugerencias))[IdeaDetailViewModel::class.java]
        scope.launch { vm.events.collect { eventos += it } }
        esperar { !vm.uiState.value.isLoading }
        return vm
    }
    private fun esperar(condicion: () -> Boolean) {
        val limite = System.nanoTime() + 5_000_000_000
        while (System.nanoTime() < limite) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            if (condicion()) return
            Thread.sleep(5)
        }
        fail("La operación no terminó.")
    }

    @Test fun validarConfirmaUnaVezDespuesDeGuardar() {
        val idea = runBlocking { ideas.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea")) }
        val sugerencia = runBlocking { sugerencias.addPending(idea, "Control de disolventes") }
        val vm = modelo(idea)
        esperar { vm.uiState.value.pendingSuggestions.size == 1 }
        vm.validateSuggestion(sugerencia)
        esperar { eventos.isNotEmpty() }
        assertNotNull(runBlocking { db.ideaSuggestionDao().findById(sugerencia) }?.validatedAt)
        vm.validateSuggestion(sugerencia)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        assertEquals(listOf(IdeaDetailEvent.ActionConfirmed), eventos)
    }

    @Test fun completarConfirmaPeroRestaurarYObservarNoRepitenLaRespuesta() {
        val idea = runBlocking { ideas.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea")) }
        val vm = modelo(idea)
        assertTrue(eventos.isEmpty())
        vm.toggleCompleted()
        esperar { vm.uiState.value.idea?.isCompleted == true && eventos.isNotEmpty() }
        assertTrue(runBlocking { ideas.findById(idea) }!!.isCompleted)
        vm.toggleCompleted()
        esperar { vm.uiState.value.idea?.isCompleted == false && !vm.uiState.value.isUpdatingCompletion }
        assertEquals(listOf(IdeaDetailEvent.ActionConfirmed), eventos)
    }

    @Test fun unaValidacionFallidaNoProduceConfirmacion() {
        val idea = runBlocking { ideas.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea")) }
        val sugerencia = runBlocking { sugerencias.addPending(idea, "Temporal") }
        val vm = modelo(idea)
        esperar { vm.uiState.value.pendingSuggestions.size == 1 }
        runBlocking { sugerencias.deletePending(idea, sugerencia) }
        vm.validateSuggestion(sugerencia)
        esperar { vm.uiState.value.validatingSuggestionId == null }
        assertTrue(eventos.isEmpty())
    }

    @Test fun validarDesdeEditorConfirmaSinCerrarNiPerderElBorrador() {
        val idea = runBlocking { ideas.insert(IdeaEntity(title = "BioGestor", shortDescription = "", fullDescription = "", icon = "idea")) }
        val sugerencia = runBlocking { sugerencias.addPending(idea, "Control de horas") }
        val vm = ViewModelProvider(store, IdeaEditorViewModel.factory(
            ideaId = idea,
            folderRepository = com.mindrop.app.data.repository.FolderRepository(db, db.folderDao()),
            ideaRepository = ideas,
            suggestionRepository = sugerencias,
            customIconRepository = com.mindrop.app.data.icon.CustomIconRepository(ApplicationProvider.getApplicationContext()),
        ))[IdeaEditorViewModel::class.java]
        val confirmaciones = mutableListOf<com.mindrop.app.ui.editor.EditorEvent>()
        scope.launch { vm.events.collect { confirmaciones += it } }
        esperar { !vm.uiState.value.isLoading && vm.uiState.value.pendingSuggestions.size == 1 }
        vm.updateName("BioGestor editado")
        vm.validateSuggestion(sugerencia)
        esperar { confirmaciones.isNotEmpty() }
        assertNotNull(runBlocking { db.ideaSuggestionDao().findById(sugerencia) }?.validatedAt)
        assertEquals("BioGestor editado", vm.uiState.value.name)
        assertTrue(vm.uiState.value.hasUnsavedChanges)
        vm.validateSuggestion(sugerencia)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        assertEquals(listOf(com.mindrop.app.ui.editor.EditorEvent.SuggestionValidated), confirmaciones)
    }
}
