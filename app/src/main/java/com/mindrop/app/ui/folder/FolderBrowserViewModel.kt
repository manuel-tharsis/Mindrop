package com.mindrop.app.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.repository.FolderContents
import com.mindrop.app.data.repository.MindropRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FolderBrowserViewModel(
    repository: MindropRepository,
) : ViewModel() {
    val contents: StateFlow<FolderContents> = repository
        .observeFolderContents(folderId = null)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FolderContents(),
        )

    companion object {
        fun factory(repository: MindropRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FolderBrowserViewModel(repository = repository)
            }
        }
    }
}
