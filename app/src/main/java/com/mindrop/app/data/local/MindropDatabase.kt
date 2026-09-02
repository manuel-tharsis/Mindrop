package com.mindrop.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mindrop.app.data.local.dao.FolderDao
import com.mindrop.app.data.local.dao.IdeaDao
import com.mindrop.app.data.local.entity.FolderEntity
import com.mindrop.app.data.local.entity.IdeaEntity

@Database(
    entities = [FolderEntity::class, IdeaEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MindropDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao

    abstract fun ideaDao(): IdeaDao

    companion object {
        private const val DATABASE_NAME = "mindrop.db"

        fun create(context: Context): MindropDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MindropDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
