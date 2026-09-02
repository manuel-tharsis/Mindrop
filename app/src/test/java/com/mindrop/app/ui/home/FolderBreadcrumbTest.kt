package com.mindrop.app.ui.home

import com.mindrop.app.data.local.entity.FolderEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderBreadcrumbTest {
    @Test
    fun breadcrumbsContainEveryAncestorInRootToCurrentOrder() {
        val folders = listOf(
            folder(id = 1, name = "Programación"),
            folder(id = 2, name = "Android", parentId = 1),
            folder(id = 3, name = "Aplicaciones", parentId = 2),
        )

        assertEquals(
            listOf(
                FolderBreadcrumb(1, "Programación"),
                FolderBreadcrumb(2, "Android"),
                FolderBreadcrumb(3, "Aplicaciones"),
            ),
            buildFolderBreadcrumbs(folders, folderId = 3),
        )
    }

    @Test
    fun breadcrumbsSupportManyLevelsWithoutHardcodedDepth() {
        val folders = (1L..30L).map { id ->
            folder(
                id = id,
                name = "Nivel $id",
                parentId = (id - 1).takeIf { it > 0 },
            )
        }

        val breadcrumbs = buildFolderBreadcrumbs(folders, folderId = 30)

        assertEquals((1L..30L).toList(), breadcrumbs.map(FolderBreadcrumb::id))
    }

    @Test
    fun malformedCycleCannotMakeBreadcrumbBuildingLoopForever() {
        val folders = listOf(
            folder(id = 1, name = "Uno", parentId = 2),
            folder(id = 2, name = "Dos", parentId = 1),
        )

        val breadcrumbs = buildFolderBreadcrumbs(folders, folderId = 1)

        assertEquals(setOf(1L, 2L), breadcrumbs.map(FolderBreadcrumb::id).toSet())
        assertEquals(2, breadcrumbs.size)
    }

    private fun folder(
        id: Long,
        name: String,
        parentId: Long? = null,
    ) = FolderEntity(
        id = id,
        name = name,
        icon = "folder",
        parentFolderId = parentId,
    )
}
