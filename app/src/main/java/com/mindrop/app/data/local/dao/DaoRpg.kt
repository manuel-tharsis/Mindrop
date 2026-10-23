package com.mindrop.app.data.local.dao

import androidx.room.*
import com.mindrop.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoRpg {
    @Query("SELECT * FROM eventos ORDER BY fecha, COALESCE(minutoDelDia, -1), id")
    fun observarEventos(): Flow<List<EventoAgenda>>
    @Query("SELECT * FROM eventos WHERE id = :id") suspend fun evento(id: Long): EventoAgenda?
    @Insert suspend fun insertarEvento(evento: EventoAgenda): Long
    @Update suspend fun actualizarEvento(evento: EventoAgenda): Int
    @Query("DELETE FROM eventos WHERE id = :id") suspend fun borrarEvento(id: Long): Int
    @Query("SELECT * FROM recordatorios WHERE id = :id") suspend fun recordatorio(id: Long): RecordatorioLocal?
    @Query("SELECT * FROM recordatorios WHERE eventoId = :id") suspend fun recordatorioEvento(id: Long): RecordatorioLocal?
    @Query("SELECT * FROM recordatorios WHERE ideaId = :id") suspend fun recordatorioIdea(id: Long): RecordatorioLocal?
    @Query("SELECT * FROM recordatorios WHERE enviado = 0") suspend fun pendientes(): List<RecordatorioLocal>
    @Insert suspend fun insertarRecordatorio(recordatorio: RecordatorioLocal): Long
    @Update suspend fun actualizarRecordatorio(recordatorio: RecordatorioLocal)
    @Query("DELETE FROM recordatorios WHERE id = :id") suspend fun borrarRecordatorio(id: Long)
    @Query("UPDATE recordatorios SET enviado = 1 WHERE id = :id AND revision = :revision AND enviado = 0")
    suspend fun reclamarEnvio(id: Long, revision: Long): Int
    @Query("SELECT * FROM logros ORDER BY instante, clave") fun observarLogros(): Flow<List<LogroDesbloqueado>>
    @Query("SELECT * FROM logros") suspend fun logros(): List<LogroDesbloqueado>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun desbloquear(logro: LogroDesbloqueado): Long
    @Query("UPDATE logros SET celebrado = 1 WHERE clave = :clave") suspend fun celebrar(clave: String)
    @Query("SELECT * FROM metricas_rpg") fun observarMetricas(): Flow<List<MetricaRpg>>
    @Query("SELECT * FROM metricas_rpg") suspend fun metricas(): List<MetricaRpg>
    @Query("SELECT valor FROM metricas_rpg WHERE clave = :clave") suspend fun metrica(clave: String): Long?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarMetrica(metrica: MetricaRpg)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun registrarAccion(accion: AccionRpg): Long
    @Query("SELECT COUNT(*) FROM ideas") suspend fun totalIdeas(): Long
    @Query("SELECT COUNT(*) FROM folders") suspend fun totalCarpetas(): Long
    @Query("SELECT COUNT(*) FROM ideas WHERE parent_idea_id IS NOT NULL") suspend fun totalSubideas(): Long
    @Query("SELECT COUNT(*) FROM ideas WHERE is_completed = 1") suspend fun totalCompletadas(): Long
    @Query("SELECT COUNT(*) FROM idea_suggestions WHERE validated_at IS NOT NULL") suspend fun totalValidadas(): Long
    @Query("SELECT COUNT(*) FROM eventos") suspend fun totalEventos(): Long
    @Query("SELECT * FROM ideas") suspend fun todasIdeas(): List<IdeaEntity>
    @Query("SELECT * FROM revisiones_ideas WHERE ideaId = :id") suspend fun revisionIdea(id: Long): RevisionIdea?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarRevision(revision: RevisionIdea)
    @Query("""SELECT ideas.* FROM ideas LEFT JOIN revisiones_ideas r ON ideas.id = r.ideaId
        WHERE is_completed = 0 AND MAX(COALESCE(r.revisada, 0), ideas.updated_at) <= :antiguedad
        AND COALESCE(r.sugerida, 0) <= :limiteRepeticion
        ORDER BY MAX(COALESCE(r.revisada, 0), ideas.updated_at, COALESCE(r.sugerida, 0)), ideas.id LIMIT 1""")
    suspend fun ideaOlvidada(antiguedad: Long, limiteRepeticion: Long): IdeaEntity?
}
