package com.mindrop.app.ui.idea

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.mindrop.app.ui.editor.components.CustomIconPicker
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
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.importCustomIcon(uri)
    }

    fun requestExit() {
        if (uiState.isSaving || uiState.isImportingIcon) return
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
        onSuggestionTextChange = viewModel::updateNewSuggestion,
        onAddSuggestion = viewModel::addSuggestion,
        onIconChange = viewModel::selectPresetIcon,
        onChooseCustomIcon = {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onUseDefaultIcon = { viewModel.selectPresetIcon(uiState.icon) },
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
    onSuggestionTextChange: (String) -> Unit,
    onAddSuggestion: () -> Unit,
    onIconChange: (String) -> Unit,
    onChooseCustomIcon: () -> Unit,
    onUseDefaultIcon: () -> Unit,
    onFolderChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formEnabled = !uiState.isSaving && !uiState.isImportingIcon
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
                        enabled = formEnabled,
                        shape = RoundedCornerShape(16.dp),
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
                        enabled = formEnabled,
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
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
                            .heightIn(min = 144.dp, max = 320.dp)
                            .focusRequester(fullDescriptionFocus),
                        label = { Text(stringResource(R.string.full_description_label)) },
                        enabled = formEnabled,
                        minLines = 5,
                        shape = RoundedCornerShape(16.dp),
                    )
                }
                item {
                    SuggestionEditorSection(
                        text = uiState.newSuggestionText,
                        addedSuggestions = uiState.newSuggestions,
                        showError = uiState.suggestionError,
                        enabled = formEnabled,
                        onTextChange = onSuggestionTextChange,
                        onAdd = onAddSuggestion,
                    )
                }
                item {
                    IconPicker(
                        selectedIcon = uiState.icon,
                        hasCustomIcon = uiState.customIconPath != null,
                        onIconSelected = onIconChange,
                        enabled = formEnabled,
                    )
                }
                item {
                    CustomIconPicker(
                        customIconPath = uiState.customIconPath,
                        isImporting = uiState.isImportingIcon,
                        onChooseImage = onChooseCustomIcon,
                        onUseDefaultIcon = onUseDefaultIcon,
                        enabled = formEnabled,
                    )
                }
                item {
                    FolderPicker(
                        label = stringResource(R.string.location_label),
                        selectedFolderId = uiState.folderId,
                        options = uiState.folderOptions,
                        onFolderSelected = onFolderChange,
                        enabled = formEnabled,
                    )
                }
                item {
                    FormActions(
                        isSaving = uiState.isSaving || uiState.isImportingIcon,
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

@Composable
private fun SuggestionEditorSection(
    text: String,
    addedSuggestions: List<String>,
    showError: Boolean,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.suggestions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.new_suggestion_label)) },
            supportingText = if (showError) {
                { Text(stringResource(R.string.suggestion_required)) }
            } else {
                null
            },
            isError = showError,
            enabled = enabled,
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
        )
        Button(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.End),
            enabled = enabled && text.isNotBlank(),
        ) {
            Text(stringResource(R.string.add_suggestion))
        }
        addedSuggestions.forEach { suggestion ->
            Text(
                text = "• $suggestion",
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                newSuggestionText = "Añadir recordatorios de mantenimiento",
                newSuggestions = listOf("Mostrar el historial por fecha"),
                folderOptions = listOf(FolderOption(1, "Proyectos / Android")),
            ),
            onNameChange = {},
            onShortDescriptionChange = {},
            onFullDescriptionChange = {},
            onSuggestionTextChange = {},
            onAddSuggestion = {},
            onIconChange = {},
            onChooseCustomIcon = {},
            onUseDefaultIcon = {},
            onFolderChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}
