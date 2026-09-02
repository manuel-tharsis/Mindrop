package com.mindrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mindrop.app.ui.navigation.MindropNavHost
import com.mindrop.app.ui.theme.MindropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as MindropApplication).repository

        setContent {
            MindropTheme {
                MindropNavHost(repository = repository)
            }
        }
    }
}
