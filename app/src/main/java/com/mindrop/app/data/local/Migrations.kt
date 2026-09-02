package com.mindrop.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE folders ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
        db.execSQL("DROP INDEX IF EXISTS index_folders_parent_folder_id")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_folders_parent_folder_id_sort_order
            ON folders(parent_folder_id, sort_order)
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ideas_new (
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
                FOREIGN KEY(folder_id) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO ideas_new (
                id,
                title,
                short_description,
                full_description,
                icon,
                custom_icon_path,
                folder_id,
                sort_order,
                created_at,
                updated_at
            )
            SELECT
                id,
                name,
                short_description,
                full_description,
                icon,
                NULL,
                folder_id,
                0,
                CAST(strftime('%s', 'now') AS INTEGER) * 1000,
                CAST(strftime('%s', 'now') AS INTEGER) * 1000
            FROM ideas
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE ideas")
        db.execSQL("ALTER TABLE ideas_new RENAME TO ideas")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_ideas_folder_id_sort_order
            ON ideas(folder_id, sort_order)
            """.trimIndent(),
        )
    }
}
