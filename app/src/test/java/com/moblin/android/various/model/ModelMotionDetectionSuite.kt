package com.moblin.android.various.model

import android.graphics.PointF
import android.os.Looper
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.FakeCaptureDevices
import com.moblin.android.platform.coremotion.CMQuaternion
import com.moblin.android.platform.coremotion.FakeMotionSensors
import com.moblin.android.platform.coremotion.FakeMotionSensors.Companion.pose
import com.moblin.android.platform.coremotion.MotionListener
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelMotionDetectionSuite {
    private lateinit var model: Model
    private lateinit var device: AVCaptureDevice
    private lateinit var sensors: FakeMotionSensors
    private var portrait = false
    private var otherListeners = emptySet<MotionListener>()
    private val tapPoint = PointF(0.3f, 0.6f)
    private val reference = pose(yaw = 30.0, roll = -90.0)

    @Before
    fun setUp() {
        sensors = FakeMotionSensors()
        model = Model()
        device = FakeCaptureDevices.makeManual()
        model.cameraDevice = CaptureDevice(device = device, id = UUID.randomUUID(), isVideoMirrored = false)
        portrait = model.stream.value.portrait
        model.stream.value.portrait = false
        runMain()
        otherListeners = sensors.motionListeners()
    }

    @After
    fun tearDown() {
        model.stopMotionDetection()
        model.stream.value.portrait = portrait
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun focusListeners(): Set<MotionListener> {
        return sensors.motionListeners() - otherListeners
    }

    private fun tapToFocus(at: CMQuaternion = reference) {
        model.setFocusPointOfInterest(focusPoint = tapPoint)
        runMain()
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        move(at)
    }

    private fun move(to: CMQuaternion) {
        sensors.sendPose(to)
        runMain()
    }

    private fun assertTappedFocusKept() {
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        assertEquals(tapPoint, model.camera.manualFocusPoint.value)
        assertEquals(1, focusListeners().size)
    }

    private fun assertBackToContinuous() {
        assertEquals(AVCaptureDevice.FocusMode.continuousAutoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.continuousAutoExposure, device.exposureMode)
        assertEquals(PointF(0.5f, 0.5f), device.focusPointOfInterest)
        assertEquals(null, model.camera.manualFocusPoint.value)
        assertTrue(focusListeners().isEmpty())
    }

    @Test
    fun tapToFocusListensToGravityAndAttitude() {
        assertTrue(focusListeners().isEmpty())
        model.setFocusPointOfInterest(focusPoint = tapPoint)
        val listener = focusListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.gravitySensor))
        assertTrue(sensors.isRegistered(listener, sensors.gameRotationVectorSensor))
        model.setFocusPointOfInterest(focusPoint = tapPoint)
        assertEquals(1, focusListeners().size)
    }

    @Test
    fun smallMovementsKeepTheTappedFocus() {
        tapToFocus()
        for (moved in listOf(
            pose(yaw = 39.0, roll = -90.0),
            pose(yaw = 21.0, roll = -90.0),
            pose(yaw = 30.0, pitch = 9.5, roll = -90.0),
            pose(yaw = 30.0, pitch = -9.5, roll = -90.0),
            pose(yaw = 30.0, roll = -81.0),
            pose(yaw = 30.0, roll = -99.0),
            pose(yaw = 37.0, pitch = 7.0, roll = -83.0),
            reference,
        )) {
            move(moved)
            assertTappedFocusKept()
        }
    }

    @Test
    fun turningMoreThanTenDegreesReturnsToContinuousModesOnce() {
        tapToFocus()
        val turned = pose(yaw = 30.0, pitch = 11.0, roll = -90.0)
        sensors.sendPose(turned)
        sensors.sendPose(turned)
        assertTappedFocusKept()
        shadowOf(Looper.getMainLooper()).runOneTask()
        assertBackToContinuous()
        val sentinel = PointF(0.9f, 0.1f)
        model.camera.setManualFocusPoint(value = sentinel)
        runMain()
        sensors.sendPose(pose(yaw = 90.0, pitch = 40.0, roll = -30.0))
        runMain()
        assertEquals(sentinel, model.camera.manualFocusPoint.value)
        assertTrue(focusListeners().isEmpty())
    }

    @Test
    fun eachAngleTriggersOnItsOwn() {
        for (turned in listOf(
            pose(yaw = 30.0, pitch = 10.5, roll = -90.0),
            pose(yaw = 30.0, pitch = -10.5, roll = -90.0),
            pose(yaw = 30.0, roll = -79.0),
            pose(yaw = 30.0, roll = -101.0),
            pose(yaw = 41.0, roll = -90.0),
            pose(yaw = 19.0, roll = -90.0),
        )) {
            tapToFocus()
            assertTappedFocusKept()
            move(turned)
            assertBackToContinuous()
        }
    }

    @Test
    fun theReferenceIsTheFirstAttitudeAfterTheTap() {
        tapToFocus()
        move(pose(yaw = 36.0, roll = -90.0))
        assertTappedFocusKept()
        move(pose(yaw = 42.0, roll = -90.0))
        assertBackToContinuous()
        tapToFocus(at = pose(yaw = 42.0, roll = -90.0))
        move(pose(yaw = 50.0, roll = -90.0))
        assertTappedFocusKept()
        move(pose(yaw = 53.0, roll = -90.0))
        assertBackToContinuous()
    }

    @Test
    fun yawWrapsAroundHalfATurn() {
        tapToFocus(at = pose(yaw = 175.0, roll = -90.0))
        move(pose(yaw = -176.0, roll = -90.0))
        assertTappedFocusKept()
        move(pose(yaw = -174.0, roll = -90.0))
        assertBackToContinuous()
    }

    @Test
    fun portraitUprightStillTriggers() {
        tapToFocus(at = pose(yaw = -60.0, pitch = 80.0))
        move(pose(yaw = -60.0, pitch = 75.0))
        assertTappedFocusKept()
        move(pose(yaw = -60.0, pitch = 65.0))
        assertBackToContinuous()
    }

    @Test
    fun manualAndAutomaticFocusStopTheSensors() {
        tapToFocus()
        assertEquals(1, focusListeners().size)
        model.setManualFocus(lensPosition = 0.4f)
        assertTrue(focusListeners().isEmpty())
        tapToFocus()
        model.setAutoFocus()
        assertBackToContinuous()
        tapToFocus()
        model.stopMotionDetection()
        assertTrue(focusListeners().isEmpty())
        move(pose(yaw = 120.0, pitch = 50.0))
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        assertFalse(model.camera.isFocusLocked.value)
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

    @Test
    fun enteringTheBackgroundWithNothingRunningStopsTheSensors() {
        model.media = Media(delegate = mediaDelegate())
        tapToFocus()
        assertEquals(1, focusListeners().size)
        model.handleApplicationDidEnterBackground()
        runMain()
        assertTrue(focusListeners().isEmpty())
        move(pose(yaw = 120.0, pitch = 50.0))
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
    }
}
