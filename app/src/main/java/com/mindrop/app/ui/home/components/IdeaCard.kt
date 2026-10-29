package com.mindrop.app.ui.home.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.icons.MindropIdeaIcon
import com.mindrop.app.ui.theme.MindropTheme
import com.mindrop.app.ui.rpg.FormaPixel
import com.mindrop.app.ui.rpg.marcoRpg
import com.mindrop.app.ui.rpg.reboteRpg
import com.mindrop.app.ui.idea.BanderaPrioridad
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun IdeaCard(
    idea: IdeaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveClick: (() -> Unit)? = null,
    hierarchyDepth: Int = 0,
    onToggleImportant: (() -> Unit)? = null,
) {
    val depth = hierarchyDepth.coerceAtLeast(0)
    val indentation = (depth.coerceAtMost(8) * 18).dp
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    val interacciones = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (depth > 0) {
                    val endX = indentation.toPx() - 4.dp.toPx()
                    val lineX = indentation.toPx() - 10.dp.toPx()
                    drawLine(
                        color = connectorColor,
                        start = androidx.compose.ui.geometry.Offset(lineX, 0f),
                        end = androidx.compose.ui.geometry.Offset(lineX, size.height / 2f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = connectorColor,
                        start = androidx.compose.ui.geometry.Offset(lineX, size.height / 2f),
                        end = androidx.compose.ui.geometry.Offset(endX, size.height / 2f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
    ) {
        ElevatedCard(
            interactionSource = interacciones,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indentation)
                .reboteRpg(interacciones)
                .marcoRpg(if (idea.esImportante) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                    if (idea.esImportante) .8f else 0f),
            shape = FormaPixel,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MindropIdeaIcon(
                    icon = idea.icon,
                    customIconPath = idea.customIconPath,
                    contentDescription = stringResource(R.string.idea_icon_content_description),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = idea.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
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
                BanderaPrioridad(idea.esImportante, cambiar = { onToggleImportant?.invoke() }, habilitada = onToggleImportant != null)
                if (onMoveClick != null) {
                    ContentActionsMenu(onMoveClick = onMoveClick)
                }
            }
        }
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
            onMoveClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
