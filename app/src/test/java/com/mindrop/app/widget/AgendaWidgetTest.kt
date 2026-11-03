package com.mindrop.app.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaWidgetTest {
    @Test fun pulsarAbreDirectamenteUnEventoSinPasarPorUnReceptorIntermedio() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = AgendaWidget.vistas(context).apply(context, FrameLayout(context))
        assertTrue(view.findViewById<android.view.View>(R.id.widget_nuevo_evento).performClick())
        val intent = shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals(ComponentName(context, EventoRapidoActivity::class.java), intent.component)
        assertNull(intent.getStringExtra("destino_local"))
        assertEquals("mindrop://widget/agenda/nuevo", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun widgetSeDeclaraSinActualizacionPeriodicaNiPermisosNuevos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = context.packageManager.getReceiverInfo(ComponentName(context, AgendaWidget::class.java),
            android.content.pm.PackageManager.GET_META_DATA)
        assertFalse(receiver.exported)
        val xml = receiver.loadXmlMetaData(context.packageManager, "android.appwidget.provider")
        xml.use {
            while (it.next() != org.xmlpull.v1.XmlPullParser.START_TAG) { }
            assertEquals("appwidget-provider", it.name)
            assertEquals(0, it.getAttributeIntValue("http://schemas.android.com/apk/res/android", "updatePeriodMillis", -1))
        }
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(permissions.contains("android.permission.WRITE_CALENDAR"))
        assertFalse(permissions.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertFalse(permissions.contains("android.permission.SYSTEM_ALERT_WINDOW"))
    }
}
