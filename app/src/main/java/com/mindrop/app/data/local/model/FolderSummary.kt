package com.mindrop.app.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.mindrop.app.data.local.entity.FolderEntity

data class FolderSummary(
    @Embedded
    val folder: FolderEntity,
    @ColumnInfo(name = "idea_count")
    val ideaCount: Int,
    @ColumnInfo(name = "child_folder_count")
    val childFolderCount: Int,
) {
    val itemCount: Int
        get() = ideaCount + childFolderCount
}
