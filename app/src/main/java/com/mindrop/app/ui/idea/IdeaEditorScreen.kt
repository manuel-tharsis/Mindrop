package com.mindrop.app.ui.idea

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
fun IdeaEditorRoute(
    viewModel: IdeaEditorViewModel,
    onFinished: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun requestExit() {
        if (uiState.hasUnsavedChanges) showDiscardDialog = true else onFinished()
    }

    BackHandler(onBack = ::requestExit)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == EditorEvent.Saved) onFinished()
        }
    }

    IdeaEditorScreen(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onShortDescriptionChange = viewModel::updateShortDescription,
        onFullDescriptionChange = viewModel::updateFullDescription,
        onIconChange = viewModel::updateIcon,
        onFolderChange = viewModel::updateFolder,
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
fun IdeaEditorScreen(
    uiState: IdeaEditorUiState,
    onNameChange: (String) -> Unit,
    onShortDescriptionChange: (String) -> Unit,
    onFullDescriptionChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onFolderChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortDescriptionFocus = remember { FocusRequester() }
    val fullDescriptionFocus = remember { FocusRequester() }
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
                            if (uiState.ideaId == null) R.string.new_idea else R.string.edit_idea,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { shortDescriptionFocus.requestFocus() },
                        ),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.shortDescription,
                        onValueChange = onShortDescriptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(shortDescriptionFocus),
                        label = { Text(stringResource(R.string.short_description_label)) },
                        minLines = 2,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { fullDescriptionFocus.requestFocus() },
                        ),
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.fullDescription,
                        onValueChange = onFullDescriptionChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fullDescriptionFocus),
                        label = { Text(stringResource(R.string.full_description_label)) },
                        minLines = 5,
                    )
                }
                item {
                    IconPicker(
                        selectedIcon = uiState.icon,
                        onIconSelected = onIconChange,
                    )
                }
                item {
                    FolderPicker(
                        label = stringResource(R.string.location_label),
                        selectedFolderId = uiState.folderId,
                        options = uiState.folderOptions,
                        onFolderSelected = onFolderChange,
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

@Preview(name = "Nueva idea", showBackground = true, widthDp = 400, heightDp = 820)
@Preview(
    name = "Editar idea oscuro",
    showBackground = true,
    widthDp = 400,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IdeaEditorPreview() {
    MindropTheme {
        IdeaEditorScreen(
            uiState = IdeaEditorUiState(
                isLoading = false,
                name = "App para el coche",
                shortDescription = "Registro de revisiones y averías",
                folderOptions = listOf(FolderOption(1, "Proyectos / Android")),
            ),
            onNameChange = {},
            onShortDescriptionChange = {},
            onFullDescriptionChange = {},
            onIconChange = {},
            onFolderChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}
