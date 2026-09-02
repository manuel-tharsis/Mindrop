package com.mindrop.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_folder_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("parent_folder_id")],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "parent_folder_id")
    val parentFolderId: Long? = null,
)
