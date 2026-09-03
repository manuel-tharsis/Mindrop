package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.IdeaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaOptionTest {
    @Test
    fun optionsShowHierarchyPaths() {
        val ideas = listOf(
            idea(1, "BioGestor"),
            idea(2, "APK Montones de Jara", parentId = 1),
            idea(3, "Sistema de pesaje", parentId = 2),
        )

        assertEquals(
            listOf(
                "BioGestor",
                "BioGestor / APK Montones de Jara",
                "BioGestor / APK Montones de Jara / Sistema de pesaje",
            ),
            buildIdeaOptions(ideas).map { it.label },
        )
    }

    @Test
    fun currentIdeaAndAllDescendantsCanBeExcludedFromParentOptions() {
        val ideas = listOf(
            idea(1, "A"),
            idea(2, "B", parentId = 1),
            idea(3, "C", parentId = 2),
            idea(4, "Otra"),
        )
        val excludedIds = descendantIdeaIds(ideas, ideaId = 1) + 1L
        val optionIds = buildIdeaOptions(ideas, excludedIds).map { it.id }

        assertEquals(setOf(1L, 2L, 3L), excludedIds)
        assertFalse(optionIds.contains(1L))
        assertFalse(optionIds.contains(2L))
        assertFalse(optionIds.contains(3L))
        assertTrue(optionIds.contains(4L))
    }

    private fun idea(id: Long, title: String, parentId: Long? = null) = IdeaEntity(
        id = id,
        title = title,
        shortDescription = "",
        fullDescription = "",
        icon = "idea",
        parentIdeaId = parentId,
    )
}
