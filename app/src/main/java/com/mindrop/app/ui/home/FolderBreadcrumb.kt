package com.mindrop.app.ui.home

import com.mindrop.app.data.local.entity.FolderEntity

data class FolderBreadcrumb(
    val id: Long,
    val name: String,
)

fun buildFolderBreadcrumbs(
    folders: List<FolderEntity>,
    folderId: Long?,
): List<FolderBreadcrumb> {
    var currentId = folderId ?: return emptyList()
    val foldersById = folders.associateBy(FolderEntity::id)
    val visited = mutableSetOf<Long>()
    val reversedPath = mutableListOf<FolderBreadcrumb>()

    while (visited.add(currentId)) {
        val folder = foldersById[currentId] ?: break
        reversedPath += FolderBreadcrumb(folder.id, folder.name)
        currentId = folder.parentFolderId ?: break
    }

    return reversedPath.asReversed()
}
