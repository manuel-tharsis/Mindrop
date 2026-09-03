package com.mindrop.app.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.editor.FolderOption

@Composable
fun FolderDestinationDialog(
    itemName: String,
    currentFolderId: Long?,
    options: List<FolderOption>,
    isMoving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit,
) {
    var selectedFolderId by rememberSaveable(itemName, currentFolderId) {
        mutableStateOf(currentFolderId)
    }

    AlertDialog(
        onDismissRequest = { if (!isMoving) onDismiss() },
        title = { Text(stringResource(R.string.move_item_title, itemName)) },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                ) {
                    item(key = "root") {
                        DestinationRow(
                            label = stringResource(R.string.root_location),
                            selected = selectedFolderId == null,
                            iconRes = R.drawable.ic_home,
                            onClick = { selectedFolderId = null },
                        )
                    }
                    items(options, key = FolderOption::id) { option ->
                        DestinationRow(
                            label = option.label,
                            selected = selectedFolderId == option.id,
                            iconRes = R.drawable.ic_folder,
                            onClick = { selectedFolderId = option.id },
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMove(selectedFolderId) },
                enabled = !isMoving && selectedFolderId != currentFolderId,
            ) {
                if (isMoving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }
                Text(stringResource(R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMoving) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun DestinationRow(
    label: String,
    selected: Boolean,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.padding(end = 10.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DeleteFolderDialog(
    folderName: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text(stringResource(R.string.delete_folder_title, folderName)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_folder_message))
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
