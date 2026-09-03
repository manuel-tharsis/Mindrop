package com.mindrop.app.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.ui.editor.buildFolderOptions
import com.mindrop.app.ui.editor.descendantFolderIds
import com.mindrop.app.ui.home.components.DeleteFolderDialog
import com.mindrop.app.ui.home.components.CompletedIdeasCard
import com.mindrop.app.ui.home.components.EmptyState
import com.mindrop.app.ui.home.components.FolderDestinationDialog
import com.mindrop.app.ui.home.components.FolderCard
import com.mindrop.app.ui.home.components.IdeaCard
import com.mindrop.app.ui.theme.MindropTheme

private sealed interface MoveRequest {
    val id: Long
    val name: String
    val currentFolderId: Long?

    data class Idea(
        override val id: Long,
        override val name: String,
        override val currentFolderId: Long?,
    ) : MoveRequest

    data class Folder(
        override val id: Long,
        override val name: String,
        override val currentFolderId: Long?,
    ) : MoveRequest
}

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onFolderClick: (Long) -> Unit,
    onIdeaClick: (Long) -> Unit,
    onEditFolder: (Long) -> Unit,
    onBack: () -> Unit,
    onBreadcrumbClick: (Long?) -> Unit,
    onCreateIdea: () -> Unit,
    onCreateFolder: () -> Unit,
    onCompletedClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddOptions by rememberSaveable { mutableStateOf(false) }
    var moveRequest by remember { mutableStateOf<MoveRequest?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.MoveCompleted -> moveRequest = null
                HomeEvent.FolderDeleted -> folderToDelete = null
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onFolderClick = onFolderClick,
        onIdeaClick = onIdeaClick,
        onEditFolder = onEditFolder,
        onMoveFolder = { folder ->
            viewModel.clearContentActionError()
            moveRequest = MoveRequest.Folder(
                id = folder.id,
                name = folder.name,
                currentFolderId = folder.parentFolderId,
            )
        },
        onDeleteFolder = { folder ->
            viewModel.clearContentActionError()
            folderToDelete = folder
        },
        onMoveIdea = { idea ->
            viewModel.clearContentActionError()
            moveRequest = MoveRequest.Idea(
                id = idea.id,
                name = idea.title,
                currentFolderId = idea.folderId,
            )
        },
        onBack = onBack,
        onBreadcrumbClick = onBreadcrumbClick,
        onAddClick = { showAddOptions = true },
        onCompletedClick = onCompletedClick,
    )

    if (showAddOptions) {
        AddOptionsSheet(
            onDismiss = { showAddOptions = false },
            onCreateIdea = {
                showAddOptions = false
                onCreateIdea()
            },
            onCreateFolder = {
                showAddOptions = false
                onCreateFolder()
            },
        )
    }

    moveRequest?.let { request ->
        val excludedFolderIds = if (request is MoveRequest.Folder) {
            descendantFolderIds(uiState.allFolders, request.id) + request.id
        } else {
            emptySet()
        }
        FolderDestinationDialog(
            itemName = request.name,
            currentFolderId = request.currentFolderId,
            options = buildFolderOptions(uiState.allFolders, excludedFolderIds),
            isMoving = uiState.isContentActionRunning,
            errorMessage = uiState.contentActionError,
            onDismiss = {
                viewModel.clearContentActionError()
                moveRequest = null
            },
            onMove = { destinationId ->
                when (request) {
                    is MoveRequest.Idea -> viewModel.moveIdea(request.id, destinationId)
                    is MoveRequest.Folder -> viewModel.moveFolder(request.id, destinationId)
                }
            },
        )
    }

    folderToDelete?.let { folder ->
        DeleteFolderDialog(
            folderName = folder.name,
            isDeleting = uiState.isContentActionRunning,
            errorMessage = uiState.contentActionError,
            onDismiss = {
                viewModel.clearContentActionError()
                folderToDelete = null
            },
            onConfirm = { viewModel.deleteFolder(folder.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOptionsSheet(
    onDismiss: () -> Unit,
    onCreateIdea: () -> Unit,
    onCreateFolder: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.add_options_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.new_idea)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_idea),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable(onClick = onCreateIdea),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.new_folder)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable(onClick = onCreateFolder),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (Long) -> Unit,
    onIdeaClick: (Long) -> Unit,
    onEditFolder: (Long) -> Unit,
    onMoveFolder: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onMoveIdea: (IdeaEntity) -> Unit,
    onBack: () -> Unit,
    onBreadcrumbClick: (Long?) -> Unit,
    onAddClick: () -> Unit,
    onCompletedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hierarchicalIdeas = remember(uiState.ideas) { buildIdeaHierarchy(uiState.ideas) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentFolder?.name
                            ?: stringResource(
                                if (uiState.folderId == null) {
                                    R.string.home_title
                                } else {
                                    R.string.folders_section_title
                                },
                            ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (uiState.folderId != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back_content_description),
                            )
                        }
                    }
                },
                actions = {
                    uiState.folderId?.let { folderId ->
                        IconButton(onClick = { onEditFolder(folderId) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(
                                    R.string.edit_folder_content_description,
                                ),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_content_description),
                )
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 104.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.folderId != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BreadcrumbBar(
                        breadcrumbs = uiState.breadcrumbs,
                        onBreadcrumbClick = onBreadcrumbClick,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            if (!uiState.hasAnyContent && uiState.searchQuery.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(
                            if (uiState.folderId == null) {
                                R.string.empty_home_title
                            } else {
                                R.string.empty_folder_title
                            },
                        ),
                        message = stringResource(
                            if (uiState.folderId == null) {
                                R.string.empty_home_message
                            } else {
                                R.string.empty_folder_message
                            },
                        ),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(text = stringResource(R.string.folders_section_title))
                }

                if (uiState.folders.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = stringResource(
                                if (uiState.folderId == null) {
                                    R.string.empty_folders_title
                                } else {
                                    R.string.empty_subfolders_title
                                },
                            ),
                            message = stringResource(
                                if (uiState.folderId == null) {
                                    R.string.empty_folders_message
                                } else {
                                    R.string.empty_subfolders_message
                                },
                            ),
                            compact = true,
                            folderStyle = true,
                        )
                    }
                } else {
                    items(
                        items = uiState.folders,
                        key = { summary -> "folder-${summary.folder.id}" },
                    ) { summary ->
                        FolderCard(
                            folderSummary = summary,
                            onClick = { onFolderClick(summary.folder.id) },
                            onEditClick = { onEditFolder(summary.folder.id) },
                            onMoveClick = { onMoveFolder(summary.folder) },
                            onDeleteClick = { onDeleteFolder(summary.folder) },
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(
                        text = stringResource(
                            if (uiState.folderId == null) {
                                R.string.root_ideas_section_title
                            } else {
                                R.string.folder_ideas_section_title
                            },
                        ),
                    )
                }

                if (uiState.ideas.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = stringResource(
                                if (uiState.searchQuery.isBlank()) {
                                    R.string.empty_ideas_title
                                } else {
                                    R.string.empty_search_title
                                },
                            ),
                            message = stringResource(
                                if (uiState.searchQuery.isBlank()) {
                                    R.string.empty_ideas_message
                                } else {
                                    R.string.empty_search_message
                                },
                            ),
                            compact = true,
                        )
                    }
                } else {
                    items(
                        items = hierarchicalIdeas,
                        key = { item -> "idea-${item.idea.id}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { item ->
                        IdeaCard(
                            idea = item.idea,
                            hierarchyDepth = item.depth,
                            onClick = { onIdeaClick(item.idea.id) },
                            onMoveClick = { onMoveIdea(item.idea) },
                        )
                    }
                }
            }

            if (uiState.folderId == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CompletedIdeasCard(
                        onClick = onCompletedClick,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<FolderBreadcrumb>,
    onBreadcrumbClick: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(breadcrumbs) {
        if (breadcrumbs.isNotEmpty()) listState.scrollToItem(breadcrumbs.size)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "root") {
            IconButton(onClick = { onBreadcrumbClick(null) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = stringResource(R.string.root_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(
            items = breadcrumbs,
            key = { _, breadcrumb -> breadcrumb.id },
        ) { index, breadcrumb ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (index == breadcrumbs.lastIndex) {
                    Text(
                        text = breadcrumb.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    TextButton(onClick = { onBreadcrumbClick(breadcrumb.id) }) {
                        Text(text = breadcrumb.name)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = shape),
        placeholder = { Text(text = stringResource(R.string.search_ideas_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.search_content_description),
            )
        },
        singleLine = true,
        shape = shape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.36f),
            unfocusedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Preview(name = "Principal con contenido", showBackground = true, widthDp = 400, heightDp = 820)
@Preview(
    name = "Principal modo oscuro",
    showBackground = true,
    widthDp = 400,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenPreview() {
    MindropTheme {
        HomeScreen(
            uiState = HomeUiState(
                folders = listOf(
                    FolderSummary(
                        folder = FolderEntity(id = 1, name = "Programación", icon = "folder"),
                        ideaCount = 7,
                        childFolderCount = 1,
                    ),
                    FolderSummary(
                        folder = FolderEntity(id = 2, name = "Casa", icon = "folder"),
                        ideaCount = 5,
                        childFolderCount = 0,
                    ),
                ),
                ideas = listOf(
                    IdeaEntity(
                        id = 1,
                        title = "IA Pokémon",
                        shortDescription = "IA que juega combates automáticamente",
                        fullDescription = "",
                        icon = "idea",
                    ),
                    IdeaEntity(
                        id = 2,
                        title = "Generador de código",
                        shortDescription = "Herramienta para generar snippets y plantillas",
                        fullDescription = "",
                        icon = "code",
                        parentIdeaId = 1,
                    ),
                ),
                hasAnyContent = true,
            ),
            onSearchQueryChange = {},
            onFolderClick = {},
            onIdeaClick = {},
            onEditFolder = {},
            onMoveFolder = {},
            onDeleteFolder = {},
            onMoveIdea = {},
            onBack = {},
            onBreadcrumbClick = {},
            onAddClick = {},
            onCompletedClick = {},
        )
    }
}

@Preview(name = "Principal vacía", showBackground = true, widthDp = 400, heightDp = 820)
@Composable
private fun EmptyHomeScreenPreview() {
    MindropTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onSearchQueryChange = {},
            onFolderClick = {},
            onIdeaClick = {},
            onEditFolder = {},
            onMoveFolder = {},
            onDeleteFolder = {},
            onMoveIdea = {},
            onBack = {},
            onBreadcrumbClick = {},
            onAddClick = {},
            onCompletedClick = {},
        )
    }
}

@Preview(name = "Carpeta anidada", showBackground = true, widthDp = 400, heightDp = 820)
@Composable
private fun NestedFolderScreenPreview() {
    MindropTheme {
        HomeScreen(
            uiState = HomeUiState(
                folderId = 3,
                currentFolder = FolderEntity(
                    id = 3,
                    name = "Aplicaciones",
                    icon = "folder",
                    parentFolderId = 2,
                ),
                breadcrumbs = listOf(
                    FolderBreadcrumb(1, "Programación"),
                    FolderBreadcrumb(2, "Android"),
                    FolderBreadcrumb(3, "Aplicaciones"),
                ),
                ideas = listOf(
                    IdeaEntity(
                        id = 1,
                        title = "Mindrop",
                        shortDescription = "Aplicación para organizar ideas",
                        fullDescription = "",
                        icon = "idea",
                        folderId = 3,
                    ),
                ),
                hasAnyContent = true,
            ),
            onSearchQueryChange = {},
            onFolderClick = {},
            onIdeaClick = {},
            onEditFolder = {},
            onMoveFolder = {},
            onDeleteFolder = {},
            onMoveIdea = {},
            onBack = {},
            onBreadcrumbClick = {},
            onAddClick = {},
            onCompletedClick = {},
        )
    }
}
