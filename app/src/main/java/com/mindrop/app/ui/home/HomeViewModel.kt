package com.mindrop.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val folders: List<FolderSummary> = emptyList(),
    val ideas: List<IdeaEntity> = emptyList(),
    val searchQuery: String = "",
    val hasAnyContent: Boolean = false,
)

class HomeViewModel(
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        folderRepository.observeChildSummaries(parentFolderId = null),
        ideaRepository.observeInFolder(folderId = null),
        searchQuery,
    ) { folders, rootIdeas, query ->
        val normalizedQuery = query.trim()
        val filteredIdeas = if (normalizedQuery.isEmpty()) {
            rootIdeas
        } else {
            rootIdeas.filter { idea ->
                idea.title.contains(normalizedQuery, ignoreCase = true) ||
                    idea.shortDescription.contains(normalizedQuery, ignoreCase = true)
            }
        }

        HomeUiState(
            folders = folders,
            ideas = filteredIdeas,
            searchQuery = query,
            hasAnyContent = folders.isNotEmpty() || rootIdeas.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(),
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    companion object {
        fun factory(
            folderRepository: FolderRepository,
            ideaRepository: IdeaRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
        }
    }
}
