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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS idea_suggestions (
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
            """
            CREATE INDEX IF NOT EXISTS index_idea_suggestions_idea_id_validated_at
            ON idea_suggestions(idea_id, validated_at)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_idea_suggestions_idea_id_update_number
            ON idea_suggestions(idea_id, update_number)
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ideas ADD COLUMN parent_idea_id INTEGER REFERENCES ideas(id) ON UPDATE NO ACTION ON DELETE SET NULL",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_ideas_parent_idea_id ON ideas(parent_idea_id)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ideas ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS eventos (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, titulo TEXT NOT NULL,
            descripcion TEXT NOT NULL, fecha TEXT NOT NULL, minutoDelDia INTEGER,
            ideaId INTEGER, minutosAntes INTEGER,
            FOREIGN KEY(ideaId) REFERENCES ideas(id) ON UPDATE NO ACTION ON DELETE SET NULL)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_eventos_fecha ON eventos(fecha)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_eventos_ideaId ON eventos(ideaId)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS recordatorios (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ideaId INTEGER, eventoId INTEGER,
            fechaHora TEXT NOT NULL, revision INTEGER NOT NULL, enviado INTEGER NOT NULL,
            FOREIGN KEY(ideaId) REFERENCES ideas(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(eventoId) REFERENCES eventos(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recordatorios_ideaId ON recordatorios(ideaId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recordatorios_eventoId ON recordatorios(eventoId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS logros (clave TEXT NOT NULL PRIMARY KEY, instante INTEGER NOT NULL, celebrado INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS acciones_rpg (clave TEXT NOT NULL PRIMARY KEY)")
        db.execSQL("CREATE TABLE IF NOT EXISTS metricas_rpg (clave TEXT NOT NULL PRIMARY KEY, valor INTEGER NOT NULL)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS revisiones_ideas (
            ideaId INTEGER NOT NULL PRIMARY KEY, revisada INTEGER NOT NULL, sugerida INTEGER NOT NULL,
            FOREIGN KEY(ideaId) REFERENCES ideas(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ideas ADD COLUMN es_importante INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""CREATE TABLE IF NOT EXISTS proyectos (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, ultimaPaginaId INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS paginas_proyecto (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, proyectoId INTEGER NOT NULL,
            titulo TEXT NOT NULL, texto TEXT NOT NULL, orden INTEGER NOT NULL,
            FOREIGN KEY(proyectoId) REFERENCES proyectos(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_paginas_proyecto_proyectoId_orden ON paginas_proyecto(proyectoId, orden)")
    }
}
