package com.mindrop.app.ui.idea

import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IdeaDetailPagesTest {
    @Test
    fun withoutUpdatesOnlyDescriptionAndSuggestionsAreShown() {
        val pages = buildIdeaDetailPages(emptyList())

        assertEquals(2, pages.size)
        assertSame(IdeaDetailPage.Description, pages.first())
        assertSame(IdeaDetailPage.Suggestions, pages.last())
    }

    @Test
    fun everyValidatedUpdateGetsItsOwnSequentialPage() {
        val pages = buildIdeaDetailPages(
            listOf(
                update(id = 3, number = 3),
                update(id = 1, number = 1),
                update(id = 2, number = 2),
            ),
        )

        assertSame(IdeaDetailPage.Description, pages.first())
        assertEquals(
            listOf(1, 2, 3),
            pages.filterIsInstance<IdeaDetailPage.Update>().map { it.value.updateNumber },
        )
        assertSame(IdeaDetailPage.Suggestions, pages.last())
    }

    @Test
    fun pendingOrMalformedRowsNeverCreateUpdatePages() {
        val pages = buildIdeaDetailPages(
            listOf(
                IdeaSuggestionEntity(id = 1, ideaId = 5, text = "Pendiente", createdAt = 10),
                IdeaSuggestionEntity(
                    id = 2,
                    ideaId = 5,
                    text = "Sin número",
                    createdAt = 10,
                    validatedAt = 20,
                ),
            ),
        )

        assertEquals(listOf(IdeaDetailPage.Description, IdeaDetailPage.Suggestions), pages)
    }

    private fun update(id: Long, number: Int) = IdeaSuggestionEntity(
        id = id,
        ideaId = 5,
        text = "Actualización $number",
        createdAt = 10,
        validatedAt = 20,
        updateNumber = number,
    )
}
