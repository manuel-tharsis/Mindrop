package com.mindrop.app.ui.navigation

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationBackStackTest {
    @Test
    fun deepFolderStackCanReturnToAnExactAncestor() {
        val navController = folderNavController()
        navController.navigate("browse/1")
        navController.navigate("browse/2")
        navController.navigate("browse/3")

        assertTrue(navController.popBackStack("browse/1", inclusive = false))
        assertEquals(1L, navController.currentBackStackEntry?.arguments?.getLong("folderId"))
        assertTrue(navController.popBackStack("home", inclusive = false))
        assertEquals("home", navController.currentDestination?.route)
    }

    @Test
    fun deepFolderStackCanBeSavedAndRestored() {
        val original = folderNavController()
        original.navigate("browse/1")
        original.navigate("browse/2")
        original.navigate("browse/3")

        val restored = folderNavController(original.saveState())

        assertEquals(3L, restored.currentBackStackEntry?.arguments?.getLong("folderId"))
        assertTrue(restored.popBackStack())
        assertEquals(2L, restored.currentBackStackEntry?.arguments?.getLong("folderId"))
    }

    private fun folderNavController(savedState: Bundle? = null): NavHostController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            if (savedState != null) restoreState(savedState)
            graph = createGraph(startDestination = "home") {
                composable("home") {}
                composable(
                    route = "browse/{folderId}",
                    arguments = listOf(
                        navArgument("folderId") { type = NavType.LongType },
                    ),
                ) {}
            }
        }
    }
}
