package com.moblin.android.platform.coremotion

import android.hardware.SensorManager
import android.os.Looper
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.pose
import com.moblin.android.platform.uikit.UIDevice
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CMAccelerometerSuite {
    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun assertAcceleration(expected: CMAcceleration, actual: CMAcceleration, tolerance: Double = 1e-5) {
        assertTrue(abs(expected.x - actual.x) <= tolerance, "x: expected ${expected.x} but was ${actual.x}")
        assertTrue(abs(expected.y - actual.y) <= tolerance, "y: expected ${expected.y} but was ${actual.y}")
        assertTrue(abs(expected.z - actual.z) <= tolerance, "z: expected ${expected.z} but was ${actual.z}")
    }

    private fun start(
        manager: CMMotionManager,
        received: MutableList<CMAccelerometerData>,
        interval: Double = 0.1,
        onMain: MutableList<Boolean> = mutableListOf(),
    ) {
        manager.accelerometerUpdateInterval = interval
        manager.startAccelerometerUpdates(to = OperationQueue.main) { data, error ->
            assertNull(error)
            onMain.add(Looper.myLooper() == Looper.getMainLooper())
            received.add(assertNotNull(data))
        }
    }

    private fun sendRaw(sensors: FakeMotionSensors, x: Float, y: Float, z: Float, stepMs: Long = 100) {
        sensors.send(sensors.accelerometerSensor!!, floatArrayOf(x, y, z))
        sensors.nowNs += stepMs * 1_000_000L
    }

    @Test
    fun restingFaceUpReadsOneGDownwardsLikeIos() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        start(manager, received)
        sendRaw(sensors, 0f, 0f, 9.81f)
        runMain()
        assertAcceleration(CMAcceleration(0.0, 0.0, -1.0), received.single().acceleration)
        assertSame(received.single(), manager.accelerometerData)
        sendRaw(sensors, 0f, 0f, SensorManager.STANDARD_GRAVITY)
        runMain()
        assertAcceleration(CMAcceleration(0.0, 0.0, -1.0), received.last().acceleration, 1e-3)
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun uprightReadsOneGAlongTheIosAxisOfTheNaturalOrientation() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        start(manager, received)
        sendRaw(sensors, 0f, 9.81f, 0f)
        runMain()
        val expected = if (UIDevice.current.isNaturalOrientationLandscape) {
            CMAcceleration(-1.0, 0.0, 0.0)
        } else {
            CMAcceleration(0.0, -1.0, 0.0)
        }
        assertAcceleration(expected, received.single().acceleration)
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun everyAxisAndMagnitudeComesThroughInG() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        start(manager, received)
        val sent = listOf(
            CMAcceleration(0.0, -1.0, 0.0),
            CMAcceleration(-1.0, 0.0, 0.0),
            CMAcceleration(0.0, 0.0, 1.0),
            CMAcceleration(0.3, -2.0, -0.5),
            CMAcceleration(-3.5, 1.25, 0.75),
        )
        for (acceleration in sent) {
            sensors.sendAcceleration(acceleration.x, acceleration.y, acceleration.z)
        }
        runMain()
        assertEquals(sent.size, received.size)
        for ((expected, data) in sent.zip(received)) {
            assertAcceleration(expected, data.acceleration)
        }
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun updatesArePacedToTheInterval() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        start(manager, received, interval = 0.1)
        repeat(50) {
            sendRaw(sensors, 0f, 0f, 9.81f, stepMs = 20)
        }
        runMain()
        assertTrue(received.size in 10..11, "${received.size} updates in one second")
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun handlerRunsLaterOnTheMainQueue() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        val onMain = mutableListOf<Boolean>()
        start(manager, received, onMain = onMain)
        sendRaw(sensors, 0f, 0f, 9.81f)
        assertTrue(received.isEmpty())
        assertNotNull(manager.accelerometerData)
        runMain()
        assertEquals(listOf(true), onMain)
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun stopUnregistersAndDropsQueuedData() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        assertTrue(manager.isAccelerometerAvailable)
        assertFalse(manager.isAccelerometerActive)
        start(manager, received)
        val listener = sensors.accelerometerListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.accelerometerSensor))
        assertTrue(manager.isAccelerometerActive)
        sendRaw(sensors, 0f, 0f, 9.81f)
        manager.stopAccelerometerUpdates()
        runMain()
        assertTrue(received.isEmpty())
        assertFalse(manager.isAccelerometerActive)
        assertNull(manager.accelerometerData)
        assertFalse(sensors.shadow.hasListener(listener))
        manager.stopAccelerometerUpdates()
        start(manager, received)
        val restarted = sensors.accelerometerListeners().single()
        assertTrue(restarted !== listener)
        start(manager, received)
        assertEquals(1, sensors.accelerometerListeners().size)
        manager.stopAccelerometerUpdates()
        assertTrue(sensors.accelerometerListeners().isEmpty())
    }

    @Test
    fun missingAccelerometerStartsNothing() {
        val sensors = FakeMotionSensors(accelerometer = false)
        val manager = CMMotionManager()
        val received = mutableListOf<CMAccelerometerData>()
        assertFalse(manager.isAccelerometerAvailable)
        start(manager, received)
        assertFalse(manager.isAccelerometerActive)
        assertTrue(sensors.accelerometerListeners().isEmpty())
        manager.stopAccelerometerUpdates()
    }

    @Test
    fun deviceMotionAndAccelerometerRunSideBySide() {
        val sensors = FakeMotionSensors(accelerometer = true)
        val manager = CMMotionManager()
        val accelerations = mutableListOf<CMAccelerometerData>()
        val motions = mutableListOf<CMDeviceMotion>()
        start(manager, accelerations)
        manager.deviceMotionUpdateInterval = 0.2
        manager.startDeviceMotionUpdates(to = OperationQueue.main) { data, _ ->
            motions.add(assertNotNull(data))
        }
        assertEquals(1, sensors.motionListeners().size)
        assertEquals(1, sensors.accelerometerListeners().size)
        manager.stopDeviceMotionUpdates()
        assertFalse(manager.isDeviceMotionActive)
        assertTrue(manager.isAccelerometerActive)
        sensors.sendAcceleration(0.0, 0.0, -1.0)
        runMain()
        assertEquals(1, accelerations.size)
        manager.startDeviceMotionUpdates(to = OperationQueue.main) { data, _ ->
            motions.add(assertNotNull(data))
        }
        manager.stopAccelerometerUpdates()
        assertTrue(manager.isDeviceMotionActive)
        assertEquals(1, sensors.motionListeners().size)
        sensors.sendPose(pose(pitch = 20.0))
        runMain()
        assertEquals(1, motions.size)
        assertEquals(1, accelerations.size)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun deviceMotionOnTheAccelerometerKeepsItsListenerWhenAccelerometerUpdatesStop() {
        val sensors = FakeMotionSensors(
            gravity = false,
            linearAcceleration = false,
            gameRotationVector = false,
            accelerometer = true,
        )
        val manager = CMMotionManager()
        val accelerations = mutableListOf<CMAccelerometerData>()
        val motions = mutableListOf<CMDeviceMotion>()
        manager.deviceMotionUpdateInterval = 0.1
        manager.startDeviceMotionUpdates(to = OperationQueue.main) { data, _ ->
            motions.add(assertNotNull(data))
        }
        start(manager, accelerations)
        val motionListener = sensors.motionListeners().single()
        assertTrue(sensors.isRegistered(motionListener, sensors.accelerometerSensor))
        sendRaw(sensors, 0f, 0f, 9.81f)
        runMain()
        assertEquals(1, motions.size)
        assertEquals(1, accelerations.size)
        manager.stopAccelerometerUpdates()
        assertTrue(sensors.isRegistered(motionListener, sensors.accelerometerSensor))
        sendRaw(sensors, 0f, 0f, 9.81f)
        runMain()
        assertEquals(2, motions.size)
        assertEquals(1, accelerations.size)
        manager.stopDeviceMotionUpdates()
    }
}
