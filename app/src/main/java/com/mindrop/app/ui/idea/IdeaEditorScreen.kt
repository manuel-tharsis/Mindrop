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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import com.mindrop.app.ui.editor.EditorEvent
import com.mindrop.app.ui.editor.IdeaLocationKind
import com.mindrop.app.ui.editor.IdeaLocationOption
import com.mindrop.app.ui.editor.components.DiscardChangesDialog
import com.mindrop.app.ui.editor.components.CustomIconPicker
import com.mindrop.app.ui.editor.components.FormActions
import com.mindrop.app.ui.editor.components.IconPicker
import com.mindrop.app.ui.editor.components.IdeaLocationPicker
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun IdeaEditorRoute(
    viewModel: IdeaEditorViewModel,
    onFinished: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var suggestionToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var draftSuggestionToDelete by rememberSaveable { mutableStateOf<Int?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.importCustomIcon(uri)
    }

    fun requestExit() {
        if (
            uiState.isSaving ||
            uiState.isImportingIcon ||
            uiState.isAddingSuggestion ||
            uiState.validatingSuggestionId != null ||
            uiState.deletingSuggestionId != null
        ) return
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
        onValidateSuggestion = viewModel::validateSuggestion,
        onDeleteSuggestion = { suggestionId -> suggestionToDelete = suggestionId },
        onDeleteDraftSuggestion = { index -> draftSuggestionToDelete = index },
        onIconChange = viewModel::selectPresetIcon,
        onChooseCustomIcon = {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onUseDefaultIcon = { viewModel.selectPresetIcon(uiState.icon) },
        onLocationChange = viewModel::updateLocation,
        onSave = viewModel::save,
        onCancel = ::requestExit,
    )

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onFinished,
            onKeepEditing = { showDiscardDialog = false },
        )
    }

    val hasSuggestionDeleteRequest = suggestionToDelete != null || draftSuggestionToDelete != null
    if (hasSuggestionDeleteRequest) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                suggestionToDelete = null
                draftSuggestionToDelete = null
            },
            title = { Text(stringResource(R.string.delete_suggestion_title)) },
            text = { Text(stringResource(R.string.delete_suggestion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        suggestionToDelete?.let(viewModel::deleteSuggestion)
                        draftSuggestionToDelete?.let(viewModel::removeDraftSuggestion)
                        suggestionToDelete = null
                        draftSuggestionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        suggestionToDelete = null
                        draftSuggestionToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
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
    onValidateSuggestion: (Long) -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    onDeleteDraftSuggestion: (Int) -> Unit,
    onIconChange: (String) -> Unit,
    onChooseCustomIcon: () -> Unit,
    onUseDefaultIcon: () -> Unit,
    onLocationChange: (IdeaLocationOption?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formEnabled = !uiState.isSaving &&
        !uiState.isImportingIcon &&
        !uiState.isAddingSuggestion &&
        uiState.validatingSuggestionId == null &&
        uiState.deletingSuggestionId == null
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
                        pendingSuggestions = uiState.pendingSuggestions,
                        showError = uiState.suggestionError,
                        actionError = uiState.suggestionActionError,
                        isAdding = uiState.isAddingSuggestion,
                        validatingSuggestionId = uiState.validatingSuggestionId,
                        deletingSuggestionId = uiState.deletingSuggestionId,
                        enabled = formEnabled,
                        onTextChange = onSuggestionTextChange,
                        onAdd = onAddSuggestion,
                        onValidate = onValidateSuggestion,
                        onDelete = onDeleteSuggestion,
                        onDeleteDraft = onDeleteDraftSuggestion,
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
                    IdeaLocationPicker(
                        selectedFolderId = uiState.folderId,
                        selectedParentIdeaId = uiState.parentIdeaId,
                        options = uiState.locationOptions,
                        onLocationSelected = onLocationChange,
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
    pendingSuggestions: List<IdeaSuggestionEntity>,
    showError: Boolean,
    actionError: String?,
    isAdding: Boolean,
    validatingSuggestionId: Long?,
    deletingSuggestionId: Long?,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    onValidate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDeleteDraft: (Int) -> Unit,
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
            if (isAdding) {
                SuggestionActionProgress()
            } else {
                Text(stringResource(R.string.add_suggestion))
            }
        }
        actionError?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        pendingSuggestions.forEach { suggestion ->
            SuggestionEditorItem(
                text = suggestion.text,
                canValidate = true,
                isValidating = validatingSuggestionId == suggestion.id,
                isDeleting = deletingSuggestionId == suggestion.id,
                enabled = enabled,
                onValidate = { onValidate(suggestion.id) },
                onDelete = { onDelete(suggestion.id) },
            )
        }
        addedSuggestions.forEachIndexed { index, suggestion ->
            SuggestionEditorItem(
                text = suggestion,
                canValidate = false,
                isValidating = false,
                isDeleting = false,
                enabled = enabled,
                onValidate = {},
                onDelete = { onDeleteDraft(index) },
            )
        }
    }
}

@Composable
private fun SuggestionEditorItem(
    text: String,
    canValidate: Boolean,
    isValidating: Boolean,
    isDeleting: Boolean,
    enabled: Boolean,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canValidate) {
                    TextButton(onClick = onValidate, enabled = enabled) {
                        if (isValidating) {
                            SuggestionActionProgress()
                        } else {
                            Text(stringResource(R.string.validate_suggestion))
                        }
                    }
                }
                TextButton(
                    onClick = onDelete,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (isDeleting) {
                        SuggestionActionProgress()
                    } else {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionActionProgress() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
    )
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
                locationOptions = listOf(
                    IdeaLocationOption(
                        key = "folder:1",
                        name = "Android",
                        path = "Proyectos / Android",
                        depth = 1,
                        kind = IdeaLocationKind.Folder,
                        icon = "folder",
                        folderId = 1,
                        parentIdeaId = null,
                    ),
                ),
            ),
            onNameChange = {},
            onShortDescriptionChange = {},
            onFullDescriptionChange = {},
            onSuggestionTextChange = {},
            onAddSuggestion = {},
            onValidateSuggestion = {},
            onDeleteSuggestion = {},
            onDeleteDraftSuggestion = {},
            onIconChange = {},
            onChooseCustomIcon = {},
            onUseDefaultIcon = {},
            onLocationChange = {},
            onSave = {},
            onCancel = {},
        )
    }
}
