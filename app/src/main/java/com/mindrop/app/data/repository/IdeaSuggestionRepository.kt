package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.IdeaSuggestionDao
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import kotlinx.coroutines.flow.Flow

class IdeaSuggestionRepository(
    private val database: MindropDatabase,
    private val suggestionDao: IdeaSuggestionDao,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    fun observePending(ideaId: Long): Flow<List<IdeaSuggestionEntity>> =
        suggestionDao.observePending(ideaId)

    fun observeUpdates(ideaId: Long): Flow<List<IdeaSuggestionEntity>> =
        suggestionDao.observeUpdates(ideaId)

    suspend fun addPending(ideaId: Long, text: String): Long = database.withTransaction {
        require(text.isNotBlank()) { "La sugerencia no puede estar vacía." }
        require(database.ideaDao().findById(ideaId) != null) { "La idea no existe." }
        suggestionDao.insert(
            IdeaSuggestionEntity(
                ideaId = ideaId,
                text = text.trim(),
                createdAt = currentTimeMillis(),
            ),
        )
    }

    suspend fun validate(ideaId: Long, suggestionId: Long): IdeaSuggestionEntity? =
        database.withTransaction {
            val suggestion = suggestionDao.findById(suggestionId)
                ?: return@withTransaction null
            if (suggestion.ideaId != ideaId) return@withTransaction null
            if (suggestion.validatedAt != null) return@withTransaction suggestion

            val currentMaximum = suggestionDao.maxUpdateNumber(suggestion.ideaId)
            check(currentMaximum < Int.MAX_VALUE) {
                "No se pueden crear más actualizaciones para esta idea."
            }
            val updateNumber = currentMaximum + 1
            val changed = suggestionDao.validatePending(
                id = suggestion.id,
                validatedAt = currentTimeMillis(),
                updateNumber = updateNumber,
            )
            check(changed == 1) { "La sugerencia ya no está pendiente." }
            suggestionDao.findById(suggestion.id)
        }
}
