package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.icon.CustomIconFileStore
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

class IdeaRepository(
    private val database: MindropDatabase,
    private val ideaDao: IdeaDao,
    private val customIconFileStore: CustomIconFileStore? = null,
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

    suspend fun update(idea: IdeaEntity): Boolean {
        var obsoleteCustomIconPath: String? = null
        val updated = database.withTransaction {
            require(idea.id > 0L) { "La idea que se actualiza debe tener un id válido." }
            require(idea.title.isNotBlank()) { "El título de la idea no puede estar vacío." }

            val storedIdea = ideaDao.findById(idea.id) ?: return@withTransaction false
            val now = currentTimeMillis()
            val nextUpdatedAt = when {
                now > storedIdea.updatedAt -> now
                storedIdea.updatedAt == Long.MAX_VALUE -> Long.MAX_VALUE
                else -> storedIdea.updatedAt + 1
            }
            val rowUpdated = ideaDao.update(
                idea.copy(
                    createdAt = storedIdea.createdAt,
                    updatedAt = nextUpdatedAt,
                ),
            ) == 1
            if (rowUpdated && storedIdea.customIconPath != idea.customIconPath) {
                obsoleteCustomIconPath = storedIdea.customIconPath
            }
            rowUpdated
        }
        if (updated) obsoleteCustomIconPath?.let { customIconFileStore?.delete(it) }
        return updated
    }

    suspend fun deleteById(id: Long): Boolean {
        var customIconPath: String? = null
        val deleted = database.withTransaction {
            val storedIdea = ideaDao.findById(id) ?: return@withTransaction false
            val rowDeleted = ideaDao.deleteById(id) == 1
            if (rowDeleted) customIconPath = storedIdea.customIconPath
            rowDeleted
        }
        if (deleted) customIconPath?.let { customIconFileStore?.delete(it) }
        return deleted
    }
}
