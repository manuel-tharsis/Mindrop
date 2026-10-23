package com.mindrop.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mindrop.app.data.local.dao.FolderDao
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.dao.IdeaSuggestionDao
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity
import com.mindrop.app.data.local.entity.IdeaSuggestionEntity
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.data.local.dao.DaoRpg

@Database(
    entities = [FolderEntity::class, IdeaEntity::class, IdeaSuggestionEntity::class,
        EventoAgenda::class, RecordatorioLocal::class, LogroDesbloqueado::class,
        AccionRpg::class, MetricaRpg::class, RevisionIdea::class],
    version = 6,
    exportSchema = true,
)
abstract class MindropDatabase : RoomDatabase() {
    abstract fun daoRpg(): DaoRpg
    abstract fun folderDao(): FolderDao

    abstract fun ideaDao(): IdeaDao

    abstract fun ideaSuggestionDao(): IdeaSuggestionDao

    companion object {
        private const val DATABASE_NAME = "mindrop.db"

        fun create(context: Context): MindropDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MindropDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                .build()
    }
}
