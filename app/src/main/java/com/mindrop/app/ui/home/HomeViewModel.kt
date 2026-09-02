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
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val folderId: Long? = null,
    val currentFolder: FolderEntity? = null,
    val breadcrumbs: List<FolderBreadcrumb> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val ideas: List<IdeaEntity> = emptyList(),
    val searchQuery: String = "",
    val hasAnyContent: Boolean = false,
)

class HomeViewModel(
    private val folderId: Long?,
    savedStateHandle: SavedStateHandle,
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) : ViewModel() {
    private val searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")
    private val savedStateHandle = savedStateHandle

    val uiState: StateFlow<HomeUiState> = combine(
        folderRepository.observeChildSummaries(parentFolderId = folderId),
        ideaRepository.observeInFolder(folderId = folderId),
        folderRepository.observeAll(),
        searchQuery,
    ) { folders, ideas, allFolders, query ->
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
            searchQuery = query,
            hasAnyContent = folders.isNotEmpty() || ideas.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(folderId = folderId),
    )

    fun updateSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
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
