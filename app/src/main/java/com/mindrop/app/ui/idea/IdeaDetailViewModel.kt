package com.mindrop.app.ui.idea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.IdeaRepository
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
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface IdeaDetailEvent {
    data object Deleted : IdeaDetailEvent
}

class IdeaDetailViewModel(
    private val ideaId: Long,
    private val ideaRepository: IdeaRepository,
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
    }

    fun delete() {
        if (_uiState.value.isDeleting || _uiState.value.idea == null) return
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
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                IdeaDetailViewModel(
                    ideaId = ideaId,
                    ideaRepository = ideaRepository,
                )
            }
        }
    }
}
