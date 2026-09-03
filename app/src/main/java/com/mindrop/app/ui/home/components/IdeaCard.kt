package com.mindrop.app.ui.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.icons.CustomIconLoadState
import com.mindrop.app.ui.icons.mindropIcon
import com.mindrop.app.ui.icons.rememberCustomIcon
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun IdeaCard(
    idea: IdeaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaIcon(idea = idea)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (idea.shortDescription.isNotBlank()) {
                    Text(
                        text = idea.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun IdeaIcon(
    idea: IdeaEntity,
    modifier: Modifier = Modifier,
) {
    val customIconState by rememberCustomIcon(idea.customIconPath)

    val iconAppearance = iconAppearance(idea.icon)
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(iconAppearance.background),
        contentAlignment = Alignment.Center,
    ) {
        if (customIconState is CustomIconLoadState.Available) {
            Image(
                bitmap = (customIconState as CustomIconLoadState.Available).bitmap,
                contentDescription = stringResource(R.string.idea_icon_content_description),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconAppearance.drawableRes),
                    contentDescription = stringResource(R.string.idea_icon_content_description),
                    modifier = Modifier.size(30.dp),
                    tint = iconAppearance.tint,
                )
            }
        }
    }
}

private data class IconAppearance(
    @param:DrawableRes val drawableRes: Int,
    val tint: Color,
    val background: Color,
)

private fun iconAppearance(icon: String): IconAppearance {
    val selectedIcon = mindropIcon(icon)
    return when (selectedIcon.key) {
        "code", "mobile", "computer", "brain" -> IconAppearance(
            drawableRes = selectedIcon.drawableRes,
            tint = Color(0xFF6D4CC7),
            background = Color(0xFFEDE5FF),
        )
        "terminal", "tools", "work", "document" -> IconAppearance(
            drawableRes = selectedIcon.drawableRes,
            tint = Color(0xFF3E6670),
            background = Color(0xFFDDEEF1),
        )
        else -> IconAppearance(
            drawableRes = selectedIcon.drawableRes,
            tint = Color(0xFF2F6F91),
            background = Color(0xFFDCEEF7),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFBF8)
@Composable
private fun IdeaCardPreview() {
    MindropTheme {
        IdeaCard(
            idea = IdeaEntity(
                title = "Monitor de batería Android",
                shortDescription = "App para controlar el estado y salud de la batería",
                fullDescription = "",
                icon = "android",
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
