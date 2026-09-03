package com.mindrop.app.ui.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.editor.FolderOption
import com.mindrop.app.ui.icons.CustomIconLoadState
import com.mindrop.app.ui.icons.mindropIcon
import com.mindrop.app.ui.icons.mindropIconOptions
import com.mindrop.app.ui.icons.rememberCustomIcon

@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hasCustomIcon: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.icon_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            mindropIconOptions.forEach { option ->
                FilterChip(
                    selected = !hasCustomIcon && mindropIcon(selectedIcon).key == option.key,
                    onClick = { onIconSelected(option.key) },
                    label = { Text(stringResource(option.labelRes)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(option.drawableRes),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun CustomIconPicker(
    customIconPath: String?,
    isImporting: Boolean,
    onChooseImage: () -> Unit,
    onUseDefaultIcon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconState by rememberCustomIcon(customIconPath)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.custom_icon_label),
            style = MaterialTheme.typography.labelLarge,
        )
        when (val state = iconState) {
            is CustomIconLoadState.Available -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = stringResource(R.string.custom_icon_preview),
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Crop,
                )
            }
            CustomIconLoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
            )
            CustomIconLoadState.Unavailable -> Text(
                text = stringResource(R.string.custom_icon_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            CustomIconLoadState.None -> Unit
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = onChooseImage,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    stringResource(
                        if (customIconPath == null) R.string.choose_image else R.string.change_image,
                    ),
                )
            }
            if (customIconPath != null) {
                TextButton(
                    onClick = onUseDefaultIcon,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.use_default_icon))
                }
            }
        }
    }
}

@Composable
fun FolderPicker(
    label: String,
    selectedFolderId: Long?,
    options: List<FolderOption>,
    onFolderSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedFolderId }?.label
        ?: stringResource(R.string.root_location)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedLabel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.wrapContentSize(),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.root_location)) },
                    onClick = {
                        onFolderSelected(null)
                        expanded = false
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onFolderSelected(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun FormActions(
    isSaving: Boolean,
    errorMessage: String?,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(stringResource(R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(stringResource(R.string.keep_editing))
            }
        },
    )
}
