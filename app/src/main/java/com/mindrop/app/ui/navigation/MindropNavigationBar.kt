package com.mindrop.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindrop.app.R
import com.mindrop.app.ui.icons.PackIcon
import com.mindrop.app.ui.icons.packPainterResource

@Composable
fun MindropNavigationBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val destinations = listOf("home" to "Inicio", "proyectos" to "Proyectos", "agenda" to "Agenda", "logros" to "Logros", "ajustes" to "Ajustes")
    val icons = listOf(R.drawable.ic_idea, R.drawable.ic_project, R.drawable.ic_calendar, R.drawable.ic_trophy, R.drawable.ic_settings)
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
            destinations.forEachIndexed { index, (route, label) ->
                NavigationBarItem(
                    selected = if (route == "home") currentRoute in listOf("home", "completed", "browse/{folderId}") else currentRoute == route,
                    onClick = { onNavigate(route) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    icon = { PackIcon(packPainterResource(icons[index]), null, Modifier.size(22.dp)) },
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}
