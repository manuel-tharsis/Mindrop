package com.mindrop.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas WHERE folder_id = :folderId ORDER BY name COLLATE NOCASE")
    fun observeInFolder(folderId: Long): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): IdeaEntity?

    @Upsert
    suspend fun save(idea: IdeaEntity): Long

    @Delete
    suspend fun delete(idea: IdeaEntity)
}
