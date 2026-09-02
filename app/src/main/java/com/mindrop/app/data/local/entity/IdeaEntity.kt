package com.mindrop.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ideas",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("folder_id")],
)
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "short_description")
    val shortDescription: String,
    @ColumnInfo(name = "full_description")
    val fullDescription: String,
    val icon: String,
    @ColumnInfo(name = "folder_id")
    val folderId: Long,
)
