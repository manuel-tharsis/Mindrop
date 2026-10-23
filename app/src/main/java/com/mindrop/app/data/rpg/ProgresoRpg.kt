package com.mindrop.app.data.rpg

import android.os.SystemClock
import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import java.time.*

// Todas las llamadas participan en la misma transacción que la acción de contenido.
class ProgresoRpg(private val db: MindropDatabase, private val reloj: () -> Long = System::currentTimeMillis,
    private val monotono: () -> Long = SystemClock::elapsedRealtime) {
    private val dao = db.daoRpg()
    suspend fun inicializar() = db.withTransaction {
        if (dao.metrica("inicializado") != null) return@withTransaction
        val ideas = dao.todasIdeas()
        val metricas = mapOf("ideas" to dao.totalIdeas(), "carpetas" to dao.totalCarpetas(),
            "subideas" to dao.totalSubideas(), "completadas" to dao.totalCompletadas(),
            "validadas" to dao.totalValidadas(), "eventos" to dao.totalEventos())
        metricas.forEach { (clave, valor) -> guardar(clave, valor) }
        ideas.forEach { idea ->
            dao.registrarAccion(AccionRpg("ideas:${idea.id}"))
            if (idea.isCompleted) dao.registrarAccion(AccionRpg("completadas:${idea.id}"))
            if (idea.parentIdeaId != null) dao.registrarAccion(AccionRpg("subideas:${idea.id}"))
        }
        db.openHelper.writableDatabase.query("SELECT id FROM folders").use { c ->
            while (c.moveToNext()) dao.registrarAccion(AccionRpg("carpetas:${c.getLong(0)}"))
        }
        db.openHelper.writableDatabase.query("SELECT id FROM idea_suggestions WHERE validated_at IS NOT NULL").use { c ->
            while (c.moveToNext()) dao.registrarAccion(AccionRpg("validadas:${c.getLong(0)}"))
        }
        guardar("inicializado", 1)
        jerarquia()
        evaluar(celebrado = true)
    }

    suspend fun registrar(tipo: String, id: Long) {
        inicializar()
        if (dao.registrarAccion(AccionRpg("$tipo:$id")) != -1L) guardar(tipo, (dao.metrica(tipo) ?: 0) + 1)
        if (tipo == "ideas" && (dao.metrica("profundidad") ?: 0) < 1) guardar("profundidad", 1)
        actividad(evaluarLogros = false)
        if (tipo == "subideas") jerarquia()
        evaluar()
    }

    suspend fun actividad(evaluarLogros: Boolean = true) {
        inicializar()
        val ahora = reloj()
        val dia = Instant.ofEpochMilli(ahora).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val ultimoDia = dao.metrica("dia_actividad")
        if (ultimoDia == dia || (ultimoDia != null && dia < ultimoDia)) return
        val anteriorMonotono = dao.metrica("reloj_monotono")
        val transcurrido = monotono()
        val pared = ahora - (dao.metrica("reloj_pared") ?: 0)
        if (ultimoDia != null && anteriorMonotono != null && transcurrido >= anteriorMonotono &&
            kotlin.math.abs(pared - (transcurrido - anteriorMonotono)) > 3_600_000) return
        val racha = if (ultimoDia != null && dia == ultimoDia + 1) (dao.metrica("racha") ?: 0) + 1 else 1
        guardar("dia_actividad", dia); guardar("reloj_pared", ahora); guardar("reloj_monotono", transcurrido)
        guardar("racha", racha); guardar("racha_maxima", maxOf(racha, dao.metrica("racha_maxima") ?: 0))
        if (evaluarLogros) evaluar()
    }

    suspend fun jerarquia() {
        val ideas = dao.todasIdeas().associateBy { it.id }
        val profundidades = mutableMapOf<Long, Long>()
        var maxima = 0L
        ideas.values.forEach { idea ->
            var actual: IdeaEntity? = idea
            val camino = linkedSetOf<Long>()
            while (actual != null && actual.id !in profundidades && camino.add(actual.id)) actual = actual.parentIdeaId?.let(ideas::get)
            var nivel = actual?.let { profundidades[it.id] } ?: 0
            camino.toList().asReversed().forEach { id -> nivel++; profundidades[id] = nivel }
            maxima = maxOf(maxima, profundidades[idea.id] ?: 0)
        }
        guardar("profundidad", maxOf(maxima, dao.metrica("profundidad") ?: 0))
    }

    suspend fun evaluar(celebrado: Boolean = false) {
        val valores = dao.metricas().associate { it.clave to it.valor }
        catalogoLogros.forEach { logro ->
            if ((valores[logro.metrica] ?: 0) >= logro.objetivo)
                dao.desbloquear(LogroDesbloqueado(logro.clave, reloj(), celebrado))
        }
    }
    private suspend fun guardar(clave: String, valor: Long) = dao.guardarMetrica(MetricaRpg(clave, valor))
}
