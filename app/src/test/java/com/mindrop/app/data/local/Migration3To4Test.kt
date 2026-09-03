package com.mindrop.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
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
class Migration3To4Test {
    private val databaseName = "migration-3-4-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationPreservesIdeasAndSuggestionsAndAddsEmptyParent() {
        createVersion3Database()

        val database = Room.databaseBuilder(context, MindropDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_3_4)
            .build()

        try {
            database.openHelper.writableDatabase
            runBlocking {
                val idea = database.ideaDao().findById(1L)
                assertNotNull(idea)
                assertEquals("Idea existente", idea?.title)
                assertNull(idea?.parentIdeaId)
                assertEquals(
                    listOf("Sugerencia conservada"),
                    database.ideaSuggestionDao().findAllForIdea(1L).map { it.text },
                )
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion3Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE folders (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                icon TEXT NOT NULL DEFAULT '',
                                parent_folder_id INTEGER,
                                sort_order INTEGER NOT NULL DEFAULT 0,
                                FOREIGN KEY(parent_folder_id) REFERENCES folders(id)
                                    ON UPDATE NO ACTION ON DELETE RESTRICT
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "CREATE INDEX index_folders_parent_folder_id_sort_order ON folders(parent_folder_id, sort_order)",
                        )
                        db.execSQL(
                            """
                            CREATE TABLE ideas (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL,
                                short_description TEXT NOT NULL,
                                full_description TEXT NOT NULL,
                                icon TEXT NOT NULL,
                                custom_icon_path TEXT,
                                folder_id INTEGER,
                                sort_order INTEGER NOT NULL,
                                created_at INTEGER NOT NULL,
                                updated_at INTEGER NOT NULL,
                                FOREIGN KEY(folder_id) REFERENCES folders(id)
                                    ON UPDATE NO ACTION ON DELETE SET NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "CREATE INDEX index_ideas_folder_id_sort_order ON ideas(folder_id, sort_order)",
                        )
                        db.execSQL(
                            """
                            CREATE TABLE idea_suggestions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                idea_id INTEGER NOT NULL,
                                text TEXT NOT NULL,
                                created_at INTEGER NOT NULL,
                                validated_at INTEGER,
                                update_number INTEGER,
                                FOREIGN KEY(idea_id) REFERENCES ideas(id)
                                    ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "CREATE INDEX index_idea_suggestions_idea_id_validated_at ON idea_suggestions(idea_id, validated_at)",
                        )
                        db.execSQL(
                            "CREATE UNIQUE INDEX index_idea_suggestions_idea_id_update_number ON idea_suggestions(idea_id, update_number)",
                        )
                        db.execSQL(
                            """
                            INSERT INTO ideas (
                                id, title, short_description, full_description, icon,
                                custom_icon_path, folder_id, sort_order, created_at, updated_at
                            ) VALUES (
                                1, 'Idea existente', 'Resumen', 'Descripción', 'idea',
                                NULL, NULL, 0, 1000, 1000
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            INSERT INTO idea_suggestions (
                                id, idea_id, text, created_at, validated_at, update_number
                            ) VALUES (
                                1, 1, 'Sugerencia conservada', 1001, NULL, NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }
}
