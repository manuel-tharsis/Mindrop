package com.mindrop.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.ui.folder.FolderEditorRoute
import com.mindrop.app.ui.folder.FolderEditorViewModel
import com.mindrop.app.ui.home.HomeRoute
import com.mindrop.app.ui.home.HomeViewModel
import com.mindrop.app.ui.idea.IdeaEditorRoute
import com.mindrop.app.ui.idea.IdeaEditorViewModel

private object Destination {
    const val HOME = "home"
    const val NEW_IDEA = "ideas/new"
    const val EDIT_IDEA = "ideas/{ideaId}"
    const val NEW_FOLDER = "folders/new"
    const val EDIT_FOLDER = "folders/{folderId}"

    fun editIdea(ideaId: Long) = "ideas/$ideaId"

    fun editFolder(folderId: Long) = "folders/$folderId"
}

@Composable
fun MindropNavHost(
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.HOME,
    ) {
        composable(route = Destination.HOME) {
            val factory = remember(folderRepository, ideaRepository) {
                HomeViewModel.factory(
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeRoute(
                viewModel = viewModel,
                onFolderClick = { folderId ->
                    navController.navigate(Destination.editFolder(folderId))
                },
                onIdeaClick = { ideaId ->
                    navController.navigate(Destination.editIdea(ideaId))
                },
                onCreateIdea = { navController.navigate(Destination.NEW_IDEA) },
                onCreateFolder = { navController.navigate(Destination.NEW_FOLDER) },
            )
        }
        composable(route = Destination.NEW_IDEA) {
            val factory = remember(folderRepository, ideaRepository) {
                IdeaEditorViewModel.factory(
                    ideaId = null,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
            val viewModel: IdeaEditorViewModel = viewModel(factory = factory)
            IdeaEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.EDIT_IDEA,
            arguments = listOf(navArgument("ideaId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val ideaId = backStackEntry.arguments?.getLong("ideaId") ?: return@composable
            val factory = remember(ideaId, folderRepository, ideaRepository) {
                IdeaEditorViewModel.factory(
                    ideaId = ideaId,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                )
            }
            val viewModel: IdeaEditorViewModel = viewModel(factory = factory)
            IdeaEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(route = Destination.NEW_FOLDER) {
            val factory = remember(folderRepository) {
                FolderEditorViewModel.factory(
                    folderId = null,
                    folderRepository = folderRepository,
                )
            }
            val viewModel: FolderEditorViewModel = viewModel(factory = factory)
            FolderEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.EDIT_FOLDER,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val factory = remember(folderId, folderRepository) {
                FolderEditorViewModel.factory(
                    folderId = folderId,
                    folderRepository = folderRepository,
                )
            }
            val viewModel: FolderEditorViewModel = viewModel(factory = factory)
            FolderEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
