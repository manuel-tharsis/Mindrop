package com.mindrop.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.ui.icons.mindropIcon
import com.mindrop.app.ui.theme.MindropTheme

private val FolderYellow = Color(0xFFFFB629)
private val FolderBackground = Color(0xFFFFF3D5)

@Composable
fun FolderCard(
    folderSummary: FolderSummary,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIcon = mindropIcon(folderSummary.folder.icon)
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FolderBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(selectedIcon.drawableRes),
                    contentDescription = stringResource(R.string.folder_icon_content_description),
                    modifier = Modifier.size(38.dp),
                    tint = FolderYellow,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderSummary.folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.item_count,
                        folderSummary.itemCount,
                        folderSummary.itemCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            ContentActionsMenu(
                onEditClick = onEditClick,
                onMoveClick = onMoveClick,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFBF8)
@Composable
private fun FolderCardPreview() {
    MindropTheme {
        FolderCard(
            folderSummary = FolderSummary(
                folder = FolderEntity(name = "Programación", icon = "folder"),
                ideaCount = 7,
                childFolderCount = 1,
            ),
            onClick = {},
            onEditClick = {},
            onMoveClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
