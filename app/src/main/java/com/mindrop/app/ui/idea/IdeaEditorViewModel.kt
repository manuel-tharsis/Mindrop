package com.mindrop.app.ui.idea

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import com.mindrop.app.data.icon.CustomIconRepository
import com.mindrop.app.data.icon.InvalidCustomIconException
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaHierarchyException
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.IdeaSuggestionRepository
import com.mindrop.app.ui.editor.EditorEvent
import com.mindrop.app.ui.editor.IdeaLocationOption
import com.mindrop.app.ui.editor.buildIdeaLocationOptions
import com.mindrop.app.ui.editor.descendantIdeaIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IdeaEditorUiState(
    val isLoading: Boolean = true,
    val ideaId: Long? = null,
    val name: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val newSuggestionText: String = "",
    val newSuggestions: List<String> = emptyList(),
    val pendingSuggestions: List<IdeaSuggestionEntity> = emptyList(),
    val suggestionError: Boolean = false,
    val suggestionActionError: String? = null,
    val icon: String = "idea",
    val customIconPath: String? = null,
    val folderId: Long? = null,
    val parentIdeaId: Long? = null,
    val locationOptions: List<IdeaLocationOption> = emptyList(),
    val nameError: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isImportingIcon: Boolean = false,
    val isAddingSuggestion: Boolean = false,
    val validatingSuggestionId: Long? = null,
    val deletingSuggestionId: Long? = null,
    val hasIdeaChanges: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

class IdeaEditorViewModel(
    private val ideaId: Long?,
    initialFolderId: Long?,
    private val folderRepository: FolderRepository,
    private val ideaRepository: IdeaRepository,
    private val suggestionRepository: IdeaSuggestionRepository,
    private val customIconRepository: CustomIconRepository,
) : ViewModel() {
    private var storedIdea: IdeaEntity? = null
    private var pendingCustomIconPath: String? = null
    private val _uiState = MutableStateFlow(
        IdeaEditorUiState(ideaId = ideaId, folderId = initialFolderId),
    )
    val uiState: StateFlow<IdeaEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EditorEvent>(capacity = Channel.BUFFERED)
    val events: Flow<EditorEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                folderRepository.observeAll(),
                ideaRepository.observeAll(),
            ) { folders, ideas -> folders to ideas }.collect { (folders, ideas) ->
                val excludedIds = ideaId?.let { currentIdeaId ->
                    descendantIdeaIds(ideas, currentIdeaId) + currentIdeaId
                }.orEmpty()
                _uiState.update { state ->
                    state.copy(
                        locationOptions = buildIdeaLocationOptions(
                            folders = folders,
                            ideas = ideas,
                            excludedIdeaIds = excludedIds,
                        ),
                    )
                }
            }
        }
        if (ideaId != null) {
            viewModelScope.launch {
                suggestionRepository.observePending(ideaId).collect { suggestions ->
                    _uiState.update { it.copy(pendingSuggestions = suggestions) }
                }
            }
        }
        viewModelScope.launch {
            loadIdea()
        }
    }

    fun updateName(value: String) = updateIdeaForm { copy(name = value, nameError = false) }

    fun updateShortDescription(value: String) = updateIdeaForm { copy(shortDescription = value) }

    fun updateFullDescription(value: String) = updateIdeaForm { copy(fullDescription = value) }

    fun updateNewSuggestion(value: String) {
        if (isBusy()) return
        _uiState.update { state ->
            state.copy(
                newSuggestionText = value,
                suggestionError = false,
                suggestionActionError = null,
                hasUnsavedChanges = state.hasIdeaChanges ||
                    value.isNotBlank() ||
                    state.newSuggestions.isNotEmpty(),
            )
        }
    }

    fun addSuggestion() {
        if (isBusy()) return
        val state = _uiState.value
        val text = state.newSuggestionText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(suggestionError = true) }
            return
        }

        if (ideaId == null) {
            _uiState.update {
                it.copy(
                    newSuggestionText = "",
                    newSuggestions = it.newSuggestions + text,
                    suggestionError = false,
                    suggestionActionError = null,
                    hasUnsavedChanges = true,
                )
            }
            return
        }

        _uiState.update {
            it.copy(isAddingSuggestion = true, suggestionActionError = null)
        }
        viewModelScope.launch {
            try {
                suggestionRepository.addPending(ideaId, text)
                _uiState.update {
                    it.copy(
                        newSuggestionText = "",
                        suggestionError = false,
                        isAddingSuggestion = false,
                        hasUnsavedChanges = it.hasIdeaChanges,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isAddingSuggestion = false,
                        suggestionActionError = "No se pudo añadir la sugerencia.",
                    )
                }
            }
        }
    }

    fun validateSuggestion(suggestionId: Long) {
        val state = _uiState.value
        if (ideaId == null || isBusy()) return
        if (state.pendingSuggestions.none { it.id == suggestionId }) return

        _uiState.update {
            it.copy(validatingSuggestionId = suggestionId, suggestionActionError = null)
        }
        viewModelScope.launch {
            try {
                check(suggestionRepository.validate(ideaId, suggestionId) != null) {
                    "La sugerencia ya no existe."
                }
                _uiState.update { current ->
                    current.copy(
                        pendingSuggestions = current.pendingSuggestions.filterNot {
                            it.id == suggestionId
                        },
                        validatingSuggestionId = null,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        validatingSuggestionId = null,
                        suggestionActionError = "No se pudo validar la sugerencia.",
                    )
                }
            }
        }
    }

    fun deleteSuggestion(suggestionId: Long) {
        val state = _uiState.value
        if (ideaId == null || isBusy()) return
        if (state.pendingSuggestions.none { it.id == suggestionId }) return

        _uiState.update {
            it.copy(deletingSuggestionId = suggestionId, suggestionActionError = null)
        }
        viewModelScope.launch {
            try {
                check(suggestionRepository.deletePending(ideaId, suggestionId)) {
                    "La sugerencia ya no existe."
                }
                _uiState.update { current ->
                    current.copy(
                        pendingSuggestions = current.pendingSuggestions.filterNot {
                            it.id == suggestionId
                        },
                        deletingSuggestionId = null,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        deletingSuggestionId = null,
                        suggestionActionError = "No se pudo eliminar la sugerencia.",
                    )
                }
            }
        }
    }

    fun removeDraftSuggestion(index: Int) {
        if (ideaId != null || isBusy()) return
        _uiState.update { state ->
            if (index !in state.newSuggestions.indices) return@update state
            val remainingSuggestions = state.newSuggestions.toMutableList().apply {
                removeAt(index)
            }
            state.copy(
                newSuggestions = remainingSuggestions,
                hasUnsavedChanges = state.hasIdeaChanges ||
                    state.newSuggestionText.isNotBlank() ||
                    remainingSuggestions.isNotEmpty(),
            )
        }
    }

    fun selectPresetIcon(value: String) {
        if (isBusy()) return
        pendingCustomIconPath?.let { path ->
            viewModelScope.launch(Dispatchers.IO) {
                customIconRepository.delete(path)
            }
        }
        pendingCustomIconPath = null
        updateIdeaForm { copy(icon = value, customIconPath = null) }
    }

    fun importCustomIcon(uri: Uri) {
        if (isBusy()) return
        _uiState.update { it.copy(isImportingIcon = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val importedPath = customIconRepository.importImage(uri)
                pendingCustomIconPath?.let { path ->
                    withContext(Dispatchers.IO) {
                        customIconRepository.delete(path)
                    }
                }
                pendingCustomIconPath = importedPath
                _uiState.update {
                    it.copy(
                        customIconPath = importedPath,
                        isImportingIcon = false,
                        hasIdeaChanges = true,
                        hasUnsavedChanges = true,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isImportingIcon = false,
                        errorMessage = if (error is InvalidCustomIconException) {
                            error.message
                        } else {
                            "No se pudo usar la imagen seleccionada."
                        },
                    )
                }
            }
        }
    }

    fun updateLocation(option: IdeaLocationOption?) = updateIdeaForm {
        copy(
            folderId = option?.folderId,
            parentIdeaId = option?.parentIdeaId,
        )
    }

    fun save() {
        val state = _uiState.value
        if (isBusy()) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = true, errorMessage = null) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val current = state
                val suggestions = current.newSuggestions + listOfNotNull(
                    current.newSuggestionText.trim().takeIf(String::isNotEmpty),
                )
                val existing = storedIdea
                if (ideaId != null && existing == null) {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "No se encontró la idea.")
                    }
                    return@launch
                }
                if (existing == null) {
                    ideaRepository.insert(
                        IdeaEntity(
                            title = current.name.trim(),
                            shortDescription = current.shortDescription,
                            fullDescription = current.fullDescription,
                            icon = current.icon,
                            customIconPath = current.customIconPath,
                            folderId = current.folderId,
                            parentIdeaId = current.parentIdeaId,
                            sortOrder = 0,
                        ),
                        pendingSuggestionTexts = suggestions,
                    )
                } else {
                    val updated = ideaRepository.update(
                        existing.copy(
                            title = current.name.trim(),
                            shortDescription = current.shortDescription,
                            fullDescription = current.fullDescription,
                            icon = current.icon,
                            customIconPath = current.customIconPath,
                            folderId = current.folderId,
                            parentIdeaId = current.parentIdeaId,
                        ),
                        pendingSuggestionTexts = suggestions,
                    )
                    check(updated) { "La idea ya no existe." }
                }

                pendingCustomIconPath = null
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        newSuggestionText = "",
                        newSuggestions = emptyList(),
                        hasIdeaChanges = false,
                        hasUnsavedChanges = false,
                    )
                }
                eventChannel.send(EditorEvent.Saved)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (error is IdeaHierarchyException) {
                            error.message
                        } else {
                            "No se pudo guardar la idea. Comprueba la ubicación."
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadIdea() {
        if (ideaId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val idea = ideaRepository.findById(ideaId)
        storedIdea = idea
        _uiState.update { state ->
            if (idea == null) {
                state.copy(
                    isLoading = false,
                    errorMessage = "No se encontró la idea.",
                )
            } else {
                state.copy(
                    isLoading = false,
                    name = idea.title,
                    shortDescription = idea.shortDescription,
                    fullDescription = idea.fullDescription,
                    icon = idea.icon,
                    customIconPath = idea.customIconPath,
                    folderId = idea.folderId,
                    parentIdeaId = idea.parentIdeaId,
                    hasIdeaChanges = false,
                    hasUnsavedChanges = false,
                )
            }
        }
    }

    private inline fun updateIdeaForm(
        transform: IdeaEditorUiState.() -> IdeaEditorUiState,
    ) {
        if (isBusy()) return
        _uiState.update { state ->
            state.transform().copy(
                hasIdeaChanges = true,
                hasUnsavedChanges = true,
                errorMessage = null,
            )
        }
    }

    private fun isBusy(): Boolean = _uiState.value.let { state ->
        state.isSaving ||
            state.isImportingIcon ||
            state.isAddingSuggestion ||
            state.validatingSuggestionId != null ||
            state.deletingSuggestionId != null
    }

    override fun onCleared() {
        pendingCustomIconPath?.let(customIconRepository::delete)
        pendingCustomIconPath = null
        super.onCleared()
    }

    companion object {
        fun factory(
            ideaId: Long?,
            initialFolderId: Long? = null,
            folderRepository: FolderRepository,
            ideaRepository: IdeaRepository,
            suggestionRepository: IdeaSuggestionRepository,
            customIconRepository: CustomIconRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                IdeaEditorViewModel(
                    ideaId = ideaId,
                    initialFolderId = initialFolderId,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                    suggestionRepository = suggestionRepository,
                    customIconRepository = customIconRepository,
                )
            }
        }
    }
}
