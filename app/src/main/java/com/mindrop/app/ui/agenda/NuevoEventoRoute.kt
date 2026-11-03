package com.mindrop.app.ui.agenda

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.data.local.entity.EventoAgenda
import com.mindrop.app.data.local.entity.IdeaEntity
import java.time.LocalDateTime

internal const val FECHA_EVENTO_GUARDADO = "fecha_evento_guardado"

/** La próxima hora completa es futura incluso si se abre el widget cerca de medianoche. */
internal fun nuevoEventoConRecordatorio(ahora: LocalDateTime = LocalDateTime.now()): EventoAgenda {
    val momento = ahora.plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return EventoAgenda(titulo = "", fecha = momento.toLocalDate().toString(),
        minutoDelDia = momento.hour * 60, minutosAntes = 0)
}

@Composable
internal fun NuevoEventoRoute(
    vm: AgendaViewModel,
    ideas: List<IdeaEntity>,
    onGuardado: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    val fechaInicial = rememberSaveable { LocalDateTime.now().toString() }
    val inicial = remember(fechaInicial) { nuevoEventoConRecordatorio(LocalDateTime.parse(fechaInicial)) }
    val error by vm.error.collectAsStateWithLifecycle()
    val guardando by vm.guardando.collectAsStateWithLifecycle()
    val alGuardar by rememberUpdatedState(onGuardado)
    LaunchedEffect(vm) { vm.guardados.collect { fecha -> fecha?.let(alGuardar) } }
    EditorEvento(0, null, inicial.fecha, ideas, error, guardando,
        guardar = vm::guardar, cancelar = onCancelar, eventoInicial = inicial)
}
