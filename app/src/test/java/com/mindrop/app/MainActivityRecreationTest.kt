package com.mindrop.app

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityRecreationTest {
    @Test
    fun activityAndNavigationHostCanBeRecreated() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()

        controller.recreate()

        assertFalse(controller.get().isFinishing)
        controller.pause().stop().destroy()
    }
}
