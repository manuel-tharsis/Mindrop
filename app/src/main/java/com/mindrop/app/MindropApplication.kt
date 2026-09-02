package com.mindrop.app

import android.app.Application
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.MindropRepository

class MindropApplication : Application() {
    val database: MindropDatabase by lazy {
        MindropDatabase.create(applicationContext)
    }

    val repository: MindropRepository by lazy {
        MindropRepository(
            folderDao = database.folderDao(),
            ideaDao = database.ideaDao(),
        )
    }
}
