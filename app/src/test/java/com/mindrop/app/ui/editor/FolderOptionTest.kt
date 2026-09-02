package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.FolderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FolderOptionTest {
    private val folders = listOf(
        FolderEntity(id = 1, name = "Programación", icon = "folder"),
        FolderEntity(id = 2, name = "Android", icon = "folder", parentFolderId = 1),
        FolderEntity(id = 3, name = "Proyectos", icon = "folder", parentFolderId = 2),
        FolderEntity(id = 4, name = "Casa", icon = "folder"),
    )

    @Test
    fun folderOptionsShowTheCompleteHierarchy() {
        assertEquals(
            listOf(
                FolderOption(1, "Programación"),
                FolderOption(2, "Programación / Android"),
                FolderOption(3, "Programación / Android / Proyectos"),
                FolderOption(4, "Casa"),
            ),
            buildFolderOptions(folders),
        )
    }

    @Test
    fun editingFolderCanExcludeItselfAndAllDescendants() {
        val excludedIds = descendantFolderIds(folders, folderId = 1) + 1L
        val options = buildFolderOptions(folders, excludedIds)

        assertEquals(listOf(FolderOption(4, "Casa")), options)
        assertFalse(options.any { it.id in setOf(1L, 2L, 3L) })
    }
}
