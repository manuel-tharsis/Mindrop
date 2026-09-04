package com.mindrop.app.ui.idea

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindrop.app.R
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import com.mindrop.app.ui.icons.MindropIdeaIcon
import com.mindrop.app.ui.theme.MindropTheme

@Composable
fun IdeaDetailRoute(
    viewModel: IdeaDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var suggestionToDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == IdeaDetailEvent.Deleted) onBack()
        }
    }

    IdeaDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onEdit = { uiState.idea?.id?.let(onEdit) },
        onDelete = { showDeleteConfirmation = true },
        onToggleCompleted = viewModel::toggleCompleted,
        onValidateSuggestion = viewModel::validateSuggestion,
        onDeleteSuggestion = { suggestionId -> suggestionToDelete = suggestionId },
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeleting) showDeleteConfirmation = false
            },
            title = { Text(stringResource(R.string.delete_idea_confirmation_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_idea_confirmation_message))
                    uiState.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::delete,
                    enabled = !uiState.isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = !uiState.isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    suggestionToDelete?.let { suggestionId ->
        AlertDialog(
            onDismissRequest = { suggestionToDelete = null },
            title = { Text(stringResource(R.string.delete_suggestion_title)) },
            text = { Text(stringResource(R.string.delete_suggestion_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        suggestionToDelete = null
                        viewModel.deleteSuggestion(suggestionId)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaDetailScreen(
    uiState: IdeaDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleCompleted: () -> Unit,
    onValidateSuggestion: (Long) -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBusy = uiState.isDeleting ||
        uiState.isUpdatingCompletion ||
        uiState.validatingSuggestionId != null ||
        uiState.deletingSuggestionId != null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isBusy) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_content_description),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        enabled = uiState.idea != null && !isBusy,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.edit_idea),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = uiState.idea != null && !isBusy,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(
                                R.string.delete_idea_content_description,
                            ),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingDetail(modifier = Modifier.padding(innerPadding))
            uiState.idea != null -> IdeaDetailContent(
                uiState = uiState,
                onToggleCompleted = onToggleCompleted,
                onValidateSuggestion = onValidateSuggestion,
                onDeleteSuggestion = onDeleteSuggestion,
                modifier = Modifier.padding(innerPadding),
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.idea_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LoadingDetail(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun IdeaDetailContent(
    uiState: IdeaDetailUiState,
    onToggleCompleted: () -> Unit,
    onValidateSuggestion: (Long) -> Unit,
    onDeleteSuggestion: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val idea = uiState.idea ?: return
    val pages = remember(uiState.updates) { buildIdeaDetailPages(uiState.updates) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    var previousUpdateCount by remember { mutableIntStateOf(uiState.updates.size) }

    LaunchedEffect(uiState.updates.size) {
        val previousSuggestionsPage = previousUpdateCount + 1
        if (
            uiState.updates.size > previousUpdateCount &&
            pagerState.currentPage == previousSuggestionsPage
        ) {
            pagerState.animateScrollToPage(uiState.updates.size + 1)
        }
        previousUpdateCount = uiState.updates.size
    }

    Column(modifier = modifier.fillMaxSize()) {
        IdeaHeader(
            idea = idea,
            isUpdatingCompletion = uiState.isUpdatingCompletion,
            isDeleting = uiState.isDeleting,
            errorMessage = uiState.errorMessage,
            onToggleCompleted = onToggleCompleted,
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            when (val page = pages.getOrNull(pageIndex)) {
                IdeaDetailPage.Description -> DescriptionPage(idea.fullDescription)
                is IdeaDetailPage.Update -> UpdatePage(page.value)
                IdeaDetailPage.Suggestions -> SuggestionsPage(
                    uiState = uiState,
                    onValidate = onValidateSuggestion,
                    onDelete = onDeleteSuggestion,
                )
                null -> Unit
            }
        }
        PagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun IdeaHeader(
    idea: IdeaEntity,
    isUpdatingCompletion: Boolean,
    isDeleting: Boolean,
    errorMessage: String?,
    onToggleCompleted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MindropIdeaIcon(
            icon = idea.icon,
            customIconPath = idea.customIconPath,
            contentDescription = stringResource(R.string.idea_icon_content_description),
            size = 88.dp,
        )
        Text(
            text = idea.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (idea.shortDescription.isNotBlank()) {
            Text(
                text = idea.shortDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onToggleCompleted,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUpdatingCompletion && !isDeleting,
        ) {
            if (isUpdatingCompletion) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            }
            Text(
                stringResource(
                    if (idea.isCompleted) {
                        R.string.mark_as_not_completed
                    } else {
                        R.string.mark_as_completed
                    },
                ),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DescriptionPage(fullDescription: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageTitle(stringResource(R.string.full_description_label))
        }
        if (fullDescription.isNotBlank()) {
            item {
                Text(text = fullDescription, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun UpdatePage(update: IdeaSuggestionEntity) {
    val updateNumber = update.updateNumber ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageTitle(stringResource(R.string.update_number, updateNumber))
        }
        item {
            Text(text = update.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SuggestionsPage(
    uiState: IdeaDetailUiState,
    onValidate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageTitle(stringResource(R.string.suggestions_title))
        }
        uiState.validationErrorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (uiState.pendingSuggestions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_current_suggestions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            itemsIndexed(
                items = uiState.pendingSuggestions,
                key = { _, suggestion -> suggestion.id },
            ) { _, suggestion ->
                PendingSuggestionCard(
                    suggestion = suggestion,
                    isValidating = uiState.validatingSuggestionId == suggestion.id,
                    isDeleting = uiState.deletingSuggestionId == suggestion.id,
                    enabled = uiState.validatingSuggestionId == null &&
                        uiState.deletingSuggestionId == null &&
                        !uiState.isDeleting &&
                        !uiState.isUpdatingCompletion,
                    onValidate = { onValidate(suggestion.id) },
                    onDelete = { onDelete(suggestion.id) },
                )
            }
        }
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PendingSuggestionCard(
    suggestion: IdeaSuggestionEntity,
    isValidating: Boolean,
    isDeleting: Boolean,
    enabled: Boolean,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = suggestion.text, style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onValidate, enabled = enabled) {
                    if (isValidating) {
                        SmallActionProgress()
                    }
                    Text(stringResource(R.string.validate_suggestion))
                }
                TextButton(
                    onClick = onDelete,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (isDeleting) {
                        SmallActionProgress()
                    }
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun SmallActionProgress() {
    CircularProgressIndicator(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(18.dp),
        strokeWidth = 2.dp,
    )
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.page_indicator_content_description,
        currentPage + 1,
        pageCount,
    )
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(List(pageCount) { it }) { index, _ ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 9.dp else 7.dp)
                    .background(
                        color = if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Preview(name = "Detalle", showBackground = true, widthDp = 400, heightDp = 820)
@Preview(
    name = "Detalle oscuro",
    showBackground = true,
    widthDp = 400,
    heightDp = 820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IdeaDetailPreview() {
    MindropTheme {
        IdeaDetailScreen(
            uiState = IdeaDetailUiState(
                isLoading = false,
                idea = IdeaEntity(
                    id = 1,
                    title = "Mindrop",
                    shortDescription = "Aplicación para organizar ideas",
                    fullDescription = "Primera línea.\n\nSegunda línea con más información.",
                    icon = "brain",
                ),
                updates = listOf(
                    IdeaSuggestionEntity(
                        id = 10,
                        ideaId = 1,
                        text = "Permitir organizar ideas en subcarpetas.",
                        createdAt = 1_000,
                        validatedAt = 2_000,
                        updateNumber = 1,
                    ),
                ),
                pendingSuggestions = listOf(
                    IdeaSuggestionEntity(
                        id = 11,
                        ideaId = 1,
                        text = "Revisar el tamaño de las tarjetas.",
                        createdAt = 3_000,
                    ),
                ),
            ),
            onBack = {},
            onEdit = {},
            onDelete = {},
            onToggleCompleted = {},
            onValidateSuggestion = {},
            onDeleteSuggestion = {},
        )
    }
}

@Preview(name = "Detalle vacío", showBackground = true, widthDp = 400, heightDp = 820)
@Composable
private fun EmptyIdeaDetailPreview() {
    MindropTheme {
        IdeaDetailScreen(
            uiState = IdeaDetailUiState(
                isLoading = false,
                idea = IdeaEntity(
                    id = 2,
                    title = "Solo un nombre",
                    shortDescription = "",
                    fullDescription = "",
                    icon = "document",
                ),
            ),
            onBack = {},
            onEdit = {},
            onDelete = {},
            onToggleCompleted = {},
            onValidateSuggestion = {},
            onDeleteSuggestion = {},
        )
    }
}
