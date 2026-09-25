package com.moblin.android.various.model

import android.os.Looper
import com.moblin.android.platform.darwin.ProcFs
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import java.io.File
import java.lang.reflect.Proxy
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
class ModelSystemMonitorSuite {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var originalSelfStat: File
    private lateinit var originalStat: File
    private lateinit var selfStat: File
    private lateinit var stat: File
    private lateinit var model: Model

    @Before
    fun setUp() {
        originalSelfStat = ProcFs.selfStat
        originalStat = ProcFs.stat
        selfStat = File(folder.root, "self-stat")
        stat = File(folder.root, "stat")
        ProcFs.selfStat = selfStat
        ProcFs.stat = stat
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        runMain()
    }

    @After
    fun tearDown() {
        ProcFs.selfStat = originalSelfStat
        ProcFs.stat = originalStat
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun handle1sTimer() {
        val method = Model::class.java.getDeclaredMethod("handle1sTimer")
        method.isAccessible = true
        method.invoke(model)
    }

    private fun writeProcess(userMilliseconds: Long, systemMilliseconds: Long) {
        val hz = ProcFs.clockTicksPerSecond
        selfStat.writeText(
            "4711 (com.moblin.android) S 1 4711 4711 0 -1 4194624 5000 0 3 0 " +
                "${userMilliseconds * hz / 1000} ${systemMilliseconds * hz / 1000} 0 0 20 0 42 0 12345\n",
        )
    }

    private fun measureTwoSeconds() {
        writeProcess(userMilliseconds = 3_000, systemMilliseconds = 1_000)
        stat.writeText("cpu0 1000 0 1000 8000 0 0 0 0 0 0\ncpu1 0 0 0 10000 0 0 0 0 0 0\n")
        handle1sTimer()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
        writeProcess(userMilliseconds = 4_200, systemMilliseconds = 1_300)
        stat.writeText("cpu0 1150 0 1050 8000 0 0 0 0 0 0\ncpu1 120 0 0 10080 0 0 0 0 0 0\n")
        handle1sTimer()
    }

    @Test
    fun systemMonitorShowsAppAndDeviceCpuInStatusTextAndRemoteControlLikeIos() {
        model.database.show.systemMonitor = true
        assertTrue(model.isShowingStatusCpu())
        measureTwoSeconds()
        assertEquals(75, model.systemMonitor.appCpu.value)
        assertEquals(160, model.systemMonitor.cpu.value)
        val expected = "75%/160% ${model.systemMonitor.ram.value} MB"
        assertEquals(expected, model.systemMonitor.format())
        assertEquals("160", model.systemMonitor.formatShort())
        assertEquals(expected, model.formatPlainText("{systemMonitor}"))
        assertEquals(expected, model.remoteControlStreamerGetStatus().third.systemMonitor?.message)
    }

    @Test
    fun deviceCpuIsTheAppCpuWhenProcStatIsClosed() {
        model.database.show.systemMonitor = true
        writeProcess(userMilliseconds = 3_000, systemMilliseconds = 1_000)
        handle1sTimer()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        writeProcess(userMilliseconds = 3_400, systemMilliseconds = 1_100)
        handle1sTimer()
        assertEquals(50, model.systemMonitor.appCpu.value)
        assertEquals(50, model.systemMonitor.cpu.value)
        assertEquals("50", model.systemMonitor.formatShort())
    }

    @Test
    fun hiddenSystemMonitorIsNeitherMeasuredNorShown() {
        model.database.show.systemMonitor = false
        assertFalse(model.isShowingStatusCpu())
        measureTwoSeconds()
        assertEquals(0, model.systemMonitor.appCpu.value)
        assertEquals(0, model.systemMonitor.cpu.value)
        assertEquals("-% - MB", model.formatPlainText("{systemMonitor}"))
        assertNull(model.remoteControlStreamerGetStatus().third.systemMonitor)
    }
}
