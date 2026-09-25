package com.moblin.android.platform.coremotion

import android.os.Looper
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.axisAngle
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.multiply
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.pose
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.rotate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CMMotionManagerSuite {
    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun assertDegrees(expected: Double, actualRadians: Double, tolerance: Double = 1e-3) {
        val actual = Math.toDegrees(actualRadians)
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected degrees but was $actual")
    }

    private fun assertAttitude(
        yaw: Double,
        pitch: Double,
        roll: Double,
        attitude: CMAttitude,
        tolerance: Double = 1e-3,
    ) {
        assertDegrees(yaw, attitude.yaw, tolerance)
        assertDegrees(pitch, attitude.pitch, tolerance)
        assertDegrees(roll, attitude.roll, tolerance)
    }

    private fun rotationVector(quaternion: CMQuaternion): FloatArray {
        return floatArrayOf(
            quaternion.x.toFloat(),
            quaternion.y.toFloat(),
            quaternion.z.toFloat(),
            quaternion.w.toFloat(),
        )
    }

    private fun gravityFromAttitude(attitude: CMAttitude): DoubleArray {
        val quaternion = attitude.quaternion
        return rotate(CMQuaternion(-quaternion.x, -quaternion.y, -quaternion.z, quaternion.w), 0.0, 0.0, -1.0)
    }

    private fun start(
        manager: CMMotionManager,
        interval: Double = 0.1,
        received: MutableList<CMDeviceMotion>,
    ) {
        manager.deviceMotionUpdateInterval = interval
        manager.startDeviceMotionUpdates(to = OperationQueue.main) { data, _ ->
            received.add(assertNotNull(data))
        }
    }

    @Test
    fun attitudeFollowsTheIosAxes() {
        assertAttitude(0.0, 0.0, 0.0, CMAttitude.fromRotationVector(floatArrayOf(0f, 0f, 0f, 1f), false))
        val topUp = axisAngle(1.0, 0.0, 0.0, 30.0)
        assertTrue(rotate(topUp, 0.0, 1.0, 0.0)[2] > 0)
        assertAttitude(0.0, 30.0, 0.0, CMAttitude.fromRotationVector(rotationVector(topUp), false))
        val rightEdgeDown = axisAngle(0.0, 1.0, 0.0, 20.0)
        assertTrue(rotate(rightEdgeDown, 1.0, 0.0, 0.0)[2] < 0)
        assertAttitude(0.0, 0.0, 20.0, CMAttitude.fromRotationVector(rotationVector(rightEdgeDown), false))
        val turnedLeft = axisAngle(0.0, 0.0, 1.0, 30.0)
        assertTrue(rotate(turnedLeft, 1.0, 0.0, 0.0)[1] > 0)
        assertAttitude(30.0, 0.0, 0.0, CMAttitude.fromRotationVector(rotationVector(turnedLeft), false))
        val upright = CMAttitude.fromRotationVector(rotationVector(axisAngle(1.0, 0.0, 0.0, 89.0)), false)
        assertDegrees(89.0, upright.pitch)
        val faceDown = CMAttitude.fromRotationVector(rotationVector(axisAngle(0.0, 1.0, 0.0, 180.0)), false)
        assertDegrees(0.0, faceDown.pitch)
        assertDegrees(180.0, abs(faceDown.roll))
        val landscapeUpright = CMAttitude.fromRotationVector(rotationVector(pose(roll = -90.0)), false)
        assertAttitude(0.0, 0.0, -90.0, landscapeUpright)
    }

    @Test
    fun eulerAnglesUseTheIosOrder() {
        for (yaw in listOf(-170.0, -90.0, -35.0, 0.0, 12.0, 90.0, 179.0)) {
            for (pitch in listOf(-80.0, -45.0, -10.0, 0.0, 25.0, 60.0, 85.0)) {
                for (roll in listOf(-179.0, -120.0, -90.0, -5.0, 0.0, 40.0, 90.0, 150.0)) {
                    val attitude = CMAttitude.fromRotationVector(rotationVector(pose(yaw, pitch, roll)), false)
                    assertAttitude(yaw, pitch, roll, attitude, 5e-3)
                }
            }
        }
    }

    @Test
    fun attitudeAgreesWithGravityInBothNaturalOrientations() {
        val poses = listOf(
            pose(),
            pose(pitch = 90.0),
            pose(roll = -90.0),
            pose(yaw = 33.0, pitch = 20.0, roll = -70.0),
            pose(yaw = -120.0, pitch = -35.0, roll = 150.0),
            pose(yaw = 75.0, pitch = 70.0, roll = 10.0),
        )
        for (quaternion in poses) {
            val androidGravity = rotate(
                CMQuaternion(-quaternion.x, -quaternion.y, -quaternion.z, quaternion.w),
                0.0,
                0.0,
                FakeMotionSensors.STANDARD_GRAVITY,
            )
            val portraitGravity = doubleArrayOf(
                -androidGravity[0] / FakeMotionSensors.STANDARD_GRAVITY,
                -androidGravity[1] / FakeMotionSensors.STANDARD_GRAVITY,
                -androidGravity[2] / FakeMotionSensors.STANDARD_GRAVITY,
            )
            val landscapeGravity = doubleArrayOf(portraitGravity[1], -portraitGravity[0], portraitGravity[2])
            val portrait = gravityFromAttitude(CMAttitude.fromRotationVector(rotationVector(quaternion), false))
            val landscape = gravityFromAttitude(CMAttitude.fromRotationVector(rotationVector(quaternion), true))
            for (index in 0 until 3) {
                assertTrue(abs(portraitGravity[index] - portrait[index]) < 1e-5, "portrait axis $index")
                assertTrue(abs(landscapeGravity[index] - landscape[index]) < 1e-5, "landscape axis $index")
            }
        }
    }

    @Test
    fun naturalLandscapeDevicesTurnTheAxesLikeGravity() {
        val natural = axisAngle(1.0, 0.0, 0.0, 90.0)
        val attitude = CMAttitude.fromRotationVector(rotationVector(natural), true)
        assertAttitude(90.0, 0.0, -90.0, attitude)
        val tilted = CMAttitude.fromRotationVector(
            rotationVector(multiply(pose(pitch = 30.0), axisAngle(0.0, 0.0, 1.0, -90.0))),
            true,
        )
        assertAttitude(0.0, 30.0, 0.0, tilted)
    }

    @Test
    fun rotationVectorWithoutScalarPartIsCompleted() {
        val quaternion = pose(yaw = 40.0, pitch = -20.0, roll = 65.0)
        val full = CMAttitude.fromRotationVector(rotationVector(quaternion), false)
        val short = CMAttitude.fromRotationVector(
            floatArrayOf(quaternion.x.toFloat(), quaternion.y.toFloat(), quaternion.z.toFloat()),
            false,
        )
        assertAttitude(Math.toDegrees(full.yaw), Math.toDegrees(full.pitch), Math.toDegrees(full.roll), short)
    }

    @Test
    fun gravityGivesPitchAndRollWithoutARotationSensor() {
        val tilt = Math.toRadians(30.0)
        assertAttitude(0.0, 30.0, 0.0, CMAttitude.fromGravity(CMAcceleration(0.0, -sin(tilt), -cos(tilt))))
        val roll = Math.toRadians(20.0)
        assertAttitude(0.0, 0.0, 20.0, CMAttitude.fromGravity(CMAcceleration(sin(roll), 0.0, -cos(roll))))
        assertAttitude(0.0, 0.0, 0.0, CMAttitude.fromGravity(CMAcceleration(0.0, 0.0, 0.0)))
    }

    @Test
    fun deviceMotionCarriesTheGameRotationVectorAttitude() {
        val sensors = FakeMotionSensors(rotationVector = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        val listener = sensors.motionListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.gravitySensor))
        assertTrue(sensors.isRegistered(listener, sensors.linearAccelerationSensor))
        assertTrue(sensors.isRegistered(listener, sensors.gameRotationVectorSensor))
        assertFalse(sensors.isRegistered(listener, sensors.rotationVectorSensor))
        sensors.sendPose(pose(yaw = 50.0, pitch = 30.0, roll = -15.0))
        runMain()
        val motion = received.single()
        assertAttitude(50.0, 30.0, -15.0, motion.attitude, 1e-2)
        val gravity = gravityFromAttitude(motion.attitude)
        assertTrue(abs(gravity[0] - motion.gravity.x) < 1e-5)
        assertTrue(abs(gravity[1] - motion.gravity.y) < 1e-5)
        assertTrue(abs(gravity[2] - motion.gravity.z) < 1e-5)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun rotationVectorIsTheFallback() {
        val sensors = FakeMotionSensors(gameRotationVector = false, rotationVector = true)
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        val listener = sensors.motionListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.rotationVectorSensor))
        sensors.sendPose(pose(yaw = -100.0, pitch = 10.0, roll = 80.0))
        runMain()
        assertAttitude(-100.0, 10.0, 80.0, received.single().attitude, 1e-2)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun deliveryWaitsForTheFirstAttitude() {
        val sensors = FakeMotionSensors()
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        val quaternion = pose(yaw = 20.0, pitch = 40.0)
        sensors.sendPose(quaternion, attitude = false)
        sensors.sendPose(quaternion, attitude = false)
        runMain()
        assertTrue(received.isEmpty())
        assertEquals(null, manager.deviceMotion)
        sensors.sendPose(quaternion)
        runMain()
        assertAttitude(20.0, 40.0, 0.0, received.single().attitude, 1e-2)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun silentAttitudeSensorFallsBackToGravityAndIsUnregistered() {
        val sensors = FakeMotionSensors()
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        val listener = sensors.motionListeners().single()
        val tilted = pose(yaw = 60.0, pitch = 25.0)
        repeat(5) {
            sensors.sendPose(tilted, attitude = false)
        }
        runMain()
        assertTrue(received.isEmpty())
        assertTrue(sensors.isRegistered(listener, sensors.gameRotationVectorSensor))
        sensors.sendPose(tilted, attitude = false)
        runMain()
        assertAttitude(0.0, 25.0, 0.0, received.single().attitude, 1e-2)
        assertFalse(sensors.isRegistered(listener, sensors.gameRotationVectorSensor))
        assertTrue(sensors.isRegistered(listener, sensors.gravitySensor))
        listener.onSensorChanged(sensors.event(sensors.gameRotationVectorSensor!!, rotationVector(tilted)))
        sensors.sendPose(tilted, attitude = false)
        runMain()
        assertAttitude(0.0, 25.0, 0.0, received.last().attitude, 1e-2)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun gravityGivesTheAttitudeWithoutARotationSensor() {
        val sensors = FakeMotionSensors(gameRotationVector = false)
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        sensors.sendPose(pose(yaw = 45.0, pitch = -30.0, roll = 50.0), attitude = false)
        runMain()
        assertAttitude(0.0, -30.0, 50.0, received.single().attitude, 1e-2)
        manager.stopDeviceMotionUpdates()
    }

    @Test
    fun stopUnregistersEverySensorAndDropsQueuedMotion() {
        val sensors = FakeMotionSensors()
        val manager = CMMotionManager()
        val received = mutableListOf<CMDeviceMotion>()
        start(manager, received = received)
        val listener = sensors.motionListeners().single()
        assertTrue(manager.isDeviceMotionActive)
        sensors.sendPose(pose(pitch = 10.0))
        manager.stopDeviceMotionUpdates()
        runMain()
        assertTrue(received.isEmpty())
        assertFalse(manager.isDeviceMotionActive)
        assertFalse(sensors.shadow.hasListener(listener))
        assertTrue(sensors.motionListeners().isEmpty())
        start(manager, received = received)
        val restarted = sensors.motionListeners().single()
        assertTrue(restarted !== listener)
        start(manager, received = received)
        assertEquals(1, sensors.motionListeners().size)
        manager.stopDeviceMotionUpdates()
        assertTrue(sensors.motionListeners().isEmpty())
    }
}
