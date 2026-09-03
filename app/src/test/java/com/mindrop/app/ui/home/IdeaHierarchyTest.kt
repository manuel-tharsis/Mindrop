package com.mindrop.app.ui.home

import com.mindrop.app.data.local.entity.IdeaEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeaHierarchyTest {
    @Test
    fun parentIsImmediatelyFollowedByItsDescendantsAtIncreasingDepth() {
        val hierarchy = buildIdeaHierarchy(
            listOf(
                idea(3, "C", parentId = 2),
                idea(2, "B", parentId = 1),
                idea(1, "A"),
                idea(4, "Otra"),
            ),
        )

        assertEquals(listOf("A", "B", "C", "Otra"), hierarchy.map { it.idea.title })
        assertEquals(listOf(0, 1, 2, 0), hierarchy.map { it.depth })
    }

    @Test
    fun childRemainsVisibleWhenItsParentIsOutsideCurrentContext() {
        val hierarchy = buildIdeaHierarchy(
            listOf(idea(2, "Subidea visible", parentId = 1)),
        )

        assertEquals(listOf("Subidea visible"), hierarchy.map { it.idea.title })
        assertEquals(0, hierarchy.single().depth)
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
