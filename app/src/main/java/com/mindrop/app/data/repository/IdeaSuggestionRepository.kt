package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.IdeaSuggestionDao
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import kotlinx.coroutines.flow.Flow
import com.mindrop.app.data.rpg.ProgresoRpg

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
        ProgresoRpg(database).inicializar()
        require(text.isNotBlank()) { "La sugerencia no puede estar vacía." }
        require(database.ideaDao().findById(ideaId) != null) { "La idea no existe." }
        val id = suggestionDao.insert(
            IdeaSuggestionEntity(
                ideaId = ideaId,
                text = text.trim(),
                createdAt = currentTimeMillis(),
            ),
        )
        ProgresoRpg(database).actividad()
        id
    }

    suspend fun validate(ideaId: Long, suggestionId: Long): IdeaSuggestionEntity? =
        database.withTransaction {
            val progreso = ProgresoRpg(database)
            progreso.inicializar()
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
            progreso.registrar("validadas", suggestion.id)
            suggestionDao.findById(suggestion.id)
        }

    suspend fun deletePending(ideaId: Long, suggestionId: Long): Boolean =
        database.withTransaction {
            suggestionDao.deletePending(ideaId = ideaId, id = suggestionId) == 1
        }
}
