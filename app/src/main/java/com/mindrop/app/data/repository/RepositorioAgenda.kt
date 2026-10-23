package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.EventoAgenda
import com.mindrop.app.data.local.entity.RecordatorioLocal
import java.time.*
import com.mindrop.app.data.rpg.ProgresoRpg

class RepositorioAgenda(private val db: MindropDatabase) {
    private val dao = db.daoRpg()
    var alCambiarRecordatorios: () -> Unit = {}
    var alCancelarRecordatorio: (Long) -> Unit = {}
    fun observarEventos() = dao.observarEventos()

    suspend fun guardar(evento: EventoAgenda): Long {
        require(evento.titulo.isNotBlank()) { "Escribe un título." }
        val fecha = LocalDate.parse(evento.fecha)
        require(evento.minutoDelDia == null || evento.minutoDelDia in 0..1439) { "Hora no válida." }
        require(evento.minutosAntes == null || evento.minutosAntes in listOf(0, 10, 30, 60, 1440))
        require(evento.minutosAntes == null || evento.minutoDelDia != null) { "El recordatorio necesita una hora." }
        var avisoCancelado: Long? = null
        val id = db.withTransaction {
            val progreso = ProgresoRpg(db)
            progreso.inicializar()
            if (evento.ideaId != null) require(db.ideaDao().findById(evento.ideaId) != null) { "La idea ya no existe." }
            if (evento.id != 0L) require(dao.evento(evento.id) != null) { "El evento ya no existe." }
            val id = if (evento.id == 0L) dao.insertarEvento(evento.copy(titulo = evento.titulo.trim())) else {
                dao.actualizarEvento(evento.copy(titulo = evento.titulo.trim()))
                evento.id
            }
            val anterior = dao.recordatorioEvento(id)
            avisoCancelado = anterior?.id
            if (evento.minutosAntes == null) {
                anterior?.let { dao.borrarRecordatorio(it.id) }
            } else {
                val momento = fecha.atStartOfDay().plusMinutes(evento.minutoDelDia!!.toLong())
                    .minusMinutes(evento.minutosAntes.toLong())
                require(momento.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > System.currentTimeMillis()) {
                    "El recordatorio debe estar en el futuro."
                }
                val recordatorio = RecordatorioLocal(
                    id = anterior?.id ?: 0, eventoId = id, fechaHora = momento.toString(),
                    revision = (anterior?.revision ?: 0) + 1,
                )
                if (anterior == null) dao.insertarRecordatorio(recordatorio) else dao.actualizarRecordatorio(recordatorio)
            }
            if (evento.id == 0L) progreso.registrar("eventos", id) else progreso.actividad()
            id
        }
        avisoCancelado?.let(alCancelarRecordatorio)
        alCambiarRecordatorios()
        return id
    }

    suspend fun eliminar(id: Long) {
        var avisoCancelado: Long? = null
        db.withTransaction {
            avisoCancelado = dao.recordatorioEvento(id)?.id
            dao.borrarEvento(id)
        }
        avisoCancelado?.let(alCancelarRecordatorio)
        alCambiarRecordatorios()
    }
}
