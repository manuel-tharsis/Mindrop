package com.mindrop.app

import android.os.Bundle
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.mindrop.app.ui.navigation.MindropNavHost
import com.mindrop.app.ui.theme.MindropTheme

class MainActivity : ComponentActivity() {
    private val destinoNotificacion = MutableStateFlow<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val mindropApplication = application as MindropApplication
        destinoNotificacion.value = if (savedInstanceState == null) intent?.getStringExtra("destino_local") else null

        setContent {
            val destino by destinoNotificacion.collectAsStateWithLifecycle()
            MindropTheme {
                MindropNavHost(
                    folderRepository = mindropApplication.folderRepository,
                    ideaRepository = mindropApplication.ideaRepository,
                    ideaSuggestionRepository = mindropApplication.ideaSuggestionRepository,
                    customIconRepository = mindropApplication.customIconRepository,
                    app = mindropApplication,
                    destinoNotificacion = destino,
                    consumirNotificacion = { destinoNotificacion.value = null },
                )
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destinoNotificacion.value = intent.getStringExtra("destino_local")
    }
    override fun onResume() {
        super.onResume()
        (application as MindropApplication).iniciarRpg()
    }
}
