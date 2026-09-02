package com.mindrop.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.ui.folder.FolderBrowserScreen
import com.mindrop.app.ui.folder.FolderBrowserViewModel

private object Destination {
    const val FOLDERS = "folders"
}

@Composable
fun MindropNavHost(
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) {
    val navController = rememberNavController()
    val factory = remember(folderRepository, ideaRepository) {
        FolderBrowserViewModel.factory(
            folderRepository = folderRepository,
            ideaRepository = ideaRepository,
        )
    }

    NavHost(
        navController = navController,
        startDestination = Destination.FOLDERS,
    ) {
        composable(route = Destination.FOLDERS) {
            val viewModel: FolderBrowserViewModel = viewModel(factory = factory)
            FolderBrowserScreen(viewModel = viewModel)
        }
    }
}
