package com.mindrop.app.ui.idea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.IdeaSuggestionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdeaDetailUiState(
    val isLoading: Boolean = true,
    val idea: IdeaEntity? = null,
    val updates: List<IdeaSuggestionEntity> = emptyList(),
    val pendingSuggestions: List<IdeaSuggestionEntity> = emptyList(),
    val validatingSuggestionId: Long? = null,
    val deletingSuggestionId: Long? = null,
    val validationErrorMessage: String? = null,
    val isUpdatingCompletion: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface IdeaDetailEvent {
    data object Deleted : IdeaDetailEvent
}

class IdeaDetailViewModel(
    private val ideaId: Long,
    private val ideaRepository: IdeaRepository,
    private val suggestionRepository: IdeaSuggestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeaDetailUiState())
    val uiState: StateFlow<IdeaDetailUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<IdeaDetailEvent>(capacity = Channel.BUFFERED)
    val events: Flow<IdeaDetailEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            ideaRepository.observeById(ideaId).collect { idea ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        idea = idea,
                        errorMessage = if (idea == null && !state.isDeleting) {
                            "No se encontró la idea."
                        } else {
                            state.errorMessage
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            suggestionRepository.observeUpdates(ideaId).collect { updates ->
                _uiState.update { it.copy(updates = updates) }
            }
        }
        viewModelScope.launch {
            suggestionRepository.observePending(ideaId).collect { suggestions ->
                _uiState.update { it.copy(pendingSuggestions = suggestions) }
            }
        }
    }

    fun validateSuggestion(suggestionId: Long) {
        val state = _uiState.value
        if (
            state.isDeleting ||
            state.isUpdatingCompletion ||
            state.deletingSuggestionId != null ||
            state.validatingSuggestionId != null
        ) return
        if (state.pendingSuggestions.none { it.id == suggestionId }) return

        _uiState.update {
            it.copy(validatingSuggestionId = suggestionId, validationErrorMessage = null)
        }
        viewModelScope.launch {
            try {
                val validated = suggestionRepository.validate(ideaId, suggestionId)
                    ?: error("La sugerencia ya no existe.")
                _uiState.update { current ->
                    current.copy(
                        updates = (current.updates + validated)
                            .distinctBy(IdeaSuggestionEntity::id)
                            .sortedBy { it.updateNumber },
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
                        validationErrorMessage = "No se pudo validar la sugerencia.",
                    )
                }
            }
        }
    }

    fun deleteSuggestion(suggestionId: Long) {
        val state = _uiState.value
        if (
            state.isDeleting ||
            state.isUpdatingCompletion ||
            state.validatingSuggestionId != null ||
            state.deletingSuggestionId != null
        ) return
        if (state.pendingSuggestions.none { it.id == suggestionId }) return

        _uiState.update {
            it.copy(deletingSuggestionId = suggestionId, validationErrorMessage = null)
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
                        validationErrorMessage = "No se pudo eliminar la sugerencia.",
                    )
                }
            }
        }
    }

    fun toggleCompleted() {
        val state = _uiState.value
        val idea = state.idea ?: return
        if (
            state.isDeleting ||
            state.isUpdatingCompletion ||
            state.validatingSuggestionId != null ||
            state.deletingSuggestionId != null
        ) return

        _uiState.update { it.copy(isUpdatingCompletion = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                check(ideaRepository.setCompleted(ideaId, !idea.isCompleted)) {
                    "La idea ya no existe."
                }
                _uiState.update { it.copy(isUpdatingCompletion = false) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isUpdatingCompletion = false,
                        errorMessage = "No se pudo cambiar el estado de la idea.",
                    )
                }
            }
        }
    }

    fun delete() {
        if (
            _uiState.value.isDeleting ||
            _uiState.value.isUpdatingCompletion ||
            _uiState.value.validatingSuggestionId != null ||
            _uiState.value.deletingSuggestionId != null ||
            _uiState.value.idea == null
        ) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                check(ideaRepository.deleteById(ideaId)) { "La idea ya no existe." }
                eventChannel.send(IdeaDetailEvent.Deleted)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = "No se pudo eliminar la idea.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            ideaId: Long,
            ideaRepository: IdeaRepository,
            suggestionRepository: IdeaSuggestionRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                IdeaDetailViewModel(
                    ideaId = ideaId,
                    ideaRepository = ideaRepository,
                    suggestionRepository = suggestionRepository,
                )
            }
        }
    }
}
