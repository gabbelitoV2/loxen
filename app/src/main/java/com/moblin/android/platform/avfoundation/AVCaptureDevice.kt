package com.moblin.android.platform.avfoundation

import android.graphics.PointF
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.media.AudioDeviceInfo
import android.util.Log
import android.util.Size
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.capture.AudioCaptureBridge
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.capture.CameraCatalog
import com.moblin.android.platform.capture.CameraControlCapabilities
import com.moblin.android.platform.capture.CameraControlState
import com.moblin.android.platform.capture.CameraControls
import com.moblin.android.platform.capture.CameraFormats
import com.moblin.android.platform.capture.ControlTrigger
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

    @Volatile
    private var resultColorTransform: ColorSpaceTransform? = null

    @Volatile
    private var whiteBalanceScale = CameraControls.defaultWhiteBalanceScale

    private var focusModeValue = FocusMode.continuousAutoFocus
    private var focusPoint = PointF(0.5f, 0.5f)
    private var heldLensPosition: Float? = null
    private var focusScan = 0L
    private var focusScanSent = 0L
    private var focusScanArmed = 0L
    private var focusScanRetried = 0L
    private var exposureModeValue = ExposureMode.continuousAutoExposure
    private var exposurePoint = PointF(0.5f, 0.5f)
    private var heldIso: Float? = null
    private var heldExposureDuration: Long? = null
    private var heldExposureBias: Float? = null
    private var exposureScan = 0L
    private var exposureScanSent = 0L
    private var exposureScanArmed = 0L
    private var exposureScanRetried = 0L
    private var whiteBalanceModeValue = WhiteBalanceMode.continuousAutoWhiteBalance
    private var heldWhiteBalanceGains: WhiteBalanceGains? = null
    private var heldColorTransform: ColorSpaceTransform? = null

    private val controls: CameraControlCapabilities?
        get() = camera?.controls

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
    var activeFormat: Format = CameraFormats.defaultFormat(formats, camera)
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

    var exposureMode: ExposureMode
        get() = synchronized(lock) { exposureModeValue }
        set(value) {
            changeExposureMode(value)
        }

    val isExposurePointOfInterestSupported: Boolean
        get() = controls?.supportsExposurePointOfInterest ?: false

    var exposurePointOfInterest: PointF
        get() = synchronized(lock) { PointF(exposurePoint.x, exposurePoint.y) }
        set(value) {
            synchronized(lock) {
                exposurePoint = PointF(value.x.coerceIn(0f, 1f), value.y.coerceIn(0f, 1f))
            }
            didChange(format = false)
        }

    val iso: Float
        get() {
            synchronized(lock) { heldExposure() }?.let { return it.first }
            val value = resultIso
            if (!value.isNaN()) {
                return value
            }
            return 100f.coerceIn(activeFormat.minISO, maxOf(activeFormat.minISO, activeFormat.maxISO))
        }

    val exposureDuration: Long
        get() {
            synchronized(lock) { heldExposure() }?.let { return it.second }
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
        get() = controls?.minExposureTargetBias ?: -8f

    val maxExposureTargetBias: Float
        get() = controls?.maxExposureTargetBias ?: 8f

    var focusMode: FocusMode
        get() = synchronized(lock) { focusModeValue }
        set(value) {
            changeFocusMode(value)
        }

    val isFocusPointOfInterestSupported: Boolean
        get() = controls?.supportsFocusPointOfInterest ?: false

    var focusPointOfInterest: PointF
        get() = synchronized(lock) { PointF(focusPoint.x, focusPoint.y) }
        set(value) {
            synchronized(lock) {
                focusPoint = PointF(value.x.coerceIn(0f, 1f), value.y.coerceIn(0f, 1f))
            }
            didChange(format = false)
        }

    val isLockingFocusWithCustomLensPositionSupported: Boolean
        get() = controls?.supportsManualFocus ?: false

    val lensPosition: Float
        get() {
            synchronized(lock) { heldLensPosition }?.let { return it }
            val value = resultLensPosition
            return if (value.isNaN()) 1f else value
        }

    val isSmoothAutoFocusSupported = false

    var whiteBalanceMode: WhiteBalanceMode
        get() = synchronized(lock) { whiteBalanceModeValue }
        set(value) {
            changeWhiteBalanceMode(value)
        }

    val isLockingWhiteBalanceWithCustomDeviceGainsSupported: Boolean
        get() = controls?.supportsCustomWhiteBalanceGains ?: false

    val maxWhiteBalanceGain: Float = 4f

    val deviceWhiteBalanceGains: WhiteBalanceGains
        get() = synchronized(lock) { heldWhiteBalanceGains }
            ?: resultWhiteBalanceGains
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

    fun isExposureModeSupported(mode: ExposureMode): Boolean {
        val controls = controls ?: return false
        return when (mode) {
            ExposureMode.locked -> controls.supportsLockedExposure
            ExposureMode.autoExpose -> controls.supportsAutoExposure
            ExposureMode.continuousAutoExposure -> controls.supportsAutoExposure
            ExposureMode.custom -> controls.supportsCustomExposure
        }
    }

    fun setExposureModeCustom(duration: Long, iso: Float, completionHandler: ((Long) -> Unit)? = null) {
        if (!isExposureModeSupported(ExposureMode.custom)) {
            logNotSupported("setExposureModeCustom")
        } else {
            changeControls {
                val format = activeFormat
                val exposure = if (duration == currentExposureDuration) {
                    exposureDuration
                } else {
                    duration.coerceIn(
                        format.minExposureDuration,
                        maxOf(format.minExposureDuration, format.maxExposureDuration)
                    )
                }
                val sensitivity = if (iso == currentISO) {
                    this.iso
                } else {
                    iso.coerceIn(format.minISO, maxOf(format.minISO, format.maxISO))
                }
                exposureModeValue = ExposureMode.custom
                heldExposureDuration = exposure
                heldIso = sensitivity
                heldExposureBias = null
            }
        }
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun setExposureTargetBias(bias: Float, completionHandler: ((Long) -> Unit)? = null) {
        val old = observedValues()
        exposureTargetBias = bias.coerceIn(minExposureTargetBias, maxOf(minExposureTargetBias, maxExposureTargetBias))
        notifyObservers(old)
        didChange(format = false)
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun isFocusModeSupported(mode: FocusMode): Boolean {
        val controls = controls ?: return false
        return when (mode) {
            FocusMode.locked -> controls.supportsManualFocus || controls.supportsAutoFocus
            FocusMode.autoFocus -> controls.supportsAutoFocus
            FocusMode.continuousAutoFocus -> controls.continuousAutoFocusMode != null
        }
    }

    fun setFocusModeLocked(lensPosition: Float, completionHandler: ((Long) -> Unit)? = null) {
        if (!isLockingFocusWithCustomLensPositionSupported) {
            logNotSupported("setFocusModeLocked")
        } else {
            changeControls {
                val position = if (lensPosition == currentLensPosition) {
                    this.lensPosition
                } else {
                    lensPosition.coerceIn(0f, 1f)
                }
                focusModeValue = FocusMode.locked
                heldLensPosition = position
            }
        }
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun isWhiteBalanceModeSupported(mode: WhiteBalanceMode): Boolean {
        val controls = controls ?: return false
        return when (mode) {
            WhiteBalanceMode.locked -> controls.supportsLockedWhiteBalance
            WhiteBalanceMode.autoWhiteBalance -> false
            WhiteBalanceMode.continuousAutoWhiteBalance -> controls.supportsAutoWhiteBalance
        }
    }

    fun setWhiteBalanceModeLocked(with: WhiteBalanceGains, completionHandler: ((Long) -> Unit)? = null) {
        if (!isLockingWhiteBalanceWithCustomDeviceGainsSupported) {
            logNotSupported("setWhiteBalanceModeLocked")
        } else {
            changeControls {
                val gains = if (with == currentWhiteBalanceGains) {
                    deviceWhiteBalanceGains
                } else {
                    WhiteBalanceGains(
                        redGain = with.redGain.coerceIn(1f, maxWhiteBalanceGain),
                        greenGain = with.greenGain.coerceIn(1f, maxWhiteBalanceGain),
                        blueGain = with.blueGain.coerceIn(1f, maxWhiteBalanceGain),
                    )
                }
                if (heldWhiteBalanceGains == null) {
                    heldColorTransform = resultColorTransform
                }
                whiteBalanceModeValue = WhiteBalanceMode.locked
                heldWhiteBalanceGains = gains
            }
        }
        completionHandler?.invoke(HostClock.nowUs())
    }

    fun deviceWhiteBalanceGains(values: WhiteBalanceTemperatureAndTintValues): WhiteBalanceGains {
        return CameraControls.temperatureGains(values.temperature, whiteBalanceScale)
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
        captureCompleted(result, result.request?.tag)
    }

    internal fun captureCompleted(result: CaptureResult, requestTag: Any?) {
        val entry = camera ?: return
        val old = observedValues()
        val distance = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
        if (distance != null && !distance.isNaN()) {
            resultLensPosition = entry.lensPosition(focusDistance = distance)
        }
        val sensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY)
        if (sensitivity != null) {
            resultIso = sensitivity.toFloat()
        }
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        if (exposureTime != null && exposureTime > 0) {
            resultExposureDuration = exposureTime / 1000
        }
        val gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
        if (gains != null) {
            whiteBalanceGainsCompleted(gains, result.get(CaptureResult.CONTROL_AWB_MODE))
        }
        val changed = controlsCompleted(
            entry.controls,
            requestTag as? ControlTrigger,
            result.get(CaptureResult.CONTROL_AF_STATE),
            result.get(CaptureResult.CONTROL_AE_STATE),
            result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM),
        )
        notifyObservers(old)
        if (changed) {
            didChange(format = false)
        }
    }

    internal fun controlState(): CameraControlState {
        return synchronized(lock) {
            val exposure = heldExposure()
            CameraControlState(
                focusMode = focusModeValue,
                focusPointOfInterest = PointF(focusPoint.x, focusPoint.y),
                lensPosition = heldLensPosition,
                exposureMode = exposureModeValue,
                exposurePointOfInterest = PointF(exposurePoint.x, exposurePoint.y),
                iso = exposure?.first,
                exposureDuration = exposure?.second,
                exposureTargetBias = exposureTargetBias,
                whiteBalanceMode = whiteBalanceModeValue,
                whiteBalanceGains = heldWhiteBalanceGains,
                colorTransform = heldColorTransform,
            )
        }
    }

    internal fun controlSessionStarted() {
        synchronized(lock) {
            focusScanSent = 0L
            focusScanArmed = 0L
            exposureScanSent = 0L
            exposureScanArmed = 0L
        }
    }

    internal fun takeControlTrigger(): ControlTrigger? {
        val controls = controls ?: return null
        synchronized(lock) {
            val focus = if (focusModeValue == FocusMode.autoFocus &&
                focusScan != focusScanSent &&
                controls.supportsAutoFocus
            ) {
                focusScan
            } else {
                0L
            }
            val exposure = if (exposureModeValue == ExposureMode.autoExpose &&
                exposureScan != exposureScanSent &&
                controls.supportsLockedExposure
            ) {
                exposureScan
            } else {
                0L
            }
            if (focus == 0L && exposure == 0L) {
                return null
            }
            if (focus != 0L) {
                focusScanSent = focus
            }
            if (exposure != 0L) {
                exposureScanSent = exposure
            }
            return ControlTrigger(focusScan = focus, exposureScan = exposure)
        }
    }

    internal fun controlTriggerFailed(trigger: ControlTrigger) {
        val retry = synchronized(lock) {
            var retry = false
            if (trigger.focusScan != 0L &&
                trigger.focusScan == focusScan &&
                focusScanSent == focusScan &&
                focusScanArmed != focusScan &&
                focusScanRetried != focusScan
            ) {
                focusScanSent = 0L
                focusScanRetried = focusScan
                retry = true
            }
            if (trigger.exposureScan != 0L &&
                trigger.exposureScan == exposureScan &&
                exposureScanSent == exposureScan &&
                exposureScanArmed != exposureScan &&
                exposureScanRetried != exposureScan
            ) {
                exposureScanSent = 0L
                exposureScanRetried = exposureScan
                retry = true
            }
            retry
        }
        if (retry) {
            didChange(format = false)
        }
    }

    private fun changeFocusMode(mode: FocusMode) {
        if (!isFocusModeSupported(mode)) {
            logNotSupported("focusMode $mode")
            return
        }
        changeControls {
            heldLensPosition = if (mode == FocusMode.locked && controls?.supportsManualFocus == true) {
                heldLensPosition ?: reportedLensPosition()
            } else {
                null
            }
            focusModeValue = mode
            if (mode == FocusMode.autoFocus) {
                focusScan += 1
            }
        }
    }

    private fun changeExposureMode(mode: ExposureMode) {
        if (!isExposureModeSupported(mode)) {
            logNotSupported("exposureMode $mode")
            return
        }
        changeControls {
            val manual = (mode == ExposureMode.locked || mode == ExposureMode.custom) &&
                controls?.supportsCustomExposure == true &&
                (heldIso != null || isExposureReported())
            val sensitivity = if (manual) iso else null
            val duration = if (manual) exposureDuration else null
            exposureModeValue = mode
            heldIso = sensitivity
            heldExposureDuration = duration
            heldExposureBias = if (manual && mode == ExposureMode.locked) exposureTargetBias else null
            if (mode == ExposureMode.autoExpose) {
                exposureScan += 1
            }
        }
    }

    private fun changeWhiteBalanceMode(mode: WhiteBalanceMode) {
        if (!isWhiteBalanceModeSupported(mode)) {
            logNotSupported("whiteBalanceMode $mode")
            return
        }
        changeControls {
            if (mode == WhiteBalanceMode.locked && controls?.supportsCustomWhiteBalanceGains == true) {
                if (heldWhiteBalanceGains == null) {
                    heldWhiteBalanceGains = resultWhiteBalanceGains
                    heldColorTransform = resultColorTransform
                }
            } else {
                heldWhiteBalanceGains = null
                heldColorTransform = null
            }
            whiteBalanceModeValue = mode
        }
    }

    private fun controlsCompleted(
        controls: CameraControlCapabilities,
        trigger: ControlTrigger?,
        afState: Int?,
        aeState: Int?,
        colorTransform: ColorSpaceTransform?,
    ): Boolean {
        synchronized(lock) {
            var changed = false
            if (trigger != null) {
                if (trigger.focusScan != 0L && trigger.focusScan == focusScan) {
                    focusScanArmed = focusScan
                }
                if (trigger.exposureScan != 0L && trigger.exposureScan == exposureScan) {
                    exposureScanArmed = exposureScan
                }
            }
            if (focusModeValue == FocusMode.autoFocus &&
                focusScan != 0L &&
                focusScanArmed == focusScan &&
                (afState == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                    afState == CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)
            ) {
                heldLensPosition = if (controls.supportsManualFocus) reportedLensPosition() else null
                focusModeValue = FocusMode.locked
                changed = true
            }
            val manualExposure = controls.supportsCustomExposure && isExposureReported()
            if (exposureModeValue == ExposureMode.autoExpose &&
                exposureScan != 0L &&
                exposureScanArmed == exposureScan &&
                (aeState == CameraMetadata.CONTROL_AE_STATE_CONVERGED ||
                    aeState == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED) &&
                (manualExposure || controls.aeLockAvailable)
            ) {
                if (manualExposure) {
                    heldIso = resultIso
                    heldExposureDuration = resultExposureDuration
                    heldExposureBias = exposureTargetBias
                }
                exposureModeValue = ExposureMode.locked
                changed = true
            }
            if (whiteBalanceModeValue == WhiteBalanceMode.locked &&
                heldWhiteBalanceGains != null &&
                heldColorTransform == null
            ) {
                heldColorTransform = colorTransform ?: CameraControls.identityTransform
                changed = true
            } else if (colorTransform != null && heldWhiteBalanceGains == null) {
                resultColorTransform = colorTransform
            }
            return changed
        }
    }

    private fun reportedLensPosition(): Float? {
        val value = resultLensPosition
        return if (value.isNaN()) null else value
    }

    private fun isExposureReported(): Boolean {
        return !resultIso.isNaN() && resultExposureDuration != kCMTimeInvalidUs
    }

    private fun heldExposure(): Pair<Float, Long>? {
        val iso = heldIso ?: return null
        val duration = heldExposureDuration ?: return null
        val bias = heldExposureBias ?: return Pair(iso, duration)
        val controls = controls ?: return Pair(iso, duration)
        return CameraControls.biasedExposure(
            iso,
            duration,
            exposureTargetBias - bias,
            controls,
            activeVideoMaxFrameDuration,
        )
    }

    private class ObservedValues(
        val lensPosition: Float,
        val iso: Float,
        val exposureDuration: Long,
        val whiteBalanceGains: WhiteBalanceGains,
    )

    private fun observedValues(): ObservedValues? {
        if (!observers.isObserved) {
            return null
        }
        return ObservedValues(lensPosition, iso, exposureDuration, deviceWhiteBalanceGains)
    }

    private fun notifyObservers(old: ObservedValues?) {
        if (old == null) {
            return
        }
        observers.didChangeValue(AVCaptureDevice::lensPosition, old.lensPosition, lensPosition)
        observers.didChangeValue(AVCaptureDevice::iso, old.iso, iso)
        observers.didChangeValue(AVCaptureDevice::exposureDuration, old.exposureDuration, exposureDuration)
        observers.didChangeValue(
            AVCaptureDevice::deviceWhiteBalanceGains,
            old.whiteBalanceGains,
            deviceWhiteBalanceGains
        )
    }

    private inline fun changeControls(block: () -> Unit) {
        val old = observedValues()
        synchronized(lock) {
            block()
        }
        notifyObservers(old)
        didChange(format = false)
    }

    private fun whiteBalanceGainsCompleted(gains: RggbChannelVector, awbMode: Int?) {
        val automatic = if (awbMode != null) {
            awbMode != CameraMetadata.CONTROL_AWB_MODE_OFF
        } else {
            synchronized(lock) { heldWhiteBalanceGains == null }
        }
        if (automatic) {
            CameraControls.whiteBalanceScale(gains)?.let { whiteBalanceScale = it }
        }
        val newValue = CameraControls.deviceGains(gains) ?: return
        val current = resultWhiteBalanceGains
        if (current != null &&
            current.redGain == newValue.redGain &&
            current.greenGain == newValue.greenGain &&
            current.blueGain == newValue.blueGain
        ) {
            return
        }
        resultWhiteBalanceGains = newValue
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

    private fun logNotSupported(what: String) {
        Log.i(TAG, "AVCaptureDevice.$what not supported by camera $uniqueID")
    }

    companion object {
        const val currentISO = 0f
        const val currentExposureDuration = kCMTimeInvalidUs
        const val currentLensPosition = -1f
        val currentWhiteBalanceGains = WhiteBalanceGains(redGain = 0f, greenGain = 0f, blueGain = 0f)
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
                ?: when (deviceType) {
                    DeviceType.builtInUltraWideCamera -> entries.firstOrNull { it.reachesUltraWide && !it.isExternal }
                    DeviceType.builtInWideAngleCamera -> entries.firstOrNull {
                        it.deviceType == DeviceType.builtInTripleCamera ||
                            it.deviceType == DeviceType.builtInDualWideCamera ||
                            it.deviceType == DeviceType.builtInDualCamera
                    }
                    DeviceType.builtInTelephotoCamera -> entries.firstOrNull {
                        it.deviceType == DeviceType.builtInTripleCamera || it.deviceType == DeviceType.builtInDualCamera
                    }
                    else -> null
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
