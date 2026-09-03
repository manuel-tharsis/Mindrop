package com.mindrop.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaSuggestionDao {
    @Query(
        """
        SELECT * FROM idea_suggestions
        WHERE idea_id = :ideaId AND validated_at IS NULL
        ORDER BY created_at ASC, id ASC
        """,
    )
    fun observePending(ideaId: Long): Flow<List<IdeaSuggestionEntity>>

    @Query(
        """
        SELECT * FROM idea_suggestions
        WHERE idea_id = :ideaId
          AND validated_at IS NOT NULL
          AND update_number IS NOT NULL
        ORDER BY update_number ASC, id ASC
        """,
    )
    fun observeUpdates(ideaId: Long): Flow<List<IdeaSuggestionEntity>>

    @Query("SELECT * FROM idea_suggestions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): IdeaSuggestionEntity?

    @Query(
        """
        SELECT * FROM idea_suggestions
        WHERE idea_id = :ideaId
        ORDER BY created_at ASC, id ASC
        """,
    )
    suspend fun findAllForIdea(ideaId: Long): List<IdeaSuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(suggestion: IdeaSuggestionEntity): Long

    @Query(
        """
        SELECT COALESCE(MAX(update_number), 0)
        FROM idea_suggestions
        WHERE idea_id = :ideaId AND validated_at IS NOT NULL
        """,
    )
    suspend fun maxUpdateNumber(ideaId: Long): Int

    @Query(
        """
        UPDATE idea_suggestions
        SET validated_at = :validatedAt, update_number = :updateNumber
        WHERE id = :id AND validated_at IS NULL
        """,
    )
    suspend fun validatePending(
        id: Long,
        validatedAt: Long,
        updateNumber: Int,
    ): Int
}
