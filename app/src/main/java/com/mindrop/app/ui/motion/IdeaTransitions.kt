package com.mindrop.app.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.mindrop.app.ui.rpg.movimientoPermitido

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalIdeaSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalIdeaVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

enum class IdeaSharedPart { Icon, Title }

/** Fuera de navegación (previews, widgets y tests de componentes) se dibuja normalmente. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.ideaSharedElement(ideaId: Long, part: IdeaSharedPart): Modifier {
    val shared = LocalIdeaSharedScope.current ?: return this
    val visible = LocalIdeaVisibilityScope.current ?: return this
    if (!movimientoPermitido()) return this
    return with(shared) {
        val state = rememberSharedContentState("idea/$ideaId/${part.name}")
        if (part == IdeaSharedPart.Icon) this@ideaSharedElement.sharedElement(state, visible,
            boundsTransform = { _, _ -> tween(220) })
        else this@ideaSharedElement.sharedBounds(state, visible, boundsTransform = { _, _ -> tween(220) })
    }
}
