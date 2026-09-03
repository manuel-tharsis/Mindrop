package com.mindrop.app

import android.app.Application
import com.mindrop.app.data.icon.CustomIconRepository
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository

class MindropApplication : Application() {
    val customIconRepository: CustomIconRepository by lazy {
        CustomIconRepository(applicationContext)
    }

    val database: MindropDatabase by lazy {
        MindropDatabase.create(applicationContext)
    }

    val folderRepository: FolderRepository by lazy {
        FolderRepository(
            database = database,
            folderDao = database.folderDao(),
        )
    }

    val ideaRepository: IdeaRepository by lazy {
        IdeaRepository(
            database = database,
            ideaDao = database.ideaDao(),
            customIconFileStore = customIconRepository,
        )
    }
}
