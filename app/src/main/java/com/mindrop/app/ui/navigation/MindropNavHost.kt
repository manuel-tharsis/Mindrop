package com.mindrop.app.ui.navigation

import com.mindrop.app.ui.icons.PackIcon as Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.mindrop.app.ui.icons.packPainterResource as painterResource
import com.mindrop.app.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mindrop.app.MindropApplication
import com.mindrop.app.ui.agenda.*
import com.mindrop.app.ui.rpg.*
import com.mindrop.app.ui.idea.DialogoRecordatorio
import com.mindrop.app.ui.theme.MindropTheme
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
import com.mindrop.app.ui.proyectos.*
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.ui.busqueda.*
import com.mindrop.app.ui.copias.*

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
    app: MindropApplication,
    destinoNotificacion: String? = null,
    consumirNotificacion: () -> Unit = {},
) {
    val navController = rememberNavController()
    val rpg: RpgViewModel = viewModel(factory = remember(app) { fabricaRpg { RpgViewModel(app) } })
    val preferencias by rpg.preferencias.collectAsStateWithLifecycle()
    val copiasVm: CopiasViewModel = viewModel(factory = remember(app) { fabricaRpg { CopiasViewModel(app) } })
    val estadoCopias by copiasVm.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(estadoCopias.restauraciones, estadoCopias.trabajando) {
        if (!estadoCopias.trabajando && estadoCopias.restauraciones > copiasVm.ultimaRestauracionMostrada) {
            copiasVm.ultimaRestauracionMostrada = estadoCopias.restauraciones
            listOf("home", "proyectos", "agenda", "logros", "ajustes").forEach { navController.clearBackStack(it) }
            navController.navigate("home") { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
            avisos.showSnackbar(estadoCopias.mensaje ?: "Copia restaurada.")
        }
    }
    val pendientes by app.borrados.pendientes.collectAsStateWithLifecycle()
    val errorBorrado by app.borrados.error.collectAsStateWithLifecycle()
    LaunchedEffect(pendientes.firstOrNull()?.clave) {
        pendientes.firstOrNull()?.let { pendiente ->
            val resultado = avisos.showSnackbar(pendiente.mensaje, "Deshacer", withDismissAction = true, duration = SnackbarDuration.Long)
            app.borrados.resolver(pendiente.clave, resultado == SnackbarResult.ActionPerformed)
        }
    }
    LaunchedEffect(errorBorrado) {
        errorBorrado?.let { avisos.showSnackbar(it); app.borrados.error.value = null }
    }
    val entrada by navController.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route.orEmpty()
    val principal = ruta in listOf("home", "proyectos", "agenda", "logros", "ajustes", "completed", "browse/{folderId}")
    LaunchedEffect(destinoNotificacion) {
        destinoNotificacion?.let { destino ->
            when {
                destino == "agenda" -> navController.navigate("agenda") { launchSingleTop = true }
                destino.startsWith("idea/") -> destino.substringAfter("idea/").toLongOrNull()?.takeIf { it > 0 }?.let {
                    navController.navigate(Destination.ideaDetail(it)) { launchSingleTop = true }
                }
            }
            consumirNotificacion()
        }
    }
    MindropTheme(tema = preferencias.tema) {
    CompositionLocalProvider(LocalMovimiento provides !preferencias.reducirMovimiento,
        LocalEfecto provides preferencias.efecto, LocalDiseno provides preferencias.diseno) {
    Surface(color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().fondoRpg(preferencias.tema, MaterialTheme.colorScheme.primary)) {
    Box(Modifier.weight(1f)) {

    NavHost(
        navController = navController,
        startDestination = Destination.HOME,
    ) {
        composable("buscar") {
            val vm: BusquedaViewModel = viewModel(factory = remember(app) { viewModelFactory {
                initializer { BusquedaViewModel(app.folderRepository, app.ideaRepository, app.proyectos, createSavedStateHandle()) }
            } })
            BusquedaScreen(vm, volver = { navController.popBackStack() },
                abrirIdea = { navController.navigate(Destination.ideaDetail(it)) },
                abrirPagina = { proyecto, pagina -> navController.navigate("proyectos/$proyecto/cuaderno?pagina=$pagina") })
        }
        composable("proyectos") {
            val vm: ProyectosViewModel = viewModel(factory = remember(app) { fabricaRpg { ProyectosViewModel(app.proyectos) } })
            val exportar: ExportarPdfViewModel = viewModel(factory = remember(app) { viewModelFactory {
                initializer { ExportarPdfViewModel(app, createSavedStateHandle()) }
            } })
            ProyectosRoute(vm, exportar) { id -> navController.navigate("proyectos/$id/cuaderno") }
        }
        composable("proyectos/{proyectoId}/cuaderno?pagina={pagina}", arguments = listOf(
            navArgument("proyectoId") { type = NavType.LongType },
            navArgument("pagina") { type = NavType.LongType; defaultValue = -1L },
        )) { entradaProyecto ->
            val id = entradaProyecto.arguments?.getLong("proyectoId") ?: return@composable
            val factory = remember(id, app) { viewModelFactory {
                initializer { CuadernoViewModel(id, app.proyectos, createSavedStateHandle()) }
            } }
            val vm: CuadernoViewModel = viewModel(factory = factory)
            CuadernoRoute(vm, preferencias) { navController.popBackStack() }
        }
        composable("agenda") {
            val ideas by rpg.ideas.collectAsStateWithLifecycle()
            val vm: AgendaViewModel = viewModel(factory = remember(app) { fabricaRpg { AgendaViewModel(app.agenda) } })
            AgendaScreen(vm, ideas) { navController.navigate(Destination.ideaDetail(it)) }
        }
        composable("logros") { PantallaLogros(rpg) }
        composable("ajustes") { PantallaAjustes(rpg) { OpcionesCopias(copiasVm) } }
        composable("ideas/{ideaId}/recordatorio", arguments = listOf(navArgument("ideaId") { type = NavType.LongType })) { entradaRecordatorio ->
            val id = entradaRecordatorio.arguments?.getLong("ideaId") ?: return@composable
            DialogoRecordatorio(id, app, salir = { navController.popBackStack() })
        }
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
                onRecordatorio = { navController.navigate("ideas/$ideaId/recordatorio") },
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
    SnackbarHost(avisos, Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp)
        .then(if (principal) Modifier else Modifier.navigationBarsPadding()))
    if (principal) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
        listOf("home" to "Inicio", "proyectos" to "Proyectos", "agenda" to "Agenda", "logros" to "Logros", "ajustes" to "Ajustes").forEachIndexed { indice, (destino, nombre) ->
            NavigationBarItem(selected = if (destino == "home") ruta in listOf("home", "completed", "browse/{folderId}") else ruta == destino,
                onClick = { navController.navigate(destino) {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                } }, icon = { Icon(painterResource(listOf(R.drawable.ic_idea, R.drawable.ic_project,
                    R.drawable.ic_calendar, R.drawable.ic_trophy, R.drawable.ic_settings)[indice]), contentDescription = null,
                    modifier = Modifier.size(24.dp)) }, label = { Text(nombre, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium) })
        }
    }
    }
    if (!estadoCopias.trabajando) CelebracionLogro(rpg)
    OperacionCopiaEnCurso(estadoCopias.trabajando)
    }
    }
    }
}

fun <T : ViewModel> fabricaRpg(crear: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = crear() as V
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
        onGlobalSearch = { navController.navigate("buscar") },
    )
}
