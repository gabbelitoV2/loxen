package com.moblin.android.platform.host

import androidx.activity.ComponentActivity
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class SystemEventsActivitiesSuite {
    @Before
    fun setUp() {
        SystemEventsState.reset()
        SystemEvents.install(RuntimeEnvironment.getApplication())
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        SystemEventsState.reset()
    }

    private fun terminations(): Int {
        return ShadowLog.getLogsForTag("SystemEvents").count { it.msg == "Application will terminate" }
    }

    @Test
    fun finishingAnActivityOnTopDoesNotTerminate() {
        val main = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        main.pause()
        val top = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        assertSame(top.get(), SystemEvents.activity)
        top.get().finish()
        top.pause()
        main.resume()
        top.stop().destroy()
        assertEquals(0, terminations())
        assertSame(main.get(), SystemEvents.activity)
        main.get().finish()
        main.pause().stop().destroy()
        assertEquals(1, terminations())
    }
}
