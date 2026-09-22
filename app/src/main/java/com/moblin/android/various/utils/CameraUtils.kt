package com.moblin.android.various.utils

import com.moblin.android.localized
import com.moblin.android.various.model.CameraId
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.moblin.android.AppDelegate

private const val CM_TIME_INVALID = Long.MIN_VALUE
private const val CM_TIME_ONE_60TH = 1_000_000L / 60

class AVCaptureDevice {
    enum class DeviceType {
        BUILT_IN_TRIPLE_CAMERA,
        BUILT_IN_DUAL_CAMERA,
        BUILT_IN_DUAL_WIDE_CAMERA,
        BUILT_IN_ULTRA_WIDE_CAMERA,
        BUILT_IN_WIDE_ANGLE_CAMERA,
        BUILT_IN_TELEPHOTO_CAMERA,
    }

    enum class Position {
        BACK,
        FRONT,
        UNSPECIFIED,
    }

    class Format {
        var minISO: Float = 0f
        var maxISO: Float = 0f
        var minExposureDuration: Long = CM_TIME_INVALID
        var maxExposureDuration: Long = CM_TIME_INVALID
    }

    data class WhiteBalanceTemperatureAndTintValues(
        val temperature: Float,
        val tint: Float,
    )

    data class WhiteBalanceGains(
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float,
    )

    var deviceType: DeviceType = DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA
    var position: Position = Position.UNSPECIFIED
    var localizedName: String = ""
    var uniqueID: String = ""
    var activeFormat: Format = Format()
    var formats: List<Format> = emptyList()
    var virtualDeviceSwitchOverVideoZoomFactors: List<Float> = emptyList()
    var activeVideoMinFrameDuration: Long = CM_TIME_INVALID
    var activeVideoMaxFrameDuration: Long = CM_TIME_INVALID
    var minAvailableVideoZoomFactor: Float = 1f
    var maxAvailableVideoZoomFactor: Float = 1f
    var maxWhiteBalanceGain: Float = 1f

    fun deviceWhiteBalanceGains(
        values: WhiteBalanceTemperatureAndTintValues,
    ): WhiteBalanceGains = TODO("Camera2 COLOR_CORRECTION_GAINS conversion from temperature and tint")

    fun temperatureAndTintValues(gains: WhiteBalanceGains): WhiteBalanceTemperatureAndTintValues =
        TODO()
    companion object {
        private val cameraManager: CameraManager
            get() = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        private fun physicalCameraCount(characteristics: CameraCharacteristics): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return 1
            }
            return maxOf(1, characteristics.physicalCameraIds.size)
        }

        fun default(deviceType: DeviceType, position: Position): AVCaptureDevice? {
            val facing = when (position) {
                Position.BACK -> CameraCharacteristics.LENS_FACING_BACK
                Position.FRONT -> CameraCharacteristics.LENS_FACING_FRONT
                Position.UNSPECIFIED -> null
            }
            val wantedPhysicalCameras = when (deviceType) {
                DeviceType.BUILT_IN_TRIPLE_CAMERA -> 3
                DeviceType.BUILT_IN_DUAL_CAMERA, DeviceType.BUILT_IN_DUAL_WIDE_CAMERA -> 2
                DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA -> 1
                DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA, DeviceType.BUILT_IN_TELEPHOTO_CAMERA -> return null
            }
            val manager = cameraManager
            val ids = runCatching { manager.cameraIdList.toList() }.getOrDefault(emptyList())
            for (id in ids) {
                val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
                if (facing != null && characteristics.get(CameraCharacteristics.LENS_FACING) != facing) {
                    continue
                }
                val physicalCameras = physicalCameraCount(characteristics)
                if (wantedPhysicalCameras > 1 && physicalCameras < wantedPhysicalCameras) {
                    continue
                }
                if (wantedPhysicalCameras == 1 && physicalCameras > 1) {
                    continue
                }
                return fromCharacteristics(id, deviceType, position, characteristics)
            }
            if (wantedPhysicalCameras == 1) {
                for (id in ids) {
                    val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
                    if (facing == null || characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                        return fromCharacteristics(id, deviceType, position, characteristics)
                    }
                }
            }
            return null
        }

        private fun fromCharacteristics(
            id: String,
            deviceType: DeviceType,
            position: Position,
            characteristics: CameraCharacteristics,
        ): AVCaptureDevice {
            val device = AVCaptureDevice()
            device.deviceType = deviceType
            device.position = position
            device.uniqueID = id
            device.localizedName = when (position) {
                Position.BACK -> localized("Back camera")
                Position.FRONT -> localized("Front camera")
                Position.UNSPECIFIED -> localized("Camera")
            } + " " + id
            val format = Format()
            characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let {
                format.minISO = it.lower.toFloat()
                format.maxISO = it.upper.toFloat()
            }
            characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let {
                format.minExposureDuration = it.lower / 1000
                format.maxExposureDuration = it.upper / 1000
            }
            device.activeFormat = format
            device.formats = listOf(format)
            device.activeVideoMinFrameDuration = CM_TIME_ONE_60TH * 2
            device.activeVideoMaxFrameDuration = CM_TIME_ONE_60TH * 2
            device.minAvailableVideoZoomFactor = 1f
            device.maxAvailableVideoZoomFactor =
                characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.let {
                    device.minAvailableVideoZoomFactor = it.lower
                    device.maxAvailableVideoZoomFactor = it.upper
                }
            }
            return device
        }
    }
}

fun AVCaptureDevice.getZoomFactorScale(hasUltraWideCamera: Boolean): Float {
    return if (hasUltraWideCamera) {
        when (deviceType) {
            AVCaptureDevice.DeviceType.BUILT_IN_TRIPLE_CAMERA,
            AVCaptureDevice.DeviceType.BUILT_IN_DUAL_WIDE_CAMERA,
            AVCaptureDevice.DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA,
            -> 0.5f
            AVCaptureDevice.DeviceType.BUILT_IN_TELEPHOTO_CAMERA ->
                (virtualDeviceSwitchOverVideoZoomFactors.lastOrNull() ?: 10.0f) / 2f
            else -> 1.0f
        }
    } else {
        when (deviceType) {
            AVCaptureDevice.DeviceType.BUILT_IN_TELEPHOTO_CAMERA ->
                virtualDeviceSwitchOverVideoZoomFactors.lastOrNull() ?: 2.0f
            else -> 1.0f
        }
    }
}

fun AVCaptureDevice.getUIZoomRange(hasUltraWideCamera: Boolean): Pair<Float, Float> {
    val factor = getZoomFactorScale(hasUltraWideCamera)
    return Pair(
        minAvailableVideoZoomFactor * factor,
        maxAvailableVideoZoomFactor * factor,
    )
}

val AVCaptureDevice.fps: Pair<Double, Double>
    get() = Pair(
        1.0 / (activeVideoMinFrameDuration / 1_000_000.0),
        1.0 / (activeVideoMaxFrameDuration / 1_000_000.0),
    )

fun AVCaptureDevice.setFps(frameRate: Double) {
    activeVideoMinFrameDuration = (1_000_000.0 / frameRate).toLong()
    activeVideoMaxFrameDuration = activeVideoMinFrameDuration
}

fun AVCaptureDevice.setAutoFps() {
    activeVideoMinFrameDuration = CM_TIME_ONE_60TH * 2
    activeVideoMaxFrameDuration = CM_TIME_ONE_60TH * 2
}

fun AVCaptureDevice.name(): String {
    if (isMac()) {
        return localizedName
    } else {
        val name = baseName()
        return when (position) {
            AVCaptureDevice.Position.BACK -> localized("Back $name")
            AVCaptureDevice.Position.FRONT -> localized("Front $name")
            else -> name
        }
    }
}

private fun AVCaptureDevice.baseName(): String {
    return when (deviceType) {
        AVCaptureDevice.DeviceType.BUILT_IN_TRIPLE_CAMERA -> localized("Triple (auto)")
        AVCaptureDevice.DeviceType.BUILT_IN_DUAL_CAMERA -> localized("Dual (auto)")
        AVCaptureDevice.DeviceType.BUILT_IN_DUAL_WIDE_CAMERA -> localized("Wide dual (auto)")
        AVCaptureDevice.DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA -> localized("Ultra wide")
        AVCaptureDevice.DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA -> localized("Wide")
        AVCaptureDevice.DeviceType.BUILT_IN_TELEPHOTO_CAMERA -> localized("Telephoto")
        else -> localizedName
    }
}

val hasUltraWideBackCamera: Boolean by lazy {
    AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA,
        AVCaptureDevice.Position.BACK) != null
}
val hasTripleBackCamera: Boolean by lazy {
    AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_TRIPLE_CAMERA,
        AVCaptureDevice.Position.BACK) != null
}
val hasDualBackCamera: Boolean by lazy {
    AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_DUAL_CAMERA,
        AVCaptureDevice.Position.BACK) != null
}
val hasWideDualBackCamera: Boolean by lazy {
    AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_DUAL_WIDE_CAMERA,
        AVCaptureDevice.Position.BACK) != null
}
val hasUltraWideFrontCamera: Boolean by lazy {
    AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA,
        AVCaptureDevice.Position.FRONT) != null
}

fun hasUltraWideCamera(position: AVCaptureDevice.Position): Boolean {
    return when (position) {
        AVCaptureDevice.Position.BACK -> hasUltraWideBackCamera
        AVCaptureDevice.Position.FRONT -> hasUltraWideFrontCamera
        else -> false
    }
}

private fun findBestBackCameraDevice(): AVCaptureDevice? {
    var device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_TRIPLE_CAMERA,
        AVCaptureDevice.Position.BACK)
    if (device == null) {
        device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_DUAL_WIDE_CAMERA,
            AVCaptureDevice.Position.BACK)
    }
    if (device == null) {
        device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_DUAL_CAMERA,
            AVCaptureDevice.Position.BACK)
    }
    if (device == null) {
        device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA,
            AVCaptureDevice.Position.BACK)
    }
    return device
}

val bestBackCameraDevice: AVCaptureDevice? by lazy { findBestBackCameraDevice() }

private fun findBestFrontCameraDevice(): AVCaptureDevice? {
    var device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_ULTRA_WIDE_CAMERA,
        AVCaptureDevice.Position.FRONT)
    if (device == null) {
        device = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA,
            AVCaptureDevice.Position.FRONT)
    }
    return device
}

val bestFrontCameraDevice: AVCaptureDevice? by lazy { findBestFrontCameraDevice() }

private fun findBestBackCameraId(): CameraId {
    return bestBackCameraDevice?.uniqueID ?: ""
}

val bestBackCameraId: CameraId by lazy { findBestBackCameraId() }

private fun cameraPosition(rawValue: String): SettingsSceneCameraPosition {
    return SettingsSceneCameraPosition.fromRawValue(rawValue)
        ?: SettingsSceneCameraPosition.fromRawValue("back")
        ?: TODO("SettingsSceneCameraPosition has no back case")
}

private fun findDefaultBackCameraPosition(): SettingsSceneCameraPosition {
    return if (hasTripleBackCamera) {
        cameraPosition("backTripleLowEnergy")
    } else if (hasWideDualBackCamera) {
        cameraPosition("backWideDualLowEnergy")
    } else if (hasDualBackCamera) {
        cameraPosition("backDualLowEnergy")
    } else {
        cameraPosition("back")
    }
}

val defaultBackCameraPosition: SettingsSceneCameraPosition by lazy { findDefaultBackCameraPosition() }

private fun findBestFrontCameraId(): String {
    return bestFrontCameraDevice?.uniqueID ?: ""
}

val bestFrontCameraId: String by lazy { findBestFrontCameraId() }

fun hasAppleLog(): Boolean {
    return false
}

fun factorToIso(device: AVCaptureDevice, factor: Float): Float {
    val minIso = device.activeFormat.minISO
    val maxIso = device.activeFormat.maxISO
    var iso = minIso + (maxIso - minIso) * factor.coerceIn(0f, 1f)
    if (!iso.isFinite()) {
        iso = 0f
    }
    return iso
}

fun factorFromIso(device: AVCaptureDevice, iso: Float): Float {
    val minIso = device.activeFormat.minISO
    val maxIso = device.activeFormat.maxISO
    var factor = (iso - minIso) / (maxIso - minIso)
    if (!factor.isFinite()) {
        factor = 0f
    }
    return factor.coerceIn(0f, 1f)
}

private val shutterSpeeds: List<Long> = listOf(
    8000, 6400, 5000, 4000, 3200, 2500, 2000, 1600, 1250, 1000, 800, 640, 500, 400, 320, 250, 240,
    200, 160, 125, 120, 100, 80, 60, 50, 48, 40, 30, 25, 24, 20, 15, 13, 10, 8,
).map { 1_000_000L / it }
private val slowestExposureWithoutFrameDuration: Long = 1_000_000L / 20

fun exposures(device: AVCaptureDevice): List<Long> {
    val fastest = device.activeFormat.minExposureDuration
    var slowest = device.activeFormat.maxExposureDuration
    val frameDuration = device.activeVideoMaxFrameDuration
    if (frameDuration != CM_TIME_INVALID && frameDuration > 0) {
        slowest = minOf(slowest, frameDuration)
    } else {
        slowest = minOf(slowest, slowestExposureWithoutFrameDuration)
    }
    slowest = maxOf(fastest, slowest)
    val filtered = shutterSpeeds.filter { it >= fastest && it <= slowest }
    if (filtered.isEmpty()) {
        return listOf(slowest)
    }
    return filtered
}

fun factorToExposure(device: AVCaptureDevice, factor: Float): Long {
    return factorToExposure(exposures(device), factor)
}

fun factorToExposure(exposures: List<Long>, factor: Float): Long {
    if (exposures.size <= 1) {
        return exposures.firstOrNull() ?: CM_TIME_ONE_60TH
    }
    val index = (factor.coerceIn(0f, 1f) * (exposures.size - 1)).roundToInt()
    return exposures[index.coerceIn(0, exposures.size - 1)]
}

fun factorFromExposure(device: AVCaptureDevice, exposure: Long): Float {
    return factorFromExposure(exposures(device), exposure)
}

fun factorFromExposure(exposures: List<Long>, exposure: Long): Float {
    if (exposures.size <= 1 || exposure <= 0) {
        return 0f
    }
    var bestIndex = 0
    var bestDistance = Double.POSITIVE_INFINITY
    for ((index, candidate) in exposures.withIndex()) {
        val distance = abs(ln(candidate.toDouble() / exposure.toDouble()))
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex.toFloat() / (exposures.size - 1).toFloat()
}

fun exposureFactorStep(device: AVCaptureDevice): Float {
    return exposureFactorStep(exposures(device))
}

fun exposureFactorStep(exposures: List<Long>): Float {
    if (exposures.size <= 1) {
        return 1f
    }
    return 1f / (exposures.size - 1).toFloat()
}

fun formatExposure(exposure: Long): String {
    val seconds = exposure / 1_000_000.0
    if (seconds <= 0 || !seconds.isFinite()) {
        return ""
    }
    return "1/${(1.0 / seconds).roundToLong()}"
}

val minimumWhiteBalanceTemperature: Float = 2200f
val maximumWhiteBalanceTemperature: Float = 10000f

fun factorToWhiteBalance(device: AVCaptureDevice, factor: Float): AVCaptureDevice.WhiteBalanceGains {
    val temperature = minimumWhiteBalanceTemperature +
        (maximumWhiteBalanceTemperature - minimumWhiteBalanceTemperature) * factor
    val temperatureAndTint = AVCaptureDevice.WhiteBalanceTemperatureAndTintValues(
        temperature = temperature,
        tint = 0f,
    )
    return device.deviceWhiteBalanceGains(temperatureAndTint)
        .clamped(maxGain = device.maxWhiteBalanceGain)
}

fun factorFromWhiteBalance(
    device: AVCaptureDevice,
    gains: AVCaptureDevice.WhiteBalanceGains,
): Float {
    val temperature = device.temperatureAndTintValues(gains).temperature
    return (temperature - minimumWhiteBalanceTemperature) /
        (maximumWhiteBalanceTemperature - minimumWhiteBalanceTemperature)
}

fun AVCaptureDevice.WhiteBalanceGains.clamped(maxGain: Float): AVCaptureDevice.WhiteBalanceGains {
    return AVCaptureDevice.WhiteBalanceGains(
        redGain = redGain.coerceIn(1f, maxGain),
        greenGain = greenGain.coerceIn(1f, maxGain),
        blueGain = blueGain.coerceIn(1f, maxGain),
    )
}

data class CMAcceleration(val x: Double, val y: Double, val z: Double)

fun calcCameraAngle(gravity: CMAcceleration, portrait: Boolean): Double {
    return if (portrait) {
        -1 * (atan2(gravity.y, gravity.x) + PI / 2)
    } else if (gravity.x > 0) {
        atan2(-gravity.x, -gravity.y) + PI / 2
    } else {
        atan2(gravity.x, gravity.y) + PI / 2
    }
}

fun useLandscapeStreamAndPortraitUi(
    device: AVCaptureDevice?,
    isLandscapeStreamAndPortraitUi: Boolean,
): Boolean = false
