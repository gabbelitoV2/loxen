package com.moblin.android.media.haishinkit.media.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.RectF
import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.SizeF
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderControlDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.media.MacScreenCapture
import com.moblin.android.media.haishinkit.media.MacScreenCaptureDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.model.screenCaptureCameraId
import com.moblin.android.various.model.screenCaptureCameraName
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import com.moblin.android.AppDelegate

private const val deltaLimit = 0.03

data class DetectionJob(
    val videoSourceId: UUID,
    val imageBuffer: Image,
    val detectFaces: Boolean,
    val detectText: Boolean
)

data class VideoUnitAttachParams(
    val devices: CaptureDevices,
    val builtinDelay: Double,
    val cameraPreviewLayers: Map<UUID, Any>,
    val attachCameraPreview: Boolean,
    val showCameraPreview: Boolean,
    val externalDisplayPreview: Boolean,
    val bufferedVideo: UUID?,
    val preferredVideoStabilizationMode: Int,
    val ignoreFramesAfterAttachSeconds: Double,
    val fillFrame: Boolean,
    val isLandscapeStreamAndPortraitUi: Boolean,
    val forceSceneTransition: Boolean,
    val macScreenCapture: Boolean,
    val attachPhotoShoot: Boolean
) {
    fun canQuickSwitchTo(other: VideoUnitAttachParams): Boolean {
        if (devices.devices.count() != other.devices.devices.count()) {
            return false
        }
        for (device in devices.devices) {
            val otherDevice = other.devices.devices.firstOrNull { it.id == device.id }
            if (otherDevice != null) {
                if (device.isVideoMirrored != otherDevice.isVideoMirrored) {
                    return false
                }
            } else {
                return false
            }
        }
        if (attachCameraPreview != other.attachCameraPreview) {
            return false
        }
        if (builtinDelay != other.builtinDelay) {
            return false
        }
        if (preferredVideoStabilizationMode != other.preferredVideoStabilizationMode) {
            return false
        }
        if (isLandscapeStreamAndPortraitUi != other.isLandscapeStreamAndPortraitUi) {
            return false
        }
        if (forceSceneTransition) {
            return false
        }
        if (macScreenCapture != other.macScreenCapture) {
            return false
        }
        if (attachPhotoShoot != other.attachPhotoShoot) {
            return false
        }
        return true
    }
}

enum class SceneSwitchTransition {
    BLUR,
    FREEZE,
    BLUR_AND_ZOOM
}

var pixelFormatType: Int = ImageFormat.YUV_420_888
var allowVideoRangePixelFormat: Boolean = false
private val detectionsQueue = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class TextDetection(
    val boundingBox: RectF
)

data class Detections(
    val face: List<Any>,
    val text: List<TextDetection>
)

class DetectionsCompletion(
    val sequenceNumber: Long,
    val sampleBuffer: MediaSample,
    val isFirstAfterAttach: Boolean,
    val isSceneSwitchTransition: Boolean,
    val sceneVideoSourceId: UUID,
    val detectionJobs: List<DetectionJob>
) {
    val detections: MutableMap<UUID, Detections> = mutableMapOf()
}

class VideoUnit : VideoCaptureSessionDelegate, MacScreenCaptureDelegate, VideoEncoderControlDelegate {
    companion object {
        val defaultFrameRate: Double = 30.0
    }

    private val captureSession = VideoCaptureSession()
    private val effectsProcessor: VideoEffectsProcessor
    private val snapshots: VideoSnapshots
    private val lowFpsImage: VideoLowFpsImage
    private val fpsEstimator = VideoFpsEstimator()
    var drawable: PreviewView? = null
    var externalDisplayDrawable: PreviewView? = null
    private val videoPreviews: MutableMap<UUID, PreviewView> = mutableMapOf()
    private var videoPreviewEnabled = false
    private var nextDetectionsSequenceNumber: Long = 0
    private var nextCompletedDetectionsSequenceNumber: Long = 0
    private val completedDetections: MutableMap<Long, DetectionsCompletion> = mutableMapOf()

    var canvasSize: Size
        get() {
            val canvas = effectsProcessor.canvasSize
            return Size(canvas.width.toInt(), canvas.height.toInt())
        }
        set(value) {
            effectsProcessor.canvasSize = SizeF(value.width.toFloat(), value.height.toFloat())
        }

    val encoder = VideoEncoder(lockQueue = processorPipelineQueue)
    var previewEncoder: VideoEncoder? = null
    var processor: Processor? = null
        set(value) {
            field = value
            captureSession.processor = value
            lowFpsImage.processor = value
            fpsEstimator.processor = value
        }

    private var sceneVideoSourceId: UUID = UUID.randomUUID()
    private var selectedBufferedVideoCameraId: UUID? = null
    private val bufferedVideos: MutableMap<UUID, BufferedVideo> = mutableMapOf()
    private val bufferedVideoBuiltins: MutableMap<Any, BufferedVideo> = mutableMapOf()
    private var blackImageBuffer: Image? = null
    private var blackFormatDescription: MediaFormat? = null
    private var blackPixelBufferPool: Any? = null
    private var latestSampleBuffer: MediaSample? = null
    private var sceneSwitchEndRendered = false
    private val frameTimer = SimpleTimer(
        queue = processorPipelineQueue.coroutineContext[CoroutineDispatcher] ?: Dispatchers.Default
    )
    private var firstFrameTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var isFirstAfterAttach = false
    private var ignoreFramesAfterAttachSeconds = 0.0
    private var configuredIgnoreFramesAfterAttachSeconds = 0.0
    private var latestSampleBufferAppendTime: Long = 0L
    private var numberOfDiscardedFrames = 0
    private var cleanRecordings = false
    private var cleanExternalDisplay = false
    private var bufferedPool: Any? = null
    private var bufferedPoolFormatDescriptionExtension: Map<Any?, Any?>? = null
    private var showCameraPreview = false
    private var screenPreviewEnabled = true
    private var externalDisplayPreview = false
    private var pixelTransferSession: Any? = null
    private var outputCounter: Long = -1
    private var startPresentationTimeStamp: Long = 0L
    private var currentAttachParams: VideoUnitAttachParams? = null
    private var macScreenCaptureActive = false

    init {
        val effectsProcessor = VideoEffectsProcessor()
        this.effectsProcessor = effectsProcessor
        snapshots = VideoSnapshots(context = AppDelegate.context)
        lowFpsImage = VideoLowFpsImage(context = AppDelegate.context)
        pixelTransferSession = null
        captureSession.delegate = this
        startFrameTimer()
    }

    fun dispose() {
        stopFrameTimer()
        MacScreenCapture.shared.stop()
    }

    fun startRunning() {
        captureSession.startRunning()
    }

    fun stopRunning() {
        captureSession.stopRunning()
    }

    fun setVideoOrientation(value: Int) {
        captureSession.videoOrientation = value
    }

    fun setTorch(value: Boolean) {
        captureSession.torch = value
    }

    fun setTorchLevel(value: Float) {
        captureSession.torchLevel = value
    }

    fun setFps(fps: Double, preferAutoFps: Boolean) {
        captureSession.setFps(fps = fps, preferAutoFps = preferAutoFps)
        startFrameTimer()
    }

    fun getFps(): Double {
        return captureSession.getFps()
    }

    fun setColorSpace(colorSpace: Int) {
        captureSession.setColorSpace(colorSpace = colorSpace)
    }

    fun setCameraControl(enabled: Boolean) {
        captureSession.setCameraControl(enabled = enabled)
    }

    fun registerEffect(effect: VideoEffect) {
        processorPipelineQueue.launch {
            effectsProcessor.registerEffect(effect)
        }
    }

    fun registerEffectBack(effect: VideoEffect) {
        processorPipelineQueue.launch {
            effectsProcessor.registerEffectBack(effect)
        }
    }

    fun unregisterEffect(effect: VideoEffect) {
        processorPipelineQueue.launch {
            effectsProcessor.unregisterEffect(effect)
        }
    }

    fun unregisterAllEffects() {
        processorPipelineQueue.launch {
            effectsProcessor.unregisterAllEffects()
        }
    }

    fun setPendingAfterAttachEffects(effects: List<VideoEffect>, rotation: Double, mirror: Boolean) {
        processorControlQueue.launch {
            processorPipelineQueue.launch {
                effectsProcessor.setPendingAfterAttachEffects(
                    effects = effects,
                    rotation = rotation,
                    mirror = mirror
                )
            }
        }
    }

    fun usePendingAfterAttachEffects() {
        processorControlQueue.launch {
            processorPipelineQueue.launch {
                effectsProcessor.usePendingAfterAttachEffects()
            }
        }
    }

    fun setScreenPreview(enabled: Boolean) {
        processorControlQueue.launch {
            processorPipelineQueue.launch {
                screenPreviewEnabled = enabled
            }
        }
    }

    fun setShowCameraPreview(show: Boolean) {
        processorControlQueue.launch {
            processorPipelineQueue.launch {
                showCameraPreview = show
            }
        }
    }

    fun setVideoPreviewEnabled(enabled: Boolean) {
        processorControlQueue.launch {
            processorPipelineQueue.launch {
                videoPreviewEnabled = enabled
            }
        }
    }

    fun setVideoPreview(cameraId: UUID, drawable: PreviewView) {
        processorPipelineQueue.launch {
            videoPreviews[cameraId] = drawable
        }
    }

    fun removeAllVideoPreviews() {
        processorPipelineQueue.launch {
            videoPreviews.clear()
        }
    }

    fun setLowFpsImage(fps: Float) {
        processorPipelineQueue.launch {
            lowFpsImage.setFps(fps = fps)
        }
    }

    fun setSceneSwitchTransition(sceneSwitchTransition: SceneSwitchTransition) {
        processorPipelineQueue.launch {
            effectsProcessor.sceneSwitchTransition = sceneSwitchTransition
        }
    }

    fun takeSnapshot(age: Float, onComplete: suspend (Bitmap, Bitmap, Bitmap) -> Unit) {
        processorPipelineQueue.launch {
            snapshots.takeSnapshot(age = age) { a, b, c ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
                    onComplete(a, b, c)
                }
            }
        }
    }

    fun takeVideoSourceSnapshot(videoSourceId: UUID, onComplete: suspend (Bitmap?) -> Unit) {
        processorPipelineQueue.launch {
            val sampleBuffer = bufferedVideos[videoSourceId]?.getLatestSampleBuffer()
            val imageBuffer = sampleBuffer?.imageBuffer
            if (sampleBuffer == null || imageBuffer == null) {
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
                return@launch
            }
            snapshots.takeVideoSourceSnapshot(imageBuffer) { bitmap ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
                    onComplete(bitmap)
                }
            }
        }
    }

    fun takePhoto() {
        processorPipelineQueue.launch {
            captureSession.takePhoto()
        }
    }

    fun setCleanRecordings(enabled: Boolean) {
        processorPipelineQueue.launch {
            cleanRecordings = enabled
        }
    }

    fun setCleanSnapshots(enabled: Boolean) {
        processorPipelineQueue.launch {
            snapshots.setCleanSnapshots(enabled = enabled)
        }
    }

    fun setCleanExternalDisplay(enabled: Boolean) {
        processorPipelineQueue.launch {
            cleanExternalDisplay = enabled
        }
    }

    fun appendBufferedVideoSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        processorPipelineQueue.launch {
            appendBufferedVideoSampleBufferInternal(cameraId = cameraId, sampleBuffer)
        }
    }

    fun addBufferedVideo(cameraId: UUID, name: String, latency: Double) {
        processorPipelineQueue.launch {
            addBufferedVideoInternal(cameraId = cameraId, name = name, latency = latency)
        }
    }

    fun removeBufferedVideo(cameraId: UUID) {
        processorPipelineQueue.launch {
            removeBufferedVideoInternal(cameraId = cameraId)
        }
    }

    fun startEncoding(delegate: VideoEncoderDelegate) {
        encoder.delegate = delegate
        encoder.controlDelegate = this
        encoder.startRunning()
    }

    fun stopEncoding() {
        encoder.stopRunning()
        processor?.delegate?.streamVideoEncoderResolution(resolution = canvasSize)
    }

    fun startPreviewEncoding(delegate: VideoEncoderDelegate, settings: VideoEncoderSettings) {
        val encoder = VideoEncoder(lockQueue = processorPipelineQueue)
        encoder.settings.mutate { it.value = settings }
        encoder.delegate = delegate
        encoder.startRunning()
        processorPipelineQueue.launch {
            previewEncoder = encoder
        }
    }

    fun stopPreviewEncoding() {
        processorPipelineQueue.launch {
            previewEncoder?.stopRunning()
            previewEncoder = null
        }
    }

    fun setSize(capture: Size, canvas: Size) {
        canvasSize = canvas
        captureSession.setCaptureSize(capture)
        processorPipelineQueue.launch {
            effectsProcessor.reset()
            bufferedPool = null
            blackImageBuffer = null
            blackFormatDescription = null
            enqueueBlackToDrawable()
        }
        processor?.delegate?.streamVideoEncoderResolution(resolution = canvasSize)
    }

    fun setGraphicsImplementation(value: SettingsGraphicsImplementation) {
        processorPipelineQueue.launch {
            effectsProcessor.setGraphicsImplementation(value = value)
        }
    }

    fun getCiImage(videoSourceId: UUID, presentationTimeUs: Long): Image? {
        val sampleBuffer = bufferedVideos[videoSourceId]?.getSampleBuffer(presentationTimeUs) ?: return null
        return sampleBuffer.imageBuffer
    }

    fun getMetalPetalImage(videoSourceId: UUID, presentationTimeUs: Long): Image? {
        val sampleBuffer = bufferedVideos[videoSourceId]?.getSampleBuffer(presentationTimeUs) ?: return null
        return sampleBuffer.imageBuffer
    }

    @Throws(Exception::class)
    fun attach(params: VideoUnitAttachParams) {
        if (currentAttachParams?.canQuickSwitchTo(params) == true) {
            attachQuickSwitch(params = params)
        } else {
            attachDefault(params = params)
        }
        currentAttachParams = params
    }

    private fun attachQuickSwitch(params: VideoUnitAttachParams) {
        processorPipelineQueue.launch {
            selectedBufferedVideoCameraId = params.bufferedVideo
            isFirstAfterAttach = true
            showCameraPreview = params.showCameraPreview
            externalDisplayPreview = params.externalDisplayPreview
            effectsProcessor.fillFrame = params.fillFrame
            val bufferedVideo = params.bufferedVideo
            if (bufferedVideo != null) {
                sceneVideoSourceId = bufferedVideo
            } else if (params.devices.hasSceneDevice) {
                val id = params.devices.devices.firstOrNull()?.id
                sceneVideoSourceId = id ?: UUID.randomUUID()
            } else {
                sceneVideoSourceId = UUID.randomUUID()
            }
            effectsProcessor.prepareForAttach()
        }
    }

    @Throws(Exception::class)
    private fun attachDefault(params: VideoUnitAttachParams) {
        updateMacScreenCapture(enabled = params.macScreenCapture)
        captureSession.stopOutputtingSampleBuffers()
        processorPipelineQueue.launch {
            configuredIgnoreFramesAfterAttachSeconds = params.ignoreFramesAfterAttachSeconds
            selectedBufferedVideoCameraId = params.bufferedVideo
            prepareFirstFrame()
            showCameraPreview = params.showCameraPreview
            externalDisplayPreview = params.externalDisplayPreview
            effectsProcessor.fillFrame = params.fillFrame
            val bufferedVideo = params.bufferedVideo
            if (bufferedVideo != null) {
                sceneVideoSourceId = bufferedVideo
            } else if (params.devices.hasSceneDevice) {
                val id = params.devices.devices.firstOrNull()?.id
                sceneVideoSourceId = id ?: UUID.randomUUID()
            } else {
                sceneVideoSourceId = UUID.randomUUID()
            }
            bufferedVideoBuiltins.clear()
            for (device in params.devices.devices) {
                val bufferedVideo = BufferedVideo(
                    cameraId = device.id,
                    name = (device.device as com.moblin.android.platform.avfoundation.AVCaptureDevice).localizedName,
                    update = false,
                    latency = params.builtinDelay,
                    processor = processor,
                    driftTracker = null
                )
                bufferedVideos.put(device.id, bufferedVideo)?.close()
                bufferedVideoBuiltins[device.device] = bufferedVideo
            }
            effectsProcessor.prepareForAttach()
        }
        captureSession.attach(params)
    }

    private fun updateMacScreenCapture(enabled: Boolean) {
        if (enabled && !macScreenCaptureActive) {
            macScreenCaptureActive = true
            MacScreenCapture.shared.delegate = this
            MacScreenCapture.shared.start(fps = captureSession.getFps())
        } else if (!enabled && macScreenCaptureActive) {
            macScreenCaptureActive = false
            MacScreenCapture.shared.stop()
        }
    }

    private fun startFrameTimer() {
        val frameInterval = 1 / captureSession.getFps()
        outputCounter = -1
        startPresentationTimeStamp = 0L
        frameTimer.startPeriodic(interval = frameInterval) {
            handleFrameTimer()
        }
    }

    private fun stopFrameTimer() {
        frameTimer.stop()
    }

    private fun makePresentationTimeStamp(): Long {
        return (outputCounter * 1_000_000 / captureSession.getFps().toInt()) + startPresentationTimeStamp
    }

    private fun handleFrameTimer() {
        outputCounter += 1
        val currentPresentationTimeUs = currentPresentationTimeStamp()
        if (startPresentationTimeStamp == 0L) {
            startPresentationTimeStamp = currentPresentationTimeUs
        }
        var presentationTimeUs = makePresentationTimeStamp()
        val deltaFromCalculatedToClock = presentationTimeUs - currentPresentationTimeUs
        if (abs(deltaFromCalculatedToClock / 1_000_000.0) > deltaLimit) {
            if (deltaFromCalculatedToClock > 0L) {
                Log.i(
                    "VideoUnit",
                    "video-unit: Adjust PTS back in time. Calculated is " +
                        "${presentationTimeUs / 1_000_000.0} " +
                        "and clock is ${currentPresentationTimeUs / 1_000_000.0}"
                )
                outputCounter -= 1
            } else {
                Log.i(
                    "VideoUnit",
                    "video-unit: Adjust PTS forward in time. Calculated is " +
                        "${presentationTimeUs / 1_000_000.0} " +
                        "and clock is ${currentPresentationTimeUs / 1_000_000.0}"
                )
                outputCounter += 1
            }
            presentationTimeUs = makePresentationTimeStamp()
        }
        handleBufferedVideo(presentationTimeUs)
        handleGapFillerTimer()
    }

    private fun handleBufferedVideo(presentationTimeUs: Long) {
        for ((cameraId, bufferedVideo) in bufferedVideos) {
            bufferedVideo.updateSampleBuffer(presentationTimeUs / 1_000_000.0)
            if (videoPreviewEnabled) {
                val sampleBuffer = bufferedVideo.getSampleBuffer(presentationTimeUs)
                if (sampleBuffer != null) {
                    enqueueVideoPreview(cameraId = cameraId, sampleBuffer = sampleBuffer)
                }
            }
        }
        val selectedBufferedVideoCameraId = selectedBufferedVideoCameraId ?: return
        for (bufferedVideoBuiltin in bufferedVideoBuiltins.values.filter { it.latency > 0 }) {
            bufferedVideoBuiltin.updateSampleBuffer(presentationTimeUs / 1_000_000.0, true)
        }
        val sampleBuffer = bufferedVideos[selectedBufferedVideoCameraId]?.getSampleBuffer(presentationTimeUs)
        if (sampleBuffer != null) {
            appendNewSampleBuffer(sampleBuffer = sampleBuffer)
        } else {
            val blackSampleBuffer = makeBlackSampleBuffer(
                durationUs = -1L,
                presentationTimeUs = presentationTimeUs,
                decodeTimeStampUs = -1L
            )
            if (blackSampleBuffer != null) {
                appendNewSampleBuffer(sampleBuffer = blackSampleBuffer)
            } else {
                Log.i("VideoUnit", "video-unit: Failed to output buffered frame")
            }
        }
    }

    private fun handleGapFillerTimer() {
        if (!isFirstAfterAttach) {
            return
        }
        var latestSampleBuffer = this.latestSampleBuffer ?: return
        val latestSampleBufferTime = effectsProcessor.latestSampleBufferTime ?: return
        val delta = (SystemClock.elapsedRealtimeNanos() - latestSampleBufferTime).nanoseconds
        if (delta <= 0.05.seconds) {
            return
        }
        val isSceneSwitchTransition = !effectsProcessor.isAtEndOfSceneSwitchTransition()
        if (!isSceneSwitchTransition && !sceneSwitchEndRendered) {
            latestSampleBuffer = renderSceneSwitchTransitionEnd(sampleBuffer = latestSampleBuffer)
            this.latestSampleBuffer = latestSampleBuffer
            sceneSwitchEndRendered = true
        }
        val timeDeltaUs = delta.inWholeMicroseconds
        val newPresentationTimeUs = latestSampleBuffer.presentationTimeUs + timeDeltaUs
        val sampleBuffer = latestSampleBuffer.replacePresentationTimeStamp(newPresentationTimeUs) ?: return
        appendSampleBuffer(
            sampleBuffer,
            isFirstAfterAttach = false,
            isSceneSwitchTransition = isSceneSwitchTransition
        )
    }

    private fun renderSceneSwitchTransitionEnd(sampleBuffer: MediaSample): MediaSample {
        val imageBuffer = sampleBuffer.imageBuffer ?: return sampleBuffer
        val outputImageBuffer = createBufferedPixelBuffer(sampleBuffer = sampleBuffer) ?: return sampleBuffer
        return effectsProcessor.renderSceneSwitchTransitionEnd(sampleBuffer, imageBuffer, outputImageBuffer)
    }

    private fun prepareFirstFrame() {
        firstFrameTime = null
        isFirstAfterAttach = true
        ignoreFramesAfterAttachSeconds = configuredIgnoreFramesAfterAttachSeconds
    }

    private fun getBufferedBufferPool(sampleBuffer: MediaSample): Any? {
        return com.moblin.android.platform.video.CVPixelBufferPool.matching(bufferedPool, sampleBuffer).also { bufferedPool = it }
    }

    private fun createBufferedPixelBuffer(sampleBuffer: MediaSample): Image? {
        return (getBufferedBufferPool(sampleBuffer) as? com.moblin.android.platform.video.CVPixelBufferPool)?.createPixelBuffer()
    }

    private fun appendBufferedVideoSampleBufferInternal(cameraId: UUID, sampleBuffer: MediaSample) {
        val bufferedVideo = bufferedVideos[cameraId] ?: return
        bufferedVideo.appendSampleBuffer(sampleBuffer)
    }

    private fun addBufferedVideoInternal(cameraId: UUID, name: String, latency: Double) {
        bufferedVideos[cameraId] = BufferedVideo(
            cameraId = cameraId,
            name = name,
            update = true,
            latency = latency,
            processor = processor,
            driftTracker = processor?.driftTracker(cameraId = cameraId, name = name)
        )
    }

    private fun removeBufferedVideoInternal(cameraId: UUID) {
        bufferedVideos.remove(cameraId)?.close()
        processor?.removeDriftTracker(cameraId = cameraId)
    }

    private fun makeBlackSampleBuffer(
        durationUs: Long,
        presentationTimeUs: Long,
        decodeTimeStampUs: Long
    ): MediaSample? {
        val buffer = blackImageBuffer ?: com.moblin.android.platform.video.makeBlackPixelBuffer(canvasSize.width, canvasSize.height)?.also { blackImageBuffer = it; blackFormatDescription = com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer(it) }
        val format = blackFormatDescription
        if (buffer == null || format == null) {
            return null
        }
        return createMediaSample(
            buffer,
            format,
            durationUs,
            presentationTimeUs,
            decodeTimeStampUs
        )
    }

    private fun appendSampleBuffer(
        sampleBuffer: MediaSample,
        isFirstAfterAttach: Boolean,
        isSceneSwitchTransition: Boolean
    ): Boolean {
        val imageBuffer = sampleBuffer.imageBuffer ?: return false
        if (sampleBuffer.presentationTimeUs <= latestSampleBufferAppendTime) {
            numberOfDiscardedFrames += 1
            return false
        }
        if (numberOfDiscardedFrames > 0) {
            Log.i(
                "VideoUnit",
                "video-unit: Discarded $numberOfDiscardedFrames old frames before " +
                    "${sampleBuffer.presentationTimeUs / 1_000_000.0}"
            )
            numberOfDiscardedFrames = 0
        }
        latestSampleBufferAppendTime = sampleBuffer.presentationTimeUs
        val presentationTimeUs = sampleBuffer.presentationTimeUs
        fpsEstimator.update(presentationTimeUs / 1_000_000.0, captureSession.getFps())
        val detectionJobs = prepareDetectionJobs(
            effectsProcessor.needsFaceDetections(presentationTimeUs / 1_000_000.0, sceneVideoSourceId),
            effectsProcessor.needsTextDetections(presentationTimeUs / 1_000_000.0, sceneVideoSourceId),
            sampleBuffer.presentationTimeUs,
            imageBuffer
        )
        val completion = DetectionsCompletion(
            sequenceNumber = nextDetectionsSequenceNumber,
            sampleBuffer = sampleBuffer,
            isFirstAfterAttach = isFirstAfterAttach,
            isSceneSwitchTransition = isSceneSwitchTransition,
            sceneVideoSourceId = sceneVideoSourceId,
            detectionJobs = detectionJobs
        )
        nextDetectionsSequenceNumber += 1
        if (detectionJobs.isNotEmpty()) {
            for (detectionJob in detectionJobs) {
                detectionsQueue.launch {
                    detectObjects(detectionJob = detectionJob, completion = completion)
                }
            }
        } else {
            detectObjectsComplete(completion)
        }
        return true
    }

    private fun detectObjects(detectionJob: DetectionJob, completion: DetectionsCompletion) {
        processorPipelineQueue.launch { completion.detections[detectionJob.videoSourceId] = Detections(face = emptyList(), text = emptyList()); detectObjectsComplete(completion) }
    }

    private fun detectObjectsComplete(completion: DetectionsCompletion) {
        if (completion.detections.count() != completion.detectionJobs.count()) {
            return
        }
        completedDetections[completion.sequenceNumber] = completion
        while (true) {
            val nextCompletion = completedDetections.remove(nextCompletedDetectionsSequenceNumber) ?: break
            appendSampleBufferWithDetections(nextCompletion)
            nextCompletedDetectionsSequenceNumber += 1
        }
    }

    private fun enqueueBlackToDrawable() {
        val sampleBuffer = makeBlackSampleBuffer(
            durationUs = -1L,
            presentationTimeUs = currentPresentationTimeStamp(),
            decodeTimeStampUs = -1L
        ) ?: return
        sampleBuffer.setAttachmentDisplayImmediately()
        drawable?.enqueue(sampleBuffer, isFirstAfterAttach = false)
    }

    private fun appendSampleBufferWithDetections(completion: DetectionsCompletion) {
        val sampleBuffer = completion.sampleBuffer
        val imageBuffer = sampleBuffer.imageBuffer ?: return
        val (modImageBuffer, modSampleBuffer) = effectsProcessor.render(
            imageBuffer,
            completion,
            this,
            captureSession.videoOrientation
        )
        if (cleanRecordings) {
            processor?.recorder?.appendVideo(sampleBuffer)
        } else {
            processor?.recorder?.appendVideo(modSampleBuffer)
        }
        modSampleBuffer.setAttachmentDisplayImmediately()
        val isFirstAfterAttach = completion.isFirstAfterAttach
        com.moblin.android.platform.core.PipelineStats.increment("vuOut")
        if (!showCameraPreview && screenPreviewEnabled) {
            drawable?.enqueue(modSampleBuffer, isFirstAfterAttach = isFirstAfterAttach)
        }
        if (externalDisplayPreview) {
            if (cleanExternalDisplay) {
                externalDisplayDrawable?.enqueue(sampleBuffer, isFirstAfterAttach = isFirstAfterAttach)
            } else {
                externalDisplayDrawable?.enqueue(modSampleBuffer, isFirstAfterAttach = isFirstAfterAttach)
            }
        }
        encoder.encodeImageBuffer(
            modImageBuffer,
            presentationTimeStamp = modSampleBuffer.presentationTimeUs,
            duration = modSampleBuffer.durationUs
        )
        previewEncoder?.encodeImageBuffer(
            modImageBuffer,
            presentationTimeStamp = modSampleBuffer.presentationTimeUs,
            duration = modSampleBuffer.durationUs
        )
        val presentationTimeUs = sampleBuffer.presentationTimeUs
        lowFpsImage.handleImageBuffer(modImageBuffer, presentationTimeUs / 1_000_000.0)
        snapshots.handleTakeSnapshot(
            sampleBuffer,
            modSampleBuffer,
            presentationTimeUs / 1_000_000.0,
            ::makeCopy
        )
    }

    private fun prepareDetectionJobs(
        faceDetectionVideoSourceIds: Set<UUID>,
        textDetectionVideoSourceIds: Set<UUID>,
        presentationTimeUs: Long,
        imageBuffer: Image
    ): List<DetectionJob> {
        val detectionJobs: MutableList<DetectionJob> = mutableListOf()
        for (videoSourceId in faceDetectionVideoSourceIds.union(textDetectionVideoSourceIds)) {
            val videoSourceImageBuffer: Image? = if (videoSourceId == sceneVideoSourceId) {
                imageBuffer
            } else {
                bufferedVideos[videoSourceId]
                    ?.getSampleBuffer(presentationTimeUs)
                    ?.imageBuffer
            }
            if (videoSourceImageBuffer == null) {
                detectionJobs.clear()
                break
            }
            detectionJobs.add(
                DetectionJob(
                    videoSourceId = videoSourceId,
                    imageBuffer = videoSourceImageBuffer,
                    detectFaces = faceDetectionVideoSourceIds.contains(videoSourceId),
                    detectText = textDetectionVideoSourceIds.contains(videoSourceId)
                )
            )
        }
        return detectionJobs
    }

    private fun appendNewSampleBuffer(sampleBuffer: MediaSample) {
        val now = TimeSource.Monotonic.markNow()
        if (firstFrameTime == null) {
            firstFrameTime = now
        }
        val first = firstFrameTime ?: return
        if (first.elapsedNow() <= ignoreFramesAfterAttachSeconds.seconds) {
            return
        }
        latestSampleBuffer = sampleBuffer
        effectsProcessor.latestSampleBufferTime = SystemClock.elapsedRealtimeNanos()
        sceneSwitchEndRendered = false
        if (appendSampleBuffer(
                sampleBuffer,
                isFirstAfterAttach = isFirstAfterAttach,
                isSceneSwitchTransition = false
            )
        ) {
            isFirstAfterAttach = false
        }
    }

    private fun makeCopy(sampleBuffer: MediaSample): MediaSample? {
        val imageBufferCopy = createBufferedPixelBuffer(sampleBuffer = sampleBuffer) ?: return null
        return com.moblin.android.platform.video.makeCopy(sampleBuffer, imageBufferCopy)
    }

    private fun appendBufferedBuiltinVideo(sampleBuffer: MediaSample, device: Any): BufferedVideo? {
        val bufferedVideo = bufferedVideoBuiltins[device] ?: return null
        if (bufferedVideo.latency <= 0) {
            bufferedVideo.setLatestSampleBuffer(sampleBuffer)
            return null
        }
        var sampleBufferCopy: MediaSample = if (bufferedVideo.numberOfBuffers() > 4) {
            makeCopy(sampleBuffer = sampleBuffer) ?: sampleBuffer
        } else {
            sampleBuffer
        }
        val presentationTimeUs = sampleBufferCopy.presentationTimeUs +
            (bufferedVideo.latency * 1_000_000).toLong()
        sampleBufferCopy = sampleBufferCopy.replacePresentationTimeStamp(presentationTimeUs) ?: sampleBufferCopy
        bufferedVideo.appendSampleBuffer(sampleBufferCopy)
        return bufferedVideo
    }

    private fun enqueueVideoPreview(cameraId: UUID, sampleBuffer: MediaSample) {
        val drawable = videoPreviews[cameraId] ?: return
        sampleBuffer.setAttachmentDisplayImmediately()
        drawable.enqueue(sampleBuffer, isFirstAfterAttach = false)
    }

    override fun videoCaptureSessionDidOutput(device: com.moblin.android.platform.avfoundation.AVCaptureDevice, cameraId: UUID?, sampleBuffer: MediaSample) {
        if (videoPreviewEnabled && cameraId != null) {
            enqueueVideoPreview(cameraId = cameraId, sampleBuffer = sampleBuffer)
        }
        if (cameraId == sceneVideoSourceId) {
            var output = sampleBuffer
            val bufferedVideo = appendBufferedBuiltinVideo(sampleBuffer, device)
            if (bufferedVideo != null) {
                for (bufferedVideoBuiltin in bufferedVideoBuiltins.values) {
                    bufferedVideoBuiltin.updateSampleBuffer(output.presentationTimeUs / 1_000_000.0, true)
                }
                output = bufferedVideo.getSampleBuffer(output.presentationTimeUs) ?: output
            }
            if (selectedBufferedVideoCameraId != null) {
                return
            }
            appendNewSampleBuffer(sampleBuffer = output)
        } else {
            appendBufferedBuiltinVideo(sampleBuffer, device)
        }
    }

    override fun videoCaptureSessionWasInterrupted() {
        processorPipelineQueue.launch {
            prepareFirstFrame()
        }
    }

    override fun videoEncoderControlResolutionChanged(encoder: VideoEncoder, resolution: Size) {
        processor?.delegate?.streamVideoEncoderResolution(resolution = resolution)
    }

    override fun macScreenCaptureDidStart(latency: Double) {
        addBufferedVideo(
            cameraId = screenCaptureCameraId,
            name = screenCaptureCameraName,
            latency = latency
        )
    }

    override fun macScreenCaptureDidStop() {
        removeBufferedVideo(cameraId = screenCaptureCameraId)
    }

    override fun macScreenCaptureDidOutputSampleBuffer(sampleBuffer: MediaSample) {
        appendBufferedVideoSampleBufferInternal(cameraId = screenCaptureCameraId, sampleBuffer)
    }
}

fun createBlackImage(width: Double, height: Double): Bitmap {
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.BLACK)
    return bitmap
}

private val MediaSample.imageBuffer: Image?
    get() = null

private val MediaSample.durationUs: Long
    get() = 0L

private val MediaSample.decodeTimeStampUs: Long
    get() = 0L

private fun MediaSample.setAttachmentDisplayImmediately(): Unit =
    Unit
private fun MediaSample.replacePresentationTimeStamp(presentationTimeUs: Long): MediaSample? =
    null
private fun createMediaSample(
    imageBuffer: Image,
    format: MediaFormat?,
    durationUs: Long,
    presentationTimeUs: Long,
    decodeTimeStampUs: Long
): MediaSample? = format?.let { com.moblin.android.common.various.create(imageBuffer, it, durationUs, presentationTimeUs, decodeTimeStampUs) }
