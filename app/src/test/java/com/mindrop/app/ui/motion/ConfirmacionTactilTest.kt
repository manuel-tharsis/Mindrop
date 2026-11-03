package com.mindrop.app.ui.motion

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConfirmacionTactilTest {
    private class Vista(context: Context) : View(context) {
        var conectada = true
        var enfocada = true
        val respuestas = mutableListOf<Int>()
        override fun isAttachedToWindow() = conectada
        override fun hasWindowFocus() = enfocada
        override fun performHapticFeedback(tipo: Int): Boolean { respuestas += tipo; return true }
    }

    @Test fun confirmacionEnVersionReciente() {
        val vista = Vista(ApplicationProvider.getApplicationContext())
        assertTrue(confirmarAccion(vista))
        assertEquals(listOf(HapticFeedbackConstants.CONFIRM), vista.respuestas)
    }

    @Test
    @Config(sdk = [23])
    fun android6UsaUnaRespuestaCompatible() {
        val vista = Vista(ApplicationProvider.getApplicationContext())
        assertTrue(confirmarAccion(vista))
        assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY), vista.respuestas)
    }

    @Test fun noSeConfirmaFueraDeLaVentanaActivaOConFeedbackDesactivado() {
        val vista = Vista(ApplicationProvider.getApplicationContext())
        vista.conectada = false; assertFalse(confirmarAccion(vista))
        vista.conectada = true; vista.enfocada = false; assertFalse(confirmarAccion(vista))
        vista.enfocada = true; vista.isHapticFeedbackEnabled = false; assertFalse(confirmarAccion(vista))
        assertTrue(vista.respuestas.isEmpty())
    }
}
