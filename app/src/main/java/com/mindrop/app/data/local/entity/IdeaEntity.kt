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
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["folder_id", "sort_order"])],
)
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "short_description")
    val shortDescription: String,
    @ColumnInfo(name = "full_description")
    val fullDescription: String,
    val icon: String,
    @ColumnInfo(name = "custom_icon_path")
    val customIconPath: String? = null,
    @ColumnInfo(name = "folder_id")
    val folderId: Long? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0,
)
