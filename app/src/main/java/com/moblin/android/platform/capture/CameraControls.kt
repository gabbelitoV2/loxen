package com.moblin.android.platform.capture

import android.graphics.PointF
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.util.Range
import android.util.Size
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal interface CaptureRequestWriter {
    fun <T> set(key: CaptureRequest.Key<T>, value: T)
}

internal class CaptureRequestBuilderWriter(private val builder: CaptureRequest.Builder) : CaptureRequestWriter {
    override fun <T> set(key: CaptureRequest.Key<T>, value: T) {
        builder.set(key, value)
    }
}

internal class CameraControlCapabilities(
    val manualSensor: Boolean,
    val manualPostProcessing: Boolean,
    val hardwareLevel: Int?,
    val minimumFocusDistance: Float,
    val afModes: Set<Int>,
    val aeModes: Set<Int>,
    val awbModes: Set<Int>,
    val maxRegionsAf: Int,
    val maxRegionsAe: Int,
    val aeLockAvailable: Boolean,
    val awbLockAvailable: Boolean,
    val sensitivityRange: Range<Int>?,
    val exposureTimeRange: Range<Long>?,
    val aeCompensationRange: Range<Int>?,
    val aeCompensationStep: Float,
) {
    private val limitedOrBetter: Boolean
        get() = when (hardwareLevel) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            -> true
            else -> false
        }

    val supportsManualFocus: Boolean
        get() = minimumFocusDistance > 0f &&
            afModes.contains(CameraMetadata.CONTROL_AF_MODE_OFF) &&
            (manualSensor || limitedOrBetter)

    val supportsAutoFocus: Boolean
        get() = minimumFocusDistance > 0f && afModes.contains(CameraMetadata.CONTROL_AF_MODE_AUTO)

    val continuousAutoFocusMode: Int?
        get() = when {
            afModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO) ->
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            afModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE) ->
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            else -> null
        }

    val supportsFocusPointOfInterest: Boolean
        get() = supportsAutoFocus && maxRegionsAf > 0

    val supportsExposurePointOfInterest: Boolean
        get() = maxRegionsAe > 0

    val supportsAutoExposure: Boolean
        get() = aeModes.isEmpty() || aeModes.contains(CameraMetadata.CONTROL_AE_MODE_ON)

    val supportsCustomExposure: Boolean
        get() = manualSensor &&
            aeModes.contains(CameraMetadata.CONTROL_AE_MODE_OFF) &&
            sensitivityRange != null &&
            exposureTimeRange != null

    val supportsLockedExposure: Boolean
        get() = supportsCustomExposure || aeLockAvailable

    val supportsAutoWhiteBalance: Boolean
        get() = awbModes.isEmpty() || awbModes.contains(CameraMetadata.CONTROL_AWB_MODE_AUTO)

    val supportsCustomWhiteBalanceGains: Boolean
        get() = manualPostProcessing && awbModes.contains(CameraMetadata.CONTROL_AWB_MODE_OFF)

    val supportsLockedWhiteBalance: Boolean
        get() = supportsCustomWhiteBalanceGains || awbLockAvailable

    val minExposureTargetBias: Float
        get() = (aeCompensationRange?.lower ?: 0) * aeCompensationStep

    val maxExposureTargetBias: Float
        get() = (aeCompensationRange?.upper ?: 0) * aeCompensationStep

    companion object {
        fun make(characteristics: CameraCharacteristics): CameraControlCapabilities {
            val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?.toSet() ?: emptySet()
            return CameraControlCapabilities(
                manualSensor = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
                manualPostProcessing = capabilities.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING
                ),
                hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL),
                minimumFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                    ?: 0f,
                afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toSet()
                    ?: emptySet(),
                aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.toSet()
                    ?: emptySet(),
                awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toSet()
                    ?: emptySet(),
                maxRegionsAf = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0,
                maxRegionsAe = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0,
                aeLockAvailable = characteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == true,
                awbLockAvailable = characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) == true,
                sensitivityRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE),
                exposureTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE),
                aeCompensationRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE),
                aeCompensationStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    ?.toFloat() ?: 0f,
            )
        }
    }
}

internal class CameraControlState(
    val focusMode: AVCaptureDevice.FocusMode,
    val focusPointOfInterest: PointF,
    val lensPosition: Float?,
    val exposureMode: AVCaptureDevice.ExposureMode,
    val exposurePointOfInterest: PointF,
    val iso: Float?,
    val exposureDuration: Long?,
    val exposureTargetBias: Float,
    val whiteBalanceMode: AVCaptureDevice.WhiteBalanceMode,
    val whiteBalanceGains: AVCaptureDevice.WhiteBalanceGains?,
    val colorTransform: ColorSpaceTransform?,
)

internal class ControlTrigger(val focusScan: Long, val exposureScan: Long)

internal class MeteringArea(val visible: Rect, val rotation: Int)

internal object CameraControls {
    private const val meteringSize = 0.15f
    private const val nanosecondsPerSecond = 1_000_000_000L

    val identityTransform = ColorSpaceTransform(
        intArrayOf(1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1)
    )

    fun apply(
        writer: CaptureRequestWriter,
        state: CameraControlState,
        capabilities: CameraControlCapabilities,
        frameRateRange: Range<Int>,
        lowLightBoost: Boolean,
        metering: MeteringArea?,
    ) {
        applyFocus(writer, state, capabilities, metering)
        applyExposure(writer, state, capabilities, frameRateRange, lowLightBoost, metering)
        applyWhiteBalance(writer, state, capabilities)
    }

    fun applyTrigger(writer: CaptureRequestWriter, trigger: ControlTrigger) {
        if (trigger.focusScan != 0L) {
            writer.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        }
        if (trigger.exposureScan != 0L) {
            writer.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START)
        }
    }

    fun lensPosition(focusDistance: Float, minimumFocusDistance: Float): Float {
        if (!(minimumFocusDistance > 0f)) {
            return 1f
        }
        return (1f - focusDistance / minimumFocusDistance).coerceIn(0f, 1f)
    }

    fun focusDistance(lensPosition: Float, minimumFocusDistance: Float): Float {
        if (!(minimumFocusDistance > 0f)) {
            return 0f
        }
        return (1f - lensPosition.coerceIn(0f, 1f)) * minimumFocusDistance
    }

    fun deviceGains(gains: RggbChannelVector): AVCaptureDevice.WhiteBalanceGains? {
        val green = (gains.greenEven + gains.greenOdd) / 2
        val minimum = minOf(gains.red, green, gains.blue)
        if (!(minimum > 0f)) {
            return null
        }
        return AVCaptureDevice.WhiteBalanceGains(
            redGain = gains.red / minimum,
            greenGain = green / minimum,
            blueGain = gains.blue / minimum,
        )
    }

    const val defaultWhiteBalanceScale = 1.8f

    fun whiteBalanceScale(gains: RggbChannelVector): Float? {
        val green = (gains.greenEven + gains.greenOdd) / 2
        if (!(green > 0f) || !(gains.red > 0f) || !(gains.blue > 0f)) {
            return null
        }
        return (sqrt(gains.red * gains.blue) / green).coerceIn(0.5f, 4f)
    }

    fun temperatureGains(temperature: Float, scale: Float): AVCaptureDevice.WhiteBalanceGains {
        val kelvin = temperature.coerceIn(1000f, 20000f)
        val red = (kelvin / 6500f).pow(0.7f) * scale
        val blue = (6500f / kelvin).pow(0.7f) * scale
        val minimum = minOf(red, 1f, blue)
        return AVCaptureDevice.WhiteBalanceGains(
            redGain = red / minimum,
            greenGain = 1f / minimum,
            blueGain = blue / minimum,
        )
    }

    fun requestGains(gains: AVCaptureDevice.WhiteBalanceGains): RggbChannelVector {
        val green = if (gains.greenGain > 0f) gains.greenGain else 1f
        return RggbChannelVector(gains.redGain / green, 1f, 1f, gains.blueGain / green)
    }

    fun exposureCompensation(bias: Float, capabilities: CameraControlCapabilities): Int? {
        val range = capabilities.aeCompensationRange ?: return null
        if (!(capabilities.aeCompensationStep > 0f)) {
            return null
        }
        return (bias / capabilities.aeCompensationStep).roundToInt().coerceIn(range.lower, range.upper)
    }

    fun sensitivity(iso: Float, capabilities: CameraControlCapabilities): Int {
        val value = iso.roundToInt()
        val range = capabilities.sensitivityRange ?: return value
        return value.coerceIn(range.lower, range.upper)
    }

    fun biasedExposure(
        iso: Float,
        duration: Long,
        bias: Float,
        capabilities: CameraControlCapabilities,
        maxDuration: Long,
    ): Pair<Float, Long> {
        if (bias == 0f || !(iso > 0f) || duration <= 0L) {
            return Pair(iso, duration)
        }
        val target = iso * 2f.pow(bias)
        val range = capabilities.sensitivityRange
        val biasedIso = if (range != null) {
            target.coerceIn(range.lower.toFloat(), maxOf(range.lower, range.upper).toFloat())
        } else {
            target
        }
        val remaining = target / biasedIso
        if (remaining == 1f) {
            return Pair(biasedIso, duration)
        }
        val timeRange = capabilities.exposureTimeRange
        val lower = timeRange?.lower?.let { it / 1000 } ?: 1L
        var upper = timeRange?.upper?.let { it / 1000 } ?: Long.MAX_VALUE
        if (maxDuration > 0) {
            upper = minOf(upper, maxDuration)
        }
        val biasedDuration = (duration * remaining.toDouble()).roundToLong().coerceIn(lower, maxOf(lower, upper))
        return Pair(biasedIso, biasedDuration)
    }

    fun minFrameDuration(frameRateRange: Range<Int>): Long {
        return nanosecondsPerSecond / maxOf(1, frameRateRange.upper)
    }

    fun maxFrameDuration(frameRateRange: Range<Int>): Long {
        return maxOf(minFrameDuration(frameRateRange), nanosecondsPerSecond / maxOf(1, frameRateRange.lower))
    }

    fun exposureTime(duration: Long, capabilities: CameraControlCapabilities, frameRateRange: Range<Int>): Long {
        val range = capabilities.exposureTimeRange
        var exposureTime = duration * 1000
        if (range != null) {
            exposureTime = exposureTime.coerceIn(range.lower, range.upper)
        }
        val maxFrameDuration = maxFrameDuration(frameRateRange)
        if (exposureTime > maxFrameDuration) {
            exposureTime = maxOf(maxFrameDuration, range?.lower ?: 0L)
        }
        return exposureTime
    }

    fun frameDuration(exposureTime: Long, frameRateRange: Range<Int>): Long {
        return exposureTime.coerceIn(minFrameDuration(frameRateRange), maxFrameDuration(frameRateRange))
    }

    fun cropRegion(area: Rect, ratio: Float): Rect {
        val zoom = maxOf(1f, ratio)
        val width = (area.width() / zoom).toInt()
        val height = (area.height() / zoom).toInt()
        val left = (area.width() - width) / 2
        val top = (area.height() - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    fun visibleRegion(field: Rect, outputSize: Size): Rect {
        if (outputSize.width <= 0 || outputSize.height <= 0 || field.isEmpty) {
            return Rect(field)
        }
        val fieldWidth = field.width().toLong()
        val fieldHeight = field.height().toLong()
        return if (fieldWidth * outputSize.height > fieldHeight * outputSize.width) {
            val width = (fieldHeight * outputSize.width / outputSize.height).toInt()
            val left = field.left + (field.width() - width) / 2
            Rect(left, field.top, left + width, field.bottom)
        } else {
            val height = (fieldWidth * outputSize.height / outputSize.width).toInt()
            val top = field.top + (field.height() - height) / 2
            Rect(field.left, top, field.right, top + height)
        }
    }

    fun pointOfInterestRotation(entry: CameraCatalog.Entry): Int {
        val rotation = CameraOrientation.rotationFor(AVCaptureVideoOrientation.landscapeRight, entry)
        return (entry.sensorOrientation + rotation) % 360
    }

    fun meteringArea(
        entry: CameraCatalog.Entry,
        zoomRatio: Float,
        usesZoomRatio: Boolean,
        outputSize: Size,
    ): MeteringArea? {
        val activeArray = entry.activeArraySize ?: return null
        val field = if (usesZoomRatio) {
            Rect(0, 0, activeArray.width(), activeArray.height())
        } else {
            cropRegion(activeArray, zoomRatio)
        }
        return MeteringArea(visibleRegion(field, outputSize), pointOfInterestRotation(entry))
    }

    fun sensorPoint(point: PointF, rotation: Int): PointF {
        val x = point.x.coerceIn(0f, 1f)
        val y = point.y.coerceIn(0f, 1f)
        return when ((rotation % 360 + 360) % 360) {
            90 -> PointF(y, 1f - x)
            180 -> PointF(1f - x, 1f - y)
            270 -> PointF(1f - y, x)
            else -> PointF(x, y)
        }
    }

    fun meteringRectangle(point: PointF, area: MeteringArea): MeteringRectangle {
        val visible = area.visible
        val sensor = sensorPoint(point, area.rotation)
        val centerX = visible.left + sensor.x * visible.width()
        val centerY = visible.top + sensor.y * visible.height()
        val halfWidth = maxOf(1f, visible.width() * meteringSize / 2)
        val halfHeight = maxOf(1f, visible.height() * meteringSize / 2)
        val left = (centerX - halfWidth).roundToInt().coerceIn(visible.left, maxOf(visible.left, visible.right - 1))
        val top = (centerY - halfHeight).roundToInt().coerceIn(visible.top, maxOf(visible.top, visible.bottom - 1))
        val right = (centerX + halfWidth).roundToInt().coerceIn(left + 1, maxOf(left + 1, visible.right))
        val bottom = (centerY + halfHeight).roundToInt().coerceIn(top + 1, maxOf(top + 1, visible.bottom))
        return MeteringRectangle(left, top, right - left, bottom - top, MeteringRectangle.METERING_WEIGHT_MAX)
    }

    private fun isCenter(point: PointF): Boolean = point.x == 0.5f && point.y == 0.5f

    private fun applyFocus(
        writer: CaptureRequestWriter,
        state: CameraControlState,
        capabilities: CameraControlCapabilities,
        metering: MeteringArea?,
    ) {
        when (state.focusMode) {
            AVCaptureDevice.FocusMode.locked -> {
                val lensPosition = state.lensPosition
                if (lensPosition != null && capabilities.supportsManualFocus) {
                    writer.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                    writer.set(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        focusDistance(lensPosition, capabilities.minimumFocusDistance)
                    )
                } else if (capabilities.supportsAutoFocus) {
                    writer.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
                }
            }
            AVCaptureDevice.FocusMode.autoFocus -> {
                if (!capabilities.supportsAutoFocus) {
                    return
                }
                writer.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
                if (capabilities.supportsFocusPointOfInterest && metering != null) {
                    writer.set(
                        CaptureRequest.CONTROL_AF_REGIONS,
                        arrayOf(meteringRectangle(state.focusPointOfInterest, metering))
                    )
                }
            }
            AVCaptureDevice.FocusMode.continuousAutoFocus -> {
                val mode = capabilities.continuousAutoFocusMode ?: return
                writer.set(CaptureRequest.CONTROL_AF_MODE, mode)
                if (capabilities.supportsFocusPointOfInterest &&
                    metering != null &&
                    !isCenter(state.focusPointOfInterest)
                ) {
                    writer.set(
                        CaptureRequest.CONTROL_AF_REGIONS,
                        arrayOf(meteringRectangle(state.focusPointOfInterest, metering))
                    )
                }
            }
        }
    }

    private fun applyExposure(
        writer: CaptureRequestWriter,
        state: CameraControlState,
        capabilities: CameraControlCapabilities,
        frameRateRange: Range<Int>,
        lowLightBoost: Boolean,
        metering: MeteringArea?,
    ) {
        val iso = state.iso
        val duration = state.exposureDuration
        if (iso != null && duration != null && capabilities.supportsCustomExposure) {
            val exposureTime = exposureTime(duration, capabilities, frameRateRange)
            writer.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
            writer.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity(iso, capabilities))
            writer.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)
            writer.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration(exposureTime, frameRateRange))
        } else {
            writer.set(
                CaptureRequest.CONTROL_AE_MODE,
                if (lowLightBoost) {
                    CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
                } else {
                    CameraMetadata.CONTROL_AE_MODE_ON
                }
            )
            when (state.exposureMode) {
                AVCaptureDevice.ExposureMode.locked, AVCaptureDevice.ExposureMode.custom -> {
                    if (capabilities.aeLockAvailable) {
                        writer.set(CaptureRequest.CONTROL_AE_LOCK, true)
                    }
                }
                AVCaptureDevice.ExposureMode.autoExpose -> {
                    if (capabilities.supportsExposurePointOfInterest && metering != null) {
                        writer.set(
                            CaptureRequest.CONTROL_AE_REGIONS,
                            arrayOf(meteringRectangle(state.exposurePointOfInterest, metering))
                        )
                    }
                }
                AVCaptureDevice.ExposureMode.continuousAutoExposure -> {
                    if (capabilities.supportsExposurePointOfInterest &&
                        metering != null &&
                        !isCenter(state.exposurePointOfInterest)
                    ) {
                        writer.set(
                            CaptureRequest.CONTROL_AE_REGIONS,
                            arrayOf(meteringRectangle(state.exposurePointOfInterest, metering))
                        )
                    }
                }
            }
        }
        exposureCompensation(state.exposureTargetBias, capabilities)?.let {
            writer.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, it)
        }
    }

    private fun applyWhiteBalance(
        writer: CaptureRequestWriter,
        state: CameraControlState,
        capabilities: CameraControlCapabilities,
    ) {
        val gains = state.whiteBalanceGains
        val transform = state.colorTransform
        val locked = state.whiteBalanceMode == AVCaptureDevice.WhiteBalanceMode.locked
        if (locked && gains != null && transform != null && capabilities.supportsCustomWhiteBalanceGains) {
            writer.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            writer.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
            writer.set(CaptureRequest.COLOR_CORRECTION_GAINS, requestGains(gains))
            writer.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, transform)
            return
        }
        if (capabilities.awbModes.contains(CameraMetadata.CONTROL_AWB_MODE_AUTO)) {
            writer.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
        }
        if (locked && gains == null && capabilities.awbLockAvailable) {
            writer.set(CaptureRequest.CONTROL_AWB_LOCK, true)
        }
    }
}
