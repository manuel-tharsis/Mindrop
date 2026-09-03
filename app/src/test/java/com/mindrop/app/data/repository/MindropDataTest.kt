package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.icon.CustomIconFileStore
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
        val grandchildId = folderRepository.insert(
            FolderEntity(name = "Nieta", icon = "folder", parentFolderId = childId),
        )

        val descendantCycle = runCatching {
            folderRepository.moveToFolder(rootId, grandchildId)
        }.exceptionOrNull()
        val selfCycle = runCatching {
            folderRepository.moveToFolder(childId, childId)
        }.exceptionOrNull()

        assertTrue(descendantCycle is FolderHierarchyException)
        assertTrue(selfCycle is FolderHierarchyException)

        assertNull(folderRepository.findById(rootId)!!.parentFolderId)
        assertEquals(rootId, folderRepository.findById(childId)!!.parentFolderId)
        assertEquals(childId, folderRepository.findById(grandchildId)!!.parentFolderId)
    }

    @Test
    fun movingFolderSupportsOtherParentsAndRoot() = runBlocking {
        val firstParentId = folderRepository.insert(
            FolderEntity(name = "Primera", icon = "folder"),
        )
        val secondParentId = folderRepository.insert(
            FolderEntity(name = "Segunda", icon = "folder"),
        )
        val folderId = folderRepository.insert(
            FolderEntity(name = "Movible", icon = "folder", parentFolderId = firstParentId),
        )

        assertTrue(folderRepository.moveToFolder(folderId, secondParentId))
        assertEquals(secondParentId, folderRepository.findById(folderId)?.parentFolderId)

        assertTrue(folderRepository.moveToFolder(folderId, null))
        assertNull(folderRepository.findById(folderId)?.parentFolderId)
    }

    @Test
    fun movingIdeaSupportsAnyFolderAndRoot() = runBlocking {
        val firstFolderId = folderRepository.insert(
            FolderEntity(name = "Primera", icon = "folder"),
        )
        val secondFolderId = folderRepository.insert(
            FolderEntity(name = "Segunda", icon = "folder"),
        )
        val ideaId = ideaRepository.insert(newIdea(title = "Movible"))

        assertTrue(ideaRepository.moveToFolder(ideaId, firstFolderId))
        assertEquals(firstFolderId, ideaRepository.findById(ideaId)?.folderId)

        assertTrue(ideaRepository.moveToFolder(ideaId, secondFolderId))
        assertEquals(secondFolderId, ideaRepository.findById(ideaId)?.folderId)

        assertTrue(ideaRepository.moveToFolder(ideaId, null))
        assertNull(ideaRepository.findById(ideaId)?.folderId)
    }

    @Test
    fun deletingNonEmptyFolderIsRejectedWithoutMovingOrDeletingContent() = runBlocking {
        val rootId = folderRepository.insert(FolderEntity(name = "Raíz", icon = "folder"))
        val childId = folderRepository.insert(
            FolderEntity(name = "Hija", icon = "folder", parentFolderId = rootId),
        )
        val ideaId = ideaRepository.insert(
            newIdea(title = "Idea conservada", folderId = rootId),
        )

        val failure = runCatching { folderRepository.deleteById(rootId) }.exceptionOrNull()

        assertTrue(failure is FolderNotEmptyException)
        failure as FolderNotEmptyException
        assertEquals(1, failure.ideaCount)
        assertEquals(1, failure.childFolderCount)
        assertNotNull(folderRepository.findById(rootId))
        assertEquals(rootId, folderRepository.findById(childId)?.parentFolderId)
        assertEquals(rootId, ideaRepository.findById(ideaId)?.folderId)
    }

    @Test
    fun deletingEmptyFolderRemovesOnlyThatFolder() = runBlocking {
        val folderId = folderRepository.insert(FolderEntity(name = "Temporal", icon = "folder"))

        assertTrue(folderRepository.deleteById(folderId))

        assertNull(folderRepository.findById(folderId))
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

    @Test
    fun observingIdeaDetailPreservesEmptyFieldsNewLinesAndLongText() = runBlocking {
        val fullDescription = buildString {
            repeat(150) { line ->
                append("Línea ")
                append(line)
                append(" con contenido para comprobar el desplazamiento.\n")
            }
        }
        val ideaId = ideaRepository.insert(
            newIdea(title = "Detalle extenso").copy(
                shortDescription = "",
                fullDescription = fullDescription,
                icon = "document",
            ),
        )

        val observed = ideaRepository.observeById(ideaId).first()

        assertEquals("", observed?.shortDescription)
        assertEquals(fullDescription, observed?.fullDescription)
        assertEquals("document", observed?.icon)
    }

    @Test
    fun observingIdeaDetailReflectsDeletion() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea(title = "Para eliminar"))

        assertNotNull(ideaRepository.observeById(ideaId).first())
        assertTrue(ideaRepository.deleteById(ideaId))
        assertNull(ideaRepository.observeById(ideaId).first())
    }

    @Test
    fun folderCanBeCreatedAndEditedWithAParent() = runBlocking {
        val parentId = folderRepository.insert(
            FolderEntity(name = "Proyectos", icon = "folder"),
        )
        val folderId = folderRepository.insert(
            FolderEntity(name = "Borrador", icon = "idea"),
        )

        val original = folderRepository.findById(folderId)!!
        assertTrue(
            folderRepository.update(
                original.copy(
                    name = "Mindrop",
                    icon = "code",
                    parentFolderId = parentId,
                ),
            ),
        )

        val updated = folderRepository.findById(folderId)!!
        assertEquals("Mindrop", updated.name)
        assertEquals("code", updated.icon)
        assertEquals(parentId, updated.parentFolderId)
    }

    @Test
    fun everyFolderLevelReturnsOnlyItsDirectFoldersAndIdeas() = runBlocking {
        val programmingId = folderRepository.insert(
            FolderEntity(name = "Programación", icon = "folder"),
        )
        val androidId = folderRepository.insert(
            FolderEntity(
                name = "Android",
                icon = "folder",
                parentFolderId = programmingId,
            ),
        )
        val applicationsId = folderRepository.insert(
            FolderEntity(
                name = "Aplicaciones",
                icon = "folder",
                parentFolderId = androidId,
            ),
        )
        ideaRepository.insert(newIdea(title = "Idea Android", folderId = androidId))
        ideaRepository.insert(newIdea(title = "Idea aplicación", folderId = applicationsId))

        assertEquals(
            listOf("Android"),
            folderRepository.observeChildren(programmingId).first().map { it.name },
        )
        assertEquals(
            listOf("Aplicaciones"),
            folderRepository.observeChildren(androidId).first().map { it.name },
        )
        assertEquals(
            listOf("Idea Android"),
            ideaRepository.observeInFolder(androidId).first().map { it.title },
        )
        assertEquals(
            listOf("Idea aplicación"),
            ideaRepository.observeInFolder(applicationsId).first().map { it.title },
        )
    }

    @Test
    fun replacingOrRemovingCustomIconDeletesTheObsoleteFileReference() = runBlocking {
        val fileStore = RecordingIconFileStore()
        val repository = IdeaRepository(
            database = database,
            ideaDao = database.ideaDao(),
            customIconFileStore = fileStore,
        ) { now }
        val ideaId = repository.insert(
            newIdea(title = "Personalizada").copy(customIconPath = "old.png"),
        )

        assertTrue(
            repository.update(
                repository.findById(ideaId)!!.copy(customIconPath = "new.png"),
            ),
        )

        assertEquals(listOf("old.png"), fileStore.deletedPaths)
        assertEquals("new.png", repository.findById(ideaId)?.customIconPath)

        assertTrue(
            repository.update(
                repository.findById(ideaId)!!.copy(customIconPath = null, icon = "star"),
            ),
        )
        assertEquals(listOf("old.png", "new.png"), fileStore.deletedPaths)
        assertNull(repository.findById(ideaId)?.customIconPath)
    }

    @Test
    fun deletingIdeaAlsoDeletesItsCustomIcon() = runBlocking {
        val fileStore = RecordingIconFileStore()
        val repository = IdeaRepository(
            database = database,
            ideaDao = database.ideaDao(),
            customIconFileStore = fileStore,
        ) { now }
        val ideaId = repository.insert(
            newIdea(title = "Temporal").copy(customIconPath = "custom.png"),
        )

        assertTrue(repository.deleteById(ideaId))

        assertEquals(listOf("custom.png"), fileStore.deletedPaths)
        assertNull(repository.findById(ideaId))
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

    private class RecordingIconFileStore : CustomIconFileStore {
        val deletedPaths = mutableListOf<String>()

        override fun delete(path: String): Boolean {
            deletedPaths += path
            return true
        }
    }
}
