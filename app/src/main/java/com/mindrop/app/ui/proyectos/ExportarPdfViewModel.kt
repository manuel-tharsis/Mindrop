package com.mindrop.app.ui.proyectos

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindrop.app.MindropApplication
import com.mindrop.app.data.cuaderno.ArchivoCuadernoPdf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class EstadoExportacionPdf(val trabajando: Boolean = false, val archivo: ArchivoCuadernoPdf? = null,
    val destinoSolicitado: Boolean = false, val mensaje: String? = null, val error: Boolean = false)

class ExportarPdfViewModel(private val app: MindropApplication, private val guardado: SavedStateHandle) : ViewModel() {
    private val estado = MutableStateFlow(EstadoExportacionPdf(trabajando = true))
    val uiState = estado.asStateFlow()
    init {
        val ruta = guardado.get<String>("pdf_archivo")
        if (ruta == null) estado.value = EstadoExportacionPdf()
        else viewModelScope.launch {
            val archivo = withContext(Dispatchers.IO) { runCatching { File(ruta).takeIf {
                it.canonicalFile.parentFile == app.exportadorPdf.directorio.canonicalFile && it.isFile
            } }.getOrNull() }
            estado.update { it.copy(trabajando = false) }
            if (archivo != null) estado.update { it.copy(archivo = ArchivoCuadernoPdf(archivo,
                    guardado["pdf_nombre"] ?: "Cuaderno.pdf", guardado["pdf_paginas"] ?: 0, guardado["pdf_ausentes"] ?: 0),
                    destinoSolicitado = guardado["pdf_solicitado"] ?: false) }
        }
    }

    fun preparar(id: Long) {
        if (estado.value.trabajando) return
        estado.update { it.copy(trabajando = true, mensaje = null) }
        viewModelScope.launch {
            try {
                limpiar()
                val archivo = app.exportadorPdf.preparar(id)
                guardado["pdf_archivo"] = archivo.archivo.absolutePath; guardado["pdf_nombre"] = archivo.nombre
                guardado["pdf_paginas"] = archivo.paginas; guardado["pdf_ausentes"] = archivo.imagenesAusentes
                guardado["pdf_solicitado"] = false
                estado.value = EstadoExportacionPdf(archivo = archivo)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                estado.value = EstadoExportacionPdf(mensaje = e.message?.takeIf { it.startsWith("Espera a que ") || it.startsWith("El cuaderno ") }
                    ?: "No se pudo preparar el PDF. El cuaderno se conserva.", error = true)
            }
        }
    }

    fun solicitarDestino() {
        guardado["pdf_solicitado"] = true
        estado.update { it.copy(destinoSolicitado = true) }
    }

    fun guardar(uri: Uri?) {
        if (estado.value.trabajando) return
        val archivo = estado.value.archivo ?: return
        estado.update { it.copy(trabajando = true) }
        viewModelScope.launch {
            try {
                if (uri != null) withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { salida -> archivo.archivo.inputStream().use { it.copyTo(salida) } }
                        ?: error("No se pudo abrir el destino.")
                }
                limpiar()
                estado.value = EstadoExportacionPdf(mensaje = if (uri == null) null else if (archivo.imagenesAusentes == 0) "PDF guardado."
                    else "PDF guardado. Hay imágenes no disponibles; se indican en el documento.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                estado.update { it.copy(trabajando = false, mensaje = "No se pudo guardar el PDF. Elige otra ubicación para reintentarlo.", error = true) }
            }
        }
    }

    fun reintentar() { guardado["pdf_solicitado"] = false; estado.update { it.copy(destinoSolicitado = false, mensaje = null) } }
    private suspend fun limpiar() {
        val anterior = estado.value.archivo?.archivo
        withContext(Dispatchers.IO) { anterior?.delete() }
        listOf("pdf_archivo", "pdf_nombre", "pdf_paginas", "pdf_ausentes", "pdf_solicitado").forEach { guardado.remove<Any>(it) }
    }
}
