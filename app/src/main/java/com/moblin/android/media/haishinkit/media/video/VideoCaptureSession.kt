package com.moblin.android.media.haishinkit.media.video

import android.hardware.camera2.CaptureRequest
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "VideoCaptureSession"

data class CaptureDevice(
    val device: Any,
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
    fun videoCaptureSessionDidOutput(device: Any, cameraId: UUID?, sampleBuffer: MediaSample)

    fun videoCaptureSessionWasInterrupted()
}

private data class CaptureSessionDevice(
    val device: CaptureDevice,
    val input: Any,
    val output: Any,
    val connection: Any,
    val photoOutput: Any?,
    val photoConnection: Any?,
) {
    fun connections(): List<Any> {
        val photoConnection = photoConnection
        return if (photoConnection != null) {
            listOf(connection, photoConnection)
        } else {
            listOf(connection)
        }
    }
}

private data class VideoFormatSearch(
    val format: Any?,
    val useAutoFrameRate: Boolean,
    val useLandscapeInPortrait: Boolean,
    val error: String?,
)

private fun makeCaptureSession(): Any = Any()

private fun setOrientation(
    device: Any?,
    isLandscapeStreamAndPortraitUi: Boolean,
    connection: Any,
    orientation: Int,
) {
    Unit
}

class VideoCaptureSession {
    var delegate: VideoCaptureSessionDelegate? = null
    var processor: Processor? = null
    val session: Any = makeCaptureSession()
    private var device: Any? = null
    private var devices: MutableList<CaptureSessionDevice> = mutableListOf()
    private var isRunning = false
    private var cameraControlsEnabled = false
    private var captureSize: Size = Size(1920, 1080)
    private var fps: Double = VideoUnit.defaultFrameRate
    private var preferAutoFps = false
    private var colorSpace: Int = MediaFormat.COLOR_STANDARD_BT709
    private var isLandscapeStreamAndPortraitUi = false

    var videoOrientation: Int = Surface.ROTATION_0
        set(value) {
            if (field == value) {
                return
            }
            field = value
            Unit
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
            setTorchMode(
                device,
                if (torch) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
            )
        }

    var torchLevel: Float = 1.0f
        set(value) {
            field = value
            val device = this.device
            if (device == null || !torch) {
                return
            }
            setTorchMode(device, CaptureRequest.FLASH_MODE_TORCH)
        }

    fun startRunning() {
        isRunning = true
        addSessionObservers()
        Unit
    }

    fun stopRunning() {
        isRunning = false
        removeSessionObservers()
        Unit
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
        Unit
    }

    fun stopOutputtingSampleBuffers() {
        Unit
    }

    @Throws(Exception::class)
    fun attach(params: VideoUnitAttachParams) {
        isLandscapeStreamAndPortraitUi = params.isLandscapeStreamAndPortraitUi
        for (device in params.devices.devices) {
            setDeviceFormat(
                device = device.device,
                fps = fps,
                preferAutoFrameRate = preferAutoFps,
                colorSpace = colorSpace,
            )
        }
        configure(params)
        updateDevicesFormat()
    }

    fun takePhoto() {
        Unit
    }

    private fun configure(params: VideoUnitAttachParams) {
        Unit
    }

    private fun attachCameraPreviewLayers(params: VideoUnitAttachParams) {
        Unit
    }

    private fun handleSessionRuntimeError(notification: Any) {
        val message = TODO("no Android counterpart for AVCaptureSessionErrorKey / AVError")
        processor?.delegate?.streamVideoCaptureSessionError(message)
        processorControlQueue.launch {
            delay(500)
            if (isRunning) {
                Unit
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
        Unit
    }

    private fun removeSessionObservers() {
        Unit
    }

    private fun sessionWasInterrupted() {
        Log.d(TAG, "video-unit: Session interruption started")
        delegate?.videoCaptureSessionWasInterrupted()
    }

    private fun sessionInterruptionEnded() {
        Log.d(TAG, "video-unit: Session interruption ended")
    }

    private fun findVideoFormat(
        device: Any,
        width: Int,
        height: Int,
        fps: Double,
        preferAutoFrameRate: Boolean,
        colorSpace: Int,
    ): VideoFormatSearch {
        TODO()
    }

    private fun reportFormatNotFound(device: Any, error: String) {
        Log.i(TAG, "video-unit: $error")
        Unit
    }

    private fun setDeviceFormat(
        device: Any?,
        fps: Double,
        preferAutoFrameRate: Boolean,
        colorSpace: Int,
    ) {
        if (device == null) {
            return
        }
        val result = findVideoFormat(
            device = device,
            width = captureSize.width,
            height = captureSize.height,
            fps = fps,
            preferAutoFrameRate = preferAutoFrameRate,
            colorSpace = colorSpace,
        )
        val error = result.error
        if (error != null) {
            reportFormatNotFound(device, error)
            return
        }
        if (result.format == null) {
            return
        }
        Unit
    }

    private fun attachDevice(device: CaptureDevice, session: Any, attachPhotoShoot: Boolean) {
        Unit
    }

    private fun removeDevices(session: Any) {
        Unit
    }

    private fun removeConnection(session: Any, connection: Any?) {
        Unit
    }

    private fun removeInput(session: Any, input: Any?) {
        Unit
    }

    private fun removeOutput(session: Any, output: Any?) {
        Unit
    }

    private fun setTorchMode(device: Any, torchMode: Int) {
        Unit
    }

    private fun updateCameraControls() {
        Unit
    }

    fun addCameraControls() {
        Unit
    }

    fun removeCameraControls() {
        Unit
    }

    fun captureOutput(output: Any, sampleBuffer: MediaSample, connection: Any) {
        val device: Any = TODO("no Android counterpart for AVCaptureConnection.inputPorts; use ImageAnalysis.Analyzer")
        val cameraId = devices.firstOrNull { it.device.device == device }?.device?.id
        delegate?.videoCaptureSessionDidOutput(device, cameraId, sampleBuffer)
    }

    fun sessionControlsDidBecomeActive(session: Any) {}

    fun sessionControlsWillEnterFullscreenAppearance(session: Any) {}

    fun sessionControlsWillExitFullscreenAppearance(session: Any) {}

    fun sessionControlsDidBecomeInactive(session: Any) {}

    fun photoOutput(photoOutput: Any, photo: Any?, error: Throwable?) {
        if (error != null) {
            Log.i(TAG, "video-unit: Photo error: $error")
            return
        }
        Unit
    }
}
