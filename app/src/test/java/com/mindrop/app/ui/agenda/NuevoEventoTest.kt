package com.mindrop.app.ui.agenda

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class NuevoEventoTest {
    @Test fun widgetProponeUnaHoraFuturaConAviso() {
        val evento = nuevoEventoConRecordatorio(LocalDateTime.parse("2026-11-03T14:37:12"))
        assertEquals("2026-11-03", evento.fecha)
        assertEquals(15 * 60, evento.minutoDelDia)
        assertEquals(0, evento.minutosAntes)
        assertEquals("", evento.titulo)
        assertNull(evento.ideaId)
    }

    @Test fun cercaDeMedianocheProponeElDiaSiguiente() {
        val evento = nuevoEventoConRecordatorio(LocalDateTime.parse("2026-12-31T23:59:59"))
        assertEquals("2027-01-01", evento.fecha)
        assertEquals(0, evento.minutoDelDia)
        assertEquals(0, evento.minutosAntes)
    }

    @Test fun enUnaHoraExactaNoProponeUnAvisoYaVencido() {
        assertEquals(600, nuevoEventoConRecordatorio(LocalDateTime.parse("2026-11-03T09:00:00")).minutoDelDia)
    }
}
