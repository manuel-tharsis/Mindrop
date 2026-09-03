package com.mindrop.app.ui.editor

import com.mindrop.app.data.local.entity.IdeaEntity

data class IdeaOption(
    val id: Long,
    val label: String,
)

fun buildIdeaOptions(
    ideas: List<IdeaEntity>,
    excludedIds: Set<Long> = emptySet(),
): List<IdeaOption> {
    val availableIdeas = ideas.filterNot { it.id in excludedIds }
    val availableIds = availableIdeas.mapTo(mutableSetOf()) { it.id }
    val childrenByParent = availableIdeas.groupBy { it.parentIdeaId }
    val result = mutableListOf<IdeaOption>()
    val visited = mutableSetOf<Long>()

    fun appendIdea(idea: IdeaEntity, parentPath: String?) {
        if (!visited.add(idea.id)) return

        val path = if (parentPath == null) idea.title else "$parentPath / ${idea.title}"
        result += IdeaOption(id = idea.id, label = path)
        childrenByParent[idea.id].orEmpty().forEach { child ->
            appendIdea(idea = child, parentPath = path)
        }
    }

    availableIdeas
        .filter { it.parentIdeaId == null || it.parentIdeaId !in availableIds }
        .forEach { rootIdea -> appendIdea(rootIdea, parentPath = null) }
    availableIdeas.filterNot { it.id in visited }.forEach { orphan ->
        appendIdea(orphan, parentPath = null)
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
