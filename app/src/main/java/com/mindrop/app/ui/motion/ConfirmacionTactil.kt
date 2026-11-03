package com.mindrop.app.ui.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

internal fun confirmarAccion(view: View): Boolean {
    if (!view.isAttachedToWindow || !view.hasWindowFocus() || !view.isHapticFeedbackEnabled) return false
    // performHapticFeedback respeta los ajustes del sistema; no usa VIBRATE ni fuerza la respuesta.
    return view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY)
}
