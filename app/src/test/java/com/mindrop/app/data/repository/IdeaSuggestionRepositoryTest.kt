package com.mindrop.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mindrop.app.data.local.MIGRATION_1_2
import com.mindrop.app.data.local.MIGRATION_2_3
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.local.entity.IdeaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IdeaSuggestionRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: MindropDatabase
    private lateinit var ideaRepository: IdeaRepository
    private lateinit var suggestionRepository: IdeaSuggestionRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MindropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
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
    fun creatingSuggestionStoresItAsPending() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea("Idea"))

        val suggestionId = suggestionRepository.addPending(ideaId, "  Añadir búsqueda  ")

        val suggestion = suggestionRepository.observePending(ideaId).first().single()
        assertEquals(suggestionId, suggestion.id)
        assertEquals("Añadir búsqueda", suggestion.text)
        assertEquals(1_000L, suggestion.createdAt)
        assertNull(suggestion.validatedAt)
        assertNull(suggestion.updateNumber)
    }

    @Test
    fun validatingSuggestionsCreatesSequentialUpdates() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea("Idea"))
        val suggestionIds = listOf("Primera", "Segunda", "Tercera").map { text ->
            suggestionRepository.addPending(ideaId, text)
        }

        suggestionIds.forEachIndexed { index, suggestionId ->
            now = 2_000L + index
            val validated = suggestionRepository.validate(ideaId, suggestionId)
            assertEquals(index + 1, validated?.updateNumber)
            assertNotNull(validated?.validatedAt)
        }

        assertEquals(emptyList<Any>(), suggestionRepository.observePending(ideaId).first())
        assertEquals(
            listOf(1, 2, 3),
            suggestionRepository.observeUpdates(ideaId).first().map { it.updateNumber },
        )
    }

    @Test
    fun differentIdeasKeepIndependentUpdateSequences() = runBlocking {
        val firstIdeaId = ideaRepository.insert(newIdea("Primera idea"))
        val secondIdeaId = ideaRepository.insert(newIdea("Segunda idea"))
        val firstIds = listOf("A", "B").map {
            suggestionRepository.addPending(firstIdeaId, it)
        }
        val secondId = suggestionRepository.addPending(secondIdeaId, "C")

        firstIds.forEach { suggestionRepository.validate(firstIdeaId, it) }
        suggestionRepository.validate(secondIdeaId, secondId)

        assertEquals(
            listOf(1, 2),
            suggestionRepository.observeUpdates(firstIdeaId).first().map { it.updateNumber },
        )
        assertEquals(
            listOf(1),
            suggestionRepository.observeUpdates(secondIdeaId).first().map { it.updateNumber },
        )
    }

    @Test
    fun concurrentValidationsNeverDuplicateUpdateNumbers() = runBlocking {
        val ideaId = ideaRepository.insert(newIdea("Concurrente"))
        val ids = (1..8).map { number ->
            suggestionRepository.addPending(ideaId, "Sugerencia $number")
        }

        coroutineScope {
            ids.map { suggestionId ->
                async(Dispatchers.Default) {
                    suggestionRepository.validate(ideaId, suggestionId)
                }
            }.awaitAll()
        }

        assertEquals(
            (1..8).toList(),
            suggestionRepository.observeUpdates(ideaId).first().mapNotNull { it.updateNumber },
        )
    }

    @Test
    fun suggestionsAndUpdatesPersistAfterDatabaseRestart() = runBlocking {
        val databaseName = "suggestion-restart-test.db"
        context.deleteDatabase(databaseName)
        var persistentDatabase = openPersistentDatabase(databaseName)

        try {
            val ideas = IdeaRepository(persistentDatabase, persistentDatabase.ideaDao()) { now }
            val suggestions = IdeaSuggestionRepository(
                persistentDatabase,
                persistentDatabase.ideaSuggestionDao(),
            ) { now }
            val ideaId = ideas.insert(
                newIdea("Persistente"),
                pendingSuggestionTexts = listOf("Validada", "Pendiente"),
            )
            val firstSuggestion = suggestions.observePending(ideaId).first().first()
            suggestions.validate(ideaId, firstSuggestion.id)
            persistentDatabase.close()

            persistentDatabase = openPersistentDatabase(databaseName)
            val reopenedSuggestions = IdeaSuggestionRepository(
                persistentDatabase,
                persistentDatabase.ideaSuggestionDao(),
            )

            assertEquals(
                listOf("Pendiente"),
                reopenedSuggestions.observePending(ideaId).first().map { it.text },
            )
            assertEquals(
                listOf(1),
                reopenedSuggestions.observeUpdates(ideaId).first().map { it.updateNumber },
            )
        } finally {
            persistentDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun deletingIdeaAlsoDeletesItsSuggestionsAndUpdates() = runBlocking {
        val ideaId = ideaRepository.insert(
            newIdea("Temporal"),
            pendingSuggestionTexts = listOf("Validada", "Pendiente"),
        )
        val firstSuggestion = suggestionRepository.observePending(ideaId).first().first()
        suggestionRepository.validate(ideaId, firstSuggestion.id)

        ideaRepository.deleteById(ideaId)

        assertEquals(emptyList<Any>(), database.ideaSuggestionDao().findAllForIdea(ideaId))
    }

    private fun openPersistentDatabase(name: String): MindropDatabase =
        Room.databaseBuilder(context, MindropDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    private fun newIdea(title: String) = IdeaEntity(
        title = title,
        shortDescription = "",
        fullDescription = "",
        icon = "idea",
    )
}
