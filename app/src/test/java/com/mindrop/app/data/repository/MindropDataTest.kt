package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MindropDataTest {
    private lateinit var database: MindropDatabase
    private lateinit var folderRepository: FolderRepository
    private lateinit var ideaRepository: IdeaRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        folderRepository = FolderRepository(database, database.folderDao())
        ideaRepository = IdeaRepository(database, database.ideaDao()) { now }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun childrenAndIdeasUseStableOrderAndSupportRoot() = runBlocking {
        val projectsId = folderRepository.insert(
            FolderEntity(name = "Proyectos", icon = "folder", sortOrder = 2),
        )
        folderRepository.insert(
            FolderEntity(name = "Android", icon = "android", sortOrder = 1),
        )
        folderRepository.insert(
            FolderEntity(name = "Programación", icon = "code", sortOrder = 1),
        )
        folderRepository.insert(
            FolderEntity(
                name = "Mindrop",
                icon = "idea",
                parentFolderId = projectsId,
                sortOrder = 0,
            ),
        )

        ideaRepository.insert(newIdea(title = "Segunda", sortOrder = 2))
        ideaRepository.insert(newIdea(title = "Beta", sortOrder = 1))
        ideaRepository.insert(newIdea(title = "Alfa", sortOrder = 1))
        ideaRepository.insert(
            newIdea(title = "Dentro de proyectos", folderId = projectsId, sortOrder = 0),
        )

        assertEquals(
            listOf("Android", "Programación", "Proyectos"),
            folderRepository.observeChildren(parentFolderId = null).first().map { it.name },
        )
        assertEquals(
            listOf("Mindrop"),
            folderRepository.observeChildren(parentFolderId = projectsId).first().map { it.name },
        )
        val projectsSummary = folderRepository.observeChildSummaries(parentFolderId = null)
            .first()
            .single { it.folder.id == projectsId }
        assertEquals(1, projectsSummary.ideaCount)
        assertEquals(1, projectsSummary.childFolderCount)
        assertEquals(2, projectsSummary.itemCount)
        assertEquals(
            listOf("Alfa", "Beta", "Segunda"),
            ideaRepository.observeInFolder(folderId = null).first().map { it.title },
        )
        assertEquals(
            listOf("Dentro de proyectos"),
            ideaRepository.observeInFolder(folderId = projectsId).first().map { it.title },
        )
    }

    @Test
    fun folderRepositoryRejectsSelfAndDescendantCycles() = runBlocking {
        val rootId = folderRepository.insert(FolderEntity(name = "Raíz", icon = "folder"))
        val childId = folderRepository.insert(
            FolderEntity(name = "Hija", icon = "folder", parentFolderId = rootId),
        )

        val descendantCycle = runCatching {
            folderRepository.update(
                folderRepository.findById(rootId)!!.copy(parentFolderId = childId),
            )
        }.exceptionOrNull()
        val selfCycle = runCatching {
            folderRepository.update(
                folderRepository.findById(childId)!!.copy(parentFolderId = childId),
            )
        }.exceptionOrNull()

        assertTrue(descendantCycle is FolderHierarchyException)
        assertTrue(selfCycle is FolderHierarchyException)

        assertNull(folderRepository.findById(rootId)!!.parentFolderId)
        assertEquals(rootId, folderRepository.findById(childId)!!.parentFolderId)
    }

    @Test
    fun deletingFolderWithChildrenIsRejected() = runBlocking {
        val rootId = folderRepository.insert(FolderEntity(name = "Raíz", icon = "folder"))
        folderRepository.insert(
            FolderEntity(name = "Hija", icon = "folder", parentFolderId = rootId),
        )

        val failure = runCatching { folderRepository.deleteById(rootId) }.exceptionOrNull()

        assertTrue(failure is FolderHasChildrenException)
        assertNotNull(folderRepository.findById(rootId))
    }

    @Test
    fun deletingLeafFolderMovesItsIdeasToRootWithoutDeletingThem() = runBlocking {
        val folderId = folderRepository.insert(FolderEntity(name = "Temporal", icon = "folder"))
        val ideaId = ideaRepository.insert(
            newIdea(title = "Idea conservada", folderId = folderId),
        )

        assertTrue(folderRepository.deleteById(folderId))

        assertNull(folderRepository.findById(folderId))
        assertEquals(ideaId, ideaRepository.findById(ideaId)?.id)
        assertNull(ideaRepository.findById(ideaId)?.folderId)
        assertEquals(
            listOf("Idea conservada"),
            ideaRepository.observeInFolder(folderId = null).first().map { it.title },
        )
    }

    @Test
    fun ideaRepositoryMaintainsCreationTimeAndAdvancesUpdateTime() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea(title = "Original"))
        val inserted = ideaRepository.findById(ideaId)!!
        assertEquals(1_000L, inserted.createdAt)
        assertEquals(1_000L, inserted.updatedAt)

        now = 2_000L
        assertTrue(ideaRepository.update(inserted.copy(title = "Actualizada", createdAt = 99L)))

        val updated = ideaRepository.findById(ideaId)!!
        assertEquals("Actualizada", updated.title)
        assertEquals(1_000L, updated.createdAt)
        assertEquals(2_000L, updated.updatedAt)
    }

    private fun newIdea(
        title: String,
        folderId: Long? = null,
        sortOrder: Long = 0,
    ) = IdeaEntity(
        title = title,
        shortDescription = "Resumen",
        fullDescription = "Descripción completa",
        icon = "idea",
        customIconPath = null,
        folderId = folderId,
        sortOrder = sortOrder,
    )
}
