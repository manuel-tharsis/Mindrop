package com.mindrop.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mindrop.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query(
        """
        SELECT * FROM folders
        WHERE (:parentFolderId IS NULL AND parent_folder_id IS NULL)
           OR parent_folder_id = :parentFolderId
        ORDER BY sort_order ASC, name COLLATE NOCASE ASC, id ASC
        """,
    )
    fun observeChildren(parentFolderId: Long?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: FolderEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(folder: FolderEntity): Int

    @Query("SELECT COUNT(*) FROM folders WHERE parent_folder_id = :folderId")
    suspend fun countChildren(folderId: Long): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
