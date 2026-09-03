package com.mindrop.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MindropIdeaIcon(
    icon: String,
    customIconPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val customIconState by rememberCustomIcon(customIconPath)
    val appearance = iconAppearance(icon)
    val cornerSize = size * 0.28f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerSize))
            .background(appearance.background),
        contentAlignment = Alignment.Center,
    ) {
        if (customIconState is CustomIconLoadState.Available) {
            Image(
                bitmap = (customIconState as CustomIconLoadState.Available).bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(appearance.drawableRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.54f),
                tint = appearance.tint,
            )
        }
    }
}

private data class IconAppearance(
    @param:DrawableRes val drawableRes: Int,
    val tint: Color,
    val background: Color,
)

@Composable
private fun iconAppearance(icon: String): IconAppearance {
    val selectedIcon = mindropIcon(icon)
    return when (selectedIcon.key) {
        "idea", "brain", "game", "star" -> IconAppearance(
            drawableRes = selectedIcon.drawableRes,
            tint = MaterialTheme.colorScheme.tertiary,
            background = MaterialTheme.colorScheme.tertiaryContainer,
        )
        else -> IconAppearance(
            drawableRes = selectedIcon.drawableRes,
            tint = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}
