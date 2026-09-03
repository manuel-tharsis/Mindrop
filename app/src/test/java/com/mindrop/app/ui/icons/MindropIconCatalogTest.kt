package com.mindrop.app.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MindropIconCatalogTest {
    @Test
    fun catalogIsSmallUniqueAndContainsTheUsefulCategories() {
        val keys = mindropIconOptions.map(MindropIconOption::key)

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.size in 12..20)
        assertTrue(
            keys.containsAll(
                listOf(
                    "idea",
                    "code",
                    "mobile",
                    "computer",
                    "car",
                    "tools",
                    "game",
                    "home",
                    "work",
                    "document",
                    "folder",
                    "star",
                    "brain",
                    "money",
                    "sport",
                ),
            ),
        )
    }

    @Test
    fun legacyAndUnknownValuesHaveSafeFallbacks() {
        assertEquals("mobile", mindropIcon("android").key)
        assertEquals("terminal", mindropIcon("cli").key)
        assertEquals("idea", mindropIcon("not-an-icon").key)
    }
}
