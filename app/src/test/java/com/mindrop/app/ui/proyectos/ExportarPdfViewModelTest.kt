package com.mindrop.app.ui.proyectos

import android.os.Looper
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.MindropApplication
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class ExportarPdfViewModelTest {
    private lateinit var app: MindropApplication
    private val stores = mutableListOf<ViewModelStore>()
    @Before fun preparar() { app = ApplicationProvider.getApplicationContext() }
    @After fun cerrar() { stores.forEach { it.clear() } }
    private fun vm(guardado: SavedStateHandle): ExportarPdfViewModel {
        val store = ViewModelStore().also { stores += it }
        val fabrica = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>) = ExportarPdfViewModel(app, guardado) as T
        }
        return ViewModelProvider(store, fabrica)[ExportarPdfViewModel::class.java]
    }
    private fun esperar(condicion: () -> Boolean) {
        val limite = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < limite) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(25))
            if (condicion()) return
            Thread.sleep(10)
        }
        fail("La exportación no terminó.")
    }
    private fun preparado(): Pair<File, SavedStateHandle> {
        val archivo = File.createTempFile("mindrop-test-", ".pdf", app.exportadorPdf.directorio).apply { writeText("Contenido preparado") }
        return archivo to SavedStateHandle(mapOf("pdf_archivo" to archivo.absolutePath, "pdf_nombre" to "BioGestor.pdf", "pdf_paginas" to 3,
            "pdf_ausentes" to 0, "pdf_solicitado" to true))
    }

    @Test fun inicioSinPdfPendienteNoAbreUnSelectorNiMuestraEspera() {
        val vm = vm(SavedStateHandle())
        assertFalse(vm.uiState.value.trabajando); assertNull(vm.uiState.value.archivo)
    }

    @Test fun recrearRecuperaPdfPreparadoSinSolicitarDosVecesElDestino() {
        val (archivo, guardado) = preparado()
        try {
            val vm = vm(guardado); esperar { vm.uiState.value.archivo != null }
            assertEquals(archivo, vm.uiState.value.archivo!!.archivo)
            assertTrue(vm.uiState.value.destinoSolicitado)
            assertEquals("BioGestor.pdf", vm.uiState.value.archivo!!.nombre)
            vm.reintentar(); assertFalse(vm.uiState.value.destinoSolicitado)
        } finally { archivo.delete() }
    }

    @Test fun cancelarSelectorLimpiaArchivoTemporalYEstado() {
        val (archivo, guardado) = preparado()
        val vm = vm(guardado); esperar { vm.uiState.value.archivo != null }
        vm.guardar(null); esperar { !vm.uiState.value.trabajando && vm.uiState.value.archivo == null }
        assertFalse(archivo.exists()); assertNull(guardado.get<String>("pdf_archivo"))
        assertNull(vm.uiState.value.mensaje)
    }

    @Test fun guardarCopiaElArchivoCompletoYEliminaSoloElTemporal() {
        val (archivo, guardado) = preparado(); val bytes = archivo.readBytes()
        val salida = File(app.cacheDir, "salida-elegida.pdf")
        val vm = vm(guardado); esperar { vm.uiState.value.archivo != null }
        vm.guardar(salida.toUri()); esperar { !vm.uiState.value.trabajando && vm.uiState.value.archivo == null }
        assertFalse(archivo.exists()); assertArrayEquals(bytes, salida.readBytes())
        assertEquals("PDF guardado.", vm.uiState.value.mensaje)
    }

    @Test fun estadoRestauradoNoPuedeApuntarAArchivosFueraDeLaCachePdf() {
        val externo = File(app.filesDir, "privado.txt").apply { writeText("no tocar") }
        val vm = vm(SavedStateHandle(mapOf("pdf_archivo" to externo.absolutePath)))
        esperar { !vm.uiState.value.trabajando }
        assertNull(vm.uiState.value.archivo); assertEquals("no tocar", externo.readText())
    }
}
