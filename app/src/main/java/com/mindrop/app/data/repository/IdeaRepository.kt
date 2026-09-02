package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

class IdeaRepository(
    private val database: MindropDatabase,
    private val ideaDao: IdeaDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeInFolder(folderId: Long?): Flow<List<IdeaEntity>> =
        ideaDao.observeInFolder(folderId)

    suspend fun findById(id: Long): IdeaEntity? = ideaDao.findById(id)

    suspend fun insert(idea: IdeaEntity): Long {
        require(idea.id == 0L) { "Una idea nueva no puede tener un id asignado." }
        require(idea.title.isNotBlank()) { "El título de la idea no puede estar vacío." }

        val now = currentTimeMillis()
        return ideaDao.insert(
            idea.copy(
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(idea: IdeaEntity): Boolean = database.withTransaction {
        require(idea.id > 0L) { "La idea que se actualiza debe tener un id válido." }
        require(idea.title.isNotBlank()) { "El título de la idea no puede estar vacío." }

        val storedIdea = ideaDao.findById(idea.id) ?: return@withTransaction false
        val now = currentTimeMillis()
        val nextUpdatedAt = when {
            now > storedIdea.updatedAt -> now
            storedIdea.updatedAt == Long.MAX_VALUE -> Long.MAX_VALUE
            else -> storedIdea.updatedAt + 1
        }

        ideaDao.update(
            idea.copy(
                createdAt = storedIdea.createdAt,
                updatedAt = nextUpdatedAt,
            ),
        ) == 1
    }

    suspend fun deleteById(id: Long): Boolean = ideaDao.deleteById(id) == 1
}
