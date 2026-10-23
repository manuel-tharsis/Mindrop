package com.mindrop.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mindrop.app.data.local.entity.*
import com.mindrop.app.data.local.model.FolderSummary
import com.mindrop.app.ui.idea.*
import com.mindrop.app.ui.home.components.FolderCard
import com.mindrop.app.ui.rpg.LocalMovimiento
import com.mindrop.app.ui.theme.MindropTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlujosRpgTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pagerValidaYPermiteDeslizarEnAmbosSentidos() {
        val sugerencia = IdeaSuggestionEntity(id = 1, ideaId = 1, text = "Añadir control de horas", createdAt = 1)
        var estado by mutableStateOf(IdeaDetailUiState(isLoading = false,
            idea = IdeaEntity(id = 1, title = "BioGestor", shortDescription = "", fullDescription = "Aplicación principal de gestión.", icon = "idea"),
            pendingSuggestions = listOf(sugerencia)))
        compose.setContent { MindropTheme {
            IdeaDetailScreen(estado, onBack = {}, onEdit = {}, onDelete = {}, onToggleCompleted = {},
                onValidateSuggestion = { estado = estado.copy(updates = listOf(sugerencia.copy(validatedAt = 2, updateNumber = 1)), pendingSuggestions = emptyList()) },
                onDeleteSuggestion = {})
        } }
        compose.onNodeWithTag("paginas_idea").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Validar").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Página 3 de 3").assertExists()
        compose.onNodeWithTag("paginas_idea").performTouchInput { swipeRight() }
        compose.onNodeWithText("Actualización 1").assertIsDisplayed()
        compose.onNodeWithTag("paginas_idea").performTouchInput { swipeRight() }
        compose.onNodeWithText("Aplicación principal de gestión.").assertIsDisplayed()
    }

    @Test fun carpetaRespetaMovimientoReducidoYSigueAbriendo() {
        var abierta = false
        compose.setContent { MindropTheme { CompositionLocalProvider(LocalMovimiento provides false) {
            FolderCard(FolderSummary(FolderEntity(id = 1, name = "Proyectos", icon = "folder"), 0, 0),
                onClick = { abierta = true }, onEditClick = {}, onMoveClick = {}, onDeleteClick = {})
        } } }
        compose.onNodeWithText("Proyectos").performClick()
        compose.waitUntil { abierta }
    }

    @Test fun apkNoSolicitaInternetNiAlmacenamientoAmplio() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        @Suppress("DEPRECATION")
        val permisos = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(Manifest.permission.INTERNET in permisos)
        assertFalse(Manifest.permission.MANAGE_EXTERNAL_STORAGE in permisos)
    }
}
