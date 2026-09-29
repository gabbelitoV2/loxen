package com.moblin.android.media.haishinkit.media.video

import android.media.MediaFormat
import com.moblin.android.platform.log.Log
import android.util.Size
import com.moblin.android.common.various.clamped
import com.moblin.android.common.various.create
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.extension.description
import com.moblin.android.media.haishinkit.extension.isFrameRateSupported
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.avfoundation.AVCaptureAspectRatio
import com.moblin.android.platform.avfoundation.AVCaptureColorSpace
import com.moblin.android.platform.avfoundation.AVCaptureConnection
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDeviceInput
import com.moblin.android.platform.avfoundation.AVCaptureInput
import com.moblin.android.platform.avfoundation.AVCaptureMultiCamSession
import com.moblin.android.platform.avfoundation.AVCaptureOutput
import com.moblin.android.platform.avfoundation.AVCapturePhoto
import com.moblin.android.platform.avfoundation.AVCapturePhotoCaptureDelegate
import com.moblin.android.platform.avfoundation.AVCapturePhotoOutput
import com.moblin.android.platform.avfoundation.AVCapturePhotoSettings
import com.moblin.android.platform.avfoundation.AVCaptureSession
import com.moblin.android.platform.avfoundation.AVCaptureSessionControlsDelegate
import com.moblin.android.platform.avfoundation.AVCaptureSessionErrorKey
import com.moblin.android.platform.avfoundation.AVCaptureSessionInterruptionEnded
import com.moblin.android.platform.avfoundation.AVCaptureSessionRuntimeError
import com.moblin.android.platform.avfoundation.AVCaptureSessionWasInterrupted
import com.moblin.android.platform.avfoundation.AVCaptureVideoDataOutput
import com.moblin.android.platform.avfoundation.AVCaptureVideoDataOutputSampleBufferDelegate
import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation
import com.moblin.android.platform.avfoundation.AVCaptureVideoPreviewLayer
import com.moblin.android.platform.avfoundation.AVError
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.PHAssetCreationRequest
import com.moblin.android.platform.avfoundation.PHAssetResourceType
import com.moblin.android.platform.avfoundation.PHPhotoLibrary
import com.moblin.android.platform.avfoundation.session
import com.moblin.android.platform.avfoundation.setSessionWithNoConnection
import com.moblin.android.platform.core.Notification
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBufferGetHeight
import com.moblin.android.platform.video.CVPixelBufferGetPixelFormatType
import com.moblin.android.platform.video.CVPixelBufferGetWidth
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.CVPixelBufferPoolCreate
import com.moblin.android.platform.video.CVPixelBufferPoolCreatePixelBuffer
import com.moblin.android.platform.video.kCVPixelBufferHeightKey
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelBufferWidthKey
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import com.moblin.android.platform.video.swapPool
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.utils.fps
import com.moblin.android.various.utils.setAutoFps
import com.moblin.android.various.utils.setFps
import com.moblin.android.various.utils.setLowLightBoost
import com.moblin.android.various.utils.useLandscapeStreamAndPortraitUi
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "VideoCaptureSession"

var nativeLowLightBoost = false

var externalCameraVideoRange = false

data class CaptureDevice(
    val device: AVCaptureDevice,
    val id: UUID,
    val isVideoMirrored: Boolean,
)

data class CaptureDevices(
    var hasSceneDevice: Boolean,
    var devices: MutableList<CaptureDevice>,
) {
    fun getSceneDevice(): CaptureDevice? {
        return if (hasSceneDevice) {
            devices.firstOrNull()
        } else {
            null
        }
    }
}

interface VideoCaptureSessionDelegate {
    fun videoCaptureSessionDidOutput(device: AVCaptureDevice, cameraId: UUID?, sampleBuffer: MediaSample)

    fun videoCaptureSessionWasInterrupted()
}

private const val kCVPixelFormatComponentRange_FullRange = "FullRange"

private const val kCVPixelFormatComponentRange_VideoRange = "VideoRange"

private fun pixelFormatComponentRange(pixelFormat: Int): String? {
    return when (pixelFormat) {
        kCVPixelFormatType_420YpCbCr8BiPlanarFullRange -> kCVPixelFormatComponentRange_FullRange
        kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange -> kCVPixelFormatComponentRange_VideoRange
        else -> com.moblin.android.platform.video.CVPixelFormatDescriptionCreateWithPixelFormatType(null, pixelFormat)?.get(com.moblin.android.platform.video.kCVPixelFormatComponentRange) as? String
    }
}

fun isVideoRangePixelFormat(pixelFormat: Int): Boolean {
    return pixelFormatComponentRange(pixelFormat) == kCVPixelFormatComponentRange_VideoRange
}

fun isFullRangePixelFormat(pixelFormat: Int): Boolean {
    return pixelFormatComponentRange(pixelFormat) == kCVPixelFormatComponentRange_FullRange
}

fun <Format : VideoFormat> filterFormatsByColorRange(
    formats: List<Format>,
    colorRange: SettingsStreamColorRange,
): List<Format> {
    val preferences: List<(Format) -> Boolean> = when (colorRange) {
        SettingsStreamColorRange.full -> listOf(
            { isFullRangePixelFormat(it.pixelFormat) },
            { it.pixelFormat != kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange },
        )
        SettingsStreamColorRange.limited -> listOf(
            { isVideoRangePixelFormat(it.pixelFormat) },
        )
    }
    for (isPreferred in preferences) {
        val preferredFormats = formats.filter(isPreferred)
        if (preferredFormats.isNotEmpty()) {
            return preferredFormats
        }
    }
    return formats
}

interface VideoFormat {
    val pixelFormat: Int
}

val AVCaptureDevice.Format.pixelFormat: Int
    get() = formatDescription.mediaSubType.rawValue

private fun isExternalCameraVideoRange(
    device: AVCaptureDevice,
    colorRange: SettingsStreamColorRange,
): Boolean {
    return externalCameraVideoRange &&
        device.deviceType == AVCaptureDevice.DeviceType.external &&
        colorRange == SettingsStreamColorRange.limited
}

private class VideoRangeRelabeler {
    private var pool: CVPixelBufferPool? = null
    private var poolSize = CGSize.zero
    private var formatDescription: MediaFormat? = null
    private var logged = false

    fun relabel(sampleBuffer: MediaSample): MediaSample? {
        val imageBuffer = sampleBuffer.imageBuffer ?: return sampleBuffer
        if (CVPixelBufferGetPixelFormatType(imageBuffer) !=
            kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
        ) {
            return sampleBuffer
        }
        val width = CVPixelBufferGetWidth(imageBuffer)
        val height = CVPixelBufferGetHeight(imageBuffer)
        if (pool == null || poolSize.width != width.toDouble() || poolSize.height != height.toDouble()) {
            val attributes: Map<String, Any> = mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
                kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
                kCVPixelBufferMetalCompatibilityKey to true,
                kCVPixelBufferWidthKey to width,
                kCVPixelBufferHeightKey to height,
            )
            pool = swapPool(pool, null)
            pool = CVPixelBufferPoolCreate(attributes)
            poolSize = CGSize(width, height)
            formatDescription = null
        }
        val currentPool = pool ?: return null
        val outputImageBuffer = CVPixelBufferPoolCreatePixelBuffer(currentPool) ?: return null
        Unit
        if (formatDescription == null) {
            formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(outputImageBuffer)
            if (!logged) {
                logged = true
                Log.i(TAG, "video-unit: Relabeled 420f to 420v: $formatDescription")
            }
        }
        val newFormatDescription = formatDescription ?: return null
        return create(
            outputImageBuffer,
            newFormatDescription,
            sampleBuffer.durationUs,
            sampleBuffer.presentationTimeUs,
            sampleBuffer.decodeTimeStampUs,
        )
    }
}

private class DeviceOutputHandler(
    private val device: AVCaptureDevice,
    private val cameraId: UUID,
    private val delegate: VideoCaptureSessionDelegate?,
    relabelToVideoRange: Boolean,
) : AVCaptureVideoDataOutputSampleBufferDelegate {
    private val relabeler: VideoRangeRelabeler? = if (relabelToVideoRange) VideoRangeRelabeler() else null

    override fun captureOutput(output: AVCaptureOutput, didOutput: MediaSample, from: AVCaptureConnection) {
        val relabeler = relabeler
        if (relabeler != null) {
            val sampleBuffer = relabeler.relabel(didOutput) ?: return
            delegate?.videoCaptureSessionDidOutput(device, cameraId, sampleBuffer)
        } else {
            delegate?.videoCaptureSessionDidOutput(device, cameraId, didOutput)
        }
    }
}

private data class CaptureSessionDevice(
    val device: CaptureDevice,
    val input: AVCaptureInput,
    val output: AVCaptureVideoDataOutput,
    val connection: AVCaptureConnection,
    val photoOutput: AVCapturePhotoOutput?,
    val photoConnection: AVCaptureConnection?,
    val outputHandler: DeviceOutputHandler,
)

private sealed class VideoFormatResult {
    data class Found(
        val format: AVCaptureDevice.Format,
        val useAutoFrameRate: Boolean,
        val useLandscapeInPortrait: Boolean,
    ) : VideoFormatResult()

    data class NotFound(
        val error: String,
    ) : VideoFormatResult()
}

private fun makeCaptureSession(): AVCaptureMultiCamSession {
    val session = AVCaptureMultiCamSession()
    session.automaticallyConfiguresCaptureDeviceForWideColor = false
    if (session.isMultitaskingCameraAccessSupported) {
        session.isMultitaskingCameraAccessEnabled = true
    }
    return session
}

private fun setOrientation(
    device: AVCaptureDevice?,
    isLandscapeStreamAndPortraitUi: Boolean,
    connection: AVCaptureConnection,
    orientation: Int,
) {
    if (device?.deviceType == AVCaptureDevice.DeviceType.external) {
        connection.videoOrientation = AVCaptureVideoOrientation.landscapeRight
    } else if (useLandscapeStreamAndPortraitUi(device, isLandscapeStreamAndPortraitUi)) {
        connection.videoOrientation = AVCaptureVideoOrientation.portrait
    } else {
        connection.videoOrientation = orientation
    }
}

class VideoCaptureSession(colorRange: SettingsStreamColorRange) :
    AVCaptureSessionControlsDelegate,
    AVCapturePhotoCaptureDelegate
{
    var delegate: VideoCaptureSessionDelegate? = null
    var processor: Processor? = null
    val session = makeCaptureSession()
    private var device: AVCaptureDevice? = null
    private var devices: MutableList<CaptureSessionDevice> = mutableListOf()
    private var isRunning = false
    private var cameraControlsEnabled = false
    private var captureSize = Size(1920, 1080)
    private var fps = VideoUnit.defaultFrameRate
    private var preferAutoFps = false
    private var colorSpace: Int = AVCaptureColorSpace.sRGB
    private var isLandscapeStreamAndPortraitUi = false
    private val colorRange: SettingsStreamColorRange = colorRange

    var videoOrientation: Int = AVCaptureVideoOrientation.portrait
        set(value) {
            if (value == field) {
                return
            }
            field = value
            session.beginConfiguration()
            try {
                for (device in devices) {
                    updateOrientation(device = device)
                }
            } finally {
                session.commitConfiguration()
            }
        }

    var torch = false
        set(value) {
            field = value
            val device = this.device
            if (device == null) {
                if (torch) {
                    processor?.delegate?.streamNoTorch()
                }
                return
            }
            setTorchMode(device, if (torch) AVCaptureDevice.TorchMode.on else AVCaptureDevice.TorchMode.off)
        }

    var torchLevel: Float = 1.0f
        set(value) {
            field = value
            val device = this.device
            if (device == null || !torch) {
                return
            }
            setTorchMode(device, AVCaptureDevice.TorchMode.on)
        }

    init {
        NotificationCenter.default.addObserver(this, AVCaptureSessionRuntimeError, session) {
            handleSessionRuntimeError(it)
        }
    }

    fun startRunning() {
        isRunning = true
        addSessionObservers()
        session.startRunning()
    }

    fun stopRunning() {
        isRunning = false
        removeSessionObservers()
        session.stopRunning()
    }

    fun getFps(): Double {
        return fps
    }

    fun setFps(fps: Double, preferAutoFps: Boolean) {
        this.fps = fps
        this.preferAutoFps = preferAutoFps
        updateDevicesFormat()
    }

    fun setColorSpace(colorSpace: Int) {
        this.colorSpace = colorSpace
        updateDevicesFormat()
    }

    fun setCaptureSize(captureSize: Size) {
        this.captureSize = captureSize
        updateDevicesFormat()
    }

    fun setCameraControl(enabled: Boolean) {
        cameraControlsEnabled = enabled
        session.beginConfiguration()
        try {
            updateCameraControls()
        } finally {
            session.commitConfiguration()
        }
    }

    fun stopOutputtingSampleBuffers() {
        for (device in devices) {
            device.output.setSampleBufferDelegate(null, processorPipelineQueue)
        }
    }

    @Throws(Exception::class)
    fun attach(params: VideoUnitAttachParams) {
        isLandscapeStreamAndPortraitUi = params.isLandscapeStreamAndPortraitUi
        session.beginConfiguration()
        try {
            removeDevices(session)
            for (device in params.devices.devices) {
                setDeviceFormat(
                    device = device.device,
                    fps = fps,
                    preferAutoFrameRate = preferAutoFps,
                    colorSpace = colorSpace,
                )
                attachDevice(device, session, params.attachPhotoShoot)
            }
            device = params.devices.getSceneDevice()?.device
            for (device in devices) {
                if (device.connection.isVideoMirroringSupported) {
                    device.connection.isVideoMirrored = device.device.isVideoMirrored
                }
                if (device.connection.isVideoStabilizationSupported) {
                    device.connection.preferredVideoStabilizationMode = params.preferredVideoStabilizationMode
                }
                updateOrientation(device = device)
                device.output.setSampleBufferDelegate(device.outputHandler, processorPipelineQueue)
            }
            updateCameraControls()
            attachCameraPreviewLayers(params = params)
        } finally {
            session.commitConfiguration()
        }
    }

    fun takePhoto() {
        for (device in devices) {
            val photoOutput = device.photoOutput ?: continue
            val settings = AVCapturePhotoSettings()
            settings.maxPhotoDimensions = photoOutput.maxPhotoDimensions
            settings.photoQualityPrioritization = AVCapturePhotoOutput.QualityPrioritization.balanced
            settings.isShutterSoundSuppressionEnabled = true
            photoOutput.capturePhoto(settings = settings, delegate = this)
        }
    }

    private fun updateOrientation(device: CaptureSessionDevice) {
        updateOrientation(device = device, connection = device.connection)
        val photoConnection = device.photoConnection
        if (photoConnection != null) {
            updateOrientation(device = device, connection = photoConnection)
        }
    }

    private fun updateOrientation(device: CaptureSessionDevice, connection: AVCaptureConnection) {
        if (!connection.isVideoOrientationSupported) {
            return
        }
        setOrientation(
            device = device.device.device,
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi,
            connection = connection,
            orientation = videoOrientation,
        )
    }

    private fun attachCameraPreviewLayers(params: VideoUnitAttachParams) {
        for ((id, value) in params.cameraPreviewLayers) {
            val previewLayer = value as? AVCaptureVideoPreviewLayer ?: continue
            val device = devices.firstOrNull { it.device.id == id }
            val port = device?.input?.ports?.firstOrNull { it.mediaType == AVMediaType.video }
            if (!params.attachCameraPreview || device == null || port == null) {
                if (previewLayer.session != null) {
                    previewLayer.session = null
                }
                continue
            }
            if (previewLayer.session !== session) {
                previewLayer.setSessionWithNoConnection(session)
            }
            val connection = AVCaptureConnection(inputPort = port, videoPreviewLayer = previewLayer)
            if (!session.canAddConnection(connection)) {
                continue
            }
            session.addConnection(connection)
        }
    }

    private fun handleSessionRuntimeError(notification: Notification) {
        val error = notification.userInfo[AVCaptureSessionErrorKey] as? AVError ?: return
        val message = error.localizedFailureReason ?: "${error.code}"
        processor?.delegate?.streamVideoCaptureSessionError(message)
        processorControlQueue.launch {
            delay(500)
            if (isRunning) {
                session.startRunning()
            }
        }
    }

    private fun updateDevicesFormat() {
        for (device in devices) {
            setDeviceFormat(
                device = device.device.device,
                fps = fps,
                preferAutoFrameRate = preferAutoFps,
                colorSpace = colorSpace,
            )
        }
    }

    private fun addSessionObservers() {
        NotificationCenter.default.addObserver(this, AVCaptureSessionWasInterrupted, session) {
            sessionWasInterrupted(it)
        }
        NotificationCenter.default.addObserver(this, AVCaptureSessionInterruptionEnded, session) {
            sessionInterruptionEnded(it)
        }
    }

    private fun removeSessionObservers() {
        NotificationCenter.default.removeObserver(this, AVCaptureSessionWasInterrupted, session)
        NotificationCenter.default.removeObserver(this, AVCaptureSessionInterruptionEnded, session)
    }

    private fun sessionWasInterrupted(notification: Notification) {
        Log.d(TAG, "video-unit: Session interruption started")
        delegate?.videoCaptureSessionWasInterrupted()
    }

    private fun sessionInterruptionEnded(notification: Notification) {
        Log.d(TAG, "video-unit: Session interruption ended")
    }

    private fun findVideoFormat(
        device: AVCaptureDevice,
        width: Int,
        height: Int,
        fps: Double,
        preferAutoFrameRate: Boolean,
        colorSpace: Int,
    ): VideoFormatResult {
        var useAutoFrameRate = false
        var useLandscapeInPortrait = false
        var formats = device.formats
        formats = formats.filter { it.isFrameRateSupported(fps) }
        if (preferAutoFrameRate) {
            val autoFrameRateFormats = formats.filter { it.isAutoVideoFrameRateSupported }
            if (autoFrameRateFormats.isNotEmpty()) {
                formats = autoFrameRateFormats
                useAutoFrameRate = true
            }
        }
        formats = formats.filter { it.formatDescription.dimensions.width == width }
        if (isLandscapeStreamAndPortraitUi) {
            val formatsWithRatio9x16 = formats.filter {
                it.supportedDynamicAspectRatios.contains(AVCaptureAspectRatio.ratio9x16)
            }
            if (formatsWithRatio9x16.isNotEmpty()) {
                formats = formatsWithRatio9x16
                useLandscapeInPortrait = true
            } else {
                formats = formats.filter { it.formatDescription.dimensions.height == height }
            }
        } else {
            formats = formats.filter { it.formatDescription.dimensions.height == height }
        }
        formats = formats.filter { it.supportedColorSpaces.contains(colorSpace) }
        if (formats.isEmpty()) {
            return VideoFormatResult.NotFound(
                "No format found matching ${height}p${fps.toInt()}, ${AVCaptureColorSpace.description(colorSpace)}",
            )
        }
        formats = formats.filter { !it.isVideoBinned }
        if (formats.isEmpty()) {
            return VideoFormatResult.NotFound("No unbinned video format found")
        }
        val formatColorRange = if (isExternalCameraVideoRange(device, colorRange)) {
            SettingsStreamColorRange.full
        } else {
            colorRange
        }
        val format = filterFormatsByColorRange(formats, formatColorRange).firstOrNull()
            ?: return VideoFormatResult.NotFound("Unsupported pixel format")
        return VideoFormatResult.Found(
            format = format,
            useAutoFrameRate = useAutoFrameRate,
            useLandscapeInPortrait = useLandscapeInPortrait,
        )
    }

    private fun reportFormatNotFound(device: AVCaptureDevice, error: String) {
        val (minFps, maxFps) = device.fps
        val activeFormat = "Using default: " +
            "${device.activeFormat.formatDescription.dimensions.height}p, " +
            "$minFps-$maxFps FPS, " +
            "${AVCaptureColorSpace.description(device.activeColorSpace)}, " +
            "${device.activeFormat.formatDescription.mediaSubType}"
        Log.i(TAG, "video-unit: $error")
        Log.i(TAG, "video-unit: $activeFormat")
        for (format in device.formats) {
            Log.i(TAG, "video-unit: Available format: $format")
        }
    }

    private fun setDeviceFormat(
        device: AVCaptureDevice?,
        fps: Double,
        preferAutoFrameRate: Boolean,
        colorSpace: Int,
    ) {
        if (device == null) {
            return
        }
        when (
            val result = findVideoFormat(
                device = device,
                width = captureSize.width,
                height = captureSize.height,
                fps = fps,
                preferAutoFrameRate = preferAutoFrameRate,
                colorSpace = colorSpace,
            )
        ) {
            is VideoFormatResult.Found -> applyDeviceFormat(
                device = device,
                format = result.format,
                fps = fps,
                colorSpace = colorSpace,
                useAutoFrameRate = result.useAutoFrameRate,
                useLandscapeInPortrait = result.useLandscapeInPortrait,
            )
            is VideoFormatResult.NotFound -> reportFormatNotFound(device, result.error)
        }
    }

    private fun applyDeviceFormat(
        device: AVCaptureDevice,
        format: AVCaptureDevice.Format,
        fps: Double,
        colorSpace: Int,
        useAutoFrameRate: Boolean,
        useLandscapeInPortrait: Boolean,
    ) {
        Log.d(TAG, "video-unit: Selected format: $format")
        try {
            device.lockForConfiguration()
            if (device.activeFormat != format) {
                device.activeFormat = format
            }
            device.activeColorSpace = colorSpace
            device.setLowLightBoost(value = nativeLowLightBoost)
            if (useAutoFrameRate) {
                device.setAutoFps()
                processor?.delegate?.streamSelectedFps(auto = true)
            } else {
                device.setFps(frameRate = fps)
                processor?.delegate?.streamSelectedFps(auto = false)
            }
            if (useLandscapeInPortrait) {
                if (format.supportedDynamicAspectRatios.contains(AVCaptureAspectRatio.ratio9x16)) {
                    device.setDynamicAspectRatio(AVCaptureAspectRatio.ratio9x16)
                }
            }
            device.unlockForConfiguration()
        } catch (error: Exception) {
            Log.i(TAG, "video-unit: Error while locking device: $error")
        }
    }

    @Throws(Exception::class)
    private fun attachDevice(device: CaptureDevice, session: AVCaptureMultiCamSession, attachPhotoShoot: Boolean) {
        val input = AVCaptureDeviceInput(device = device.device)
        val output = AVCaptureVideoDataOutput()
        val relabelToVideoRange = isExternalCameraVideoRange(device.device, colorRange)
        output.videoSettings = mapOf(
            kCVPixelBufferPixelFormatTypeKey to if (relabelToVideoRange) {
                kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
            } else {
                colorRange.pixelFormatType()
            },
        )
        var connection: AVCaptureConnection? = null
        val port = input.ports.firstOrNull { it.mediaType == AVMediaType.video }
        if (port != null) {
            connection = AVCaptureConnection(inputPorts = listOf(port), output = output)
        }
        var failed = false
        if (session.canAddInput(input)) {
            session.addInputWithNoConnections(input)
        } else {
            failed = true
        }
        if (session.canAddOutput(output)) {
            session.addOutputWithNoConnections(output)
        } else {
            failed = true
        }
        if (connection != null && session.canAddConnection(connection)) {
            session.addConnection(connection)
        } else {
            failed = true
        }
        var photoOutput: AVCapturePhotoOutput? = null
        var photoConnection: AVCaptureConnection? = null
        if (attachPhotoShoot) {
            val newPhotoOutput = AVCapturePhotoOutput()
            photoOutput = newPhotoOutput
            val photoPort = input.ports.firstOrNull { it.mediaType == AVMediaType.video }
            if (photoPort != null) {
                photoConnection = AVCaptureConnection(inputPorts = listOf(photoPort), output = newPhotoOutput)
            }
            if (session.canAddOutput(newPhotoOutput)) {
                session.addOutputWithNoConnections(newPhotoOutput)
            } else {
                failed = true
            }
            val newPhotoConnection = photoConnection
            if (newPhotoConnection != null && session.canAddConnection(newPhotoConnection)) {
                session.addConnection(newPhotoConnection)
            } else {
                failed = true
            }
            newPhotoOutput.maxPhotoDimensions = device.device.activeFormat.supportedMaxPhotoDimensions.last()
            newPhotoOutput.maxPhotoQualityPrioritization = AVCapturePhotoOutput.QualityPrioritization.balanced
        }
        if (failed) {
            processor?.delegate?.streamVideoAttachCameraError()
        } else {
            devices.add(
                CaptureSessionDevice(
                    device = device,
                    input = input,
                    output = output,
                    connection = connection!!,
                    photoOutput = photoOutput,
                    photoConnection = photoConnection,
                    outputHandler = DeviceOutputHandler(
                        device = device.device,
                        cameraId = device.id,
                        delegate = delegate,
                        relabelToVideoRange = relabelToVideoRange,
                    ),
                )
            )
        }
    }

    private fun removeDevices(session: AVCaptureMultiCamSession) {
        for (device in devices) {
            removeConnection(session, device.photoConnection)
            removeOutput(session, device.photoOutput)
            removeConnection(session, device.connection)
            removeInput(session, device.input)
            removeOutput(session, device.output)
        }
        devices.clear()
    }

    private fun removeConnection(session: AVCaptureMultiCamSession, connection: AVCaptureConnection?) {
        if (connection != null && session.connections.contains(connection)) {
            session.removeConnection(connection)
        }
    }

    private fun removeInput(session: AVCaptureMultiCamSession, input: AVCaptureInput?) {
        if (input != null && session.inputs.contains(input)) {
            session.removeInput(input)
        }
    }

    private fun removeOutput(session: AVCaptureMultiCamSession, output: AVCaptureOutput?) {
        if (output != null && session.outputs.contains(output)) {
            session.removeOutput(output)
        }
    }

    private fun setTorchMode(device: AVCaptureDevice, torchMode: AVCaptureDevice.TorchMode) {
        if (!device.isTorchModeSupported(torchMode)) {
            if (torchMode == AVCaptureDevice.TorchMode.on) {
                processor?.delegate?.streamNoTorch()
            }
            return
        }
        try {
            device.lockForConfiguration()
            if (torchMode == AVCaptureDevice.TorchMode.on) {
                device.setTorchModeOn(level = torchLevel.clamped(to = 0.01f..1.0f))
            } else {
                device.torchMode = torchMode
            }
            device.unlockForConfiguration()
        } catch (error: Exception) {
            Log.i(TAG, "video-unit: Error while setting torch: $error")
        }
    }

    private fun updateCameraControls() {
        if (session.supportsControls) {
            removeCameraControls()
            addCameraControls()
        }
    }

    fun addCameraControls() {
    }

    fun removeCameraControls() {
    }

    override fun sessionControlsDidBecomeActive(session: AVCaptureSession) {}

    override fun sessionControlsWillEnterFullscreenAppearance(session: AVCaptureSession) {}

    override fun sessionControlsWillExitFullscreenAppearance(session: AVCaptureSession) {}

    override fun sessionControlsDidBecomeInactive(session: AVCaptureSession) {}

    override fun photoOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: Throwable?) {
        if (error != null) {
            Log.i(TAG, "video-unit: Photo error: $error")
            return
        }
        val photoData = didFinishProcessingPhoto.fileDataRepresentation()
        if (photoData != null) {
            PHPhotoLibrary.shared().performChanges({
                val creationRequest = PHAssetCreationRequest.forAsset()
                creationRequest.addResource(with = PHAssetResourceType.photo, data = photoData, options = null)
            }) { _, saveError ->
                if (saveError != null) {
                    Log.i(TAG, "video-unit: Error saving photo: ${saveError.localizedMessage}")
                    return@performChanges
                }
            }
        }
    }
}
