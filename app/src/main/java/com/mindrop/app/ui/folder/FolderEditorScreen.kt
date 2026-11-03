package com.mindrop.app.ui.folder

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.ui.editor.EditorEvent
import com.mindrop.app.ui.editor.FolderOption
import com.mindrop.app.ui.editor.components.DiscardChangesDialog
import com.mindrop.app.ui.editor.components.FolderPicker
import com.mindrop.app.ui.editor.components.FormActions
import com.mindrop.app.ui.editor.components.IconPicker
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun FolderEditorRoute(
    viewModel: FolderEditorViewModel,
    onFinished: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun requestExit() {
        if (uiState.isSaving) return
        if (uiState.hasUnsavedChanges) showDiscardDialog = true else onFinished()
    }

    BackHandler(onBack = ::requestExit)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == EditorEvent.Saved) onFinished()
        }
    }

    FolderEditorScreen(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onIconChange = viewModel::updateIcon,
        onParentChange = viewModel::updateParent,
        onSave = viewModel::save,
        onCancel = ::requestExit,
    )

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onFinished,
            onKeepEditing = { showDiscardDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderEditorScreen(
    uiState: FolderEditorUiState,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onParentChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formEnabled = !uiState.isSaving
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val finishKeyboardInput = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.folderId == null) R.string.new_folder else R.string.edit_folder,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            finishKeyboardInput()
                            onCancel()
                        },
                        enabled = formEnabled,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.name_label)) },
                        supportingText = if (uiState.nameError) {
                            { Text(stringResource(R.string.name_required)) }
                        } else {
                            null
                        },
                        isError = uiState.nameError,
                        enabled = formEnabled,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { finishKeyboardInput() }),
                    )
                }
                item {
                    IconPicker(
                        selectedIcon = uiState.icon,
                        onIconSelected = onIconChange,
                        enabled = formEnabled,
                    )
                }
                item {
                    FolderPicker(
                        label = stringResource(R.string.parent_folder_label),
                        selectedFolderId = uiState.parentFolderId,
                        options = uiState.parentOptions,
                        onFolderSelected = onParentChange,
                        enabled = formEnabled,
                    )
                }
                item {
                    FormActions(
                        isSaving = uiState.isSaving,
                        errorMessage = uiState.errorMessage,
                        onSave = {
                            finishKeyboardInput()
                            onSave()
                        },
                        onCancel = {
                            finishKeyboardInput()
                            onCancel()
                        },
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Nueva carpeta", showBackground = true, widthDp = 400, heightDp = 820)
@Preview(
    name = "Editar carpeta oscuro",
    showBackground = true,
    widthDp = 400,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FolderEditorPreview() {
    MindropTheme {
        FolderEditorScreen(
            uiState = FolderEditorUiState(
                isLoading = false,
                name = "Android",
                parentOptions = listOf(
                    FolderOption(1, "Programación"),
                    FolderOption(2, "Programación / Proyectos"),
                ),
            ),
            onNameChange = {},
            onIconChange = {},
            onParentChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}
