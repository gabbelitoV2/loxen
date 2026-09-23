package com.moblin.android.platform.capture

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.platform.avfoundation.AVCaptureConnection
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDeviceInput
import com.moblin.android.platform.avfoundation.AVCapturePhotoCaptureDelegate
import com.moblin.android.platform.avfoundation.AVCapturePhotoOutput
import com.moblin.android.platform.avfoundation.AVCapturePhotoSettings
import com.moblin.android.platform.avfoundation.AVCaptureSession
import com.moblin.android.platform.avfoundation.AVCaptureSessionErrorKey
import com.moblin.android.platform.avfoundation.AVCaptureSessionInterruptionEnded
import com.moblin.android.platform.avfoundation.AVCaptureSessionInterruptionReasonKey
import com.moblin.android.platform.avfoundation.AVCaptureSessionRuntimeError
import com.moblin.android.platform.avfoundation.AVCaptureSessionWasInterrupted
import com.moblin.android.platform.avfoundation.AVCaptureVideoDataOutput
import com.moblin.android.platform.avfoundation.AVCaptureVideoPreviewLayer
import com.moblin.android.platform.avfoundation.AVError
import com.moblin.android.platform.avfoundation.AVCapturePhoto
import com.moblin.android.platform.core.NotificationCenter
import java.util.IdentityHashMap
import java.util.concurrent.Executor
import kotlinx.coroutines.launch

internal class VideoBinding(
    val input: AVCaptureDeviceInput,
    val entry: CameraCatalog.Entry,
    val dataConnection: AVCaptureConnection?,
    val photoConnection: AVCaptureConnection?,
    val previewConnections: List<AVCaptureConnection>,
) {
    val device: AVCaptureDevice
        get() = input.device

    val previewLayers: List<AVCaptureVideoPreviewLayer> = previewConnections.mapNotNull { it.videoPreviewLayer }
}

internal object Camera2Engine {
    const val TAG = "MoblinCamera"

    private val thread = HandlerThread("camera").apply { start() }
    val handler = Handler(thread.looper)
    val executor = Executor { command -> handler.post(command) }

    private val lock = Any()
    private val streamsBySession = IdentityHashMap<AVCaptureSession, HashMap<String, CameraStream>>()
    private val activeStreams = HashMap<String, CameraStream>()
    private var availabilityRegistered = false

    @Volatile
    var isInBackground = false
        private set

    @Volatile
    var isForegroundServiceRunning = false

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            cameraBecameAvailable(cameraId)
        }
    }

    val manager: CameraManager?
        get() = CameraCatalog.manager

    val mayUseCamera: Boolean
        get() = !isInBackground || isForegroundServiceRunning

    fun hasCameraPermission(): Boolean {
        return try {
            AppDelegate.context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } catch (error: Throwable) {
            false
        }
    }

    fun sync(session: AVCaptureSession) {
        if (!session.isRunning) {
            return
        }
        registerAvailabilityCallback()
        val bindings = makeBindings(session)
        val toRelease = ArrayList<CameraStream>()
        val toStart = ArrayList<CameraStream>()
        synchronized(lock) {
            val streams = streamsBySession.getOrPut(session) { HashMap() }
            val wanted = bindings.associateBy { it.entry.id }
            for ((id, stream) in streams.entries.toList()) {
                if (!wanted.containsKey(id)) {
                    streams.remove(id)
                    if (activeStreams[id] === stream) {
                        activeStreams.remove(id)
                    }
                    toRelease.add(stream)
                }
            }
            for ((id, binding) in wanted) {
                var stream = streams[id]
                if (stream == null) {
                    stream = CameraStream(session, binding)
                    streams[id] = stream
                } else {
                    stream.binding = binding
                }
                val other = activeStreams[id]
                if (other != null && other !== stream) {
                    streamsBySession[other.session]?.remove(id)
                    toRelease.add(other)
                }
                activeStreams[id] = stream
                toStart.add(stream)
            }
        }
        for (stream in toRelease) {
            stream.release()
        }
        for (stream in toStart) {
            if (!mayUseCamera) {
                stream.markInterrupted()
                continue
            }
            if (stream.isConfigured) {
                stream.refresh()
            } else {
                stream.open()
            }
        }
    }

    fun stop(session: AVCaptureSession) {
        val streams = synchronized(lock) {
            val streams = streamsBySession.remove(session)?.values?.toList() ?: emptyList()
            for (stream in streams) {
                if (activeStreams[stream.entry.id] === stream) {
                    activeStreams.remove(stream.entry.id)
                }
            }
            streams
        }
        for (stream in streams) {
            stream.release()
        }
        session.isInterrupted = false
    }

    fun canAddCamera(session: AVCaptureSession, device: AVCaptureDevice): Boolean {
        val entry = device.camera ?: return true
        val others = session.connections
            .mapNotNull { it.deviceInput()?.device?.camera?.id }
            .filter { it != entry.id }
            .toSet()
        if (others.isEmpty()) {
            return true
        }
        val ids = others + entry.id
        val allowed = CameraCatalog.concurrentCameraIds().any { it.containsAll(ids) }
        if (!allowed) {
            Log.i(TAG, "Cameras $ids cannot stream concurrently")
        }
        return allowed
    }

    fun deviceConfigurationChanged(device: AVCaptureDevice, formatChanged: Boolean) {
        for (stream in streams()) {
            if (stream.device === device) {
                stream.refresh()
            }
        }
    }

    fun connectionChanged(connection: AVCaptureConnection) {
        for (stream in streams()) {
            if (stream.binding.dataConnection === connection) {
                stream.refresh()
            }
        }
    }

    fun capturePhoto(output: AVCapturePhotoOutput, settings: AVCapturePhotoSettings, delegate: AVCapturePhotoCaptureDelegate) {
        val stream = streams().firstOrNull { it.binding.photoConnection?.output === output }
        if (stream == null) {
            processorControlQueue.launch {
                delegate.photoOutput(
                    output,
                    AVCapturePhoto(null),
                    AVError(AVError.sessionNotRunning, "Photo output is not running")
                )
            }
            return
        }
        stream.capturePhoto(output, settings, delegate)
    }

    fun cameraPermissionGranted() {
        Log.i(TAG, "Camera permission granted")
        processorControlQueue.launch {
            for (session in runningSessions()) {
                sync(session)
            }
        }
    }

    fun applicationDidEnterBackground() {
        isInBackground = true
        if (isForegroundServiceRunning) {
            return
        }
        for (stream in streams()) {
            stream.markInterrupted()
        }
    }

    fun applicationWillEnterForeground() {
        isInBackground = false
        processorControlQueue.launch {
            for (stream in streams()) {
                if (!stream.session.isRunning || stream.isReleased) {
                    continue
                }
                if (stream.isConfigured) {
                    stream.endInterruption()
                } else {
                    stream.open()
                }
            }
        }
    }

    fun postRuntimeError(session: AVCaptureSession, error: AVError) {
        Log.i(TAG, "Runtime error ${error.code}: ${error.localizedFailureReason}")
        processorControlQueue.launch {
            NotificationCenter.default.post(
                AVCaptureSessionRuntimeError,
                session,
                mapOf<String, Any?>(AVCaptureSessionErrorKey to error)
            )
        }
    }

    fun postInterrupted(session: AVCaptureSession, reason: Int) {
        Log.i(TAG, "Session interrupted, reason $reason")
        session.isInterrupted = true
        processorControlQueue.launch {
            NotificationCenter.default.post(
                AVCaptureSessionWasInterrupted,
                session,
                mapOf<String, Any?>(AVCaptureSessionInterruptionReasonKey to reason)
            )
        }
    }

    fun postInterruptionEnded(session: AVCaptureSession) {
        Log.i(TAG, "Session interruption ended")
        session.isInterrupted = false
        processorControlQueue.launch {
            NotificationCenter.default.post(AVCaptureSessionInterruptionEnded, session)
        }
    }

    private fun cameraBecameAvailable(cameraId: String) {
        if (!mayUseCamera) {
            return
        }
        val stream = synchronized(lock) { activeStreams[cameraId] } ?: return
        if (!stream.isInterrupted || stream.isReleased || !stream.session.isRunning) {
            return
        }
        processorControlQueue.launch {
            if (stream.isInterrupted && !stream.isReleased && stream.session.isRunning && mayUseCamera) {
                stream.open()
            }
        }
    }

    private fun streams(): List<CameraStream> {
        return synchronized(lock) { streamsBySession.values.flatMap { it.values } }
    }

    private fun runningSessions(): List<AVCaptureSession> {
        return synchronized(lock) { streamsBySession.keys.filter { it.isRunning } }
    }

    private fun registerAvailabilityCallback() {
        synchronized(lock) {
            if (availabilityRegistered) {
                return
            }
            availabilityRegistered = true
        }
        try {
            manager?.registerAvailabilityCallback(availabilityCallback, handler)
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to register camera availability callback: $error")
        }
    }

    private fun makeBindings(session: AVCaptureSession): List<VideoBinding> {
        val connections = session.connections
        val bindings = ArrayList<VideoBinding>()
        for (input in session.inputs.filterIsInstance<AVCaptureDeviceInput>()) {
            val entry = input.device.camera ?: continue
            val inputConnections = connections.filter { connection ->
                connection.isEnabled && connection.inputPorts.any { it.input === input }
            }
            val dataConnection = inputConnections.firstOrNull { it.output is AVCaptureVideoDataOutput }
            val photoConnection = inputConnections.firstOrNull { it.output is AVCapturePhotoOutput }
            val previewConnections = inputConnections.filter { it.videoPreviewLayer != null }
            if (dataConnection == null && photoConnection == null && previewConnections.isEmpty()) {
                continue
            }
            bindings.add(VideoBinding(input, entry, dataConnection, photoConnection, previewConnections))
        }
        return bindings
    }
}
