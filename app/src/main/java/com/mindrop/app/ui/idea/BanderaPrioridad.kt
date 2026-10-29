package com.mindrop.app.ui.idea

import com.mindrop.app.ui.icons.PackIcon as Icon

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import androidx.compose.ui.res.stringResource
import com.mindrop.app.R

@Composable
fun BanderaPrioridad(importante: Boolean, cambiar: () -> Unit, habilitada: Boolean = true) {
    IconToggleButton(checked = importante, onCheckedChange = { cambiar() }, enabled = habilitada) {
        Icon(painterResource(R.drawable.ic_flag),
            contentDescription = stringResource(if (importante) R.string.quitar_importancia else R.string.marcar_importancia),
            tint = if (importante) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
