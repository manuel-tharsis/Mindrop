package com.mindrop.app.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CompletedIdeasUiState(
    val ideas: List<IdeaEntity> = emptyList(),
    val searchQuery: String = "",
    val hasCompletedIdeas: Boolean = false,
)

class CompletedIdeasViewModel(
    savedStateHandle: SavedStateHandle,
    ideaRepository: IdeaRepository,
) : ViewModel() {
    private val searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")
    private val savedStateHandle = savedStateHandle

    val uiState: StateFlow<CompletedIdeasUiState> = combine(
        ideaRepository.observeCompleted(),
        searchQuery,
    ) { completedIdeas, query ->
        val normalizedQuery = query.trim()
        val filteredIdeas = if (normalizedQuery.isEmpty()) {
            completedIdeas
        } else {
            completedIdeas.filter { idea ->
                idea.title.contains(normalizedQuery, ignoreCase = true) ||
                    idea.shortDescription.contains(normalizedQuery, ignoreCase = true)
            }
        }

        CompletedIdeasUiState(
            ideas = filteredIdeas,
            searchQuery = query,
            hasCompletedIdeas = completedIdeas.isNotEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CompletedIdeasUiState(),
    )

    fun updateSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }

    companion object {
        fun factory(ideaRepository: IdeaRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CompletedIdeasViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        ideaRepository = ideaRepository,
                    )
                }
            }

        private const val SEARCH_QUERY_KEY = "completedSearchQuery"
    }
}
