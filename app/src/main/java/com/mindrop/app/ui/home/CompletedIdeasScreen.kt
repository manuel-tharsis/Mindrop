package com.mindrop.app.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.home.components.EmptyState
import com.mindrop.app.ui.home.components.IdeaCard
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun CompletedIdeasRoute(
    viewModel: CompletedIdeasViewModel,
    onBack: () -> Unit,
    onIdeaClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CompletedIdeasScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onBack = onBack,
        onIdeaClick = onIdeaClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedIdeasScreen(
    uiState: CompletedIdeasUiState,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onIdeaClick: (Long) -> Unit,
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
                        text = stringResource(R.string.completed_ideas_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
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
                bottom = 40.dp,
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

            if (hierarchicalIdeas.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(
                            if (uiState.searchQuery.isNotBlank() && uiState.hasCompletedIdeas) {
                                R.string.empty_search_title
                            } else {
                                R.string.empty_completed_title
                            },
                        ),
                        message = stringResource(
                            if (uiState.searchQuery.isNotBlank() && uiState.hasCompletedIdeas) {
                                R.string.empty_search_message
                            } else {
                                R.string.empty_completed_message
                            },
                        ),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(text = stringResource(R.string.completed_ideas_section_title))
                }
                items(
                    items = hierarchicalIdeas,
                    key = { item -> "completed-idea-${item.idea.id}" },
                    span = { GridItemSpan(maxLineSpan) },
                ) { item ->
                    IdeaCard(
                        idea = item.idea,
                        hierarchyDepth = item.depth,
                        onClick = { onIdeaClick(item.idea.id) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Completadas", showBackground = true, widthDp = 400, heightDp = 820)
@Preview(
    name = "Completadas oscuro",
    showBackground = true,
    widthDp = 400,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CompletedIdeasScreenPreview() {
    MindropTheme {
        CompletedIdeasScreen(
            uiState = CompletedIdeasUiState(
                ideas = listOf(
                    IdeaEntity(
                        id = 1,
                        title = "BioGestor",
                        shortDescription = "Gestión para el laboratorio",
                        fullDescription = "",
                        icon = "brain",
                        isCompleted = true,
                    ),
                    IdeaEntity(
                        id = 2,
                        title = "APK Montones de Jara",
                        shortDescription = "Aplicación derivada",
                        fullDescription = "",
                        icon = "mobile",
                        parentIdeaId = 1,
                        isCompleted = true,
                    ),
                ),
                hasCompletedIdeas = true,
            ),
            onSearchQueryChange = {},
            onBack = {},
            onIdeaClick = {},
        )
    }
}
