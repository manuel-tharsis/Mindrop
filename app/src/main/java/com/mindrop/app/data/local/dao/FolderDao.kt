package com.mindrop.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mindrop.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query(
        """
        SELECT * FROM folders
        WHERE (:parentFolderId IS NULL AND parent_folder_id IS NULL)
           OR parent_folder_id = :parentFolderId
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeChildren(parentFolderId: Long?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FolderEntity?

    @Upsert
    suspend fun save(folder: FolderEntity): Long

    @Delete
    suspend fun delete(folder: FolderEntity)
}
