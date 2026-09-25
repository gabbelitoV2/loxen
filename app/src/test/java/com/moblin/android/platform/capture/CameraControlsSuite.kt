package com.moblin.android.platform.capture

import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.util.Range
import android.util.Size
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraControlsSuite {
    private val fullAfModes = setOf(
        CameraMetadata.CONTROL_AF_MODE_OFF,
        CameraMetadata.CONTROL_AF_MODE_AUTO,
        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    )

    private fun capabilities(
        manualSensor: Boolean = true,
        manualPostProcessing: Boolean = true,
        hardwareLevel: Int? = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        minimumFocusDistance: Float = 10f,
        afModes: Set<Int> = fullAfModes,
        aeModes: Set<Int> = setOf(CameraMetadata.CONTROL_AE_MODE_OFF, CameraMetadata.CONTROL_AE_MODE_ON),
        awbModes: Set<Int> = setOf(CameraMetadata.CONTROL_AWB_MODE_OFF, CameraMetadata.CONTROL_AWB_MODE_AUTO),
        maxRegionsAf: Int = 1,
        maxRegionsAe: Int = 1,
        aeLockAvailable: Boolean = true,
        awbLockAvailable: Boolean = true,
        sensitivityRange: Range<Int>? = Range(50, 3200),
        exposureTimeRange: Range<Long>? = Range(10_000L, 500_000_000L),
        aeCompensationRange: Range<Int>? = Range(-12, 12),
        aeCompensationStep: Float = 1f / 6,
    ) = CameraControlCapabilities(
        manualSensor = manualSensor,
        manualPostProcessing = manualPostProcessing,
        hardwareLevel = hardwareLevel,
        minimumFocusDistance = minimumFocusDistance,
        afModes = afModes,
        aeModes = aeModes,
        awbModes = awbModes,
        maxRegionsAf = maxRegionsAf,
        maxRegionsAe = maxRegionsAe,
        aeLockAvailable = aeLockAvailable,
        awbLockAvailable = awbLockAvailable,
        sensitivityRange = sensitivityRange,
        exposureTimeRange = exposureTimeRange,
        aeCompensationRange = aeCompensationRange,
        aeCompensationStep = aeCompensationStep,
    )

    private fun state(
        focusMode: AVCaptureDevice.FocusMode = AVCaptureDevice.FocusMode.continuousAutoFocus,
        focusPointOfInterest: PointF = PointF(0.5f, 0.5f),
        lensPosition: Float? = null,
        exposureMode: AVCaptureDevice.ExposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure,
        exposurePointOfInterest: PointF = PointF(0.5f, 0.5f),
        iso: Float? = null,
        exposureDuration: Long? = null,
        exposureTargetBias: Float = 0f,
        whiteBalanceMode: AVCaptureDevice.WhiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance,
        whiteBalanceGains: AVCaptureDevice.WhiteBalanceGains? = null,
        colorTransform: ColorSpaceTransform? = null,
    ) = CameraControlState(
        focusMode = focusMode,
        focusPointOfInterest = focusPointOfInterest,
        lensPosition = lensPosition,
        exposureMode = exposureMode,
        exposurePointOfInterest = exposurePointOfInterest,
        iso = iso,
        exposureDuration = exposureDuration,
        exposureTargetBias = exposureTargetBias,
        whiteBalanceMode = whiteBalanceMode,
        whiteBalanceGains = whiteBalanceGains,
        colorTransform = colorTransform,
    )

    private val metering = MeteringArea(Rect(0, 375, 4000, 2625), 0)

    private fun request(
        state: CameraControlState,
        capabilities: CameraControlCapabilities = capabilities(),
        frameRateRange: Range<Int> = Range(30, 30),
        lowLightBoost: Boolean = false,
        metering: MeteringArea? = this.metering,
    ): FakeCaptureRequest {
        val request = FakeCaptureRequest()
        CameraControls.apply(request, state, capabilities, frameRateRange, lowLightBoost, metering)
        return request
    }

    private val transform = ColorSpaceTransform(
        intArrayOf(16, 10, -4, 10, -2, 10, -3, 10, 14, 10, -1, 10, 0, 10, -5, 10, 15, 10)
    )

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 1e-6f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }

    @Test
    fun automaticModesLeaveTheCameraInCharge() {
        val request = request(state())
        assertEquals(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO, request[CaptureRequest.CONTROL_AF_MODE])
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(0, request[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        for (key in listOf(
            CaptureRequest.LENS_FOCUS_DISTANCE,
            CaptureRequest.CONTROL_AF_REGIONS,
            CaptureRequest.CONTROL_AE_REGIONS,
            CaptureRequest.CONTROL_AE_LOCK,
            CaptureRequest.CONTROL_AWB_LOCK,
            CaptureRequest.SENSOR_SENSITIVITY,
            CaptureRequest.SENSOR_EXPOSURE_TIME,
            CaptureRequest.SENSOR_FRAME_DURATION,
            CaptureRequest.COLOR_CORRECTION_MODE,
            CaptureRequest.COLOR_CORRECTION_GAINS,
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
        )) {
            assertFalse(request.has(key), "unexpected $key")
        }
    }

    @Test
    fun continuousAutoFocusFallsBackToPictureModeAndFixedFocusSetsNothing() {
        val picture = request(
            state(),
            capabilities(afModes = setOf(CameraMetadata.CONTROL_AF_MODE_AUTO, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)),
        )
        assertEquals(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE, picture[CaptureRequest.CONTROL_AF_MODE])
        val fixed = request(state(), capabilities(minimumFocusDistance = 0f, afModes = setOf(CameraMetadata.CONTROL_AF_MODE_OFF)))
        assertFalse(fixed.has(CaptureRequest.CONTROL_AF_MODE))
    }

    @Test
    fun lockedLensPositionTurnsAutofocusOffAtTheInverseDistance() {
        val request = request(state(focusMode = AVCaptureDevice.FocusMode.locked, lensPosition = 0.75f))
        assertEquals(CameraMetadata.CONTROL_AF_MODE_OFF, request[CaptureRequest.CONTROL_AF_MODE])
        assertEquals(2.5f, request[CaptureRequest.LENS_FOCUS_DISTANCE])
        assertFalse(request.has(CaptureRequest.CONTROL_AF_REGIONS))
        val infinity = request(state(focusMode = AVCaptureDevice.FocusMode.locked, lensPosition = 1f))
        assertEquals(0f, infinity[CaptureRequest.LENS_FOCUS_DISTANCE])
        val nearest = request(state(focusMode = AVCaptureDevice.FocusMode.locked, lensPosition = 0f))
        assertEquals(10f, nearest[CaptureRequest.LENS_FOCUS_DISTANCE])
    }

    @Test
    fun lockedFocusWithoutLensControlHoldsTheLensInAutoMode() {
        val request = request(
            state(focusMode = AVCaptureDevice.FocusMode.locked, lensPosition = null),
            capabilities(manualSensor = false, hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY),
        )
        assertEquals(CameraMetadata.CONTROL_AF_MODE_AUTO, request[CaptureRequest.CONTROL_AF_MODE])
        assertFalse(request.has(CaptureRequest.LENS_FOCUS_DISTANCE))
    }

    @Test
    fun lensPositionAndFocusDistanceAreInverses() {
        for (position in listOf(0f, 0.1f, 0.25f, 0.37f, 0.5f, 0.75f, 0.9f, 1f)) {
            val distance = CameraControls.focusDistance(position, 10f)
            assertClose(position, CameraControls.lensPosition(distance, 10f))
        }
        for (distance in listOf(0f, 1f, 2.5f, 7.3f, 10f)) {
            val position = CameraControls.lensPosition(distance, 10f)
            assertClose(distance, CameraControls.focusDistance(position, 10f), 1e-5f)
        }
        assertEquals(1f, CameraControls.lensPosition(3f, 0f))
        assertEquals(0f, CameraControls.focusDistance(0.3f, 0f))
    }

    @Test
    fun autoFocusMetersAtThePointOfInterest() {
        val request = request(
            state(focusMode = AVCaptureDevice.FocusMode.autoFocus, focusPointOfInterest = PointF(0.25f, 0.5f))
        )
        assertEquals(CameraMetadata.CONTROL_AF_MODE_AUTO, request[CaptureRequest.CONTROL_AF_MODE])
        val region = assertNotNull(request[CaptureRequest.CONTROL_AF_REGIONS]).single()
        assertEquals(MeteringRectangle(700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX), region)
        assertFalse(request.has(CaptureRequest.CONTROL_AF_TRIGGER))
    }

    @Test
    fun pointOfInterestNeedsRegionsAndAnActiveArray() {
        val focusState = state(focusMode = AVCaptureDevice.FocusMode.autoFocus, focusPointOfInterest = PointF(0.2f, 0.2f))
        assertFalse(request(focusState, capabilities(maxRegionsAf = 0)).has(CaptureRequest.CONTROL_AF_REGIONS))
        assertFalse(request(focusState, metering = null).has(CaptureRequest.CONTROL_AF_REGIONS))
        val exposureState = state(exposureMode = AVCaptureDevice.ExposureMode.autoExpose)
        assertFalse(request(exposureState, capabilities(maxRegionsAe = 0)).has(CaptureRequest.CONTROL_AE_REGIONS))
        assertTrue(request(exposureState).has(CaptureRequest.CONTROL_AE_REGIONS))
    }

    @Test
    fun continuousModesMeterOnlyAwayFromTheCenter() {
        val center = request(state())
        assertFalse(center.has(CaptureRequest.CONTROL_AF_REGIONS))
        assertFalse(center.has(CaptureRequest.CONTROL_AE_REGIONS))
        val offCenter = request(
            state(focusPointOfInterest = PointF(0.1f, 0.9f), exposurePointOfInterest = PointF(0.1f, 0.9f))
        )
        assertEquals(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO, offCenter[CaptureRequest.CONTROL_AF_MODE])
        assertTrue(offCenter.has(CaptureRequest.CONTROL_AF_REGIONS))
        assertTrue(offCenter.has(CaptureRequest.CONTROL_AE_REGIONS))
    }

    @Test
    fun customExposureTurnsAutoExposureOffAndHoldsTheFrameRate() {
        val request = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 1234.4f, exposureDuration = 8_000L)
        )
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, request[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(1234, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(8_000_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(33_333_333L, request[CaptureRequest.SENSOR_FRAME_DURATION])
        assertFalse(request.has(CaptureRequest.CONTROL_AE_REGIONS))
        assertFalse(request.has(CaptureRequest.CONTROL_AE_LOCK))
        val long = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 100f, exposureDuration = 100_000L)
        )
        assertEquals(33_333_333L, long[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(33_333_333L, long[CaptureRequest.SENSOR_FRAME_DURATION])
        val variable = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 100f, exposureDuration = 50_000L),
            frameRateRange = Range(15, 30),
        )
        assertEquals(50_000_000L, variable[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(50_000_000L, variable[CaptureRequest.SENSOR_FRAME_DURATION])
        val fast = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 100f, exposureDuration = 125L),
            frameRateRange = Range(15, 30),
        )
        assertEquals(125_000L, fast[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(33_333_333L, fast[CaptureRequest.SENSOR_FRAME_DURATION])
    }

    @Test
    fun customExposureIsClampedToTheSensorRanges() {
        val request = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 10_000f, exposureDuration = 1L)
        )
        assertEquals(3200, request[CaptureRequest.SENSOR_SENSITIVITY])
        assertEquals(10_000L, request[CaptureRequest.SENSOR_EXPOSURE_TIME])
        assertEquals(33_333_333L, request[CaptureRequest.SENSOR_FRAME_DURATION])
    }

    @Test
    fun lockedExposureWithoutManualSensorUsesTheAutoExposureLock() {
        val locked = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.locked),
            capabilities(manualSensor = false),
        )
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, locked[CaptureRequest.CONTROL_AE_MODE])
        assertEquals(true, locked[CaptureRequest.CONTROL_AE_LOCK])
        assertFalse(locked.has(CaptureRequest.SENSOR_EXPOSURE_TIME))
        val noLock = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.locked),
            capabilities(manualSensor = false, aeLockAvailable = false),
        )
        assertFalse(noLock.has(CaptureRequest.CONTROL_AE_LOCK))
    }

    @Test
    fun autoExposeMetersAtThePointOfInterest() {
        val request = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.autoExpose, exposurePointOfInterest = PointF(0.5f, 0.5f))
        )
        assertEquals(CameraMetadata.CONTROL_AE_MODE_ON, request[CaptureRequest.CONTROL_AE_MODE])
        val region = assertNotNull(request[CaptureRequest.CONTROL_AE_REGIONS]).single()
        assertEquals(MeteringRectangle(1700, 1331, 600, 338, MeteringRectangle.METERING_WEIGHT_MAX), region)
        assertFalse(request.has(CaptureRequest.CONTROL_AE_LOCK))
    }

    @Test
    fun lowLightBoostOnlyAppliesToAutomaticExposure() {
        val automatic = request(state(), lowLightBoost = true)
        assertEquals(
            CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
            automatic[CaptureRequest.CONTROL_AE_MODE]
        )
        val custom = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 100f, exposureDuration = 1_000L),
            lowLightBoost = true,
        )
        assertEquals(CameraMetadata.CONTROL_AE_MODE_OFF, custom[CaptureRequest.CONTROL_AE_MODE])
    }

    @Test
    fun exposureBiasIsSentInCompensationSteps() {
        val capabilities = capabilities()
        assertEquals(-2f, capabilities.minExposureTargetBias)
        assertEquals(2f, capabilities.maxExposureTargetBias)
        val cases = listOf(0f to 0, 0.5f to 3, 1f to 6, -0.4f to -2, 1.95f to 12, 2f to 12, -2f to -12, 5f to 12, -5f to -12)
        for ((bias, steps) in cases) {
            assertEquals(steps, request(state(exposureTargetBias = bias))[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        }
        val thirds = capabilities(aeCompensationRange = Range(-6, 6), aeCompensationStep = 1f / 3)
        assertEquals(-2f, thirds.minExposureTargetBias)
        assertEquals(2f, thirds.maxExposureTargetBias)
        assertEquals(2, request(state(exposureTargetBias = 0.7f), thirds)[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        val custom = request(
            state(exposureMode = AVCaptureDevice.ExposureMode.custom, iso = 100f, exposureDuration = 1_000L, exposureTargetBias = 1f)
        )
        assertEquals(6, custom[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION])
        val none = capabilities(aeCompensationRange = null, aeCompensationStep = 0f)
        assertEquals(0f, none.minExposureTargetBias)
        assertEquals(0f, none.maxExposureTargetBias)
        assertFalse(request(state(exposureTargetBias = 1f), none).has(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION))
    }

    @Test
    fun lockedWhiteBalanceSendsGainsWithTheHeldTransform() {
        val gains = AVCaptureDevice.WhiteBalanceGains(redGain = 1.8f, greenGain = 1f, blueGain = 2.2f)
        val request = request(
            state(
                whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.locked,
                whiteBalanceGains = gains,
                colorTransform = transform,
            )
        )
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_OFF, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(
            CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX,
            request[CaptureRequest.COLOR_CORRECTION_MODE]
        )
        val sent = assertNotNull(request[CaptureRequest.COLOR_CORRECTION_GAINS])
        assertEquals(listOf(1.8f, 1f, 1f, 2.2f), listOf(sent.red, sent.greenEven, sent.greenOdd, sent.blue))
        assertEquals(transform, request[CaptureRequest.COLOR_CORRECTION_TRANSFORM])
        assertFalse(request.has(CaptureRequest.CONTROL_AWB_LOCK))
    }

    @Test
    fun lockedWhiteBalanceWaitsForTheAutomaticTransform() {
        val request = request(
            state(
                whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.locked,
                whiteBalanceGains = AVCaptureDevice.WhiteBalanceGains(1.8f, 1f, 2.2f),
                colorTransform = null,
            )
        )
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, request[CaptureRequest.CONTROL_AWB_MODE])
        assertFalse(request.has(CaptureRequest.COLOR_CORRECTION_GAINS))
        assertFalse(request.has(CaptureRequest.CONTROL_AWB_LOCK))
    }

    @Test
    fun lockedWhiteBalanceWithoutManualPostProcessingUsesTheLock() {
        val request = request(
            state(whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.locked),
            capabilities(manualPostProcessing = false),
        )
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, request[CaptureRequest.CONTROL_AWB_MODE])
        assertEquals(true, request[CaptureRequest.CONTROL_AWB_LOCK])
        assertFalse(request.has(CaptureRequest.COLOR_CORRECTION_MODE))
    }

    @Test
    fun whiteBalanceGainsRoundTripThroughTheRequest() {
        val cases = listOf(
            AVCaptureDevice.WhiteBalanceGains(1.8f, 1f, 2.2f),
            AVCaptureDevice.WhiteBalanceGains(1f, 2.1361f, 4f),
            AVCaptureDevice.WhiteBalanceGains(1.827f, 1.351f, 1f),
            AVCaptureDevice.WhiteBalanceGains(1f, 1f, 1f),
        )
        for (gains in cases) {
            val back = assertNotNull(CameraControls.deviceGains(CameraControls.requestGains(gains)))
            assertClose(gains.redGain, back.redGain, 1e-5f)
            assertClose(gains.greenGain, back.greenGain, 1e-5f)
            assertClose(gains.blueGain, back.blueGain, 1e-5f)
            assertEquals(1f, CameraControls.requestGains(gains).greenEven)
        }
        assertNull(CameraControls.deviceGains(android.hardware.camera2.params.RggbChannelVector(0f, 1f, 1f, 1f)))
    }

    @Test
    fun biasedExposureScalesTheSensitivityAndThenTheShutter() {
        val limits = capabilities(sensitivityRange = Range(100, 1600))
        assertEquals(Pair(400f, 10_000L), CameraControls.biasedExposure(400f, 10_000L, 0f, limits, 33_333L))
        assertEquals(Pair(800f, 10_000L), CameraControls.biasedExposure(400f, 10_000L, 1f, limits, 33_333L))
        assertEquals(Pair(100f, 5_000L), CameraControls.biasedExposure(200f, 10_000L, -2f, limits, 33_333L))
        assertEquals(Pair(1600f, 33_333L), CameraControls.biasedExposure(800f, 20_000L, 2f, limits, 33_333L))
        assertEquals(Pair(100f, 10L), CameraControls.biasedExposure(100f, 20L, -8f, limits, 33_333L))
    }

    @Test
    fun temperatureGainsPassThroughTheCameraAutomaticGains() {
        val cases = listOf(
            RggbChannelVector(2.03f, 1f, 1f, 1.72f),
            RggbChannelVector(1.25f, 1f, 1f, 2.9f),
            RggbChannelVector(4f, 2f, 2f, 3f),
        )
        for (gains in cases) {
            val scale = assertNotNull(CameraControls.whiteBalanceScale(gains))
            val device = assertNotNull(CameraControls.deviceGains(gains))
            val temperature = 6500f * (device.redGain / device.blueGain).pow(1f / 1.4f)
            val computed = CameraControls.temperatureGains(temperature, scale)
            assertClose(device.redGain, computed.redGain, 1e-4f)
            assertClose(device.greenGain, computed.greenGain, 1e-4f)
            assertClose(device.blueGain, computed.blueGain, 1e-4f)
        }
        assertNull(CameraControls.whiteBalanceScale(RggbChannelVector(0f, 1f, 1f, 1f)))
        assertEquals(4f, CameraControls.whiteBalanceScale(RggbChannelVector(40f, 1f, 1f, 40f)))
        assertEquals(AVCaptureDevice.WhiteBalanceGains(1f, 1f, 1f), CameraControls.temperatureGains(6500f, 1f))
        val warm = CameraControls.temperatureGains(2200f, CameraControls.defaultWhiteBalanceScale)
        val cool = CameraControls.temperatureGains(10000f, CameraControls.defaultWhiteBalanceScale)
        assertTrue(warm.blueGain > warm.redGain && cool.redGain > cool.blueGain)
    }

    @Test
    fun triggersStartAScanOnlyForWhatWasRequested() {
        val focus = FakeCaptureRequest.trigger(ControlTrigger(focusScan = 3, exposureScan = 0))
        assertEquals(CameraMetadata.CONTROL_AF_TRIGGER_START, focus[CaptureRequest.CONTROL_AF_TRIGGER])
        assertFalse(focus.has(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER))
        val both = FakeCaptureRequest.trigger(ControlTrigger(focusScan = 3, exposureScan = 1))
        assertEquals(CameraMetadata.CONTROL_AF_TRIGGER_START, both[CaptureRequest.CONTROL_AF_TRIGGER])
        assertEquals(
            CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START,
            both[CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER]
        )
    }

    @Test
    fun pointOfInterestMapsToTheSensorForEveryRotation() {
        val point = PointF(0.2f, 0.3f)
        val expected = mapOf(
            0 to PointF(0.2f, 0.3f),
            90 to PointF(0.3f, 0.8f),
            180 to PointF(0.8f, 0.7f),
            270 to PointF(0.7f, 0.2f),
        )
        for ((rotation, sensor) in expected) {
            val mapped = CameraControls.sensorPoint(point, rotation)
            assertClose(sensor.x, mapped.x)
            assertClose(sensor.y, mapped.y)
        }
        assertEquals(CameraControls.sensorPoint(point, 90), CameraControls.sensorPoint(point, -270))
        val clamped = CameraControls.sensorPoint(PointF(-1f, 2f), 0)
        assertEquals(PointF(0f, 1f), clamped)
    }

    @Test
    fun sensorPointRotatedForwardIsThePointOfInterest() {
        val point = PointF(0.1f, 0.65f)
        for (rotation in listOf(0, 90, 180, 270)) {
            val sensor = CameraControls.sensorPoint(point, rotation)
            val output = when (rotation) {
                90 -> PointF(1f - sensor.y, sensor.x)
                180 -> PointF(1f - sensor.x, 1f - sensor.y)
                270 -> PointF(sensor.y, 1f - sensor.x)
                else -> sensor
            }
            assertClose(point.x, output.x)
            assertClose(point.y, output.y)
        }
    }

    @Test
    fun meteringRectangleFollowsRotationAndStaysInsideTheVisibleArea() {
        val visible = Rect(0, 375, 4000, 2625)
        val topLeft = CameraControls.meteringRectangle(PointF(0f, 0f), MeteringArea(visible, 0))
        assertEquals(MeteringRectangle(0, 375, 300, 169, MeteringRectangle.METERING_WEIGHT_MAX), topLeft)
        val rotated = CameraControls.meteringRectangle(PointF(0f, 0f), MeteringArea(visible, 90))
        assertEquals(MeteringRectangle(0, 2456, 300, 169, MeteringRectangle.METERING_WEIGHT_MAX), rotated)
        val upsideDown = CameraControls.meteringRectangle(PointF(0f, 0f), MeteringArea(visible, 180))
        assertEquals(MeteringRectangle(3700, 2456, 300, 169, MeteringRectangle.METERING_WEIGHT_MAX), upsideDown)
        val other = CameraControls.meteringRectangle(PointF(0f, 0f), MeteringArea(visible, 270))
        assertEquals(MeteringRectangle(3700, 375, 300, 169, MeteringRectangle.METERING_WEIGHT_MAX), other)
        for (rotation in listOf(0, 90, 180, 270)) {
            for (point in listOf(PointF(0f, 0f), PointF(1f, 1f), PointF(0.5f, 0.5f), PointF(0.99f, 0.01f))) {
                val region = CameraControls.meteringRectangle(point, MeteringArea(visible, rotation))
                assertTrue(region.x >= visible.left && region.y >= visible.top)
                assertTrue(region.x + region.width <= visible.right && region.y + region.height <= visible.bottom)
                assertTrue(region.width > 0 && region.height > 0)
            }
        }
    }

    @Test
    fun visibleRegionCropsTheFieldOfViewToTheOutput() {
        assertEquals(Rect(0, 375, 4000, 2625), CameraControls.visibleRegion(Rect(0, 0, 4000, 3000), Size(1920, 1080)))
        assertEquals(Rect(0, 0, 4000, 3000), CameraControls.visibleRegion(Rect(0, 0, 4000, 3000), Size(1440, 1080)))
        assertEquals(Rect(500, 0, 3500, 3000), CameraControls.visibleRegion(Rect(0, 0, 4000, 3000), Size(1080, 1080)))
        val zoomed = CameraControls.cropRegion(Rect(0, 0, 4000, 3000), 2f)
        assertEquals(Rect(1000, 750, 3000, 2250), zoomed)
        assertEquals(Rect(1000, 937, 3000, 2062), CameraControls.visibleRegion(zoomed, Size(1920, 1080)))
    }
}
