package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity

enum class IdeaLocationKind {
    Folder,
    Idea,
}

data class IdeaLocationOption(
    val key: String,
    val name: String,
    val path: String,
    val depth: Int,
    val kind: IdeaLocationKind,
    val icon: String,
    val folderId: Long?,
    val parentIdeaId: Long?,
)

fun buildIdeaLocationOptions(
    folders: List<FolderEntity>,
    ideas: List<IdeaEntity>,
    excludedIdeaIds: Set<Long> = emptySet(),
): List<IdeaLocationOption> {
    val availableIdeas = ideas.filterNot { it.id in excludedIdeaIds }
    val foldersByParent = folders.groupBy { it.parentFolderId }
    val ideasByFolder = availableIdeas.groupBy { it.folderId }
    val knownFolderIds = folders.mapTo(mutableSetOf()) { it.id }
    val result = mutableListOf<IdeaLocationOption>()
    val visitedFolders = mutableSetOf<Long>()
    val visitedIdeas = mutableSetOf<Long>()

    fun appendIdeas(folderId: Long?, folderPath: String?, baseDepth: Int) {
        val ideasInFolder = ideasByFolder[folderId].orEmpty()
        val idsInFolder = ideasInFolder.mapTo(mutableSetOf()) { it.id }
        val childrenByParent = ideasInFolder.groupBy { it.parentIdeaId }

        fun appendIdea(idea: IdeaEntity, parentPath: String?, depth: Int) {
            if (!visitedIdeas.add(idea.id)) return
            val path = when {
                parentPath != null -> "$parentPath / ${idea.title}"
                folderPath != null -> "$folderPath / ${idea.title}"
                else -> idea.title
            }
            result += IdeaLocationOption(
                key = "idea-${idea.id}",
                name = idea.title,
                path = path,
                depth = depth,
                kind = IdeaLocationKind.Idea,
                icon = idea.icon,
                folderId = idea.folderId,
                parentIdeaId = idea.id,
            )
            childrenByParent[idea.id].orEmpty().forEach { child ->
                appendIdea(child, parentPath = path, depth = depth + 1)
            }
        }

        ideasInFolder
            .filter { it.parentIdeaId == null || it.parentIdeaId !in idsInFolder }
            .forEach { idea -> appendIdea(idea, parentPath = null, depth = baseDepth) }
        ideasInFolder.filterNot { it.id in visitedIdeas }.forEach { remainingIdea ->
            appendIdea(remainingIdea, parentPath = null, depth = baseDepth)
        }
    }

    fun appendFolder(folder: FolderEntity, parentPath: String?, depth: Int) {
        if (!visitedFolders.add(folder.id)) return
        val path = if (parentPath == null) folder.name else "$parentPath / ${folder.name}"
        result += IdeaLocationOption(
            key = "folder-${folder.id}",
            name = folder.name,
            path = path,
            depth = depth,
            kind = IdeaLocationKind.Folder,
            icon = folder.icon.ifBlank { "folder" },
            folderId = folder.id,
            parentIdeaId = null,
        )
        foldersByParent[folder.id].orEmpty().forEach { childFolder ->
            appendFolder(childFolder, parentPath = path, depth = depth + 1)
        }
        appendIdeas(folderId = folder.id, folderPath = path, baseDepth = depth + 1)
    }

    foldersByParent[null].orEmpty().forEach { folder ->
        appendFolder(folder, parentPath = null, depth = 0)
    }
    folders.filterNot { it.id in visitedFolders }.forEach { orphanFolder ->
        appendFolder(orphanFolder, parentPath = null, depth = 0)
    }
    appendIdeas(folderId = null, folderPath = null, baseDepth = 0)
    availableIdeas
        .filter { it.folderId != null && it.folderId !in knownFolderIds && it.id !in visitedIdeas }
        .forEach { orphanIdea ->
            result += IdeaLocationOption(
                key = "idea-${orphanIdea.id}",
                name = orphanIdea.title,
                path = orphanIdea.title,
                depth = 0,
                kind = IdeaLocationKind.Idea,
                icon = orphanIdea.icon,
                folderId = orphanIdea.folderId,
                parentIdeaId = orphanIdea.id,
            )
            visitedIdeas += orphanIdea.id
        }

    return result
}

fun descendantIdeaIds(
    ideas: List<IdeaEntity>,
    ideaId: Long,
): Set<Long> {
    val childrenByParent = ideas.groupBy { it.parentIdeaId }
    val descendants = mutableSetOf<Long>()
    val pending = ArrayDeque(childrenByParent[ideaId].orEmpty())

    while (pending.isNotEmpty()) {
        val idea = pending.removeFirst()
        if (descendants.add(idea.id)) {
            pending.addAll(childrenByParent[idea.id].orEmpty())
        }
    }

    return descendants
}
