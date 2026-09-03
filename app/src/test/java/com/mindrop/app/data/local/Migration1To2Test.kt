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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration1To2Test {
    private val databaseName = "migration-1-2-test.db"
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
    fun migrationPreservesExistingFoldersAndIdeas() {
        createVersion1Database()

        val database = Room.databaseBuilder(context, MindropDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

        try {
            database.openHelper.writableDatabase

            runBlocking {
                val folder = database.folderDao().findById(1L)!!
                assertEquals("Programación", folder.name)
                assertEquals("", folder.icon)
                assertEquals(0L, folder.sortOrder)

                val idea = database.ideaDao().findById(1L)!!
                assertEquals("Idea anterior", idea.title)
                assertEquals(1L, idea.folderId)
                assertNull(idea.customIconPath)
                assertNull(idea.parentIdeaId)
                assertEquals(0L, idea.sortOrder)
                assertTrue(idea.createdAt > 0L)
                assertEquals(idea.createdAt, idea.updatedAt)
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion1Database() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE folders (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                parent_folder_id INTEGER,
                                FOREIGN KEY(parent_folder_id) REFERENCES folders(id)
                                    ON UPDATE NO ACTION ON DELETE RESTRICT
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "CREATE INDEX index_folders_parent_folder_id ON folders(parent_folder_id)",
                        )
                        db.execSQL(
                            """
                            CREATE TABLE ideas (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                short_description TEXT NOT NULL,
                                full_description TEXT NOT NULL,
                                icon TEXT NOT NULL,
                                folder_id INTEGER NOT NULL,
                                FOREIGN KEY(folder_id) REFERENCES folders(id)
                                    ON UPDATE NO ACTION ON DELETE RESTRICT
                            )
                            """.trimIndent(),
                        )
                        db.execSQL("CREATE INDEX index_ideas_folder_id ON ideas(folder_id)")
                        db.execSQL(
                            "INSERT INTO folders (id, name, parent_folder_id) VALUES (1, 'Programación', NULL)",
                        )
                        db.execSQL(
                            """
                            INSERT INTO ideas (
                                id,
                                name,
                                short_description,
                                full_description,
                                icon,
                                folder_id
                            ) VALUES (
                                1,
                                'Idea anterior',
                                'Resumen',
                                'Descripción',
                                'idea',
                                1
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
