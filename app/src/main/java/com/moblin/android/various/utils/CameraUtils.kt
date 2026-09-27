package com.moblin.android.various.utils

import com.moblin.android.common.various.clamped
import com.moblin.android.localized
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.avfoundation.AVCaptureAspectRatio
import com.moblin.android.platform.avfoundation.AVCaptureColorSpace
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.various.model.CameraId
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun AVCaptureDevice.getZoomFactorScale(hasUltraWideCamera: Boolean): Float {
    return if (hasUltraWideCamera) {
        when (deviceType) {
            AVCaptureDevice.DeviceType.builtInTripleCamera,
            AVCaptureDevice.DeviceType.builtInDualWideCamera,
            AVCaptureDevice.DeviceType.builtInUltraWideCamera,
            -> 0.5f
            AVCaptureDevice.DeviceType.builtInTelephotoCamera ->
                (virtualDeviceSwitchOverVideoZoomFactors.lastOrNull() ?: 10.0f) / 2
            else -> 1.0f
        }
    } else {
        when (deviceType) {
            AVCaptureDevice.DeviceType.builtInTelephotoCamera ->
                virtualDeviceSwitchOverVideoZoomFactors.lastOrNull() ?: 2.0f
            else -> 1.0f
        }
    }
}

fun AVCaptureDevice.getUIZoomRange(hasUltraWideCamera: Boolean): Pair<Float, Float> {
    val factor = getZoomFactorScale(hasUltraWideCamera = hasUltraWideCamera)
    return Pair(minAvailableVideoZoomFactor * factor, maxAvailableVideoZoomFactor * factor)
}

val AVCaptureDevice.fps: Pair<Double, Double>
    get() = Pair(1 / cmTimeSeconds(activeVideoMinFrameDuration), 1 / cmTimeSeconds(activeVideoMaxFrameDuration))

private fun cmTimeSeconds(time: Long): Double {
    return if (time == kCMTimeInvalidUs) Double.NaN else time / 1_000_000.0
}

fun AVCaptureDevice.setFps(frameRate: Double) {
    if (activeFormat.isAutoVideoFrameRateSupported) {
        isAutoVideoFrameRateEnabled = false
    }
    val timescale = (100 * frameRate).toInt()
    val duration = if (timescale > 0) 100 * 1_000_000L / timescale else kCMTimeInvalidUs
    activeVideoMinFrameDuration = duration
    activeVideoMaxFrameDuration = duration
}

fun AVCaptureDevice.setAutoFps() {
    activeVideoMinFrameDuration = kCMTimeInvalidUs
    activeVideoMaxFrameDuration = kCMTimeInvalidUs
    isAutoVideoFrameRateEnabled = true
}

fun AVCaptureDevice.setLowLightBoost(value: Boolean) {
    if (!isLowLightBoostSupported) {
        return
    }
    automaticallyEnablesLowLightBoostWhenAvailable = value
}

fun AVCaptureDevice.name(): String {
    if (isMac()) {
        return localizedName
    } else {
        val name = baseName()
        return when (position) {
            AVCaptureDevice.Position.back -> localized("Back $name")
            AVCaptureDevice.Position.front -> localized("Front $name")
            else -> name
        }
    }
}

private fun AVCaptureDevice.baseName(): String {
    return when (deviceType) {
        AVCaptureDevice.DeviceType.builtInTripleCamera -> localized("Triple (auto)")
        AVCaptureDevice.DeviceType.builtInDualCamera -> localized("Dual (auto)")
        AVCaptureDevice.DeviceType.builtInDualWideCamera -> localized("Wide dual (auto)")
        AVCaptureDevice.DeviceType.builtInUltraWideCamera -> localized("Ultra wide")
        AVCaptureDevice.DeviceType.builtInWideAngleCamera -> localized("Wide")
        AVCaptureDevice.DeviceType.builtInTelephotoCamera -> localized("Telephoto")
        else -> localizedName
    }
}

val hasUltraWideBackCamera: Boolean by lazy {
    AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInUltraWideCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.back,
    ) != null
}
val hasTripleBackCamera: Boolean by lazy {
    AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInTripleCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.back,
    ) != null
}
val hasDualBackCamera: Boolean by lazy {
    AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInDualCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.back,
    ) != null
}
val hasWideDualBackCamera: Boolean by lazy {
    AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInDualWideCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.back,
    ) != null
}
val hasUltraWideFrontCamera: Boolean by lazy {
    AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInUltraWideCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.front,
    ) != null
}

fun hasUltraWideCamera(position: AVCaptureDevice.Position): Boolean {
    return when (position) {
        AVCaptureDevice.Position.back -> hasUltraWideBackCamera
        AVCaptureDevice.Position.front -> hasUltraWideFrontCamera
        else -> false
    }
}

private fun findBestBackCameraDevice(): AVCaptureDevice? {
    var device = AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInTripleCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.back,
    )
    if (device == null) {
        device = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInDualWideCamera,
            AVMediaType.video,
            AVCaptureDevice.Position.back,
        )
    }
    if (device == null) {
        device = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInDualCamera,
            AVMediaType.video,
            AVCaptureDevice.Position.back,
        )
    }
    if (device == null) {
        device = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInWideAngleCamera,
            AVMediaType.video,
            AVCaptureDevice.Position.back,
        )
    }
    return device
}

val bestBackCameraDevice: AVCaptureDevice? by lazy { findBestBackCameraDevice() }

private fun findBestFrontCameraDevice(): AVCaptureDevice? {
    var device = AVCaptureDevice.default(
        AVCaptureDevice.DeviceType.builtInUltraWideCamera,
        AVMediaType.video,
        AVCaptureDevice.Position.front,
    )
    if (device == null) {
        device = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInWideAngleCamera,
            AVMediaType.video,
            AVCaptureDevice.Position.front,
        )
    }
    return device
}

val bestFrontCameraDevice: AVCaptureDevice? by lazy { findBestFrontCameraDevice() }

private fun findBestBackCameraId(): CameraId {
    return bestBackCameraDevice?.uniqueID ?: ""
}

val bestBackCameraId: CameraId by lazy { findBestBackCameraId() }

private fun findDefaultBackCameraPosition(): SettingsSceneCameraPosition {
    return if (hasTripleBackCamera) {
        SettingsSceneCameraPosition.backTripleLowEnergy
    } else if (hasWideDualBackCamera) {
        SettingsSceneCameraPosition.backWideDualLowEnergy
    } else if (hasDualBackCamera) {
        SettingsSceneCameraPosition.backDualLowEnergy
    } else {
        SettingsSceneCameraPosition.back
    }
}

val defaultBackCameraPosition: SettingsSceneCameraPosition by lazy { findDefaultBackCameraPosition() }

private fun findBestFrontCameraId(): String {
    return bestFrontCameraDevice?.uniqueID ?: ""
}

val bestFrontCameraId: String by lazy { findBestFrontCameraId() }

fun hasAppleLog(): Boolean {
    for (format in bestBackCameraDevice?.formats ?: emptyList()) {
        if (format.supportedColorSpaces.contains(AVCaptureColorSpace.appleLog)) {
            return true
        }
    }
    return false
}

fun hasHlg(): Boolean {
    for (format in bestBackCameraDevice?.formats ?: emptyList()) {
        if (format.supportedColorSpaces.contains(AVCaptureColorSpace.HLG_BT2020)) {
            return true
        }
    }
    return false
}

fun factorToIso(device: AVCaptureDevice, factor: Float): Float {
    val minIso = device.activeFormat.minISO
    val maxIso = device.activeFormat.maxISO
    var iso = minIso + (maxIso - minIso) * factor.clamped(to = 0f..1f)
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
    return factor.clamped(to = 0f..1f)
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
    if (frameDuration != kCMTimeInvalidUs && frameDuration > 0) {
        slowest = minOf(slowest, frameDuration)
    } else {
        slowest = minOf(slowest, slowestExposureWithoutFrameDuration)
    }
    slowest = maxOf(fastest, slowest)
    val exposures = shutterSpeeds.filter { it >= fastest && it <= slowest }
    if (exposures.isEmpty()) {
        return listOf(slowest)
    }
    return exposures
}

fun factorToExposure(device: AVCaptureDevice, factor: Float): Long {
    return factorToExposure(exposures = exposures(device = device), factor = factor)
}

fun factorToExposure(exposures: List<Long>, factor: Float): Long {
    if (exposures.size <= 1) {
        return exposures.firstOrNull() ?: (1_000_000L / 60)
    }
    val index = (factor.clamped(to = 0f..1f) * (exposures.size - 1)).roundToInt()
    return exposures[index]
}

fun factorFromExposure(device: AVCaptureDevice, exposure: Long): Float {
    return factorFromExposure(exposures = exposures(device = device), exposure = exposure)
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
    return exposureFactorStep(exposures = exposures(device = device))
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
    return "1/${(1 / seconds).roundToLong()}"
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

fun factorFromWhiteBalance(device: AVCaptureDevice, gains: AVCaptureDevice.WhiteBalanceGains): Float {
    val temperature = device.temperatureAndTintValues(gains).temperature
    return (temperature - minimumWhiteBalanceTemperature) /
        (maximumWhiteBalanceTemperature - minimumWhiteBalanceTemperature)
}

fun AVCaptureDevice.WhiteBalanceGains.clamped(maxGain: Float): AVCaptureDevice.WhiteBalanceGains {
    return AVCaptureDevice.WhiteBalanceGains(
        redGain = redGain.clamped(to = 1f..maxGain),
        greenGain = greenGain.clamped(to = 1f..maxGain),
        blueGain = blueGain.clamped(to = 1f..maxGain),
    )
}

typealias CMAcceleration = com.moblin.android.platform.coremotion.CMAcceleration

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
): Boolean {
    if (isLandscapeStreamAndPortraitUi && device?.dynamicAspectRatio == AVCaptureAspectRatio.ratio9x16) {
        return true
    }
    return false
}
