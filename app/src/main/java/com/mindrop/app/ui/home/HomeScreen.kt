package com.mindrop.app.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.mindrop.app.ui.home.components.EmptyState
import com.mindrop.app.ui.home.components.FolderCard
import com.mindrop.app.ui.home.components.IdeaCard
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onFolderClick: (Long) -> Unit,
    onIdeaClick: (Long) -> Unit,
    onAddClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onFolderClick = onFolderClick,
        onIdeaClick = onIdeaClick,
        onAddClick = onAddClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (Long) -> Unit,
    onIdeaClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            if (!uiState.hasAnyContent && uiState.searchQuery.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.empty_home_title),
                        message = stringResource(R.string.empty_home_message),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(text = stringResource(R.string.folders_section_title))
                }

                if (uiState.folders.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = stringResource(R.string.empty_folders_title),
                            message = stringResource(R.string.empty_folders_message),
                            compact = true,
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
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(text = stringResource(R.string.root_ideas_section_title))
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
                        items = uiState.ideas,
                        key = { idea -> "idea-${idea.id}" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { idea ->
                        IdeaCard(
                            idea = idea,
                            onClick = { onIdeaClick(idea.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(R.string.search_ideas_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.search_content_description),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun SectionTitle(
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
                    ),
                ),
                hasAnyContent = true,
            ),
            onSearchQueryChange = {},
            onFolderClick = {},
            onIdeaClick = {},
            onAddClick = {},
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
            onAddClick = {},
        )
    }
}
