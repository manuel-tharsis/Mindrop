package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.IdeaEntity
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
class IdeaHierarchyRepositoryTest {
    private lateinit var database: MindropDatabase
    private lateinit var repository: IdeaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MindropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = IdeaRepository(database, database.ideaDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun ideaWithoutParentIsStoredAtHierarchyRoot() = runBlocking {
        val ideaId = repository.insert(newIdea("BioGestor"))

        assertNull(repository.findById(ideaId)?.parentIdeaId)
    }

    @Test
    fun ideaCanDeriveFromAnotherIdea() = runBlocking {
        val parentId = repository.insert(newIdea("BioGestor"))
        val childId = repository.insert(
            newIdea("APK Montones de Jara", parentIdeaId = parentId),
        )

        assertEquals(parentId, repository.findById(childId)?.parentIdeaId)
    }

    @Test
    fun hierarchySupportsSeveralLevels() = runBlocking {
        val rootId = repository.insert(newIdea("A"))
        val childId = repository.insert(newIdea("B", parentIdeaId = rootId))
        val grandchildId = repository.insert(newIdea("C", parentIdeaId = childId))

        assertNull(repository.findById(rootId)?.parentIdeaId)
        assertEquals(rootId, repository.findById(childId)?.parentIdeaId)
        assertEquals(childId, repository.findById(grandchildId)?.parentIdeaId)
    }

    @Test
    fun ideaCannotBeItsOwnParent() = runBlocking {
        val ideaId = repository.insert(newIdea("A"))
        val idea = repository.findById(ideaId)!!

        val failure = runCatching {
            repository.update(idea.copy(parentIdeaId = ideaId))
        }.exceptionOrNull()

        assertTrue(failure is IdeaHierarchyException)
        assertNull(repository.findById(ideaId)?.parentIdeaId)
    }

    @Test
    fun ideaCannotBeMovedBelowAnIndirectDescendant() = runBlocking {
        val rootId = repository.insert(newIdea("A"))
        val childId = repository.insert(newIdea("B", parentIdeaId = rootId))
        val grandchildId = repository.insert(newIdea("C", parentIdeaId = childId))
        val root = repository.findById(rootId)!!

        val failure = runCatching {
            repository.update(root.copy(parentIdeaId = grandchildId))
        }.exceptionOrNull()

        assertTrue(failure is IdeaHierarchyException)
        assertNull(repository.findById(rootId)?.parentIdeaId)
        assertEquals(rootId, repository.findById(childId)?.parentIdeaId)
        assertEquals(childId, repository.findById(grandchildId)?.parentIdeaId)
    }

    @Test
    fun deletingParentPreservesSubideasAndMovesThemToHierarchyRoot() = runBlocking {
        val parentId = repository.insert(newIdea("Padre"))
        val childId = repository.insert(newIdea("Subidea", parentIdeaId = parentId))
        val grandchildId = repository.insert(newIdea("Subidea anidada", parentIdeaId = childId))

        assertTrue(repository.deleteById(parentId))

        assertNull(repository.findById(parentId))
        val preservedChild = repository.findById(childId)
        assertNotNull(preservedChild)
        assertNull(preservedChild?.parentIdeaId)
        assertEquals(childId, repository.findById(grandchildId)?.parentIdeaId)
    }

    private fun newIdea(title: String, parentIdeaId: Long? = null) = IdeaEntity(
        title = title,
        shortDescription = "",
        fullDescription = "",
        icon = "idea",
        parentIdeaId = parentIdeaId,
    )
}
