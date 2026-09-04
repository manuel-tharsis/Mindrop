package com.mindrop.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY sort_order ASC, title COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<IdeaEntity>>

    @Query(
        """
        SELECT * FROM ideas
        WHERE is_completed = 0
          AND (
              (:folderId IS NULL AND folder_id IS NULL)
              OR folder_id = :folderId
          )
        ORDER BY sort_order ASC, title COLLATE NOCASE ASC, id ASC
        """,
    )
    fun observeInFolder(folderId: Long?): Flow<List<IdeaEntity>>

    @Query(
        """
        SELECT * FROM ideas
        WHERE is_completed = 1
        ORDER BY sort_order ASC, title COLLATE NOCASE ASC, id ASC
        """,
    )
    fun observeCompleted(): Flow<List<IdeaEntity>>

    @Query("SELECT COUNT(*) FROM ideas WHERE is_completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT * FROM ideas WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<IdeaEntity?>

    @Query("SELECT * FROM ideas WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): IdeaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(idea: IdeaEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(idea: IdeaEntity): Int

    @Query(
        """
        UPDATE ideas
        SET is_completed = :isCompleted, updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun setCompleted(id: Long, isCompleted: Boolean, updatedAt: Long): Int

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
