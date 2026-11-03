package com.mindrop.app.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindrop.app.MindropApplication
import com.mindrop.app.ui.agenda.AgendaViewModel
import com.mindrop.app.ui.theme.MindropTheme

/** Ventana normal de la app, transparente: no necesita permiso de superposición. */
class EventoRapidoActivity : ComponentActivity() {
    private var peticionFoco by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MindropApplication
        setContent {
            val vm: AgendaViewModel = viewModel(factory = remember(app) {
                viewModelFactory { initializer { AgendaViewModel(app.agenda) } }
            })
            MindropTheme { EventoRapidoScreen(vm, peticionFoco, finalizar = ::finish) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        peticionFoco++
    }
}
