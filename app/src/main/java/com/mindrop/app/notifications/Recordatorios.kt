package com.mindrop.app.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.room.withTransaction
import androidx.work.*
import com.mindrop.app.MainActivity
import com.mindrop.app.MindropApplication
import com.mindrop.app.R
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.data.preferences.RepositorioPreferencias
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.concurrent.TimeUnit

fun instanteRecordatorio(fechaHora: String, zona: ZoneId = ZoneId.systemDefault()): Long =
    LocalDateTime.parse(fechaHora).atZone(zona).toInstant().toEpochMilli()

fun enSilencio(hora: Int, desde: Int, hasta: Int): Boolean =
    if (desde > hasta) hora >= desde || hora < hasta else hora in desde until hasta

class ProgramadorRecordatorios(private val contexto: Context, private val db: MindropDatabase) {
    private val cerrojo = Mutex()
    suspend fun reconciliar() = cerrojo.withLock {
        val gestor = WorkManager.getInstance(contexto)
        // Las filas eliminadas invalidan sus trabajos; el Worker consulta Room antes de notificar.
        db.daoRpg().pendientes().forEach { aviso ->
            val retraso = instanteRecordatorio(aviso.fechaHora) - System.currentTimeMillis()
            if (retraso < -24 * 60 * 60 * 1000L) {
                db.daoRpg().reclamarEnvio(aviso.id, aviso.revision)
            } else {
                gestor.enqueueUniqueWork("aviso-${aviso.id}", ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<TrabajadorAviso>()
                        .setInputData(workDataOf("id" to aviso.id, "revision" to aviso.revision))
                        .setInitialDelay(retraso.coerceAtLeast(0), TimeUnit.MILLISECONDS).build())
            }
        }
    }

    fun cancelar(id: Long) {
        WorkManager.getInstance(contexto).cancelUniqueWork("aviso-$id")
        NotificationManagerCompat.from(contexto).cancel("aviso", id.toInt())
    }

    suspend fun programarIdea(ideaId: Long, fechaHora: String?) {
        var anteriorId: Long? = null
        db.withTransaction {
            val idea = db.ideaDao().findById(ideaId) ?: error("La idea ya no existe.")
            val anterior = db.daoRpg().recordatorioIdea(ideaId)
            anteriorId = anterior?.id
            if (fechaHora == null) anterior?.let { db.daoRpg().borrarRecordatorio(it.id) } else {
                require(!idea.isCompleted) { "Primero marca la idea como no completada." }
                require(instanteRecordatorio(fechaHora) > System.currentTimeMillis()) { "Selecciona una fecha y hora futuras." }
                val nuevo = RecordatorioLocal(id = anterior?.id ?: 0, ideaId = ideaId, fechaHora = fechaHora,
                    revision = (anterior?.revision ?: 0) + 1)
                if (anterior == null) db.daoRpg().insertarRecordatorio(nuevo) else db.daoRpg().actualizarRecordatorio(nuevo)
            }
        }
        anteriorId?.let(::cancelar)
        reconciliar()
    }

    fun activarRedescubrimiento() {
        // Un único sondeo diario. La frecuencia elegida y el silencio se comprueban antes del envío.
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("redescubrimiento", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<TrabajadorRedescubrimiento>(24, TimeUnit.HOURS).build())
    }
    fun desactivarRedescubrimiento() {
        WorkManager.getInstance(contexto).cancelUniqueWork("redescubrimiento")
        NotificationManagerCompat.from(contexto).cancel("redescubrimiento", 0)
    }
}

fun puedeNotificar(contexto: Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(contexto).areNotificationsEnabled()

fun publicarAviso(contexto: Context, etiqueta: String, numero: Int, titulo: String, texto: String, ruta: String): Boolean {
    if (!puedeNotificar(contexto)) return false
    val gestor = contexto.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) gestor.createNotificationChannel(NotificationChannel("mindrop_local", "Recordatorios de Mindrop", NotificationManager.IMPORTANCE_DEFAULT))
    val intent = Intent(contexto, MainActivity::class.java).apply {
        data = "mindrop://$ruta".toUri()
        putExtra("destino_local", ruta)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendiente = PendingIntent.getActivity(contexto, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    try {
        NotificationManagerCompat.from(contexto).notify(etiqueta, numero,
            NotificationCompat.Builder(contexto, "mindrop_local").setSmallIcon(R.drawable.ic_idea)
                .setContentTitle(titulo).setContentText(texto).setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setContentIntent(pendiente).setAutoCancel(true).build())
    } catch (_: SecurityException) { return false }
    return true
}

class TrabajadorAviso(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MindropApplication
        val dao = app.database.daoRpg()
        val id = inputData.getLong("id", -1)
        val revision = inputData.getLong("revision", -1)
        val aviso = dao.recordatorio(id) ?: return Result.success()
        if (aviso.enviado || aviso.revision != revision) return Result.success()
        val momento = instanteRecordatorio(aviso.fechaHora)
        if (momento > System.currentTimeMillis() + 60_000) { app.programador.reconciliar(); return Result.success() }
        val idea = aviso.ideaId?.let { app.ideaRepository.findById(it) }
        val evento = aviso.eventoId?.let { dao.evento(it) }
        if (aviso.ideaId != null && (idea == null || idea.isCompleted)) return Result.success()
        if (aviso.eventoId != null && evento == null) return Result.success()
        if (!puedeNotificar(applicationContext)) return Result.success()
        // Reclamación transaccional: un trabajo repetido nunca vuelve a enviar esta revisión.
        if (dao.reclamarEnvio(id, revision) == 1) publicarAviso(applicationContext, "aviso", id.toInt(),
            evento?.titulo ?: "¡Tienes una idea pendiente!", idea?.title ?: evento?.descripcion.orEmpty(),
            (idea?.id ?: evento?.ideaId)?.let { "idea/$it" } ?: "agenda")
        return Result.success()
    }
}

class TrabajadorRedescubrimiento(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MindropApplication
        val p = app.preferencias.preferencias.first()
        if (p.diasRedescubrimiento == 0 || !puedeNotificar(applicationContext)) return Result.success()
        val ahora = System.currentTimeMillis()
        val dao = app.database.daoRpg()
        val ultimo = dao.metrica("ultimo_redescubrimiento") ?: 0
        val intervalo = p.diasRedescubrimiento * 86_400_000L
        if (ahora - ultimo < intervalo) return Result.success()
        if (enSilencio(LocalTime.now().hour, p.silencioDesde, p.silencioHasta)) {
            val retraso = LocalDate.now().plusDays(if (LocalTime.now().hour >= p.silencioDesde && p.silencioDesde > p.silencioHasta) 1 else 0)
                .atTime(p.silencioHasta, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - ahora
            WorkManager.getInstance(applicationContext).enqueueUniqueWork("redescubrimiento_horario", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<TrabajadorRedescubrimiento>().setInitialDelay(retraso.coerceAtLeast(60_000), TimeUnit.MILLISECONDS).build())
            return Result.success()
        }
        app.database.withTransaction {
            val previo = dao.metrica("ultimo_redescubrimiento") ?: 0
            if (ahora - previo < intervalo) return@withTransaction
            val idea = dao.ideaOlvidada(ahora - 7 * 86_400_000L, ahora - maxOf(intervalo, 14 * 86_400_000L)) ?: return@withTransaction
            val revision = dao.revisionIdea(idea.id) ?: RevisionIdea(idea.id)
            dao.guardarRevision(revision.copy(sugerida = ahora))
            dao.guardarMetrica(MetricaRpg("ultimo_redescubrimiento", ahora))
            publicarAviso(applicationContext, "redescubrimiento", 0, "¡Una idea del pasado ha reaparecido!", idea.title, "idea/${idea.id}")
        }
        return Result.success()
    }
}

class ReceptorSistema : BroadcastReceiver() {
    override fun onReceive(contexto: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        WorkManager.getInstance(contexto).enqueueUniqueWork("reconciliar", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<TrabajadorReconciliacion>().build())
    }
}

class TrabajadorReconciliacion(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        (applicationContext as MindropApplication).programador.reconciliar()
        return Result.success()
    }
}
