package com.mindrop.app

import android.app.Application
import com.mindrop.app.data.icon.CustomIconRepository
import com.mindrop.app.data.local.MindropDatabase
import com.mindrop.app.data.repository.FolderRepository
import com.mindrop.app.data.repository.IdeaRepository
import com.mindrop.app.data.repository.IdeaSuggestionRepository
import com.mindrop.app.data.repository.RepositorioAgenda
import com.mindrop.app.data.repository.RepositorioLogros
import com.mindrop.app.data.repository.RepositorioProyectos
import com.mindrop.app.data.preferences.RepositorioPreferencias
import com.mindrop.app.notifications.ProgramadorRecordatorios
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MindropApplication : Application() {
    val borrados = com.mindrop.app.data.repository.BorradosRecuperables()
    val proyectos by lazy { RepositorioProyectos(database, borrados) }
    val copias by lazy { com.mindrop.app.data.repository.RepositorioCopias(applicationContext, database, borrados) }
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val preferencias by lazy { RepositorioPreferencias(applicationContext) }
    val logros by lazy { RepositorioLogros(database, preferencias) }
    val programador by lazy { ProgramadorRecordatorios(applicationContext, database) }
    val agenda by lazy { RepositorioAgenda(database).apply {
        alCambiarRecordatorios = { ambito.launch { programador.reconciliar() } }
        alCancelarRecordatorio = programador::cancelar
    } }
    fun iniciarRpg() = ambito.launch {
        logros.iniciar()
        programador.reconciliar()
        if (preferencias.preferencias.first().diasRedescubrimiento > 0) programador.activarRedescubrimiento()
    }
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
            borrados = borrados,
        ).apply { alCancelarRecordatorio = programador::cancelar }
    }

    val ideaSuggestionRepository: IdeaSuggestionRepository by lazy {
        IdeaSuggestionRepository(
            database = database,
            suggestionDao = database.ideaSuggestionDao(),
        )
    }
}
