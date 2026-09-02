package com.mindrop.app.ui.idea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.ui.editor.EditorEvent
import com.mindrop.app.ui.editor.FolderOption
import com.mindrop.app.ui.editor.buildFolderOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdeaEditorUiState(
    val isLoading: Boolean = true,
    val ideaId: Long? = null,
    val name: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val icon: String = "idea",
    val folderId: Long? = null,
    val folderOptions: List<FolderOption> = emptyList(),
    val nameError: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

class IdeaEditorViewModel(
    private val ideaId: Long?,
    initialFolderId: Long?,
    private val folderRepository: FolderRepository,
    private val ideaRepository: IdeaRepository,
) : ViewModel() {
    private var storedIdea: IdeaEntity? = null
    private val _uiState = MutableStateFlow(
        IdeaEditorUiState(ideaId = ideaId, folderId = initialFolderId),
    )
    val uiState: StateFlow<IdeaEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            folderRepository.observeAll().collect { folders ->
                _uiState.update { state ->
                    state.copy(folderOptions = buildFolderOptions(folders))
                }
            }
        }
        viewModelScope.launch {
            loadIdea()
        }
    }

    fun updateName(value: String) = updateForm { copy(name = value, nameError = false) }

    fun updateShortDescription(value: String) = updateForm { copy(shortDescription = value) }

    fun updateFullDescription(value: String) = updateForm { copy(fullDescription = value) }

    fun updateIcon(value: String) = updateForm { copy(icon = value) }

    fun updateFolder(folderId: Long?) = updateForm { copy(folderId = folderId) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = true, errorMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val current = _uiState.value
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
                            shortDescription = current.shortDescription.trim(),
                            fullDescription = current.fullDescription.trim(),
                            icon = current.icon,
                            customIconPath = null,
                            folderId = current.folderId,
                            sortOrder = 0,
                        ),
                    )
                } else {
                    val updated = ideaRepository.update(
                        existing.copy(
                            title = current.name.trim(),
                            shortDescription = current.shortDescription.trim(),
                            fullDescription = current.fullDescription.trim(),
                            icon = current.icon,
                            folderId = current.folderId,
                        ),
                    )
                    check(updated) { "La idea ya no existe." }
                }

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _events.emit(EditorEvent.Saved)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No se pudo guardar la idea. Comprueba la ubicación.",
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
                    folderId = idea.folderId,
                    hasUnsavedChanges = false,
                )
            }
        }
    }

    private inline fun updateForm(
        transform: IdeaEditorUiState.() -> IdeaEditorUiState,
    ) {
        _uiState.update { state ->
            state.transform().copy(
                hasUnsavedChanges = true,
                errorMessage = null,
            )
        }
    }

    companion object {
        fun factory(
            ideaId: Long?,
            initialFolderId: Long? = null,
            folderRepository: FolderRepository,
            ideaRepository: IdeaRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                IdeaEditorViewModel(
                    ideaId = ideaId,
                    initialFolderId = initialFolderId,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
        }
    }
}
