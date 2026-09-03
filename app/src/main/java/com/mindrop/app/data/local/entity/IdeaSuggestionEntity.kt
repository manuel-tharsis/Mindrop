package com.mindrop.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "idea_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = IdeaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idea_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["idea_id", "validated_at"]),
        Index(value = ["idea_id", "update_number"], unique = true),
    ],
)
data class IdeaSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "idea_id")
    val ideaId: Long,
    val text: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "validated_at")
    val validatedAt: Long? = null,
    @ColumnInfo(name = "update_number")
    val updateNumber: Int? = null,
)
