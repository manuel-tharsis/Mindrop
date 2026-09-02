package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.FolderEntity

data class FolderOption(
    val id: Long,
    val label: String,
)

fun buildFolderOptions(
    folders: List<FolderEntity>,
    excludedIds: Set<Long> = emptySet(),
): List<FolderOption> {
    val availableFolders = folders.filterNot { it.id in excludedIds }
    val childrenByParent = availableFolders.groupBy { it.parentFolderId }
    val result = mutableListOf<FolderOption>()
    val visited = mutableSetOf<Long>()

    fun appendFolder(folder: FolderEntity, parentPath: String?) {
        if (!visited.add(folder.id)) return

        val path = if (parentPath == null) folder.name else "$parentPath / ${folder.name}"
        result += FolderOption(id = folder.id, label = path)
        childrenByParent[folder.id].orEmpty().forEach { child ->
            appendFolder(folder = child, parentPath = path)
        }
    }

    childrenByParent[null].orEmpty().forEach { rootFolder ->
        appendFolder(folder = rootFolder, parentPath = null)
    }
    availableFolders.filterNot { it.id in visited }.forEach { orphan ->
        appendFolder(folder = orphan, parentPath = null)
    }

    return result
}

fun descendantFolderIds(
    folders: List<FolderEntity>,
    folderId: Long,
): Set<Long> {
    val childrenByParent = folders.groupBy { it.parentFolderId }
    val descendants = mutableSetOf<Long>()
    val pending = ArrayDeque(childrenByParent[folderId].orEmpty())

    while (pending.isNotEmpty()) {
        val folder = pending.removeFirst()
        if (descendants.add(folder.id)) {
            pending.addAll(childrenByParent[folder.id].orEmpty())
        }
    }

    return descendants
}
