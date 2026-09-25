package com.moblin.android.platform.avfoundation

import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.util.Range
import com.moblin.android.platform.capture.CameraControls
import com.moblin.android.platform.capture.FakeCaptureRequest
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AVCaptureDeviceManualControlsSuite {
    private val transform = ColorSpaceTransform(
        intArrayOf(16, 10, -4, 10, -2, 10, -3, 10, 14, 10, -1, 10, 0, 10, -5, 10, 15, 10)
    )

    private fun deliver(
        device: AVCaptureDevice,
        focusDistance: Float? = null,
        sensitivity: Int? = null,
        exposureTimeNs: Long? = null,
        gains: RggbChannelVector? = null,
        afState: Int? = null,
        aeState: Int? = null,
        colorTransform: ColorSpaceTransform? = null,
        awbMode: Int? = null,
        requestTag: Any? = null,
    ) {
        FakeCaptureDevices.deliver(
            device,
            FakeCaptureDevices.result(
                focusDistance = focusDistance,
                sensitivity = sensitivity,
                exposureTimeNs = exposureTimeNs,
                gains = gains,
                afState = afState,
                aeState = aeState,
                colorTransform = colorTransform,
                awbMode = awbMode,
            ),
            requestTag,
        )
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 1e-6f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }

    @Test
    fun cameraWithoutManualCapabilitiesSupportsNoManualControls() {
        val device = FakeCaptureDevices.make()
        assertFalse(device.isLockingFocusWithCustomLensPositionSupported)
        assertFalse(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom))
        assertFalse(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.locked))
        assertFalse(device.isLockingWhiteBalanceWithCustomDeviceGainsSupported)
        assertFalse(device.isFocusPointOfInterestSupported)
        assertFalse(device.isExposurePointOfInterestSupported)
        assertFalse(device.isFocusModeSupported(AVCaptureDevice.FocusMode.continuousAutoFocus))
        assertTrue(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.continuousAutoExposure))
        assertTrue(device.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance))
        assertFalse(device.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.autoWhiteBalance))
    }

    @Test
    fun manualCameraSupportsEveryControl() {
        val device = FakeCaptureDevices.makeManual()
        assertTrue(device.isLockingFocusWithCustomLensPositionSupported)
        assertTrue(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom))
        assertTrue(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.locked))
        assertTrue(device.isExposureModeSupported(AVCaptureDevice.ExposureMode.autoExpose))
        assertTrue(device.isLockingWhiteBalanceWithCustomDeviceGainsSupported)
        assertTrue(device.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.locked))
        assertTrue(device.isFocusPointOfInterestSupported)
        assertTrue(device.isExposurePointOfInterestSupported)
        for (mode in AVCaptureDevice.FocusMode.entries) {
            assertTrue(device.isFocusModeSupported(mode), "$mode")
        }
    }

    @Test
    fun rangesComeFromTheCharacteristics() {
        val device = FakeCaptureDevices.makeManual(
            sensitivityRange = Range(64, 6400),
            exposureTimeRange = Range(13_500L, 250_000_000L),
            compensationRange = Range(-6, 6),
            compensationStep = android.util.Rational(1, 3),
        )
        assertEquals(64f, device.activeFormat.minISO)
        assertEquals(6400f, device.activeFormat.maxISO)
        assertEquals(13L, device.activeFormat.minExposureDuration)
        assertEquals(250_000L, device.activeFormat.maxExposureDuration)
        assertEquals(-2f, device.minExposureTargetBias)
        assertEquals(2f, device.maxExposureTargetBias)
        assertEquals(4f, device.maxWhiteBalanceGain)
    }

    @Test
    fun manualFocusNeedsAnAdjustableLensAndLensControl() {
        val fixed = FakeCaptureDevices.makeManual(minimumFocusDistance = 0f)
        assertFalse(fixed.isLockingFocusWithCustomLensPositionSupported)
        assertFalse(fixed.isFocusPointOfInterestSupported)
        assertTrue(fixed.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom))
        val limited = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
        assertTrue(limited.isLockingFocusWithCustomLensPositionSupported)
        assertFalse(limited.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom))
        assertTrue(limited.isExposureModeSupported(AVCaptureDevice.ExposureMode.locked))
        assertFalse(limited.isLockingWhiteBalanceWithCustomDeviceGainsSupported)
        val legacy = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        )
        assertFalse(legacy.isLockingFocusWithCustomLensPositionSupported)
        val noOff = FakeCaptureDevices.makeManual(
            afModes = intArrayOf(CameraMetadata.CONTROL_AF_MODE_AUTO, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO),
        )
        assertFalse(noOff.isLockingFocusWithCustomLensPositionSupported)
        assertTrue(noOff.isFocusPointOfInterestSupported)
        val noRegions = FakeCaptureDevices.makeManual(maxRegionsAf = 0, maxRegionsAe = 0)
        assertFalse(noRegions.isFocusPointOfInterestSupported)
        assertFalse(noRegions.isExposurePointOfInterestSupported)
    }

    @Test
    fun customExposureNeedsManualSensorAndAutoExposureOff() {
        val noOff = FakeCaptureDevices.makeManual(aeModes = intArrayOf(CameraMetadata.CONTROL_AE_MODE_ON))
        assertFalse(noOff.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom))
        val noWhiteBalanceOff = FakeCaptureDevices.makeManual(awbModes = intArrayOf(CameraMetadata.CONTROL_AWB_MODE_AUTO))
        assertFalse(noWhiteBalanceOff.isLockingWhiteBalanceWithCustomDeviceGainsSupported)
        assertTrue(noWhiteBalanceOff.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.locked))
        val noLocks = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            aeLockAvailable = false,
            awbLockAvailable = false,
        )
        assertFalse(noLocks.isExposureModeSupported(AVCaptureDevice.ExposureMode.locked))
        assertFalse(noLocks.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.locked))
    }

    @Test
    fun lockedLensPositionReadsBackAndRoundTripsThroughTheCamera() {
        val device = FakeCaptureDevices.makeManual()
        deliver(device, focusDistance = 5f)
        for (position in listOf(0.37f, 0f, 1f, 0.75f)) {
            device.lockForConfiguration()
            device.setFocusModeLocked(lensPosition = position)
            device.unlockForConfiguration()
            assertEquals(position, device.lensPosition)
            assertEquals(AVCaptureDevice.FocusMode.locked, device.focusMode)
            val distance = assertNotNull(FakeCaptureRequest.of(device)[CaptureRequest.LENS_FOCUS_DISTANCE])
            assertClose(position, CameraControls.lensPosition(distance, 10f))
            deliver(device, focusDistance = distance)
            assertEquals(position, device.lensPosition)
        }
        deliver(device, focusDistance = 9f)
        assertEquals(0.75f, device.lensPosition)
        device.setFocusModeLocked(lensPosition = AVCaptureDevice.currentLensPosition)
        assertEquals(0.75f, device.lensPosition)
        device.focusMode = AVCaptureDevice.FocusMode.continuousAutoFocus
        assertClose(0.1f, device.lensPosition)
        assertNull(FakeCaptureRequest.of(device)[CaptureRequest.LENS_FOCUS_DISTANCE])
    }

    @Test
    fun customExposureReadsBackWhatWasSetAndIgnoresTheSensor() {
        val device = FakeCaptureDevices.makeManual()
        deliver(device, sensitivity = 400, exposureTimeNs = 10_000_000L)
        device.lockForConfiguration()
        device.setExposureModeCustom(duration = 8_000L, iso = 1234.5f)
        device.unlockForConfiguration()
        assertEquals(AVCaptureDevice.ExposureMode.custom, device.exposureMode)
        assertEquals(1234.5f, device.iso)
        assertEquals(8_000L, device.exposureDuration)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(1235, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(8_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        deliver(device, sensitivity = 1235, exposureTimeNs = 8_000_000L)
        assertEquals(1234.5f, device.iso)
        assertEquals(8_000L, device.exposureDuration)
        device.setExposureModeCustom(duration = AVCaptureDevice.currentExposureDuration, iso = 200f)
        assertEquals(200f, device.iso)
        assertEquals(8_000L, device.exposureDuration)
        device.setExposureModeCustom(duration = 2_000L, iso = AVCaptureDevice.currentISO)
        assertEquals(200f, device.iso)
        assertEquals(2_000L, device.exposureDuration)
        device.exposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure
        assertEquals(1235f, device.iso)
        assertEquals(8_000L, device.exposureDuration)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_MODE])
    }

    @Test
    fun customExposureIsClampedToTheFormat() {
        val device = FakeCaptureDevices.makeManual()
        device.setExposureModeCustom(duration = 1_000_000_000L, iso = 1f)
        assertEquals(device.activeFormat.maxExposureDuration, device.exposureDuration)
        assertEquals(device.activeFormat.minISO, device.iso)
    }

    @Test
    fun lockedWhiteBalanceReadsBackTheGains() {
        val device = FakeCaptureDevices.makeManual()
        deliver(device, gains = RggbChannelVector(2f, 1f, 1f, 1.5f), colorTransform = transform)
        val gains = AVCaptureDevice.WhiteBalanceGains(redGain = 1.2f, greenGain = 1f, blueGain = 2.7f)
        device.lockForConfiguration()
        device.setWhiteBalanceModeLocked(with = gains)
        device.unlockForConfiguration()
        assertEquals(AVCaptureDevice.WhiteBalanceMode.locked, device.whiteBalanceMode)
        assertEquals(gains, device.deviceWhiteBalanceGains)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_OFF, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(transform, request[CaptureRequest.COLOR_CORRECTION_TRANSFORM])
        val sent = assertNotNull(request[CaptureRequest.COLOR_CORRECTION_GAINS])
        val echoed = assertNotNull(CameraControls.deviceGains(sent))
        assertClose(gains.redGain, echoed.redGain, 1e-5f)
        assertClose(gains.blueGain, echoed.blueGain, 1e-5f)
        deliver(device, gains = RggbChannelVector(3f, 1f, 1f, 1f), colorTransform = CameraControls.identityTransform)
        assertEquals(gains, device.deviceWhiteBalanceGains)
        assertEquals(transform, FakeCaptureRequest.of(device)[CaptureRequest.COLOR_CORRECTION_TRANSFORM])
        device.setWhiteBalanceModeLocked(with = AVCaptureDevice.WhiteBalanceGains(0.5f, 1f, 9f))
        assertEquals(AVCaptureDevice.WhiteBalanceGains(1f, 1f, 4f), device.deviceWhiteBalanceGains)
        device.setWhiteBalanceModeLocked(with = AVCaptureDevice.currentWhiteBalanceGains)
        assertEquals(AVCaptureDevice.WhiteBalanceGains(1f, 1f, 4f), device.deviceWhiteBalanceGains)
        device.whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance
        assertEquals(AVCaptureDevice.WhiteBalanceGains(3f, 1f, 1f), device.deviceWhiteBalanceGains)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AWB_MODE])
    }

    @Test
    fun whiteBalanceLockedBeforeAnyResultWaitsForTheAutomaticTransform() {
        val device = FakeCaptureDevices.makeManual()
        val gains = AVCaptureDevice.WhiteBalanceGains(redGain = 1.5f, greenGain = 1f, blueGain = 2f)
        device.setWhiteBalanceModeLocked(with = gains)
        assertEquals(gains, device.deviceWhiteBalanceGains)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AWB_MODE])
        deliver(device, gains = RggbChannelVector(2f, 1f, 1f, 1.5f), colorTransform = transform)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_OFF, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(transform, request[CaptureRequest.COLOR_CORRECTION_TRANSFORM])
        assertEquals(gains, device.deviceWhiteBalanceGains)
    }

    @Test
    fun lockingTheCurrentValuesHoldsWhatTheCameraReported() {
        val device = FakeCaptureDevices.makeManual()
        deliver(
            device,
            focusDistance = 2.5f,
            sensitivity = 640,
            exposureTimeNs = 4_000_000L,
            gains = RggbChannelVector(2f, 1f, 1f, 3f),
            colorTransform = transform,
        )
        device.focusMode = AVCaptureDevice.FocusMode.locked
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        device.whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.locked
        deliver(device, focusDistance = 8f, sensitivity = 100, exposureTimeNs = 1_000_000L, gains = RggbChannelVector(1f, 1f, 1f, 1f))
        assertEquals(0.75f, device.lensPosition)
        assertEquals(640f, device.iso)
        assertEquals(4_000L, device.exposureDuration)
        assertEquals(AVCaptureDevice.WhiteBalanceGains(2f, 1f, 3f), device.deviceWhiteBalanceGains)
        val request = FakeCaptureRequest.of(device)
        assertEquals(2.5f, request[CaptureRequest.LENS_FOCUS_DISTANCE])
        assertEquals(640, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(4_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_OFF, request[CaptureRequest.CONTROL_AWB_MODE])
    }

    @Test
    fun lockedExposureWithoutAutoExposureLockHoldsWhatTheCameraReported() {
        val device = FakeCaptureDevices.makeManual(aeLockAvailable = false)
        deliver(device, sensitivity = 640, exposureTimeNs = 4_000_000L)
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        deliver(device, sensitivity = 100, exposureTimeNs = 1_000_000L)
        assertEquals(640f, device.iso)
        assertEquals(4_000L, device.exposureDuration)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(640, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(4_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertFalse(request.has(CaptureRequest.CONTROL_AE_LOCK))
    }

    @Test
    fun lockingAfterCustomExposureKeepsTheCustomValues() {
        val device = FakeCaptureDevices.makeManual()
        device.setExposureModeCustom(duration = 2_000L, iso = 800f)
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        assertEquals(800f, device.iso)
        assertEquals(2_000L, device.exposureDuration)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_MODE])
    }

    @Test
    fun lockingBeforeTheCameraReportedValuesUsesTheCameraLocks() {
        val device = FakeCaptureDevices.makeManual()
        device.focusMode = AVCaptureDevice.FocusMode.locked
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        device.whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.locked
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AF_MODE_AUTO, request[CaptureRequest.CONTROL_AF_MODE])
        assertFalse(request.has(CaptureRequest.LENS_FOCUS_DISTANCE))
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(true, request[CaptureRequest.CONTROL_AE_LOCK])
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(true, request[CaptureRequest.CONTROL_AWB_LOCK])
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        val trigger = assertNotNull(device.takeControlTrigger())
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED, requestTag = trigger)
        assertEquals(AVCaptureDevice.FocusMode.locked, device.focusMode)
        assertEquals(CameraMetadata.CONTROL_AF_MODE_AUTO, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AF_MODE])
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val exposureTrigger = assertNotNull(device.takeControlTrigger())
        deliver(device, aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED, requestTag = exposureTrigger)
        assertEquals(AVCaptureDevice.ExposureMode.locked, device.exposureMode)
        assertEquals(true, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_LOCK])
    }

    @Test
    fun observersSeeSetValuesButNotTheSensorWhileLocked() {
        val device = FakeCaptureDevices.makeManual()
        deliver(device, focusDistance = 5f, sensitivity = 400, exposureTimeNs = 10_000_000L)
        val focus = mutableListOf<Float>()
        val iso = mutableListOf<Float>()
        val exposure = mutableListOf<Long>()
        val whiteBalance = mutableListOf<AVCaptureDevice.WhiteBalanceGains>()
        val observations = listOf(
            device.observe(AVCaptureDevice::lensPosition) { observed, _ -> focus.add(observed.lensPosition) },
            device.observe(AVCaptureDevice::iso) { observed, _ -> iso.add(observed.iso) },
            device.observe(AVCaptureDevice::exposureDuration) { observed, _ -> exposure.add(observed.exposureDuration) },
            device.observe(AVCaptureDevice::deviceWhiteBalanceGains) { observed, _ ->
                whiteBalance.add(observed.deviceWhiteBalanceGains)
            },
        )
        device.setFocusModeLocked(lensPosition = 0.2f)
        device.setExposureModeCustom(duration = 2_000L, iso = 800f)
        val gains = AVCaptureDevice.WhiteBalanceGains(1.4f, 1f, 1.9f)
        device.setWhiteBalanceModeLocked(with = gains)
        deliver(device, focusDistance = 1f, sensitivity = 100, exposureTimeNs = 1_000_000L, gains = RggbChannelVector(3f, 1f, 1f, 1f))
        assertEquals(listOf(0.2f), focus)
        assertEquals(listOf(800f), iso)
        assertEquals(listOf(2_000L), exposure)
        assertEquals(listOf(gains), whiteBalance)
        device.focusMode = AVCaptureDevice.FocusMode.continuousAutoFocus
        device.exposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure
        assertEquals(listOf(0.2f, 0.9f), focus)
        assertEquals(listOf(800f, 100f), iso)
        assertEquals(listOf(2_000L, 1_000L), exposure)
        observations.forEach { it.invalidate() }
    }

    @Test
    fun unsupportedSettersChangeNothing() {
        val device = FakeCaptureDevices.make()
        deliver(device, focusDistance = 5f)
        device.setFocusModeLocked(lensPosition = 0.2f)
        assertEquals(0.5f, device.lensPosition)
        assertEquals(AVCaptureDevice.FocusMode.continuousAutoFocus, device.focusMode)
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        assertEquals(AVCaptureDevice.FocusMode.continuousAutoFocus, device.focusMode)
        device.setWhiteBalanceModeLocked(with = AVCaptureDevice.WhiteBalanceGains(2f, 1f, 2f))
        assertEquals(AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance, device.whiteBalanceMode)
        device.exposureMode = AVCaptureDevice.ExposureMode.custom
        assertEquals(AVCaptureDevice.ExposureMode.continuousAutoExposure, device.exposureMode)
        assertNull(device.takeControlTrigger())
    }

    @Test
    fun autoFocusTriggersOnceAndLocksWhenTheScanEnds() {
        val device = FakeCaptureDevices.makeManual()
        device.lockForConfiguration()
        device.focusPointOfInterest = PointF(0.25f, 0.5f)
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        device.unlockForConfiguration()
        assertEquals(PointF(0.25f, 0.5f), device.focusPointOfInterest)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AF_MODE_AUTO, request[CaptureRequest.CONTROL_AF_MODE])
        assertEquals(
            MeteringRectangle(700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX),
            assertNotNull(request[CaptureRequest.CONTROL_AF_REGIONS]).single()
        )
        val trigger = assertNotNull(device.takeControlTrigger())
        assertTrue(trigger.focusScan != 0L)
        assertEquals(0L, trigger.exposureScan)
        assertEquals(
            CameraMetadata.CONTROL_AF_TRIGGER_START,
            FakeCaptureRequest.trigger(trigger)[CaptureRequest.CONTROL_AF_TRIGGER]
        )
        assertNull(device.takeControlTrigger())
        deliver(device, focusDistance = 4f, afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED)
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        deliver(device, focusDistance = 4f, afState = CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN, requestTag = trigger)
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        deliver(device, focusDistance = 3f, afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED)
        assertEquals(AVCaptureDevice.FocusMode.locked, device.focusMode)
        assertEquals(0.7f, device.lensPosition)
        deliver(device, focusDistance = 9f, afState = CameraMetadata.CONTROL_AF_STATE_INACTIVE)
        assertEquals(0.7f, device.lensPosition)
        assertClose(3f, assertNotNull(FakeCaptureRequest.of(device)[CaptureRequest.LENS_FOCUS_DISTANCE]), 1e-5f)
        assertNull(device.takeControlTrigger())
    }

    @Test
    fun anotherTapStartsANewScanAndAStaleTriggerDoesNotFinishIt() {
        val device = FakeCaptureDevices.makeManual()
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        val first = assertNotNull(device.takeControlTrigger())
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED, requestTag = first)
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        val second = assertNotNull(device.takeControlTrigger())
        assertTrue(second.focusScan > first.focusScan)
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED, requestTag = second)
        assertEquals(AVCaptureDevice.FocusMode.locked, device.focusMode)
    }

    @Test
    fun scanInProgressIsTriggeredAgainAfterASessionRestart() {
        val device = FakeCaptureDevices.makeManual()
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val trigger = assertNotNull(device.takeControlTrigger())
        assertTrue(trigger.focusScan != 0L && trigger.exposureScan != 0L)
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN, requestTag = trigger)
        device.controlSessionStarted()
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED, aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED)
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        val again = assertNotNull(device.takeControlTrigger())
        assertEquals(trigger.focusScan, again.focusScan)
        assertEquals(trigger.exposureScan, again.exposureScan)
        deliver(
            device,
            focusDistance = 5f,
            afState = CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED,
            sensitivity = 320,
            exposureTimeNs = 5_000_000L,
            aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED,
            requestTag = again,
        )
        assertEquals(AVCaptureDevice.FocusMode.locked, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.locked, device.exposureMode)
        device.controlSessionStarted()
        assertNull(device.takeControlTrigger())
        assertEquals(0.5f, device.lensPosition)
        assertEquals(320f, device.iso)
    }

    @Test
    fun autoExposeLocksTheConvergedExposure() {
        val device = FakeCaptureDevices.makeManual()
        device.exposurePointOfInterest = PointF(0.5f, 0.5f)
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        assertTrue(request.has(CaptureRequest.CONTROL_AE_REGIONS))
        val trigger = assertNotNull(device.takeControlTrigger())
        assertEquals(0L, trigger.focusScan)
        assertEquals(
            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START,
            FakeCaptureRequest.trigger(trigger)[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER]
        )
        deliver(device, sensitivity = 500, exposureTimeNs = 9_000_000L, aeState = CameraMetadata.CONTROL_AE_STATE_PRECAPTURE, requestTag = trigger)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        deliver(device, sensitivity = 400, exposureTimeNs = 10_000_000L, aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED)
        assertEquals(AVCaptureDevice.ExposureMode.locked, device.exposureMode)
        deliver(device, sensitivity = 100, exposureTimeNs = 1_000_000L)
        assertEquals(400f, device.iso)
        assertEquals(10_000L, device.exposureDuration)
        val locked = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, locked[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(400, locked[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(10_000_000L, locked[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertFalse(locked.has(CaptureRequest.CONTROL_AE_REGIONS))
        assertNull(device.takeControlTrigger())
        device.controlSessionStarted()
        assertEquals(400, FakeCaptureRequest.of(device)[CaptureRequest.SENSOR_SENSITIVITY])
    }

    @Test
    fun exposureBiasStillChangesTheExposureAfterAutoExposeLocks() {
        val device = FakeCaptureDevices.makeManual()
        device.setExposureTargetBias(0.5f)
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val trigger = assertNotNull(device.takeControlTrigger())
        deliver(device, sensitivity = 400, exposureTimeNs = 10_000_000L, aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED, requestTag = trigger)
        assertEquals(AVCaptureDevice.ExposureMode.locked, device.exposureMode)
        assertEquals(400, FakeCaptureRequest.of(device)[CaptureRequest.SENSOR_SENSITIVITY])
        val iso = mutableListOf<Float>()
        val observation = device.observe(AVCaptureDevice::iso) { observed, _ -> iso.add(observed.iso) }
        device.setExposureTargetBias(1.5f)
        assertEquals(800f, device.iso)
        assertEquals(10_000L, device.exposureDuration)
        assertEquals(listOf(800f), iso)
        var request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(800, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(10_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        device.setExposureTargetBias(-1.5f)
        assertEquals(100f, device.iso)
        device.setExposureTargetBias(-2f)
        request = FakeCaptureRequest.of(device)
        assertEquals(71, request[CaptureRequest.SENSOR_SENSITIVITY])
        device.setExposureTargetBias(0.5f)
        assertEquals(400f, device.iso)
        assertEquals(10_000L, device.exposureDuration)
        observation.invalidate()
    }

    @Test
    fun exposureBiasMovesTheShutterWhenTheSensitivityIsAtItsLimit() {
        val device = FakeCaptureDevices.makeManual(sensitivityRange = Range(100, 1600))
        deliver(device, sensitivity = 200, exposureTimeNs = 10_000_000L)
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        device.setExposureTargetBias(-2f)
        assertEquals(100f, device.iso)
        assertEquals(5_000L, device.exposureDuration)
        device.setExposureTargetBias(2f)
        assertEquals(800f, device.iso)
        assertEquals(10_000L, device.exposureDuration)
        device.exposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure
        device.setExposureTargetBias(0f)
        deliver(device, sensitivity = 800, exposureTimeNs = 20_000_000L)
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        device.setExposureTargetBias(2f)
        assertEquals(1600f, device.iso)
        assertEquals(33_333L, device.exposureDuration)
        assertEquals(33_333_000L, FakeCaptureRequest.of(device)[CaptureRequest.SENSOR_EXPOSURE_TIME])
    }

    @Test
    fun exposureBiasDoesNotChangeCustomExposure() {
        val device = FakeCaptureDevices.makeManual()
        device.setExposureModeCustom(duration = 2_000L, iso = 800f)
        device.setExposureTargetBias(1f)
        assertEquals(800f, device.iso)
        assertEquals(2_000L, device.exposureDuration)
        val request = FakeCaptureRequest.of(device)
        assertEquals(800, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(2_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
    }

    @Test
    fun exposureBiasWithTheAutoExposureLockIsSentAsCompensation() {
        val device = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
        )
        device.exposureMode = AVCaptureDevice.ExposureMode.locked
        device.setExposureTargetBias(1f)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(true, request[CaptureRequest.CONTROL_AE_LOCK])
        assertEquals(6, request[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
    }

    @Test
    fun failedTriggerIsSentAgainOnce() {
        val device = FakeCaptureDevices.makeManual()
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val first = assertNotNull(device.takeControlTrigger())
        assertNull(device.takeControlTrigger())
        device.controlTriggerFailed(first)
        val again = assertNotNull(device.takeControlTrigger())
        assertEquals(first.focusScan, again.focusScan)
        assertEquals(first.exposureScan, again.exposureScan)
        device.controlTriggerFailed(again)
        assertNull(device.takeControlTrigger())
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        val next = assertNotNull(device.takeControlTrigger())
        assertTrue(next.focusScan > first.focusScan)
        assertEquals(0L, next.exposureScan)
        device.controlSessionStarted()
        device.controlTriggerFailed(next)
        val resent = assertNotNull(device.takeControlTrigger())
        assertEquals(next.focusScan, resent.focusScan)
        device.controlTriggerFailed(resent)
        assertNotNull(device.takeControlTrigger())
        device.controlTriggerFailed(resent)
        assertNull(device.takeControlTrigger())
    }

    @Test
    fun failedTriggerAfterTheScanStartedIsNotSentAgain() {
        val device = FakeCaptureDevices.makeManual()
        device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        val trigger = assertNotNull(device.takeControlTrigger())
        deliver(device, afState = CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN, requestTag = trigger)
        device.controlTriggerFailed(trigger)
        assertNull(device.takeControlTrigger())
    }

    @Test
    fun whiteBalanceTemperatureFollowsTheCameraAutomaticGains() {
        val device = FakeCaptureDevices.makeManual()
        for (gains in listOf(RggbChannelVector(2.03f, 1f, 1f, 1.72f), RggbChannelVector(1.25f, 1f, 1f, 2.9f))) {
            deliver(device, gains = gains, colorTransform = transform)
            val observed = device.deviceWhiteBalanceGains
            val temperature = device.temperatureAndTintValues(observed)
            val computed = device.deviceWhiteBalanceGains(temperature)
            assertClose(observed.redGain, computed.redGain, 1e-4f)
            assertClose(observed.greenGain, computed.greenGain, 1e-4f)
            assertClose(observed.blueGain, computed.blueGain, 1e-4f)
        }
    }

    @Test
    fun whiteBalanceScaleIsLearnedOnlyFromAutomaticFrames() {
        val device = FakeCaptureDevices.makeManual()
        deliver(device, gains = RggbChannelVector(2f, 1f, 1f, 2f), colorTransform = transform)
        val temperature = AVCaptureDevice.WhiteBalanceTemperatureAndTintValues(temperature = 4000f, tint = 0f)
        val before = device.deviceWhiteBalanceGains(temperature)
        device.setWhiteBalanceModeLocked(with = AVCaptureDevice.WhiteBalanceGains(1f, 1f, 1f))
        deliver(device, gains = RggbChannelVector(1f, 1f, 1f, 1f))
        deliver(device, gains = RggbChannelVector(1f, 1f, 1f, 1f), awbMode = CameraMetadata.CONTROL_AWB_MODE_OFF)
        assertEquals(before, device.deviceWhiteBalanceGains(temperature))
        deliver(device, gains = RggbChannelVector(1.5f, 1f, 1f, 1.5f), awbMode = CameraMetadata.CONTROL_AWB_MODE_AUTO)
        assertTrue(device.deviceWhiteBalanceGains(temperature) != before)
    }

    @Test
    fun autoExposeWithoutManualSensorLocksWithTheAutoExposureLock() {
        val device = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
        )
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        val trigger = assertNotNull(device.takeControlTrigger())
        deliver(device, sensitivity = 400, aeState = CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED, requestTag = trigger)
        assertEquals(AVCaptureDevice.ExposureMode.locked, device.exposureMode)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(true, request[CaptureRequest.CONTROL_AE_LOCK])
        deliver(device, sensitivity = 800)
        assertEquals(800f, device.iso)
    }

    @Test
    fun autoExposeWithoutAnyLockKeepsMeteringAtThePoint() {
        val device = FakeCaptureDevices.makeManual(
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            aeLockAvailable = false,
        )
        device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        assertNull(device.takeControlTrigger())
        deliver(device, aeState = CameraMetadata.CONTROL_AE_STATE_CONVERGED)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        assertTrue(FakeCaptureRequest.of(device).has(CaptureRequest.CONTROL_AE_REGIONS))
    }

    @Test
    fun exposureTargetBiasReadsBackAndIsSentInSteps() {
        val device = FakeCaptureDevices.makeManual()
        device.setExposureTargetBias(0.5f)
        assertEquals(0.5f, device.exposureTargetBias)
        assertEquals(3, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        device.setExposureTargetBias(-1.3f)
        assertEquals(-1.3f, device.exposureTargetBias)
        assertEquals(-8, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        device.setExposureTargetBias(3f)
        assertEquals(2f, device.exposureTargetBias)
        assertEquals(12, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
    }

    @Test
    fun pointOfInterestRotationFollowsTheSensorAndFacing() {
        val cases = listOf(
            Triple(CameraCharacteristics.LENS_FACING_BACK, 90, 0),
            Triple(CameraCharacteristics.LENS_FACING_BACK, 270, 180),
            Triple(CameraCharacteristics.LENS_FACING_BACK, 0, 270),
            Triple(CameraCharacteristics.LENS_FACING_BACK, 180, 90),
            Triple(CameraCharacteristics.LENS_FACING_FRONT, 270, 0),
            Triple(CameraCharacteristics.LENS_FACING_FRONT, 90, 180),
            Triple(CameraCharacteristics.LENS_FACING_EXTERNAL, 0, 0),
        )
        for ((facing, sensorOrientation, rotation) in cases) {
            val device = FakeCaptureDevices.makeManual(facing = facing, sensorOrientation = sensorOrientation)
            val entry = assertNotNull(device.camera)
            assertEquals(rotation, CameraControls.pointOfInterestRotation(entry), "facing $facing sensor $sensorOrientation")
        }
    }

    @Test
    fun pointOfInterestLandsOnTheSensorForEachOrientation() {
        val point = PointF(0.25f, 0.5f)
        val expected = mapOf(
            90 to MeteringRectangle(700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX),
            270 to MeteringRectangle(2700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX),
            0 to MeteringRectangle(1700, 769, 600, 337, MeteringRectangle.METERING_WEIGHT_MAX),
            180 to MeteringRectangle(1700, 1894, 600, 337, MeteringRectangle.METERING_WEIGHT_MAX),
        )
        for ((sensorOrientation, region) in expected) {
            val device = FakeCaptureDevices.makeManual(sensorOrientation = sensorOrientation)
            device.focusPointOfInterest = point
            device.focusMode = AVCaptureDevice.FocusMode.autoFocus
            val request = FakeCaptureRequest.of(device)
            assertEquals(region, assertNotNull(request[CaptureRequest.CONTROL_AF_REGIONS]).single(), "sensor $sensorOrientation")
        }
    }
}
