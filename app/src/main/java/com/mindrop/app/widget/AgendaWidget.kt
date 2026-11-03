package com.mindrop.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.mindrop.app.MainActivity
import com.mindrop.app.R

/** Acceso rápido: no consulta datos ni ejecuta trabajos periódicos en el escritorio. */
class AgendaWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, vistas(context)) }
    }

    companion object {
        const val DESTINO_NUEVO_EVENTO = "agenda/nuevo"

        internal fun vistas(context: Context): RemoteViews {
            val intent = Intent(context, MainActivity::class.java).apply {
                data = "mindrop://widget/agenda/nuevo".toUri()
                putExtra("destino_local", DESTINO_NUEVO_EVENTO)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val abrir = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.widget_agenda).apply {
                setOnClickPendingIntent(R.id.widget_nuevo_evento, abrir)
            }
        }
    }
}
