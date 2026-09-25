package com.moblin.android.platform.avfoundation

import android.graphics.PointF
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.RggbChannelVector
import android.media.AudioDeviceInfo
import android.util.Log
import android.util.Size
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.capture.AudioCaptureBridge
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.capture.CameraCatalog
import com.moblin.android.platform.capture.CameraFormats
import com.moblin.android.platform.core.HostClock
import com.moblin.android.platform.core.KeyValueObservers
import com.moblin.android.platform.core.NSKeyValueObservation
import com.moblin.android.platform.core.NSKeyValueObservedChange
import com.moblin.android.platform.core.NSKeyValueObservingOptions
import kotlin.math.abs
import kotlin.math.pow
import kotlin.reflect.KProperty1

private const val TAG = "MoblinCamera"

class AVCaptureDevice private constructor(
    val uniqueID: String,
    val localizedName: String,
    val position: Position,
    val deviceType: DeviceType,
    private val mediaType: AVMediaType,
    internal val camera: CameraCatalog.Entry?,
    val audioDeviceInfo: AudioDeviceInfo?,
    val dataSourceID: Int?,
) {
    enum class Position(val rawValue: Int) {
        front(2),
        back(1),
        unspecified(0),
        ;

        companion object {
            val FRONT = front
            val BACK = back
            val UNSPECIFIED = unspecified
        }
    }

    enum class DeviceType {
        builtInTripleCamera,
        builtInDualCamera,
        builtInDualWideCamera,
        builtInUltraWideCamera,
        builtInWideAngleCamera,
        builtInTelephotoCamera,
        external,
        microphone,
        ;

        companion object {
            val BUILT_IN_TRIPLE_CAMERA = builtInTripleCamera
            val BUILT_IN_DUAL_CAMERA = builtInDualCamera
            val BUILT_IN_DUAL_WIDE_CAMERA = builtInDualWideCamera
            val BUILT_IN_ULTRA_WIDE_CAMERA = builtInUltraWideCamera
            val BUILT_IN_WIDE_ANGLE_CAMERA = builtInWideAngleCamera
            val BUILT_IN_TELEPHOTO_CAMERA = builtInTelephotoCamera
            val EXTERNAL = external
            val MICROPHONE = microphone
        }
    }

    enum class TorchMode {
        off,
        on,
        auto,
    }

    enum class ExposureMode {
        locked,
        autoExpose,
        continuousAutoExposure,
        custom,
    }

    enum class FocusMode {
        locked,
        autoFocus,
        continuousAutoFocus,
    }

    enum class WhiteBalanceMode {
        locked,
        autoWhiteBalance,
        continuousAutoWhiteBalance,
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

    class Format internal constructor(
        val formatDescription: FormatDescription,
        val videoSupportedFrameRateRanges: List<AVFrameRateRange>,
        val supportedColorSpaces: List<Int>,
        val videoMaxZoomFactor: Float,
        val supportedMaxPhotoDimensions: List<Size>,
        val minISO: Float,
        val maxISO: Float,
        val minExposureDuration: Long,
        val maxExposureDuration: Long,
    ) {
        val isAutoVideoFrameRateSupported = false
        val isVideoBinned = false
        val isVideoHDRSupported = false
        val isHighPhotoQualitySupported = false
        val supportedDynamicAspectRatios: List<AVCaptureAspectRatio> = emptyList()

        class FormatDescription(val dimensions: Dimensions, val mediaSubType: MediaSubType)

        class Dimensions(val width: Int, val height: Int) {
            override fun toString(): String = "${width}x$height"
        }

        class MediaSubType(val rawValue: Int) {
            override fun toString(): String {
                val chars = CharArray(4) { index -> Char((rawValue shr (24 - 8 * index)) and 0xFF) }
                return String(chars)
            }
        }

        override fun toString(): String {
            val ranges = videoSupportedFrameRateRanges.joinToString(", ") { it.toString() }
            val colorSpaces = supportedColorSpaces.joinToString(", ") { colorSpaceName(it) }
            return "${formatDescription.dimensions}, { $ranges fps}, '${formatDescription.mediaSubType}', " +
                "color spaces: [$colorSpaces], max zoom: $videoMaxZoomFactor"
        }
    }

    class DiscoverySession(deviceTypes: List<DeviceType>, mediaType: AVMediaType, position: Position) {
        val devices: List<AVCaptureDevice> = if (mediaType == AVMediaType.video) {
            CameraCatalog.entries()
                .filter { deviceTypes.contains(it.deviceType) }
                .filter { position == Position.unspecified || it.position == position }
                .map { videoDevice(it) }
        } else if (deviceTypes.contains(DeviceType.microphone)) {
            listOfNotNull(default(AVMediaType.audio))
        } else {
            emptyList()
        }

        val supportedMultiCamDeviceSets: List<Set<AVCaptureDevice>>
            get() = CameraCatalog.concurrentCameraIds().map { ids ->
                devices.filter { device -> device.camera?.id?.let { ids.contains(it) } == true }.toSet()
            }.filter { it.size > 1 }
    }

    private val lock = Any()
    private var configurationLockCount = 0
    private var pendingControlsChange = false
    private var pendingFormatChange = false
    private var rampGeneration = 0L
    private var rampTarget = 1f
    private var rampRate = 0f
    private val observers = KeyValueObservers(this)

    @Volatile
    private var resultLensPosition = Float.NaN

    @Volatile
    private var resultIso = Float.NaN

    @Volatile
    private var resultExposureDuration = kCMTimeInvalidUs

    @Volatile
    private var resultWhiteBalanceGains: WhiteBalanceGains? = null

    val formats: List<Format> by lazy {
        val entry = camera ?: return@lazy emptyList()
        CameraFormats.make(entry)
    }

    val isConnected: Boolean
        get() {
            val entry = camera ?: return true
            if (!entry.isExternal) {
                return true
            }
            return CameraCatalog.isPresent(entry.id)
        }

    val isSuspended = false

    val manufacturer: String = android.os.Build.MANUFACTURER

    val modelID: String = android.os.Build.MODEL

    @Volatile
    var activeFormat: Format = CameraFormats.defaultFormat(formats)
        set(value) {
            val changed = field !== value
            field = value
            if (changed) {
                didChange(format = true)
            }
        }

    @Volatile
    var activeColorSpace: Int = AVCaptureColorSpace.sRGB
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = true)
            }
        }

    @Volatile
    var activeVideoMinFrameDuration: Long = 1_000_000L / 30
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = false)
            }
        }

    @Volatile
    var activeVideoMaxFrameDuration: Long = 1_000_000L / 30
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = false)
            }
        }

    @Volatile
    var isAutoVideoFrameRateEnabled: Boolean = false
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = false)
            }
        }

    val minAvailableVideoZoomFactor: Float
        get() = camera?.minZoomFactor ?: 1f

    val maxAvailableVideoZoomFactor: Float
        get() = camera?.maxZoomFactor ?: 1f

    val virtualDeviceSwitchOverVideoZoomFactors: List<Float>
        get() = camera?.switchOverZoomFactors ?: emptyList()

    val displayVideoZoomFactorMultiplier: Float
        get() = camera?.zoomScale ?: 1f

    val isVirtualDevice: Boolean
        get() = when (deviceType) {
            DeviceType.builtInTripleCamera, DeviceType.builtInDualCamera, DeviceType.builtInDualWideCamera -> true
            else -> false
        }

    @Volatile
    private var zoomFactor: Float = 1f.coerceIn(minAvailableVideoZoomFactor, maxAvailableVideoZoomFactor)

    var videoZoomFactor: Float
        get() = zoomFactor
        set(value) {
            synchronized(lock) {
                rampGeneration += 1
                zoomFactor = value.coerceIn(minAvailableVideoZoomFactor, maxAvailableVideoZoomFactor)
            }
            didChange(format = false)
        }

    val isRampingVideoZoom: Boolean
        get() = synchronized(lock) { rampRate > 0f && zoomFactor != rampTarget }

    val hasTorch: Boolean
        get() = camera?.hasFlash ?: false

    val isTorchAvailable: Boolean
        get() = hasTorch

    val maxAvailableTorchLevel: Float = 1f

    @Volatile
    var torchMode: TorchMode = TorchMode.off
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = false)
            }
        }

    @Volatile
    var torchLevel: Float = 0f
        private set

    val isTorchActive: Boolean
        get() = torchMode == TorchMode.on

    val isLowLightBoostSupported: Boolean
        get() = camera?.lowLightBoostSupported ?: false

    @Volatile
    var automaticallyEnablesLowLightBoostWhenAvailable: Boolean = false
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                didChange(format = false)
            }
        }

    val isLowLightBoostEnabled: Boolean
        get() = isLowLightBoostSupported && automaticallyEnablesLowLightBoostWhenAvailable

    @Volatile
    var dynamicAspectRatio: AVCaptureAspectRatio? = null
        private set

    var exposureMode: ExposureMode = ExposureMode.continuousAutoExposure
        set(value) {
            if (value != ExposureMode.continuousAutoExposure) {
                logNotImplemented("exposureMode $value")
            }
            field = value
        }

    val isExposurePointOfInterestSupported = false

    var exposurePointOfInterest: PointF = PointF(0.5f, 0.5f)

    val iso: Float
        get() {
            val value = resultIso
            if (!value.isNaN()) {
                return value
            }
            return 100f.coerceIn(activeFormat.minISO, maxOf(activeFormat.minISO, activeFormat.maxISO))
        }

    val exposureDuration: Long
        get() {
            val value = resultExposureDuration
            if (value != kCMTimeInvalidUs) {
                return value
            }
            val frameDuration = activeVideoMaxFrameDuration
            val duration = if (frameDuration > 0) minOf(frameDuration, 1_000_000L / 60) else 1_000_000L / 60
            return duration.coerceIn(
                activeFormat.minExposureDuration,
                maxOf(activeFormat.minExposureDuration, activeFormat.maxExposureDuration)
            )
        }

    @Volatile
    var exposureTargetBias: Float = 0f
        private set

    val exposureTargetOffset: Float = 0f

    val minExposureTargetBias: Float
        get() {
            val entry = camera ?: return -8f
            val range = entry.aeCompensationRange ?: return 0f
            return range.lower * entry.aeCompensationStep
        }

    val maxExposureTargetBias: Float
        get() {
            val entry = camera ?: return 8f
            val range = entry.aeCompensationRange ?: return 0f
            return range.upper * entry.aeCompensationStep
        }

    var focusMode: FocusMode = FocusMode.continuousAutoFocus
        set(value) {
            if (value != FocusMode.continuousAutoFocus) {
                logNotImplemented("focusMode $value")
            }
            field = value
        }

    val isFocusPointOfInterestSupported = false

    var focusPointOfInterest: PointF = PointF(0.5f, 0.5f)

    val isLockingFocusWithCustomLensPositionSupported = false

    val lensPosition: Float
        get() {
            val value = resultLensPosition
            return if (value.isNaN()) 1f else value
        }

    val isSmoothAutoFocusSupported = false

    var whiteBalanceMode: WhiteBalanceMode = WhiteBalanceMode.continuousAutoWhiteBalance
        set(value) {
            if (value != WhiteBalanceMode.continuousAutoWhiteBalance) {
                logNotImplemented("whiteBalanceMode $value")
            }
            field = value
        }

    val isLockingWhiteBalanceWithCustomDeviceGainsSupported = false

    val maxWhiteBalanceGain: Float = 4f

    val deviceWhiteBalanceGains: WhiteBalanceGains
        get() = resultWhiteBalanceGains
            ?: deviceWhiteBalanceGains(WhiteBalanceTemperatureAndTintValues(temperature = 5000f, tint = 0f))

    val grayWorldDeviceWhiteBalanceGains: WhiteBalanceGains
        get() = deviceWhiteBalanceGains

    fun hasMediaType(mediaType: AVMediaType): Boolean = this.mediaType == mediaType

    fun lockForConfiguration() {
        synchronized(lock) {
            configurationLockCount += 1
        }
    }

    fun unlockForConfiguration() {
        var format = false
        var controls = false
        synchronized(lock) {
            if (configurationLockCount > 0) {
                configurationLockCount -= 1
            }
            if (configurationLockCount == 0) {
                format = pendingFormatChange
                controls = pendingControlsChange
                pendingFormatChange = false
                pendingControlsChange = false
            }
        }
        if (format || controls) {
            Camera2Engine.deviceConfigurationChanged(this, formatChanged = format)
        }
    }

    fun ramp(toVideoZoomFactor: Float, withRate: Float) {
        val target = toVideoZoomFactor.coerceIn(minAvailableVideoZoomFactor, maxAvailableVideoZoomFactor)
        if (withRate <= 0f) {
            videoZoomFactor = target
            return
        }
        val generation = synchronized(lock) {
            rampGeneration += 1
            rampTarget = target
            rampRate = abs(withRate)
            rampGeneration
        }
        val startNs = HostClock.nowNs()
        Camera2Engine.handler.post { rampStep(generation, startNs) }
    }

    fun cancelVideoZoomRamp() {
        synchronized(lock) {
            rampGeneration += 1
            rampRate = 0f
        }
    }

    fun isTorchModeSupported(mode: TorchMode): Boolean {
        return when (mode) {
            TorchMode.off -> true
            TorchMode.on -> hasTorch
            TorchMode.auto -> false
        }
    }

    fun setTorchModeOn(level: Float) {
        torchLevel = level.coerceIn(0f, 1f)
        if (torchMode == TorchMode.on) {
            didChange(format = false)
        } else {
            torchMode = TorchMode.on
        }
    }

    fun isExposureModeSupported(mode: ExposureMode): Boolean = mode == ExposureMode.continuousAutoExposure

    fun setExposureModeCustom(duration: Long, iso: Float, completionHandler: ((Long) -> Unit)? = null) {
        val exposure = if (duration == currentExposureDuration) exposureDuration else duration
        val sensitivity = if (iso == currentISO) this.iso else iso
        logNotImplemented("setExposureModeCustom duration=$exposure iso=$sensitivity")
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun setExposureTargetBias(bias: Float, completionHandler: ((Long) -> Unit)? = null) {
        exposureTargetBias = bias.coerceIn(minExposureTargetBias, maxOf(minExposureTargetBias, maxExposureTargetBias))
        didChange(format = false)
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun isFocusModeSupported(mode: FocusMode): Boolean = mode == FocusMode.continuousAutoFocus

    fun setFocusModeLocked(lensPosition: Float, completionHandler: ((Long) -> Unit)? = null) {
        logNotImplemented("setFocusModeLocked lensPosition=$lensPosition")
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun isWhiteBalanceModeSupported(mode: WhiteBalanceMode): Boolean =
        mode == WhiteBalanceMode.continuousAutoWhiteBalance

    fun setWhiteBalanceModeLocked(with: WhiteBalanceGains, completionHandler: ((Long) -> Unit)? = null) {
        logNotImplemented("setWhiteBalanceModeLocked $with")
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun deviceWhiteBalanceGains(values: WhiteBalanceTemperatureAndTintValues): WhiteBalanceGains {
        val temperature = values.temperature.coerceIn(1000f, 20000f)
        val red = (temperature / 6500f).pow(0.7f)
        val blue = (6500f / temperature).pow(0.7f)
        val minimum = minOf(red, 1f, blue)
        return WhiteBalanceGains(redGain = red / minimum, greenGain = 1f / minimum, blueGain = blue / minimum)
    }

    fun temperatureAndTintValues(gains: WhiteBalanceGains): WhiteBalanceTemperatureAndTintValues {
        if (gains.redGain <= 0f || gains.blueGain <= 0f) {
            return WhiteBalanceTemperatureAndTintValues(temperature = 6500f, tint = 0f)
        }
        val temperature = 6500f * (gains.redGain / gains.blueGain).pow(1f / 1.4f)
        return WhiteBalanceTemperatureAndTintValues(temperature = temperature, tint = 0f)
    }

    fun setDynamicAspectRatio(dynamicAspectRatio: AVCaptureAspectRatio, completionHandler: ((Long, Throwable?) -> Unit)? = null) {
        logNotImplemented("setDynamicAspectRatio $dynamicAspectRatio")
        completionHandler?.invoke(HostClock.nowUs(), null)
    }

    fun <Value> observe(
        keyPath: KProperty1<AVCaptureDevice, Value>,
        options: Set<NSKeyValueObservingOptions> = emptySet(),
        changeHandler: (AVCaptureDevice, NSKeyValueObservedChange<Value>) -> Unit,
    ): NSKeyValueObservation {
        if (keyPath.name !in observableKeys) {
            Log.i(TAG, "AVCaptureDevice.${keyPath.name} is not observable")
        }
        return observers.observe(keyPath, options, changeHandler)
    }

    override fun toString(): String = localizedName

    internal fun captureCompleted(result: CaptureResult) {
        val entry = camera ?: return
        val distance = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
        if (distance != null && !distance.isNaN()) {
            val oldValue = lensPosition
            val newValue = entry.lensPosition(focusDistance = distance)
            resultLensPosition = newValue
            if (newValue != oldValue && observers.isObserved) {
                observers.didChangeValue(AVCaptureDevice::lensPosition, oldValue, newValue)
            }
        }
        val sensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY)
        if (sensitivity != null) {
            val oldValue = iso
            val newValue = sensitivity.toFloat()
            resultIso = newValue
            if (newValue != oldValue && observers.isObserved) {
                observers.didChangeValue(AVCaptureDevice::iso, oldValue, newValue)
            }
        }
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        if (exposureTime != null && exposureTime > 0) {
            val oldValue = exposureDuration
            val newValue = exposureTime / 1000
            resultExposureDuration = newValue
            if (newValue != oldValue && observers.isObserved) {
                observers.didChangeValue(AVCaptureDevice::exposureDuration, oldValue, newValue)
            }
        }
        val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
        if (gains != null) {
            whiteBalanceGainsCompleted(gains)
        }
    }

    private fun whiteBalanceGainsCompleted(gains: RggbChannelVector) {
        val green = (gains.greenEven + gains.greenOdd) / 2
        val minimum = minOf(gains.red, green, gains.blue)
        if (!(minimum > 0f)) {
            return
        }
        val redGain = gains.red / minimum
        val greenGain = green / minimum
        val blueGain = gains.blue / minimum
        val current = resultWhiteBalanceGains
        if (current != null &&
            current.redGain == redGain &&
            current.greenGain == greenGain &&
            current.blueGain == blueGain
        ) {
            return
        }
        val oldValue = current ?: deviceWhiteBalanceGains
        val newValue = WhiteBalanceGains(redGain = redGain, greenGain = greenGain, blueGain = blueGain)
        resultWhiteBalanceGains = newValue
        if (newValue != oldValue && observers.isObserved) {
            observers.didChangeValue(AVCaptureDevice::deviceWhiteBalanceGains, oldValue, newValue)
        }
    }

    internal fun cameraZoomRatio(): Float {
        val entry = camera ?: return 1f
        return (zoomFactor * entry.zoomScale).coerceIn(entry.zoomRatioLower, entry.zoomRatioUpper)
    }

    private fun rampStep(generation: Long, previousNs: Long) {
        val nowNs = HostClock.nowNs()
        val seconds = (nowNs - previousNs) / 1_000_000_000f
        var done = false
        synchronized(lock) {
            if (generation != rampGeneration) {
                return
            }
            val current = zoomFactor
            val step = 2f.pow(rampRate * seconds)
            val next = if (rampTarget > current) {
                minOf(current * step, rampTarget)
            } else {
                maxOf(current / step, rampTarget)
            }
            zoomFactor = next
            done = next == rampTarget
            if (done) {
                rampRate = 0f
            }
        }
        didChange(format = false)
        if (!done) {
            Camera2Engine.handler.postDelayed({ rampStep(generation, nowNs) }, 33)
        }
    }

    private fun didChange(format: Boolean) {
        synchronized(lock) {
            if (configurationLockCount > 0) {
                if (format) {
                    pendingFormatChange = true
                } else {
                    pendingControlsChange = true
                }
                return
            }
        }
        Camera2Engine.deviceConfigurationChanged(this, formatChanged = format)
    }

    private fun logNotImplemented(what: String) {
        Log.i(TAG, "AVCaptureDevice.$what not implemented yet")
    }

    companion object {
        const val currentISO = 0f
        const val currentExposureDuration = kCMTimeInvalidUs
        private val observableKeys = setOf("lensPosition", "iso", "exposureDuration", "deviceWhiteBalanceGains")
        private val devicesLock = Any()
        private val videoDevices = HashMap<String, AVCaptureDevice>()
        private val audioDevices = HashMap<String, AVCaptureDevice>()

        fun withUniqueID(uniqueID: String): AVCaptureDevice? {
            synchronized(devicesLock) {
                videoDevices[uniqueID]?.let { return it }
                audioDevices[uniqueID]?.let { return it }
            }
            val entry = CameraCatalog.entry(uniqueID) ?: return null
            return videoDevice(entry)
        }

        fun default(deviceType: DeviceType, mediaType: AVMediaType?, position: Position): AVCaptureDevice? {
            if (mediaType == AVMediaType.audio || deviceType == DeviceType.microphone) {
                return if (deviceType == DeviceType.microphone) default(AVMediaType.audio) else null
            }
            val entries = CameraCatalog.entries().filter {
                position == Position.unspecified || it.position == position
            }
            val entry = entries.firstOrNull { it.deviceType == deviceType }
                ?: if (deviceType == DeviceType.builtInUltraWideCamera) {
                    entries.firstOrNull { it.reachesUltraWide && !it.isExternal }
                } else {
                    null
                }
            return entry?.let { videoDevice(it) }
        }

        fun default(deviceType: DeviceType, position: Position): AVCaptureDevice? {
            return default(deviceType, AVMediaType.video, position)
        }

        fun default(mediaType: AVMediaType): AVCaptureDevice? {
            return when (mediaType) {
                AVMediaType.audio -> AudioCaptureBridge.preferredCaptureDevice()
                    ?: makeAudioDevice(
                        uniqueID = "builtin",
                        localizedName = "Built-In Microphone",
                        audioDeviceInfo = null,
                        dataSourceID = null
                    )
                AVMediaType.video -> default(DeviceType.builtInWideAngleCamera, AVMediaType.video, Position.back)
                    ?: CameraCatalog.entries().firstOrNull()?.let { videoDevice(it) }
            }
        }

        fun makeAudioDevice(
            uniqueID: String,
            localizedName: String,
            audioDeviceInfo: AudioDeviceInfo?,
            dataSourceID: Int?,
        ): AVCaptureDevice {
            return synchronized(devicesLock) {
                val existing = audioDevices[uniqueID]
                if (existing != null &&
                    existing.localizedName == localizedName &&
                    existing.audioDeviceInfo?.id == audioDeviceInfo?.id &&
                    existing.dataSourceID == dataSourceID
                ) {
                    existing
                } else {
                    makeAudioDeviceLocked(uniqueID, localizedName, audioDeviceInfo, dataSourceID)
                }
            }
        }

        private fun makeAudioDeviceLocked(
            uniqueID: String,
            localizedName: String,
            audioDeviceInfo: AudioDeviceInfo?,
            dataSourceID: Int?,
        ): AVCaptureDevice {
            val device = AVCaptureDevice(
                uniqueID = uniqueID,
                localizedName = localizedName,
                position = Position.unspecified,
                deviceType = DeviceType.microphone,
                mediaType = AVMediaType.audio,
                camera = null,
                audioDeviceInfo = audioDeviceInfo,
                dataSourceID = dataSourceID,
            )
            audioDevices[uniqueID] = device
            return device
        }

        internal fun videoDevice(entry: CameraCatalog.Entry): AVCaptureDevice {
            return synchronized(devicesLock) {
                videoDevices.getOrPut(entry.id) {
                    AVCaptureDevice(
                        uniqueID = entry.id,
                        localizedName = entry.localizedName,
                        position = entry.position,
                        deviceType = entry.deviceType,
                        mediaType = AVMediaType.video,
                        camera = entry,
                        audioDeviceInfo = null,
                        dataSourceID = null,
                    )
                }
            }
        }

        internal fun colorSpaceName(colorSpace: Int): String {
            return when (colorSpace) {
                AVCaptureColorSpace.sRGB -> "SRGB"
                AVCaptureColorSpace.P3_D65 -> "P3_D65"
                AVCaptureColorSpace.HLG_BT2020 -> "HLG_BT2020"
                AVCaptureColorSpace.appleLog -> "Apple Log"
                else -> "Unknown"
            }
        }
    }
}

fun setCameraZoomLevel(device: AVCaptureDevice?, level: Float, rate: Float?): Float? {
    if (device == null) {
        Log.i("Media", "Device not ready to zoom")
        return null
    }
    val clampedLevel = level.coerceIn(1.0f, maxOf(1.0f, device.activeFormat.videoMaxZoomFactor))
    try {
        device.lockForConfiguration()
        if (rate != null) {
            device.ramp(toVideoZoomFactor = clampedLevel, withRate = rate)
        } else {
            device.videoZoomFactor = clampedLevel
        }
        device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i("Media", "While locking device for ramp: $error")
    }
    return clampedLevel
}

fun stopCameraZoomLevel(device: AVCaptureDevice?): Float? {
    if (device == null) {
        Log.i("Media", "Device not ready to zoom")
        return null
    }
    try {
        device.lockForConfiguration()
        device.videoZoomFactor = device.videoZoomFactor
        device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i("Media", "While locking device for stop: $error")
    }
    return device.videoZoomFactor
}
