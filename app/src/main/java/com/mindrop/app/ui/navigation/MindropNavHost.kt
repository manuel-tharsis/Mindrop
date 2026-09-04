package com.mindrop.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mindrop.app.data.icon.CustomIconRepository
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.IdeaSuggestionRepository
import com.mindrop.app.ui.folder.FolderEditorRoute
import com.mindrop.app.ui.folder.FolderEditorViewModel
import com.mindrop.app.ui.home.CompletedIdeasRoute
import com.mindrop.app.ui.home.CompletedIdeasViewModel
import com.mindrop.app.ui.home.HomeRoute
import com.mindrop.app.ui.home.HomeViewModel
import com.mindrop.app.ui.idea.IdeaDetailRoute
import com.mindrop.app.ui.idea.IdeaDetailViewModel
import com.mindrop.app.ui.idea.IdeaEditorRoute
import com.mindrop.app.ui.idea.IdeaEditorViewModel

private const val ROOT_FOLDER_ID = -1L

private object Destination {
    const val HOME = "home"
    const val COMPLETED = "completed"
    const val BROWSE_FOLDER = "browse/{folderId}"
    const val NEW_IDEA = "ideas/new?folderId={folderId}"
    const val IDEA_DETAIL = "ideas/{ideaId}"
    const val EDIT_IDEA = "ideas/{ideaId}/edit"
    const val NEW_FOLDER = "folders/new?parentFolderId={parentFolderId}"
    const val EDIT_FOLDER = "folders/{folderId}/edit"

    fun browseFolder(folderId: Long) = "browse/$folderId"

    fun newIdea(folderId: Long?) = "ideas/new?folderId=${folderId ?: ROOT_FOLDER_ID}"

    fun ideaDetail(ideaId: Long) = "ideas/$ideaId"

    fun editIdea(ideaId: Long) = "ideas/$ideaId/edit"

    fun newFolder(parentFolderId: Long?) =
        "folders/new?parentFolderId=${parentFolderId ?: ROOT_FOLDER_ID}"

    fun editFolder(folderId: Long) = "folders/$folderId/edit"
}

@Composable
fun MindropNavHost(
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
    ideaSuggestionRepository: IdeaSuggestionRepository,
    customIconRepository: CustomIconRepository,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.HOME,
    ) {
        composable(route = Destination.HOME) {
            FolderBrowserDestination(
                folderId = null,
                navController = navController,
                folderRepository = folderRepository,
                ideaRepository = ideaRepository,
            )
        }
        composable(route = Destination.COMPLETED) {
            val factory = remember(ideaRepository) {
                CompletedIdeasViewModel.factory(ideaRepository)
            }
            val viewModel: CompletedIdeasViewModel = viewModel(factory = factory)
            CompletedIdeasRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onIdeaClick = { ideaId ->
                    navController.navigate(Destination.ideaDetail(ideaId))
                },
            )
        }
        composable(
            route = Destination.BROWSE_FOLDER,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            FolderBrowserDestination(
                folderId = folderId,
                navController = navController,
                folderRepository = folderRepository,
                ideaRepository = ideaRepository,
            )
        }
        composable(
            route = Destination.NEW_IDEA,
            arguments = listOf(
                navArgument("folderId") {
                    type = NavType.LongType
                    defaultValue = ROOT_FOLDER_ID
                },
            ),
        ) { backStackEntry ->
            val initialFolderId = backStackEntry.arguments
                ?.getLong("folderId")
                ?.takeUnless { it == ROOT_FOLDER_ID }
            val factory = remember(
                initialFolderId,
                folderRepository,
                ideaRepository,
                ideaSuggestionRepository,
                customIconRepository,
            ) {
                IdeaEditorViewModel.factory(
                    ideaId = null,
                    initialFolderId = initialFolderId,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                    suggestionRepository = ideaSuggestionRepository,
                    customIconRepository = customIconRepository,
                )
            }
            val viewModel: IdeaEditorViewModel = viewModel(factory = factory)
            IdeaEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.IDEA_DETAIL,
            arguments = listOf(navArgument("ideaId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val ideaId = backStackEntry.arguments?.getLong("ideaId") ?: return@composable
            val factory = remember(ideaId, ideaRepository, ideaSuggestionRepository) {
                IdeaDetailViewModel.factory(
                    ideaId = ideaId,
                    ideaRepository = ideaRepository,
                    suggestionRepository = ideaSuggestionRepository,
                )
            }
            val viewModel: IdeaDetailViewModel = viewModel(factory = factory)
            IdeaDetailRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { editedIdeaId ->
                    navController.navigate(Destination.editIdea(editedIdeaId))
                },
            )
        }
        composable(
            route = Destination.EDIT_IDEA,
            arguments = listOf(navArgument("ideaId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val ideaId = backStackEntry.arguments?.getLong("ideaId") ?: return@composable
            val factory = remember(
                ideaId,
                folderRepository,
                ideaRepository,
                ideaSuggestionRepository,
                customIconRepository,
            ) {
                IdeaEditorViewModel.factory(
                    ideaId = ideaId,
                    folderRepository = folderRepository,
                    ideaRepository = ideaRepository,
                    suggestionRepository = ideaSuggestionRepository,
                    customIconRepository = customIconRepository,
                )
            }
            val viewModel: IdeaEditorViewModel = viewModel(factory = factory)
            IdeaEditorRoute(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.NEW_FOLDER,
            arguments = listOf(
                navArgument("parentFolderId") {
                    type = NavType.LongType
                    defaultValue = ROOT_FOLDER_ID
                },
            ),
        ) { backStackEntry ->
            val initialParentFolderId = backStackEntry.arguments
                ?.getLong("parentFolderId")
                ?.takeUnless { it == ROOT_FOLDER_ID }
            val factory = remember(initialParentFolderId, folderRepository) {
                FolderEditorViewModel.factory(
                    folderId = null,
                    initialParentFolderId = initialParentFolderId,
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

@Composable
private fun FolderBrowserDestination(
    folderId: Long?,
    navController: NavHostController,
    folderRepository: FolderRepository,
    ideaRepository: IdeaRepository,
) {
    val factory = remember(folderId, folderRepository, ideaRepository) {
        HomeViewModel.factory(
            folderId = folderId,
            folderRepository = folderRepository,
            ideaRepository = ideaRepository,
        )
    }
    val viewModel: HomeViewModel = viewModel(factory = factory)

    HomeRoute(
        viewModel = viewModel,
        onFolderClick = { childFolderId ->
            navController.navigate(Destination.browseFolder(childFolderId))
        },
        onIdeaClick = { ideaId ->
            navController.navigate(Destination.ideaDetail(ideaId))
        },
        onEditFolder = { editedFolderId ->
            navController.navigate(Destination.editFolder(editedFolderId))
        },
        onBack = { navController.popBackStack() },
        onBreadcrumbClick = { targetFolderId ->
            val targetRoute = targetFolderId?.let(Destination::browseFolder) ?: Destination.HOME
            if (!navController.popBackStack(targetRoute, inclusive = false)) {
                navController.navigate(targetRoute)
            }
        },
        onCreateIdea = {
            navController.navigate(Destination.newIdea(folderId))
        },
        onCreateFolder = {
            navController.navigate(Destination.newFolder(folderId))
        },
        onCompletedClick = {
            navController.navigate(Destination.COMPLETED)
        },
    )
}
