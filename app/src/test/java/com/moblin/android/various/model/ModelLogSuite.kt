package com.moblin.android.various.model

import android.os.Looper
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.platform.log.Log
import com.moblin.android.various.logger
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelLogSuite {
    @Before
    fun setUp() {
        SystemEventsState.reset()
    }

    @After
    fun tearDown() {
        logger.handler = null
        logger.debugEnabled = false
        SystemEventsState.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun loggedLinesReachTheInAppLogAndTheLogFile() {
        val model = Model()
        model.setupLogging()
        Log.i("RemoteControlStreamer", "remote-control-streamer: Connected")
        Log.d("HttpServer", "http-server: Start")
        runMain()
        assertTrue(model.log.any { it.message.endsWith(" remote-control-streamer: Connected") })
        assertTrue(model.fileLog.any { it.endsWith(" remote-control-streamer: Connected") })
        assertFalse(model.log.any { it.message.endsWith(" http-server: Start") })
    }

    @Test
    fun debugLoggingTurnsOnDebugLines() {
        val model = Model()
        model.setupLogging()
        model.setDebugLogging(on = true)
        assertTrue(logger.debugEnabled)
        Log.d("HttpServer", "http-server: Start")
        runMain()
        assertTrue(model.log.any { it.message.endsWith(" http-server: Start") })
        assertTrue("Debug: true" in File(model.formatLog(model.log.toList())).readText())
        model.setDebugLogging(on = false)
        assertFalse(logger.debugEnabled)
    }
}
