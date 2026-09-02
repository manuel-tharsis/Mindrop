package com.mindrop.app.data.repository

import com.mindrop.app.data.local.dao.FolderDao
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class FolderContents(
    val folders: List<FolderEntity> = emptyList(),
    val ideas: List<IdeaEntity> = emptyList(),
)

class MindropRepository(
    private val folderDao: FolderDao,
    private val ideaDao: IdeaDao,
) {
    fun observeFolderContents(folderId: Long?): Flow<FolderContents> {
        val folders = folderDao.observeChildren(folderId)
        val ideas = if (folderId == null) {
            flowOf(emptyList())
        } else {
            ideaDao.observeInFolder(folderId)
        }

        return combine(folders, ideas) { childFolders, folderIdeas ->
            FolderContents(
                folders = childFolders,
                ideas = folderIdeas,
            )
        }
    }

    suspend fun findFolder(id: Long): FolderEntity? = folderDao.findById(id)

    suspend fun saveFolder(folder: FolderEntity): Long = folderDao.save(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.delete(folder)

    suspend fun findIdea(id: Long): IdeaEntity? = ideaDao.findById(id)

    suspend fun saveIdea(idea: IdeaEntity): Long = ideaDao.save(idea)

    suspend fun deleteIdea(idea: IdeaEntity) = ideaDao.delete(idea)
}
