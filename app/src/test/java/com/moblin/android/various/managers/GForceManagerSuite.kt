package com.moblin.android.various.managers

import android.hardware.SensorManager
import android.os.Looper
import com.moblin.android.platform.coremotion.CMMotionManager
import com.moblin.android.platform.coremotion.FakeMotionSensors
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class GForceManagerSuite {
    private lateinit var sensors: FakeMotionSensors
    private lateinit var motionManager: CMMotionManager
    private lateinit var manager: GForceManager

    @Before
    fun setUp() {
        sensors = FakeMotionSensors(accelerometer = true)
        motionManager = CMMotionManager()
        manager = GForceManager(motionManager = motionManager)
    }

    @After
    fun tearDown() {
        manager.stop()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun send(x: Double, y: Double, z: Double) {
        sensors.sendAcceleration(x, y, z)
        runMain()
    }

    private fun rest(count: Int = 1) {
        repeat(count) {
            send(0.0, 0.0, -1.0)
        }
    }

    private fun sendRaw(z: Float, stepMs: Long) {
        sensors.send(sensors.accelerometerSensor!!, floatArrayOf(0f, 0f, z))
        sensors.nowNs += stepMs * 1_000_000L
    }

    private fun assertGForce(now: Double, recentMax: Double, max: Double, tolerance: Double = 1e-4) {
        val latest = assertNotNull(manager.getLatest())
        assertTrue(abs(now - latest.now) <= tolerance, "now: expected $now but was ${latest.now}")
        assertTrue(
            abs(recentMax - latest.recentMax) <= tolerance,
            "recentMax: expected $recentMax but was ${latest.recentMax}",
        )
        assertTrue(abs(max - latest.max) <= tolerance, "max: expected $max but was ${latest.max}")
    }

    @Test
    fun restingReadsOneGNotMetresPerSecondSquared() {
        manager.start()
        sendRaw(SensorManager.STANDARD_GRAVITY, stepMs = 100)
        runMain()
        assertGForce(now = 1.0, recentMax = 1.0, max = 1.0, tolerance = 1e-3)
        send(0.0, -0.8, -0.6)
        assertGForce(now = 1.0, recentMax = 1.0, max = 1.0, tolerance = 1e-3)
        send(0.6, 0.0, 0.8)
        assertGForce(now = 1.0, recentMax = 1.0, max = 1.0, tolerance = 1e-3)
    }

    @Test
    fun startRegistersTheAccelerometerOnceAtATenthOfASecondAndStopUnregistersIt() {
        assertGForce(now = 0.0, recentMax = 0.0, max = 0.0)
        assertTrue(sensors.accelerometerListeners().isEmpty())
        manager.start()
        val listener = sensors.accelerometerListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.accelerometerSensor))
        assertTrue(motionManager.isAccelerometerActive)
        assertEquals(0.1, motionManager.accelerometerUpdateInterval)
        manager.start()
        assertSame(listener, sensors.accelerometerListeners().single())
        rest()
        manager.stop()
        assertTrue(sensors.accelerometerListeners().isEmpty())
        assertFalse(motionManager.isAccelerometerActive)
        manager.stop()
        assertTrue(sensors.accelerometerListeners().isEmpty())
        assertGForce(now = 1.0, recentMax = 1.0, max = 1.0)
    }

    @Test
    fun peaksAndTheRecentMaximumFollowSwift() {
        manager.start()
        rest()
        assertGForce(now = 1.0, recentMax = 1.0, max = 1.0)
        send(0.0, 0.0, -3.0)
        assertGForce(now = 3.0, recentMax = 3.0, max = 3.0)
        rest()
        assertGForce(now = 1.0, recentMax = 3.0, max = 3.0)
        rest(49)
        assertGForce(now = 1.0, recentMax = 2.8125, max = 3.0)
        send(0.0, -2.0, 0.0)
        val progress = 0.51
        assertGForce(now = 2.0, recentMax = (1 - progress * progress * progress * progress) * 3, max = 3.0)
        send(0.0, -4.0, 0.0)
        assertGForce(now = 4.0, recentMax = 4.0, max = 4.0)
        rest(90)
        assertGForce(now = 1.0, recentMax = (1 - 0.9 * 0.9 * 0.9 * 0.9) * 4, max = 4.0)
        rest(10)
        assertGForce(now = 1.0, recentMax = 1.0, max = 4.0)
        send(0.0, 0.0, -1.5)
        assertGForce(now = 1.5, recentMax = 1.5, max = 4.0)
        rest(200)
        assertGForce(now = 1.0, recentMax = 1.0, max = 4.0)
    }

    @Test
    fun restartResetsTheRecentMaximumButKeepsTheMaximum() {
        manager.start()
        rest()
        send(0.0, 0.0, -2.5)
        rest()
        manager.stop()
        manager.start()
        assertGForce(now = 1.0, recentMax = 0.0, max = 2.5)
        send(0.0, 0.0, -1.2)
        assertGForce(now = 1.2, recentMax = 1.2, max = 2.5)
    }

    @Test
    fun theRecentMaximumDecaysOverTenSecondsAtFiftyHertz() {
        manager.start()
        sendRaw(3 * 9.81f, stepMs = 20)
        repeat(249) {
            sendRaw(9.81f, stepMs = 20)
        }
        runMain()
        assertGForce(now = 1.0, recentMax = 2.8125, max = 3.0, tolerance = 0.02)
        repeat(250) {
            sendRaw(9.81f, stepMs = 20)
        }
        runMain()
        assertGForce(now = 1.0, recentMax = 1.0, max = 3.0)
    }

    @Test
    fun magnitudeIncludesEveryAxis() {
        manager.start()
        send(1.0, -2.0, 2.0)
        assertGForce(now = 3.0, recentMax = 3.0, max = 3.0)
        send(0.5, 0.5, -0.5)
        assertGForce(now = sqrt(0.75), recentMax = 3.0, max = 3.0)
    }
}
