package com.mindrop.app.data.local.entity

import androidx.room.*

@Entity(tableName = "eventos", foreignKeys = [ForeignKey(
    entity = IdeaEntity::class, parentColumns = ["id"], childColumns = ["ideaId"],
    onDelete = ForeignKey.SET_NULL,
)], indices = [Index("fecha"), Index("ideaId")])
data class EventoAgenda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String = "",
    val fecha: String,
    val minutoDelDia: Int? = null,
    val ideaId: Long? = null,
    val minutosAntes: Int? = null,
)

@Entity(tableName = "recordatorios", foreignKeys = [
    ForeignKey(entity = IdeaEntity::class, parentColumns = ["id"], childColumns = ["ideaId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = EventoAgenda::class, parentColumns = ["id"], childColumns = ["eventoId"], onDelete = ForeignKey.CASCADE),
], indices = [Index(value = ["ideaId"], unique = true), Index(value = ["eventoId"], unique = true)])
data class RecordatorioLocal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideaId: Long? = null,
    val eventoId: Long? = null,
    val fechaHora: String,
    val revision: Long = 1,
    val enviado: Boolean = false,
)

@Entity(tableName = "logros")
data class LogroDesbloqueado(
    @PrimaryKey val clave: String,
    val instante: Long,
    val celebrado: Boolean = false,
)

@Entity(tableName = "acciones_rpg")
data class AccionRpg(@PrimaryKey val clave: String)

@Entity(tableName = "metricas_rpg")
data class MetricaRpg(@PrimaryKey val clave: String, val valor: Long)

@Entity(tableName = "revisiones_ideas", foreignKeys = [ForeignKey(
    entity = IdeaEntity::class, parentColumns = ["id"], childColumns = ["ideaId"], onDelete = ForeignKey.CASCADE,
)])
data class RevisionIdea(@PrimaryKey val ideaId: Long, val revisada: Long = 0, val sugerida: Long = 0)
