package com.mindrop.app.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25])
class AccesosRapidosTest {
    @Test fun registraDosAccesosSinDuplicarlosNiVaciarLaPila() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repeat(2) { AccesosRapidos.registrar(context) }
        val accesos = context.getSystemService(ShortcutManager::class.java).dynamicShortcuts
        assertEquals(setOf("nueva_idea", "nuevo_evento"), accesos.map { it.id }.toSet())
        assertEquals(2, accesos.size)
        accesos.forEach {
            val intent = it.intent!!
            assertEquals(MainActivity::class.java.name, intent.component!!.className)
            assertEquals(context.packageName, intent.component!!.packageName)
            assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        }
        assertEquals(setOf(AccesosRapidos.NUEVA_IDEA, AccesosRapidos.NUEVO_EVENTO),
            accesos.map { it.intent!!.getStringExtra("destino_local") }.toSet())
    }

    @Test
    @Config(sdk = [23])
    fun android6ConservaElInicioNormalSinCargarApisDeAccesos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AccesosRapidos.registrar(context)
        val intent = AccesosRapidos.intent(context, AccesosRapidos.NUEVA_IDEA)
        assertEquals(AccesosRapidos.NUEVA_IDEA, intent.getStringExtra("destino_local"))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
