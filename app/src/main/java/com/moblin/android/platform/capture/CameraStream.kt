package com.moblin.android.platform.capture

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.opengl.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.platform.avfoundation.AVCaptureColorSpace
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCapturePhoto
import com.moblin.android.platform.avfoundation.AVCapturePhotoCaptureDelegate
import com.moblin.android.platform.avfoundation.AVCapturePhotoOutput
import com.moblin.android.platform.avfoundation.AVCapturePhotoSettings
import com.moblin.android.platform.avfoundation.AVCaptureSession
import com.moblin.android.platform.avfoundation.AVCaptureSessionInterruptionReason
import com.moblin.android.platform.avfoundation.AVCaptureVideoDataOutput
import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation
import com.moblin.android.platform.avfoundation.AVError
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.uikit.UIDevice
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.GlRenderer
import com.moblin.android.platform.video.PixelBufferTurn
import com.moblin.android.platform.video.kCVImageBufferColorPrimariesKey
import com.moblin.android.platform.video.kCVImageBufferColorPrimaries_ITU_R_2020
import com.moblin.android.platform.video.kCVImageBufferTransferFunctionKey
import com.moblin.android.platform.video.kCVImageBufferTransferFunction_ITU_R_2100_HLG
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrix_ITU_R_2020
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.layer
import com.moblin.android.platform.video.releaseLease
import com.moblin.android.platform.video.retainLease
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal class CameraStream(val session: AVCaptureSession, initialBinding: VideoBinding) {
    private class PendingPhoto(val output: AVCapturePhotoOutput, val delegate: AVCapturePhotoCaptureDelegate)

    @Volatile
    var binding: VideoBinding = initialBinding

    val entry: CameraCatalog.Entry
        get() = binding.entry

    val device: AVCaptureDevice
        get() = binding.device

    private val id = initialBinding.entry.id
    private val clock = CameraClock(initialBinding.entry.timestampSource)
    private val interrupted = AtomicBoolean(false)

    @Volatile
    var isReleased = false
        private set

    @Volatile
    var isConfigured = false
        private set

    val isInterrupted: Boolean
        get() = interrupted.get()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var configuredSize: Size? = null
    private var configuredColorSpace = AVCaptureColorSpace.sRGB
    private var configuredWithPhoto = false
    private var photoDisabled = false
    private var sessionGeneration = 0
    private var openLatch: CountDownLatch? = null
    private var closeLatch: CountDownLatch? = null
    private val pendingPhotos = ArrayDeque<PendingPhoto>()
    private var lastLoggedConfiguration = ""

    @Volatile
    private var surfaceTexture: SurfaceTexture? = null

    @Volatile
    private var surface: Surface? = null

    @Volatile
    private var oesTexture = 0

    @Volatile
    private var bufferSize = Size(1920, 1080)

    private val stMatrix = FloatArray(16)
    private val adjustedMatrix = FloatArray(16)
    private var pool: CVPixelBufferPool? = null
    private var lastRotation = -1
    private var lastVideoOrientation = -1
    private var lastFrameErrorLogMs = 0L
    private var lastResultErrorLogMs = 0L

    private val frameListener = SurfaceTexture.OnFrameAvailableListener { onFrameAvailable() }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            completedSession: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            if (isReleased) {
                return
            }
            try {
                device.captureCompleted(result)
            } catch (error: Throwable) {
                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastResultErrorLogMs > 5000) {
                    lastResultErrorLogMs = nowMs
                    Log.e(TAG, "Capture result handling failed for camera $id", error)
                }
            }
        }

        override fun onCaptureFailed(
            failedSession: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure,
        ) {
            if (isReleased) {
                return
            }
            val trigger = request.tag as? ControlTrigger ?: return
            Log.i(TAG, "Focus and exposure trigger failed for camera $id, reason ${failure.reason}")
            device.controlTriggerFailed(trigger)
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            guarded("onOpened") {
                if (isReleased) {
                    camera.close()
                    latchDone()
                    return
                }
                cameraDevice = camera
                createCaptureSessionOnCameraThread(camera)
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            guarded("onDisconnected") {
                Log.i(TAG, "Camera $id disconnected")
                forget(camera)
                if (!isReleased) {
                    markInterrupted(AVCaptureSessionInterruptionReason.videoDeviceInUseByAnotherClient)
                }
                latchDone()
            }
        }

        override fun onError(camera: CameraDevice, error: Int) {
            guarded("onError") {
                val name = errorName(error)
                Log.i(TAG, "Camera $id error $name")
                forget(camera)
                if (!isReleased) {
                    if (!Camera2Engine.mayUseCamera) {
                        markInterrupted(AVCaptureSessionInterruptionReason.videoDeviceNotAvailableInBackground)
                    } else if (error == CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ||
                        error == CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE ||
                        error == CameraDevice.StateCallback.ERROR_CAMERA_DISABLED
                    ) {
                        markInterrupted(AVCaptureSessionInterruptionReason.videoDeviceInUseByAnotherClient)
                    } else {
                        Camera2Engine.postRuntimeError(session, AVError(error, name))
                    }
                }
                latchDone()
            }
        }

        override fun onClosed(camera: CameraDevice) {
            closeLatch?.countDown()
        }
    }

    fun open(): Boolean {
        if (isReleased) {
            return false
        }
        if (!Camera2Engine.hasCameraPermission()) {
            Log.i(TAG, "Camera permission not granted, not opening camera $id")
            return false
        }
        if (!Camera2Engine.mayUseCamera) {
            markInterrupted(AVCaptureSessionInterruptionReason.videoDeviceNotAvailableInBackground)
            return false
        }
        if (!ensureSurfaceTexture()) {
            return false
        }
        val latch = CountDownLatch(1)
        Camera2Engine.handler.post {
            guarded("open") {
                openLatch?.countDown()
                openLatch = latch
                openOnCameraThread()
            }
        }
        if (!latch.await(3, TimeUnit.SECONDS)) {
            Log.i(TAG, "Timed out opening camera $id")
        }
        return isConfigured
    }

    fun refresh() {
        Camera2Engine.handler.post {
            guarded("refresh") {
                if (isReleased || captureSession == null) {
                    return@guarded
                }
                val camera = cameraDevice ?: return@guarded
                if (needsReconfigure()) {
                    createCaptureSessionOnCameraThread(camera)
                } else {
                    applyRepeatingRequestOnCameraThread()
                }
            }
        }
    }

    fun markInterrupted(reason: Int = AVCaptureSessionInterruptionReason.videoDeviceNotAvailableInBackground) {
        if (isReleased || !interrupted.compareAndSet(false, true)) {
            return
        }
        Camera2Engine.postInterrupted(session, reason)
    }

    fun endInterruption() {
        if (interrupted.compareAndSet(true, false)) {
            Camera2Engine.postInterruptionEnded(session)
        }
    }

    fun release() {
        if (isReleased) {
            return
        }
        isReleased = true
        val latch = CountDownLatch(1)
        Camera2Engine.handler.post {
            var camera: CameraDevice? = null
            try {
                sessionGeneration += 1
                isConfigured = false
                try {
                    captureSession?.close()
                } catch (error: Throwable) {
                    Log.i(TAG, "Failed to close capture session of camera $id: $error")
                }
                captureSession = null
                imageReader?.close()
                imageReader = null
                failPendingPhotos()
                latchDone()
                camera = cameraDevice
                cameraDevice = null
                if (camera != null) {
                    closeLatch = latch
                    camera.close()
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to release camera $id", error)
                latch.countDown()
            }
            if (camera == null) {
                latch.countDown()
            }
        }
        if (!latch.await(1, TimeUnit.SECONDS)) {
            Log.i(TAG, "Timed out closing camera $id")
        }
        Log.i(TAG, "Released camera $id")
        val surfaceTexture = surfaceTexture
        val surface = surface
        val texture = oesTexture
        this.surfaceTexture = null
        this.surface = null
        oesTexture = 0
        PipelineThread.post {
            surfaceTexture?.setOnFrameAvailableListener(null)
            surfaceTexture?.release()
            surface?.release()
            GlRenderer.deleteTexture(texture)
            pool?.invalidate()
            pool = null
        }
    }

    fun capturePhoto(output: AVCapturePhotoOutput, settings: AVCapturePhotoSettings, delegate: AVCapturePhotoCaptureDelegate) {
        Camera2Engine.handler.post {
            guarded("capturePhoto") {
                val pending = PendingPhoto(output, delegate)
                val reader = imageReader
                val captureSession = captureSession
                val camera = cameraDevice
                val surface = surface
                if (reader == null || captureSession == null || camera == null || surface == null) {
                    deliverPhoto(pending, null, AVError(AVError.sessionNotRunning, "Photo output is not ready"))
                    return@guarded
                }
                pendingPhotos.addLast(pending)
                val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                builder.addTarget(surface)
                builder.addTarget(reader.surface)
                applyControls(builder)
                val orientation = binding.photoConnection?.videoOrientation ?: AVCaptureVideoOrientation.portrait
                builder.set(CaptureRequest.JPEG_ORIENTATION, CameraOrientation.jpegOrientation(orientation, entry))
                builder.set(CaptureRequest.JPEG_QUALITY, 95.toByte())
                Log.i(TAG, "Taking photo with camera $id, requested ${settings.maxPhotoDimensions}")
                captureSession.capture(
                    builder.build(),
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureFailed(
                            failedSession: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure,
                        ) {
                            guarded("onCaptureFailed") {
                                if (pendingPhotos.remove(pending)) {
                                    deliverPhoto(
                                        pending,
                                        null,
                                        AVError(AVError.unknown, "Photo capture failed with reason ${failure.reason}")
                                    )
                                }
                            }
                        }
                    },
                    Camera2Engine.handler
                )
            }
        }
    }

    private fun latchDone() {
        openLatch?.countDown()
        openLatch = null
    }

    private fun forget(camera: CameraDevice) {
        try {
            camera.close()
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to close camera $id: $error")
        }
        if (cameraDevice === camera) {
            cameraDevice = null
            captureSession = null
            isConfigured = false
            sessionGeneration += 1
            imageReader?.close()
            imageReader = null
            failPendingPhotos()
        }
    }

    private fun ensureSurfaceTexture(): Boolean {
        if (surfaceTexture != null) {
            return true
        }
        return try {
            PipelineThread.runSync {
                val texture = GlRenderer.createOesTexture()
                if (texture == 0) {
                    false
                } else {
                    val newSurfaceTexture = SurfaceTexture(texture)
                    newSurfaceTexture.setOnFrameAvailableListener(frameListener, PipelineThread.handler)
                    oesTexture = texture
                    surfaceTexture = newSurfaceTexture
                    surface = Surface(newSurfaceTexture)
                    true
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to create surface texture for camera $id", error)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun openOnCameraThread() {
        if (isReleased) {
            latchDone()
            return
        }
        val camera = cameraDevice
        if (camera != null) {
            createCaptureSessionOnCameraThread(camera)
            return
        }
        val manager = Camera2Engine.manager
        if (manager == null) {
            Log.i(TAG, "No camera manager")
            latchDone()
            return
        }
        val natural = if (UIDevice.current.isNaturalOrientationLandscape) "landscape" else "portrait"
        Log.i(
            TAG,
            "open $id natural=$natural sensorOrientation=${entry.sensorOrientation} timestampSource=${clock.sourceName}"
        )
        try {
            manager.openCamera(id, stateCallback, Camera2Engine.handler)
        } catch (error: SecurityException) {
            Log.i(TAG, "No permission to open camera $id: $error")
            latchDone()
        } catch (error: CameraAccessException) {
            Log.i(TAG, "Failed to open camera $id: $error")
            markInterrupted(AVCaptureSessionInterruptionReason.videoDeviceInUseByAnotherClient)
            latchDone()
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to open camera $id: $error")
            latchDone()
        }
    }

    private fun createCaptureSessionOnCameraThread(camera: CameraDevice) {
        val surfaceTexture = surfaceTexture
        val surface = surface
        if (surfaceTexture == null || surface == null || isReleased) {
            latchDone()
            return
        }
        val dimensions = device.activeFormat.formatDescription.dimensions
        val size = Size(dimensions.width, dimensions.height)
        surfaceTexture.setDefaultBufferSize(size.width, size.height)
        bufferSize = size
        configuredSize = size
        val colorSpace = device.activeColorSpace
        configuredColorSpace = colorSpace
        val photoOutput = binding.photoConnection?.output as? AVCapturePhotoOutput
        configuredWithPhoto = photoOutput != null
        val withPhoto = photoOutput != null && !photoDisabled
        isConfigured = false
        captureSession = null
        sessionGeneration += 1
        val generation = sessionGeneration
        device.controlSessionStarted()
        imageReader?.close()
        imageReader = null
        failPendingPhotos()
        val outputs = ArrayList<OutputConfiguration>()
        val videoOutput = OutputConfiguration(surface)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoOutput.setMirrorMode(OutputConfiguration.MIRROR_MODE_NONE)
            if (colorSpace == AVCaptureColorSpace.HLG_BT2020 && entry.hlgSupported) {
                videoOutput.setDynamicRangeProfile(DynamicRangeProfiles.HLG10)
            }
        }
        outputs.add(videoOutput)
        if (withPhoto && photoOutput != null) {
            val reader = makeImageReader(photoOutput.maxPhotoDimensions)
            imageReader = reader
            outputs.add(OutputConfiguration(reader.surface))
        }
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(configuredSession: CameraCaptureSession) {
                guarded("onConfigured") {
                    if (generation != sessionGeneration || isReleased || cameraDevice !== camera) {
                        configuredSession.close()
                        return
                    }
                    captureSession = configuredSession
                    isConfigured = true
                    lastLoggedConfiguration = ""
                    if (needsReconfigure()) {
                        createCaptureSessionOnCameraThread(camera)
                        return
                    }
                    applyRepeatingRequestOnCameraThread()
                    endInterruption()
                    latchDone()
                }
            }

            override fun onConfigureFailed(failedSession: CameraCaptureSession) {
                guarded("onConfigureFailed") {
                    if (generation != sessionGeneration) {
                        return
                    }
                    Log.i(TAG, "Failed to configure camera $id ${size.width}x${size.height} photo=$withPhoto")
                    if (withPhoto) {
                        photoDisabled = true
                        createCaptureSessionOnCameraThread(camera)
                        return
                    }
                    Camera2Engine.postRuntimeError(session, AVError(AVError.unknown, "Camera configuration failed"))
                    latchDone()
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                camera.createCaptureSession(
                    SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputs, Camera2Engine.executor, callback)
                )
            } else {
                @Suppress("DEPRECATION")
                camera.createCaptureSessionByOutputConfigurations(outputs, callback, Camera2Engine.handler)
            }
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to create capture session for camera $id: $error")
            latchDone()
        }
    }

    private fun needsReconfigure(): Boolean {
        val dimensions = device.activeFormat.formatDescription.dimensions
        val size = configuredSize ?: return true
        return size.width != dimensions.width ||
            size.height != dimensions.height ||
            configuredColorSpace != device.activeColorSpace ||
            configuredWithPhoto != (binding.photoConnection?.output is AVCapturePhotoOutput)
    }

    private fun applyRepeatingRequestOnCameraThread() {
        val captureSession = captureSession ?: return
        val camera = cameraDevice ?: return
        val surface = surface ?: return
        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(surface)
            val fpsRange = applyControls(builder)
            CaptureFrameRate.value = fpsRange.upper.toDouble()
            captureSession.setRepeatingRequest(builder.build(), captureCallback, Camera2Engine.handler)
            submitControlTrigger(captureSession, camera, surface)
            val size = bufferSize
            val description = "${size.width}x${size.height} @${fpsRange.upper} fps range [${fpsRange.lower},${fpsRange.upper}]"
            if (description != lastLoggedConfiguration) {
                lastLoggedConfiguration = description
                Log.i(TAG, "configured $description")
            }
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to set repeating request for camera $id: $error")
        }
    }

    private fun submitControlTrigger(captureSession: CameraCaptureSession, camera: CameraDevice, surface: Surface) {
        val trigger = device.takeControlTrigger() ?: return
        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        builder.addTarget(surface)
        applyControls(builder)
        CameraControls.applyTrigger(CaptureRequestBuilderWriter(builder), trigger)
        builder.setTag(trigger)
        captureSession.capture(builder.build(), captureCallback, Camera2Engine.handler)
    }

    private fun applyControls(builder: CaptureRequest.Builder): Range<Int> {
        val device = device
        val entry = entry
        val fpsRange = targetFpsRange(device, entry)
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        val stabilization = binding.dataConnection?.preferredVideoStabilizationMode ?: 0
        builder.set(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            if (stabilization != 0 && entry.stabilizationSupported) {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            } else {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            }
        )
        val torchOn = device.torchMode == AVCaptureDevice.TorchMode.on && entry.hasFlash
        val lowLightBoost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            device.isLowLightBoostEnabled &&
            !torchOn
        if (torchOn) {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && entry.torchMaxLevel > 1) {
                val level = if (device.torchLevel > 0f) device.torchLevel else 1f
                builder.set(
                    CaptureRequest.FLASH_STRENGTH_LEVEL,
                    (level * entry.torchMaxLevel).roundToInt().coerceIn(1, entry.torchMaxLevel)
                )
            }
        } else {
            builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
        }
        val ratio = device.cameraZoomRatio()
        val usesZoomRatio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && entry.usesZoomRatio
        if (usesZoomRatio) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, ratio)
        } else {
            entry.activeArraySize?.let { area ->
                builder.set(CaptureRequest.SCALER_CROP_REGION, CameraControls.cropRegion(area, ratio))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && entry.rotateAndCropNoneSupported) {
            builder.set(CaptureRequest.SCALER_ROTATE_AND_CROP, CameraMetadata.SCALER_ROTATE_AND_CROP_NONE)
        }
        CameraControls.apply(
            CaptureRequestBuilderWriter(builder),
            device.controlState(),
            entry.controls,
            fpsRange,
            lowLightBoost,
            CameraControls.meteringArea(entry, ratio, usesZoomRatio, configuredSize ?: bufferSize),
        )
        return fpsRange
    }

    private fun targetFpsRange(device: AVCaptureDevice, entry: CameraCatalog.Entry): Range<Int> {
        val formatMaxFrameRate = device.activeFormat.videoSupportedFrameRateRanges.maxOfOrNull { it.maxFrameRate } ?: 30.0
        val ranges = entry.aeFpsRanges.filter { it.upper <= formatMaxFrameRate + 0.5 }.ifEmpty { entry.aeFpsRanges }
        if (ranges.isEmpty()) {
            return Range(30, 30)
        }
        val minFrameDuration = device.activeVideoMinFrameDuration
        val maxFrameDuration = device.activeVideoMaxFrameDuration
        if (device.isAutoVideoFrameRateEnabled || minFrameDuration <= 0) {
            return ranges.maxWithOrNull(compareBy<Range<Int>>({ it.upper }, { -it.lower })) ?: ranges.first()
        }
        val maxFps = (1_000_000.0 / minFrameDuration).roundToInt()
        val minFps = if (maxFrameDuration > 0) (1_000_000.0 / maxFrameDuration).roundToInt() else maxFps
        return ranges.firstOrNull { it.lower == minFps && it.upper == maxFps }
            ?: ranges.filter { it.upper == maxFps }.maxByOrNull { it.lower }
            ?: ranges.filter { it.lower <= maxFps && it.upper >= maxFps }.minByOrNull { it.upper - it.lower }
            ?: ranges.minByOrNull { abs(it.upper - maxFps) }
            ?: ranges.first()
    }

    private fun makeImageReader(requested: Size): ImageReader {
        val sizes = entry.jpegSizes
        val size = sizes.firstOrNull { it.width == requested.width && it.height == requested.height }
            ?: sizes.lastOrNull()
            ?: bufferSize
        val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 2)
        reader.setOnImageAvailableListener({ onPhotoAvailable(it) }, Camera2Engine.handler)
        return reader
    }

    private fun onPhotoAvailable(reader: ImageReader) {
        guarded("onPhotoAvailable") {
            val image = reader.acquireNextImage() ?: return
            val data = try {
                val buffer = image.planes[0].buffer
                ByteArray(buffer.remaining()).also { buffer.get(it) }
            } finally {
                image.close()
            }
            val pending = pendingPhotos.removeFirstOrNull() ?: return
            deliverPhoto(pending, data, null)
        }
    }

    private fun deliverPhoto(pending: PendingPhoto, data: ByteArray?, error: Throwable?) {
        processorControlQueue.launch {
            pending.delegate.photoOutput(pending.output, AVCapturePhoto(data), error)
        }
    }

    private fun failPendingPhotos() {
        while (pendingPhotos.isNotEmpty()) {
            deliverPhoto(pendingPhotos.removeFirst(), null, AVError(AVError.unknown, "Camera closed"))
        }
    }

    private fun onFrameAvailable() {
        try {
            handleFrame()
        } catch (error: Throwable) {
            val nowMs = SystemClock.elapsedRealtime()
            if (nowMs - lastFrameErrorLogMs > 5000) {
                lastFrameErrorLogMs = nowMs
                Log.e(TAG, "Frame handling failed for camera $id", error)
            }
        } finally {
            PixelBufferTurn.end()
        }
    }

    private fun handleFrame() {
        val surfaceTexture = surfaceTexture ?: return
        if (isReleased) {
            return
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(stMatrix)
        PipelineStats.increment("camIn")
        val binding = binding
        val dataConnection = binding.dataConnection
        val output = dataConnection?.output as? AVCaptureVideoDataOutput
        val delegate = output?.sampleBufferDelegate
        val previewLayers = binding.previewLayers
        if (delegate == null && previewLayers.isEmpty()) {
            return
        }
        val connection = dataConnection ?: binding.previewConnections.firstOrNull() ?: return
        val entry = binding.entry
        val videoOrientation = connection.videoOrientation
        val rotation = CameraOrientation.rotationFor(videoOrientation, entry)
        if (rotation != lastRotation || videoOrientation != lastVideoOrientation) {
            lastRotation = rotation
            lastVideoOrientation = videoOrientation
            Log.i(TAG, "videoOrientation=$videoOrientation rotationCw=$rotation camera=$id")
        }
        val size = bufferSize
        val swapped = CameraOrientation.isSwapped(rotation, entry)
        val width = if (swapped) size.height else size.width
        val height = if (swapped) size.width else size.height
        val buffer = pixelBufferPool(width, height, output).createPixelBuffer()
        if (buffer == null) {
            PipelineStats.increment("camDrop")
            return
        }
        val matrix = if (entry.isFront && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Matrix.multiplyMM(adjustedMatrix, 0, stMatrix, 0, horizontalFlip, 0)
            adjustedMatrix
        } else {
            stMatrix
        }
        if (configuredColorSpace == AVCaptureColorSpace.HLG_BT2020 && entry.hlgSupported) {
            buffer.attachments[kCVImageBufferColorPrimariesKey] = kCVImageBufferColorPrimaries_ITU_R_2020
            buffer.attachments[kCVImageBufferTransferFunctionKey] = kCVImageBufferTransferFunction_ITU_R_2100_HLG
            buffer.attachments[kCVImageBufferYCbCrMatrixKey] = kCVImageBufferYCbCrMatrix_ITU_R_2020
        }
        GlRenderer.drawOes(oesTexture, matrix, buffer, rotation, connection.isVideoMirrored)
        for (previewLayer in previewLayers) {
            try {
                previewLayer.layer.drawOnPipeline(buffer)
            } catch (error: Throwable) {
                Log.i(TAG, "Failed to draw camera preview: $error")
            }
        }
        if (delegate == null || output == null || dataConnection == null) {
            return
        }
        val sample = MediaSample(
            emptyData,
            clock.toPresentationTimeUs(surfaceTexture.timestamp),
            true,
            CMVideoFormatDescriptionCreateForImageBuffer(buffer),
            buffer,
        )
        val queue = output.sampleBufferCallbackQueue
        if (queue == null || queue.coroutineContext[ContinuationInterceptor] === PipelineThread.dispatcher) {
            delegate.captureOutput(output, sample, dataConnection)
        } else {
            retainLease(buffer)
            queue.launch {
                try {
                    delegate.captureOutput(output, sample, dataConnection)
                } finally {
                    releaseLease(buffer)
                }
            }
        }
    }

    private fun pixelBufferPool(width: Int, height: Int, output: AVCaptureVideoDataOutput?): CVPixelBufferPool {
        val existing = pool
        if (existing != null && existing.width == width && existing.height == height) {
            return existing
        }
        val pixelFormatType = (output?.videoSettings?.get(kCVPixelBufferPixelFormatTypeKey) as? Number)?.toInt()
            ?: kCVPixelFormatType_32BGRA
        val newPool = CVPixelBufferPool(width, height, pixelFormatType, 12)
        newPool.name = "camera"
        existing?.invalidate()
        pool = newPool
        return newPool
    }

    private inline fun guarded(what: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "Camera $id $what failed", error)
            latchDone()
        }
    }

    private fun errorName(error: Int): String {
        return when (error) {
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "ERROR_CAMERA_IN_USE"
            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "ERROR_MAX_CAMERAS_IN_USE"
            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "ERROR_CAMERA_DISABLED"
            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "ERROR_CAMERA_DEVICE"
            CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "ERROR_CAMERA_SERVICE"
            else -> "ERROR_$error"
        }
    }

    private companion object {
        const val TAG = "MoblinCamera"
        val emptyData = ByteArray(0)
        val horizontalFlip = floatArrayOf(
            -1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            1f, 0f, 0f, 1f,
        )
    }
}

internal object CaptureFrameRate {
    @Volatile
    var value: Double = 30.0
}
