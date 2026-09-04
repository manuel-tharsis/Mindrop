package com.mindrop.app.ui.idea

import com.mindrop.app.data.local.entity.IdeaSuggestionEntity

sealed interface IdeaDetailPage {
    data object Description : IdeaDetailPage

    data class Update(val value: IdeaSuggestionEntity) : IdeaDetailPage

    data object Suggestions : IdeaDetailPage
}

fun buildIdeaDetailPages(updates: List<IdeaSuggestionEntity>): List<IdeaDetailPage> = buildList {
    add(IdeaDetailPage.Description)
    updates
        .filter { it.validatedAt != null && it.updateNumber != null }
        .sortedWith(compareBy<IdeaSuggestionEntity> { it.updateNumber }.thenBy { it.id })
        .forEach { update -> add(IdeaDetailPage.Update(update)) }
    add(IdeaDetailPage.Suggestions)
}
