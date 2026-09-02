package com.mindrop.app.data.repository

import androidx.room.withTransaction
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.dao.FolderDao
import com.mindrop.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderHierarchyException(message: String) : IllegalArgumentException(message)

class FolderHasChildrenException(
    val folderId: Long,
) : IllegalStateException("La carpeta $folderId contiene subcarpetas y no se puede borrar.")

class FolderRepository(
    private val database: MindropDatabase,
    private val folderDao: FolderDao,
) {
    fun observeChildren(parentFolderId: Long?): Flow<List<FolderEntity>> =
        folderDao.observeChildren(parentFolderId)

    suspend fun findById(id: Long): FolderEntity? = folderDao.findById(id)

    suspend fun insert(folder: FolderEntity): Long = database.withTransaction {
        require(folder.id == 0L) { "Una carpeta nueva no puede tener un id asignado." }
        require(folder.name.isNotBlank()) { "El nombre de la carpeta no puede estar vacío." }
        validateParent(folderId = null, parentFolderId = folder.parentFolderId)
        folderDao.insert(folder)
    }

    suspend fun update(folder: FolderEntity): Boolean = database.withTransaction {
        require(folder.id > 0L) { "La carpeta que se actualiza debe tener un id válido." }
        require(folder.name.isNotBlank()) { "El nombre de la carpeta no puede estar vacío." }

        if (folderDao.findById(folder.id) == null) {
            return@withTransaction false
        }

        validateParent(folderId = folder.id, parentFolderId = folder.parentFolderId)
        folderDao.update(folder) == 1
    }

    /**
     * Borra únicamente carpetas sin subcarpetas. Las ideas de la carpeta pasan a la raíz
     * mediante la clave foránea ON DELETE SET NULL, por lo que nunca se eliminan en cascada.
     */
    suspend fun deleteById(id: Long): Boolean = database.withTransaction {
        if (folderDao.findById(id) == null) {
            return@withTransaction false
        }
        if (folderDao.countChildren(id) > 0) {
            throw FolderHasChildrenException(folderId = id)
        }
        folderDao.deleteById(id) == 1
    }

    private suspend fun validateParent(
        folderId: Long?,
        parentFolderId: Long?,
    ) {
        var ancestorId = parentFolderId ?: return
        val visitedIds = mutableSetOf<Long>()

        while (true) {
            if (ancestorId == folderId) {
                throw FolderHierarchyException("Una carpeta no puede ser descendiente de sí misma.")
            }
            if (!visitedIds.add(ancestorId)) {
                throw FolderHierarchyException("La jerarquía de carpetas contiene un ciclo.")
            }

            val ancestor = folderDao.findById(ancestorId)
                ?: throw FolderHierarchyException("La carpeta padre $ancestorId no existe.")
            ancestorId = ancestor.parentFolderId ?: return
        }
    }
}
