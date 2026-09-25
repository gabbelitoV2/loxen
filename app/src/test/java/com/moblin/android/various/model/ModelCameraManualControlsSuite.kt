package com.moblin.android.various.model

import android.graphics.PointF
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.os.Looper
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.FakeCaptureDevices
import com.moblin.android.platform.capture.FakeCaptureRequest
import com.moblin.android.platform.uikit.UIDevice
import com.moblin.android.platform.uikit.UIDeviceOrientation
import com.moblin.android.various.utils.clamped
import com.moblin.android.various.utils.exposures
import com.moblin.android.various.utils.factorFromExposure
import com.moblin.android.various.utils.factorFromIso
import com.moblin.android.various.utils.factorFromWhiteBalance
import com.moblin.android.various.utils.factorToExposure
import com.moblin.android.various.utils.factorToIso
import com.moblin.android.various.utils.factorToWhiteBalance
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelCameraManualControlsSuite {
    private lateinit var model: Model
    private lateinit var device: AVCaptureDevice
    private lateinit var captureDevice: CaptureDevice
    private var portrait = false

    @Before
    fun setUp() {
        model = Model()
        device = FakeCaptureDevices.makeManual()
        captureDevice = CaptureDevice(device = device, id = UUID.randomUUID(), isVideoMirrored = false)
        model.cameraDevice = captureDevice
        portrait = model.stream.value.portrait
    }

    @After
    fun tearDown() {
        model.stream.value.portrait = portrait
        setDeviceOrientation(UIDeviceOrientation.unknown)
    }

    private fun setDeviceOrientation(orientation: Int) {
        val field = UIDevice::class.java.getDeclaredField("orientation")
        field.isAccessible = true
        field.setInt(UIDevice.current, orientation)
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 1e-6f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }

    @Test
    fun manualControlsFollowTheCamera() {
        assertTrue(model.isCameraSupportingManualFocus())
        assertTrue(model.isCameraSupportingManualExposureAndIso())
        assertTrue(model.isCameraSupportingManualWhiteBalance())
        val plain = FakeCaptureDevices.make()
        model.cameraDevice = CaptureDevice(device = plain, id = UUID.randomUUID(), isVideoMirrored = false)
        assertFalse(model.isCameraSupportingManualFocus())
        assertFalse(model.isCameraSupportingManualExposureAndIso())
        assertFalse(model.isCameraSupportingManualWhiteBalance())
        model.cameraDevice = null
        assertFalse(model.isCameraSupportingManualFocus())
    }

    @Test
    fun focusSliderReadsBackItsValue() {
        model.startObservingFocus()
        for (value in listOf(0.42f, 0f, 1f, 0.8f)) {
            model.setManualFocus(lensPosition = value)
            runMain()
            assertEquals(value, device.lensPosition)
            assertEquals(value, model.camera.lockedFocus.value)
            assertEquals(value, model.camera.lockedFocuses[captureDevice])
            assertTrue(model.camera.isFocusLocked.value)
        }
        FakeCaptureDevices.deliver(device, FakeCaptureDevices.result(focusDistance = 9f))
        runMain()
        assertEquals(0.8f, model.camera.lockedFocus.value)
        assertClose(2f, assertNotNull(FakeCaptureRequest.of(device)[CaptureRequest.LENS_FOCUS_DISTANCE]), 1e-5f)
    }

    @Test
    fun isoSliderReadsBackItsValue() {
        model.startObservingIso()
        for (factor in listOf(0.3f, 0f, 1f, 0.55f)) {
            model.setManualIso(factor = factor)
            runMain()
            assertEquals(factorToIso(device = device, factor = factor), device.iso)
            assertClose(factor, factorFromIso(device = device, iso = device.iso))
            assertClose(factor, model.camera.lockedIso.value)
            assertTrue(model.camera.isExposureAndIsoLocked.value)
        }
        FakeCaptureDevices.deliver(device, FakeCaptureDevices.result(sensitivity = 100))
        runMain()
        assertClose(0.55f, model.camera.lockedIso.value)
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_MODE])
    }

    @Test
    fun exposureSliderReadsBackItsValue() {
        model.startObservingExposure()
        val count = exposures(device = device).size
        assertTrue(count > 2)
        for (index in listOf(0, count - 1, count / 2)) {
            val factor = index.toFloat() / (count - 1)
            model.setManualExposure(factor = factor)
            runMain()
            val exposure = factorToExposure(device = device, factor = factor)
            assertEquals(exposure, device.exposureDuration)
            assertEquals(factor, factorFromExposure(device = device, exposure = device.exposureDuration))
            assertEquals(factor, model.camera.lockedExposure.value)
            assertEquals(exposure, model.camera.exposure.value)
            assertEquals(exposure * 1000, FakeCaptureRequest.of(device)[CaptureRequest.SENSOR_EXPOSURE_TIME])
        }
    }

    @Test
    fun isoAndExposureKeepEachOther() {
        model.setManualIso(factor = 0.5f)
        val iso = device.iso
        val factor = 2f / (exposures(device = device).size - 1)
        model.setManualExposure(factor = factor)
        assertEquals(iso, device.iso)
        assertEquals(factorToExposure(device = device, factor = factor), device.exposureDuration)
        model.setManualIso(factor = 0.25f)
        assertEquals(factorToExposure(device = device, factor = factor), device.exposureDuration)
        model.setAutoExposureAndIso()
        assertEquals(AVCaptureDevice.ExposureMode.continuousAutoExposure, device.exposureMode)
        assertFalse(model.camera.isExposureAndIsoLocked.value)
        FakeCaptureDevices.deliver(device, FakeCaptureDevices.result(sensitivity = 777))
        assertEquals(777f, device.iso)
    }

    @Test
    fun whiteBalanceSliderReadsBackItsValue() {
        model.startObservingWhiteBalance()
        for (factor in listOf(0.5f, 0.2f, 1f, 0.35f)) {
            model.setManualWhiteBalance(factor = factor)
            runMain()
            val gains = factorToWhiteBalance(device = device, factor = factor)
            assertEquals(gains, device.deviceWhiteBalanceGains)
            val readBack = factorFromWhiteBalance(
                device = device,
                gains = device.deviceWhiteBalanceGains.clamped(maxGain = device.maxWhiteBalanceGain),
            )
            assertClose(factor, readBack, 1e-4f)
            assertClose(factor, model.camera.lockedWhiteBalance.value, 1e-4f)
            assertTrue(model.camera.isWhiteBalanceLocked.value)
        }
        FakeCaptureDevices.deliver(device, FakeCaptureDevices.result(gains = RggbChannelVector(3f, 1f, 1f, 1f)))
        runMain()
        assertClose(0.35f, model.camera.lockedWhiteBalance.value, 1e-4f)
        model.setAutoWhiteBalance()
        assertEquals(AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance, device.whiteBalanceMode)
        assertEquals(AVCaptureDevice.WhiteBalanceGains(3f, 1f, 1f), device.deviceWhiteBalanceGains)
    }

    @Test
    fun lockingWhiteBalanceAtTheShownValueKeepsTheCameraColors() {
        val transform = android.hardware.camera2.params.ColorSpaceTransform(
            intArrayOf(16, 10, -4, 10, -2, 10, -3, 10, 14, 10, -1, 10, 0, 10, -5, 10, 15, 10)
        )
        for (automatic in listOf(RggbChannelVector(2.03f, 1f, 1f, 1.72f), RggbChannelVector(1.25f, 1f, 1f, 2.9f))) {
            model.setAutoWhiteBalance()
            FakeCaptureDevices.deliver(device, FakeCaptureDevices.result(gains = automatic, colorTransform = transform))
            model.startObservingWhiteBalance()
            model.setManualWhiteBalance(factor = model.camera.lockedWhiteBalance.value)
            val request = FakeCaptureRequest.of(device)
            assertEquals(CameraMetadata.CONTROL_AWB_MODE_OFF, request[CaptureRequest.CONTROL_AWB_MODE])
            val sent = assertNotNull(request[CaptureRequest.COLOR_CORRECTION_GAINS])
            assertClose(automatic.red, sent.red, 1e-3f)
            assertClose(1f, sent.greenEven, 1e-6f)
            assertClose(automatic.blue, sent.blue, 1e-3f)
            model.stopObservingWhiteBalance()
        }
    }

    @Test
    fun exposureBiasReachesTheCameraInSteps() {
        model.setExposureBias(bias = 0.5f)
        assertEquals(0.5f, device.exposureTargetBias)
        assertEquals(3, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        model.setExposureBias(bias = 3f)
        assertEquals(0.5f, device.exposureTargetBias)
        model.setExposureBias(bias = -2f)
        assertEquals(-2f, device.exposureTargetBias)
        assertEquals(-12, FakeCaptureRequest.of(device)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
    }

    @Test
    fun tapToFocusInPortraitMapsTheTapToTheSensor() {
        model.stream.value.portrait = true
        model.setFocusPointOfInterest(focusPoint = PointF(0.5f, 0.75f))
        assertEquals(PointF(0.75f, 0.5f), device.focusPointOfInterest)
        assertEquals(PointF(0.75f, 0.5f), device.exposurePointOfInterest)
        assertEquals(AVCaptureDevice.FocusMode.autoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.autoExpose, device.exposureMode)
        assertEquals(PointF(0.5f, 0.75f), model.camera.manualFocusPoint.value)
        val request = FakeCaptureRequest.of(device)
        val region = MeteringRectangle(2700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX)
        assertEquals(region, assertNotNull(request[CaptureRequest.CONTROL_AF_REGIONS]).single())
        assertEquals(region, assertNotNull(request[CaptureRequest.CONTROL_AE_REGIONS]).single())
        val trigger = assertNotNull(device.takeControlTrigger())
        assertTrue(trigger.focusScan != 0L && trigger.exposureScan != 0L)
    }

    @Test
    fun tapToFocusInLandscapeFollowsTheDeviceOrientation() {
        model.stream.value.portrait = false
        setDeviceOrientation(UIDeviceOrientation.landscapeLeft)
        model.setFocusPointOfInterest(focusPoint = PointF(0.2f, 0.3f))
        assertEquals(PointF(0.2f, 0.3f), device.focusPointOfInterest)
        setDeviceOrientation(UIDeviceOrientation.landscapeRight)
        model.setFocusPointOfInterest(focusPoint = PointF(0.2f, 0.25f))
        assertEquals(PointF(0.8f, 0.75f), device.focusPointOfInterest)
        assertEquals(PointF(0.8f, 0.75f), device.exposurePointOfInterest)
    }

    @Test
    fun autoFocusReturnsToContinuousModesAtTheCenter() {
        model.setManualFocus(lensPosition = 0.3f)
        model.stream.value.portrait = true
        model.setFocusPointOfInterest(focusPoint = PointF(0.1f, 0.1f))
        model.setAutoFocus()
        assertEquals(AVCaptureDevice.FocusMode.continuousAutoFocus, device.focusMode)
        assertEquals(AVCaptureDevice.ExposureMode.continuousAutoExposure, device.exposureMode)
        assertEquals(PointF(0.5f, 0.5f), device.focusPointOfInterest)
        assertEquals(null, model.camera.manualFocusPoint.value)
        assertFalse(model.camera.isFocusLocked.value)
        val request = FakeCaptureRequest.of(device)
        assertEquals(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO, request[CaptureRequest.CONTROL_AF_MODE])
        assertFalse(request.has(CaptureRequest.CONTROL_AF_REGIONS))
        assertFalse(request.has(CaptureRequest.CONTROL_AE_REGIONS))
    }

    @Test
    fun lockedValuesAreReappliedAfterTheCameraIsAttachedAgain() {
        model.setManualIso(factor = 0.4f)
        model.setManualWhiteBalance(factor = 0.6f)
        val iso = device.iso
        val gains = device.deviceWhiteBalanceGains
        model.setAutoWhiteBalance()
        model.setExposureAndIsoAfterCameraAttach(device = captureDevice)
        model.setWhiteBalanceAfterCameraAttach(device = captureDevice)
        assertEquals(AVCaptureDevice.ExposureMode.custom, device.exposureMode)
        assertEquals(iso, device.iso)
        assertEquals(AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance, device.whiteBalanceMode)
        model.setManualWhiteBalance(factor = 0.6f)
        model.setWhiteBalanceAfterCameraAttach(device = captureDevice)
        assertEquals(gains, device.deviceWhiteBalanceGains)
    }
}
