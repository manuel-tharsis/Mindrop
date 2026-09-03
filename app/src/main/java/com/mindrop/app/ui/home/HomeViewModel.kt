package com.mindrop.app.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.data.repository.FolderHierarchyException
import com.mindrop.app.data.repository.FolderNotEmptyException
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val folderId: Long? = null,
    val currentFolder: FolderEntity? = null,
    val breadcrumbs: List<FolderBreadcrumb> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val ideas: List<IdeaEntity> = emptyList(),
    val allFolders: List<FolderEntity> = emptyList(),
    val searchQuery: String = "",
    val hasAnyContent: Boolean = false,
    val isContentActionRunning: Boolean = false,
    val contentActionError: String? = null,
)

sealed interface HomeEvent {
    data object MoveCompleted : HomeEvent
    data object FolderDeleted : HomeEvent
}

private data class ContentActionState(
    val isRunning: Boolean = false,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val folderId: Long?,
    savedStateHandle: SavedStateHandle,
    private val folderRepository: FolderRepository,
    private val ideaRepository: IdeaRepository,
) : ViewModel() {
    private val searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")
    private val savedStateHandle = savedStateHandle
    private val contentActionState = MutableStateFlow(ContentActionState())
    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        folderRepository.observeChildSummaries(parentFolderId = folderId),
        ideaRepository.observeInFolder(folderId = folderId),
        folderRepository.observeAll(),
        searchQuery,
        contentActionState,
    ) { folders, ideas, allFolders, query, actionState ->
        val normalizedQuery = query.trim()
        val filteredIdeas = if (normalizedQuery.isEmpty()) {
            ideas
        } else {
            ideas.filter { idea ->
                idea.title.contains(normalizedQuery, ignoreCase = true) ||
                    idea.shortDescription.contains(normalizedQuery, ignoreCase = true)
            }
        }

        HomeUiState(
            folderId = folderId,
            currentFolder = folderId?.let { id -> allFolders.firstOrNull { it.id == id } },
            breadcrumbs = buildFolderBreadcrumbs(allFolders, folderId),
            folders = folders,
            ideas = filteredIdeas,
            allFolders = allFolders,
            searchQuery = query,
            hasAnyContent = folders.isNotEmpty() || ideas.isNotEmpty(),
            isContentActionRunning = actionState.isRunning,
            contentActionError = actionState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(folderId = folderId),
    )

    fun updateSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }

    fun clearContentActionError() {
        contentActionState.update { it.copy(errorMessage = null) }
    }

    fun moveIdea(ideaId: Long, destinationFolderId: Long?) {
        runContentAction(
            successEvent = HomeEvent.MoveCompleted,
            defaultError = "No se pudo mover la idea.",
        ) {
            ideaRepository.moveToFolder(ideaId, destinationFolderId)
        }
    }

    fun moveFolder(folderId: Long, destinationFolderId: Long?) {
        runContentAction(
            successEvent = HomeEvent.MoveCompleted,
            defaultError = "No se pudo mover la carpeta.",
        ) {
            folderRepository.moveToFolder(folderId, destinationFolderId)
        }
    }

    fun deleteFolder(folderId: Long) {
        runContentAction(
            successEvent = HomeEvent.FolderDeleted,
            defaultError = "No se pudo eliminar la carpeta.",
        ) {
            folderRepository.deleteById(folderId)
        }
    }

    private fun runContentAction(
        successEvent: HomeEvent,
        defaultError: String,
        action: suspend () -> Boolean,
    ) {
        if (contentActionState.value.isRunning) return
        viewModelScope.launch {
            contentActionState.value = ContentActionState(isRunning = true)
            try {
                check(action()) { "El elemento ya no existe." }
                contentActionState.value = ContentActionState()
                _events.emit(successEvent)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                contentActionState.value = ContentActionState(
                    errorMessage = when (error) {
                        is FolderNotEmptyException -> folderNotEmptyMessage(error)
                        is FolderHierarchyException -> error.message ?: defaultError
                        else -> defaultError
                    },
                )
            }
        }
    }

    private fun folderNotEmptyMessage(error: FolderNotEmptyException): String {
        val ideas = if (error.ideaCount == 1) "1 idea" else "${error.ideaCount} ideas"
        val folders = if (error.childFolderCount == 1) {
            "1 subcarpeta"
        } else {
            "${error.childFolderCount} subcarpetas"
        }
        return "La carpeta contiene $ideas y $folders. No se ha eliminado nada. " +
            "Mueve o elimina primero su contenido."
    }

    companion object {
        fun factory(
            folderId: Long?,
            folderRepository: FolderRepository,
            ideaRepository: IdeaRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    folderId = folderId,
                    savedStateHandle = createSavedStateHandle(),
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
        }

        private const val SEARCH_QUERY_KEY = "searchQuery"
    }
}
