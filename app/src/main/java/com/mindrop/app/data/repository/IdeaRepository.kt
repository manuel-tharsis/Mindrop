package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.icon.CustomIconFileStore
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class IdeaRepository(
    private val database: MindropDatabase,
    private val ideaDao: IdeaDao,
    private val customIconFileStore: CustomIconFileStore? = null,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeAll(): Flow<List<IdeaEntity>> = ideaDao.observeAll()

    fun observeInFolder(folderId: Long?): Flow<List<IdeaEntity>> =
        ideaDao.observeInFolder(folderId)

    fun observeCompleted(): Flow<List<IdeaEntity>> = ideaDao.observeCompleted()

    fun observeCompletedCount(): Flow<Int> = ideaDao.observeCompletedCount()

    fun observeById(id: Long): Flow<IdeaEntity?> = ideaDao.observeById(id)

    suspend fun findById(id: Long): IdeaEntity? = ideaDao.findById(id)

    suspend fun insert(
        idea: IdeaEntity,
        pendingSuggestionTexts: List<String> = emptyList(),
    ): Long = database.withTransaction {
        require(idea.id == 0L) { "Una idea nueva no puede tener un id asignado." }
        require(idea.title.isNotBlank()) { "El título de la idea no puede estar vacío." }
        validateParentIdea(ideaId = null, parentIdeaId = idea.parentIdeaId)
        val suggestions = validatedSuggestionTexts(pendingSuggestionTexts)

        val now = currentTimeMillis()
        val ideaId = ideaDao.insert(
            idea.copy(
                createdAt = now,
                updatedAt = now,
            ),
        )
        insertPendingSuggestions(ideaId, suggestions, now)
        ideaId
    }

    suspend fun update(
        idea: IdeaEntity,
        pendingSuggestionTexts: List<String> = emptyList(),
    ): Boolean {
        var obsoleteCustomIconPath: String? = null
        val updated = database.withTransaction {
            require(idea.id > 0L) { "La idea que se actualiza debe tener un id válido." }
            require(idea.title.isNotBlank()) { "El título de la idea no puede estar vacío." }
            val suggestions = validatedSuggestionTexts(pendingSuggestionTexts)

            val storedIdea = ideaDao.findById(idea.id) ?: return@withTransaction false
            validateParentIdea(ideaId = idea.id, parentIdeaId = idea.parentIdeaId)
            val updatedAt = nextUpdatedAt(storedIdea.updatedAt)
            val rowUpdated = ideaDao.update(
                idea.copy(
                    createdAt = storedIdea.createdAt,
                    updatedAt = updatedAt,
                ),
            ) == 1
            if (rowUpdated) {
                insertPendingSuggestions(idea.id, suggestions, updatedAt)
            }
            if (rowUpdated && storedIdea.customIconPath != idea.customIconPath) {
                obsoleteCustomIconPath = storedIdea.customIconPath
            }
            rowUpdated
        }
        if (updated) deleteCustomIcon(obsoleteCustomIconPath)
        return updated
    }

    suspend fun moveToFolder(ideaId: Long, folderId: Long?): Boolean =
        database.withTransaction {
            if (folderId != null && database.folderDao().findById(folderId) == null) {
                throw IllegalArgumentException("La carpeta de destino no existe.")
            }
            val storedIdea = ideaDao.findById(ideaId) ?: return@withTransaction false
            if (storedIdea.folderId == folderId) return@withTransaction true

            ideaDao.update(
                storedIdea.copy(
                    folderId = folderId,
                    updatedAt = nextUpdatedAt(storedIdea.updatedAt),
                ),
            ) == 1
        }

    suspend fun setCompleted(ideaId: Long, isCompleted: Boolean): Boolean =
        database.withTransaction {
            val storedIdea = ideaDao.findById(ideaId) ?: return@withTransaction false
            if (storedIdea.isCompleted == isCompleted) return@withTransaction true

            ideaDao.setCompleted(
                id = ideaId,
                isCompleted = isCompleted,
                updatedAt = nextUpdatedAt(storedIdea.updatedAt),
            ) == 1
        }

    suspend fun deleteById(id: Long): Boolean {
        var customIconPath: String? = null
        val deleted = database.withTransaction {
            val storedIdea = ideaDao.findById(id) ?: return@withTransaction false
            val rowDeleted = ideaDao.deleteById(id) == 1
            if (rowDeleted) customIconPath = storedIdea.customIconPath
            rowDeleted
        }
        if (deleted) deleteCustomIcon(customIconPath)
        return deleted
    }

    private suspend fun deleteCustomIcon(path: String?) {
        if (path == null || customIconFileStore == null) return
        withContext(Dispatchers.IO) {
            customIconFileStore.delete(path)
        }
    }

    private fun validatedSuggestionTexts(texts: List<String>): List<String> = texts.map { text ->
        require(text.isNotBlank()) { "La sugerencia no puede estar vacía." }
        text.trim()
    }

    private suspend fun insertPendingSuggestions(
        ideaId: Long,
        texts: List<String>,
        createdAt: Long,
    ) {
        texts.forEach { text ->
            database.ideaSuggestionDao().insert(
                IdeaSuggestionEntity(
                    ideaId = ideaId,
                    text = text,
                    createdAt = createdAt,
                ),
            )
        }
    }

    private suspend fun validateParentIdea(ideaId: Long?, parentIdeaId: Long?) {
        var ancestorId = parentIdeaId ?: return
        val visitedIds = mutableSetOf<Long>()

        while (true) {
            if (ancestorId == ideaId) {
                throw IdeaHierarchyException(
                    "Una idea no puede derivar de sí misma ni de una subidea suya.",
                )
            }
            if (!visitedIds.add(ancestorId)) {
                throw IdeaHierarchyException("La jerarquía de ideas contiene un ciclo.")
            }

            val ancestor = ideaDao.findById(ancestorId)
                ?: throw IdeaHierarchyException("La idea padre seleccionada ya no existe.")
            ancestorId = ancestor.parentIdeaId ?: return
        }
    }

    private fun nextUpdatedAt(previousUpdatedAt: Long): Long {
        val now = currentTimeMillis()
        return when {
            now > previousUpdatedAt -> now
            previousUpdatedAt == Long.MAX_VALUE -> Long.MAX_VALUE
            else -> previousUpdatedAt + 1
        }
    }
}

class IdeaHierarchyException(message: String) : IllegalArgumentException(message)
