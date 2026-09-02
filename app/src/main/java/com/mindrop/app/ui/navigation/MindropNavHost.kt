package com.mindrop.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mindrop.app.data.repository.MindropRepository
import com.mindrop.app.ui.folder.FolderBrowserScreen
import com.mindrop.app.ui.folder.FolderBrowserViewModel

private object Destination {
    const val FOLDERS = "folders"
}

@Composable
fun MindropNavHost(
    repository: MindropRepository,
) {
    val navController = rememberNavController()
    val factory = remember(repository) {
        FolderBrowserViewModel.factory(repository = repository)
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
