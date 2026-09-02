package com.mindrop.app.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.repository.FolderHierarchyException
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.ui.editor.EditorEvent
import com.mindrop.app.ui.editor.FolderOption
import com.mindrop.app.ui.editor.buildFolderOptions
import com.mindrop.app.ui.editor.descendantFolderIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FolderEditorUiState(
    val isLoading: Boolean = true,
    val folderId: Long? = null,
    val name: String = "",
    val icon: String = "folder",
    val parentFolderId: Long? = null,
    val parentOptions: List<FolderOption> = emptyList(),
    val nameError: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

class FolderEditorViewModel(
    private val folderId: Long?,
    private val folderRepository: FolderRepository,
) : ViewModel() {
    private var storedFolder: FolderEntity? = null
    private val _uiState = MutableStateFlow(FolderEditorUiState(folderId = folderId))
    val uiState: StateFlow<FolderEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            folderRepository.observeAll().collect { folders ->
                val excludedIds = folderId?.let { currentId ->
                    descendantFolderIds(folders, currentId) + currentId
                }.orEmpty()
                _uiState.update { state ->
                    state.copy(
                        parentOptions = buildFolderOptions(
                            folders = folders,
                            excludedIds = excludedIds,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            loadFolder()
        }
    }

    fun updateName(value: String) = updateForm { copy(name = value, nameError = false) }

    fun updateIcon(value: String) = updateForm { copy(icon = value) }

    fun updateParent(parentFolderId: Long?) = updateForm { copy(parentFolderId = parentFolderId) }

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
                val existing = storedFolder
                if (folderId != null && existing == null) {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "No se encontró la carpeta.")
                    }
                    return@launch
                }
                if (existing == null) {
                    folderRepository.insert(
                        FolderEntity(
                            name = current.name.trim(),
                            icon = current.icon,
                            parentFolderId = current.parentFolderId,
                            sortOrder = 0,
                        ),
                    )
                } else {
                    val updated = folderRepository.update(
                        existing.copy(
                            name = current.name.trim(),
                            icon = current.icon,
                            parentFolderId = current.parentFolderId,
                        ),
                    )
                    check(updated) { "La carpeta ya no existe." }
                }

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _events.emit(EditorEvent.Saved)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (error is FolderHierarchyException) {
                            error.message
                        } else {
                            "No se pudo guardar la carpeta. Comprueba la carpeta padre."
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadFolder() {
        if (folderId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val folder = folderRepository.findById(folderId)
        storedFolder = folder
        _uiState.update { state ->
            if (folder == null) {
                state.copy(
                    isLoading = false,
                    errorMessage = "No se encontró la carpeta.",
                )
            } else {
                state.copy(
                    isLoading = false,
                    name = folder.name,
                    icon = folder.icon,
                    parentFolderId = folder.parentFolderId,
                    hasUnsavedChanges = false,
                )
            }
        }
    }

    private inline fun updateForm(
        transform: FolderEditorUiState.() -> FolderEditorUiState,
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
            folderId: Long?,
            folderRepository: FolderRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FolderEditorViewModel(
                    folderId = folderId,
                    folderRepository = folderRepository,
                )
            }
        }
    }
}
