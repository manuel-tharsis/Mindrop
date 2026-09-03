package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.ui.home.buildIdeaHierarchy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompletedIdeasRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: MindropDatabase
    private lateinit var folderRepository: FolderRepository
    private lateinit var ideaRepository: IdeaRepository
    private lateinit var suggestionRepository: IdeaSuggestionRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MindropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        folderRepository = FolderRepository(database, database.folderDao())
        ideaRepository = IdeaRepository(database, database.ideaDao()) { now }
        suggestionRepository = IdeaSuggestionRepository(
            database,
            database.ideaSuggestionDao(),
        ) { now }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun newIdeaIsActiveByDefault() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea("Activa"))

        assertFalse(ideaRepository.findById(ideaId)!!.isCompleted)
        assertEquals(listOf(ideaId), ideaRepository.observeInFolder(null).first().map { it.id })
        assertTrue(ideaRepository.observeCompleted().first().isEmpty())
    }

    @Test
    fun completionAndRestorationPreserveFolderAndParentRelations() = runBlocking {
        val folderId = folderRepository.insert(FolderEntity(name = "Proyectos", icon = "folder"))
        val parentId = ideaRepository.insert(newIdea("BioGestor", folderId = folderId))
        val childId = ideaRepository.insert(
            newIdea(
                "APK Montones de Jara",
                folderId = folderId,
                parentIdeaId = parentId,
            ),
        )

        assertTrue(ideaRepository.setCompleted(childId, true))
        val completedChild = ideaRepository.findById(childId)!!
        assertTrue(completedChild.isCompleted)
        assertEquals(folderId, completedChild.folderId)
        assertEquals(parentId, completedChild.parentIdeaId)
        assertEquals(listOf(parentId), ideaRepository.observeInFolder(folderId).first().map { it.id })
        assertEquals(listOf(childId), ideaRepository.observeCompleted().first().map { it.id })

        now = 2_000L
        assertTrue(ideaRepository.setCompleted(childId, false))
        val restoredChild = ideaRepository.findById(childId)!!
        assertFalse(restoredChild.isCompleted)
        assertEquals(folderId, restoredChild.folderId)
        assertEquals(parentId, restoredChild.parentIdeaId)
        assertEquals(
            listOf("BioGestor", "APK Montones de Jara"),
            buildIdeaHierarchy(ideaRepository.observeInFolder(folderId).first())
                .map { it.idea.title },
        )
    }

    @Test
    fun activeChildRemainsVisibleWhenItsParentIsCompleted() = runBlocking {
        val parentId = ideaRepository.insert(newIdea("Padre"))
        val childId = ideaRepository.insert(newIdea("Subidea activa", parentIdeaId = parentId))

        ideaRepository.setCompleted(parentId, true)

        val visibleItems = buildIdeaHierarchy(ideaRepository.observeInFolder(null).first())
        assertEquals(listOf(childId), visibleItems.map { it.idea.id })
        assertEquals(0, visibleItems.single().depth)
        assertEquals(parentId, ideaRepository.findById(childId)?.parentIdeaId)
    }

    @Test
    fun completedFolderIdeasAreHiddenFromSummaryButStillPreventFolderDeletion() = runBlocking {
        val folderId = folderRepository.insert(FolderEntity(name = "Archivo", icon = "folder"))
        val ideaId = ideaRepository.insert(newIdea("Terminada", folderId = folderId))
        ideaRepository.setCompleted(ideaId, true)

        val summary = folderRepository.observeChildSummaries(null).first().single()
        assertEquals(0, summary.ideaCount)
        assertTrue(runCatching { folderRepository.deleteById(folderId) }.exceptionOrNull() is FolderNotEmptyException)
        assertEquals(folderId, ideaRepository.findById(ideaId)?.folderId)
    }

    @Test
    fun agreedV110FlowKeepsUpdatesHierarchyAndLocation() = runBlocking {
        val folderId = folderRepository.insert(FolderEntity(name = "Proyectos", icon = "folder"))
        val bioGestorId = ideaRepository.insert(newIdea("BioGestor", folderId = folderId))
        val apkId = ideaRepository.insert(
            newIdea(
                "APK Montones de Jara",
                folderId = folderId,
                parentIdeaId = bioGestorId,
            ),
        )
        assertEquals(
            listOf("BioGestor" to 0, "APK Montones de Jara" to 1),
            buildIdeaHierarchy(ideaRepository.observeInFolder(folderId).first())
                .map { it.idea.title to it.depth },
        )

        val firstSuggestionId = suggestionRepository.addPending(
            bioGestorId,
            "Añadir control de disolventes",
        )
        suggestionRepository.validate(bioGestorId, firstSuggestionId)
        val secondSuggestionId = suggestionRepository.addPending(
            bioGestorId,
            "Añadir módulo de horas",
        )
        suggestionRepository.validate(bioGestorId, secondSuggestionId)
        assertEquals(
            listOf(
                1 to "Añadir control de disolventes",
                2 to "Añadir módulo de horas",
            ),
            suggestionRepository.observeUpdates(bioGestorId).first()
                .map { it.updateNumber to it.text },
        )

        ideaRepository.setCompleted(bioGestorId, true)
        assertEquals(listOf(apkId), ideaRepository.observeInFolder(folderId).first().map { it.id })
        assertEquals(listOf(bioGestorId), ideaRepository.observeCompleted().first().map { it.id })
        assertEquals(2, suggestionRepository.observeUpdates(bioGestorId).first().size)

        ideaRepository.setCompleted(bioGestorId, false)
        val restored = ideaRepository.findById(bioGestorId)!!
        assertFalse(restored.isCompleted)
        assertEquals(folderId, restored.folderId)
        assertNull(restored.parentIdeaId)
        assertEquals(bioGestorId, ideaRepository.findById(apkId)?.parentIdeaId)
        assertEquals(
            listOf("BioGestor", "APK Montones de Jara"),
            buildIdeaHierarchy(ideaRepository.observeInFolder(folderId).first())
                .map { it.idea.title },
        )
    }

    @Test
    fun completedStatePersistsAfterDatabaseRestart() = runBlocking {
        val databaseName = "completed-restart-test.db"
        context.deleteDatabase(databaseName)
        var persistentDatabase = openPersistentDatabase(databaseName)

        try {
            var ideas = IdeaRepository(persistentDatabase, persistentDatabase.ideaDao())
            val ideaId = ideas.insert(newIdea("Persistente", folderId = null))
            ideas.setCompleted(ideaId, true)
            persistentDatabase.close()

            persistentDatabase = openPersistentDatabase(databaseName)
            ideas = IdeaRepository(persistentDatabase, persistentDatabase.ideaDao())

            assertTrue(ideas.findById(ideaId)!!.isCompleted)
            assertTrue(ideas.observeInFolder(null).first().isEmpty())
            assertEquals(listOf(ideaId), ideas.observeCompleted().first().map { it.id })
        } finally {
            persistentDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun openPersistentDatabase(name: String): MindropDatabase =
        Room.databaseBuilder(context, MindropDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(
                com.mindrop.app.data.local.MIGRATION_1_2,
                com.mindrop.app.data.local.MIGRATION_2_3,
                com.mindrop.app.data.local.MIGRATION_3_4,
                com.mindrop.app.data.local.MIGRATION_4_5,
            )
            .build()

    private fun newIdea(
        title: String,
        folderId: Long? = null,
        parentIdeaId: Long? = null,
    ) = IdeaEntity(
        title = title,
        shortDescription = "",
        fullDescription = "",
        icon = "idea",
        folderId = folderId,
        parentIdeaId = parentIdeaId,
    )
}
