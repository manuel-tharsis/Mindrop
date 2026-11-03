package com.mindrop.app.shortcuts

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.mindrop.app.MainActivity
import com.mindrop.app.R

object AccesosRapidos {
    const val NUEVA_IDEA = "ideas/nueva"
    const val NUEVO_EVENTO = "agenda/nuevo"

    fun registrar(contexto: Context) {
        if (Build.VERSION.SDK_INT >= 25) {
            // Un lanzador que no admita accesos no debe impedir abrir Mindrop.
            try { Api25.registrar(contexto) }
            catch (_: IllegalStateException) { }
            catch (_: SecurityException) { }
        }
    }

    internal fun intent(contexto: Context, destino: String) = Intent(contexto, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = "mindrop://acceso/$destino".toUri()
        putExtra("destino_local", destino)
        // Los accesos estáticos fuerzan CLEAR_TASK. Los dinámicos conservan el borrador y la pila.
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    @RequiresApi(25)
    private object Api25 {
        fun registrar(contexto: Context) {
            val gestor = contexto.getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
            if (gestor.isRateLimitingActive) return
            val idea = android.content.pm.ShortcutInfo.Builder(contexto, "nueva_idea")
                .setShortLabel("Nueva idea").setLongLabel("Crear una idea")
                .setIcon(android.graphics.drawable.Icon.createWithResource(contexto, R.drawable.pack_basic_idea))
                .setIntent(intent(contexto, NUEVA_IDEA)).setRank(0).build()
            val evento = android.content.pm.ShortcutInfo.Builder(contexto, "nuevo_evento")
                .setShortLabel("Nuevo evento").setLongLabel("Crear un evento con recordatorio")
                .setIcon(android.graphics.drawable.Icon.createWithResource(contexto, R.drawable.pack_basic_calendar))
                .setIntent(intent(contexto, NUEVO_EVENTO)).setRank(1).build()
            gestor.dynamicShortcuts = listOf(idea, evento)
        }
    }
}
