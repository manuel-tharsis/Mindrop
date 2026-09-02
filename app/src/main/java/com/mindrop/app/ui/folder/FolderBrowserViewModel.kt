package com.mindrop.app.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class FolderContents(
    val folders: List<FolderEntity> = emptyList(),
    val ideas: List<IdeaEntity> = emptyList(),
)

class FolderBrowserViewModel(
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) : ViewModel() {
    val contents: StateFlow<FolderContents> = combine(
        folderRepository.observeChildren(parentFolderId = null),
        ideaRepository.observeInFolder(folderId = null),
    ) { folders, ideas ->
        FolderContents(folders = folders, ideas = ideas)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FolderContents(),
        )

    companion object {
        fun factory(
            folderRepository: FolderRepository,
            ideaRepository: IdeaRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FolderBrowserViewModel(
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
        }
    }
}
