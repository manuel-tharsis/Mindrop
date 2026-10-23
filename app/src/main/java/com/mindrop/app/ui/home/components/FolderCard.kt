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
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.mindrop.app.ui.rpg.movimientoPermitido
import com.mindrop.app.ui.rpg.DestelloPixel
import com.mindrop.app.ui.rpg.reboteRpg
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mindrop.app.ui.rpg.IconoPixel
import com.mindrop.app.ui.rpg.FormaPixel
import com.mindrop.app.ui.rpg.marcoRpg

@Composable
fun FolderCard(
    folderSummary: FolderSummary,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var abriendo by remember { mutableStateOf(false) }
    val ambito = rememberCoroutineScope()
    val movimiento = movimientoPermitido()
    val interacciones = remember { MutableInteractionSource() }
    ElevatedCard(
        interactionSource = interacciones,
        onClick = {
            if (!abriendo) ambito.launch {
                abriendo = true
                if (movimiento) delay(180)
                onClick()
                abriendo = false
            }
        },
        modifier = modifier.fillMaxWidth().reboteRpg(interacciones).marcoRpg(MaterialTheme.colorScheme.secondary),
        shape = FormaPixel,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                IconoPixel(tipo = "carpeta", abierto = abriendo)
                if (folderSummary.folder.icon.isNotBlank() && folderSummary.folder.icon != "folder") {
                    Icon(
                        painterResource(mindropIcon(folderSummary.folder.icon).drawableRes),
                        contentDescription = stringResource(R.string.folder_icon_content_description),
                        modifier = Modifier.size(16.dp).align(Alignment.BottomEnd),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (abriendo) DestelloPixel(folderSummary.folder.id)
            }
                ContentActionsMenu(onEditClick = onEditClick, onMoveClick = onMoveClick, onDeleteClick = onDeleteClick)
            }
            Column {
                Text(
                    text = folderSummary.folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
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
