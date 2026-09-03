package com.mindrop.app.ui.home

import com.mindrop.app.data.local.entity.IdeaEntity

data class IdeaHierarchyItem(
    val idea: IdeaEntity,
    val depth: Int,
)

fun buildIdeaHierarchy(ideas: List<IdeaEntity>): List<IdeaHierarchyItem> {
    val ideasById = ideas.associateBy { it.id }
    val childrenByParent = ideas.groupBy { it.parentIdeaId }
    val result = mutableListOf<IdeaHierarchyItem>()
    val visited = mutableSetOf<Long>()

    fun appendIdea(idea: IdeaEntity, depth: Int) {
        if (!visited.add(idea.id)) return
        result += IdeaHierarchyItem(idea = idea, depth = depth)
        childrenByParent[idea.id].orEmpty().forEach { child ->
            appendIdea(child, depth = depth + 1)
        }
    }

    ideas
        .filter { it.parentIdeaId == null || it.parentIdeaId !in ideasById }
        .forEach { rootIdea -> appendIdea(rootIdea, depth = 0) }

    // Datos antiguos dañados o un padre fuera del contexto nunca deben ocultar una idea.
    ideas.filterNot { it.id in visited }.forEach { remainingIdea ->
        appendIdea(remainingIdea, depth = 0)
    }

    return result
}
