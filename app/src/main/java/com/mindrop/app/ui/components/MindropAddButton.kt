package com.mindrop.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.icons.PackIcon
import com.mindrop.app.ui.icons.packPainterResource

@Composable
fun MindropAddButton(description: String, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, shape = CircleShape, containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)) {
        PackIcon(packPainterResource(R.drawable.ic_add), description, Modifier.size(56.dp))
    }
}
