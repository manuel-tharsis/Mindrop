package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaLocationOptionTest {
    @Test
    fun selectorMixesFoldersAndIdeasUsingTheirHierarchy() {
        val folders = listOf(
            folder(1, "Proyectos"),
            folder(2, "Android", parentId = 1),
        )
        val ideas = listOf(
            idea(10, "BioGestor", folderId = 2),
            idea(11, "APK Montones de Jara", folderId = 2, parentId = 10),
        )

        val options = buildIdeaLocationOptions(folders, ideas)

        assertEquals(
            listOf(
                "Proyectos",
                "Proyectos / Android",
                "Proyectos / Android / BioGestor",
                "Proyectos / Android / BioGestor / APK Montones de Jara",
            ),
            options.map { it.path },
        )
        assertEquals(
            listOf(
                IdeaLocationKind.Folder,
                IdeaLocationKind.Folder,
                IdeaLocationKind.Idea,
                IdeaLocationKind.Idea,
            ),
            options.map { it.kind },
        )
    }

    @Test
    fun selectingFolderClearsParentAndSelectingIdeaInheritsItsFolder() {
        val options = buildIdeaLocationOptions(
            folders = listOf(folder(2, "Android")),
            ideas = listOf(idea(10, "BioGestor", folderId = 2)),
        )

        val folderOption = options.first { it.kind == IdeaLocationKind.Folder }
        val ideaOption = options.first { it.kind == IdeaLocationKind.Idea }

        assertEquals(2L, folderOption.folderId)
        assertNull(folderOption.parentIdeaId)
        assertEquals(2L, ideaOption.folderId)
        assertEquals(10L, ideaOption.parentIdeaId)
    }

    @Test
    fun currentIdeaAndDescendantsAreExcludedToPreventCycles() {
        val ideas = listOf(
            idea(1, "A"),
            idea(2, "B", parentId = 1),
            idea(3, "C", parentId = 2),
            idea(4, "Otra"),
        )
        val excludedIds = descendantIdeaIds(ideas, ideaId = 1) + 1L
        val optionIds = buildIdeaLocationOptions(
            folders = emptyList(),
            ideas = ideas,
            excludedIdeaIds = excludedIds,
        ).filter { it.kind == IdeaLocationKind.Idea }.mapNotNull { it.parentIdeaId }

        assertEquals(setOf(1L, 2L, 3L), excludedIds)
        assertFalse(optionIds.contains(1L))
        assertFalse(optionIds.contains(2L))
        assertFalse(optionIds.contains(3L))
        assertTrue(optionIds.contains(4L))
    }

    private fun folder(id: Long, name: String, parentId: Long? = null) = FolderEntity(
        id = id,
        name = name,
        icon = "folder",
        parentFolderId = parentId,
    )

    private fun idea(
        id: Long,
        title: String,
        folderId: Long? = null,
        parentId: Long? = null,
    ) = IdeaEntity(
        id = id,
        title = title,
        shortDescription = "",
        fullDescription = "",
        icon = "idea",
        folderId = folderId,
        parentIdeaId = parentId,
    )
}
