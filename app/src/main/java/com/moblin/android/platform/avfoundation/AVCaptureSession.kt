package com.moblin.android.platform.avfoundation

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.capture.AudioCaptureBridge
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.capture.CameraCatalog
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope

private const val TAG = "MoblinCamera"

open class AVCaptureInput {
    open val ports: List<AVCaptureInputPort> = emptyList()
}

class AVCaptureInputPort(val input: AVCaptureInput, val mediaType: AVMediaType) {
    var isEnabled = true
}

class AVCaptureDeviceInput(val device: AVCaptureDevice) : AVCaptureInput() {
    override val ports: List<AVCaptureInputPort> = listOf(
        AVCaptureInputPort(
            input = this,
            mediaType = if (device.hasMediaType(AVMediaType.audio)) AVMediaType.audio else AVMediaType.video
        )
    )

    init {
        if (!device.isConnected) {
            throw AVError(AVError.deviceNotConnected, "Cannot Open")
        }
    }
}

open class AVCaptureOutput {
    @Volatile
    internal var session: AVCaptureSession? = null

    val connections: List<AVCaptureConnection>
        get() = session?.connectionsFor(this) ?: emptyList()

    fun connection(with: AVMediaType): AVCaptureConnection? {
        return connections.firstOrNull { connection -> connection.inputPorts.any { it.mediaType == with } }
    }
}

interface AVCaptureVideoDataOutputSampleBufferDelegate {
    fun captureOutput(output: AVCaptureOutput, didOutput: MediaSample, from: AVCaptureConnection)
}

class AVCaptureVideoDataOutput : AVCaptureOutput() {
    var videoSettings: Map<String, Any> = emptyMap()

    var alwaysDiscardsLateVideoFrames = true

    @Volatile
    var sampleBufferDelegate: AVCaptureVideoDataOutputSampleBufferDelegate? = null
        private set

    @Volatile
    var sampleBufferCallbackQueue: CoroutineScope? = null
        private set

    fun setSampleBufferDelegate(delegate: AVCaptureVideoDataOutputSampleBufferDelegate?, queue: CoroutineScope?) {
        sampleBufferCallbackQueue = queue
        sampleBufferDelegate = delegate
    }
}

class AVCaptureConnection(val inputPorts: List<AVCaptureInputPort>, val output: AVCaptureOutput?) {
    constructor(inputPort: AVCaptureInputPort, videoPreviewLayer: AVCaptureVideoPreviewLayer) : this(listOf(inputPort), null) {
        this.videoPreviewLayer = videoPreviewLayer
    }

    var videoPreviewLayer: AVCaptureVideoPreviewLayer? = null
        private set

    @Volatile
    internal var session: AVCaptureSession? = null

    var isEnabled = true

    val isActive: Boolean
        get() = session?.isRunning == true

    @Volatile
    var isVideoMirrored = false

    val isVideoMirroringSupported = true

    var automaticallyAdjustsVideoMirroring = false

    @Volatile
    var videoOrientation: Int = AVCaptureVideoOrientation.portrait

    val isVideoOrientationSupported = true

    @Volatile
    var preferredVideoStabilizationMode: Int = AVCaptureVideoStabilizationMode.off
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                Camera2Engine.connectionChanged(this)
            }
        }

    val activeVideoStabilizationMode: Int
        get() = if (isVideoStabilizationSupported) preferredVideoStabilizationMode else AVCaptureVideoStabilizationMode.off

    val isVideoStabilizationSupported: Boolean
        get() = deviceInput()?.device?.camera?.stabilizationSupported == true

    internal fun deviceInput(): AVCaptureDeviceInput? = inputPorts.firstOrNull()?.input as? AVCaptureDeviceInput
}

interface AVCaptureSessionControlsDelegate {
    fun sessionControlsDidBecomeActive(session: AVCaptureSession)

    fun sessionControlsWillEnterFullscreenAppearance(session: AVCaptureSession)

    fun sessionControlsWillExitFullscreenAppearance(session: AVCaptureSession)

    fun sessionControlsDidBecomeInactive(session: AVCaptureSession)
}

open class AVCaptureSession {
    private val lock = Any()
    private val inputList = ArrayList<AVCaptureInput>()
    private val outputList = ArrayList<AVCaptureOutput>()
    private val connectionList = ArrayList<AVCaptureConnection>()
    private var configurationDepth = 0

    val inputs: List<AVCaptureInput>
        get() = synchronized(lock) { inputList.toList() }

    val outputs: List<AVCaptureOutput>
        get() = synchronized(lock) { outputList.toList() }

    val connections: List<AVCaptureConnection>
        get() = synchronized(lock) { connectionList.toList() }

    var automaticallyConfiguresCaptureDeviceForWideColor = true

    var automaticallyConfiguresApplicationAudioSession = true

    var usesApplicationAudioSession = true

    @Volatile
    var isRunning = false
        private set

    @Volatile
    var isInterrupted = false
        internal set

    val supportsControls = false

    val synchronizationClock: CMClock?
        get() = CMClockGetHostTimeClock()

    fun beginConfiguration() {
        synchronized(lock) {
            configurationDepth += 1
        }
    }

    fun commitConfiguration() {
        val apply = synchronized(lock) {
            if (configurationDepth > 0) {
                configurationDepth -= 1
            }
            configurationDepth == 0
        }
        if (apply) {
            applyConfiguration()
        }
    }

    fun canAddInput(input: AVCaptureInput): Boolean {
        val deviceInput = input as? AVCaptureDeviceInput
        val alreadyAdded = synchronized(lock) {
            inputList.any { it === input || (deviceInput != null && (it as? AVCaptureDeviceInput)?.device === deviceInput.device) }
        }
        if (alreadyAdded) {
            return false
        }
        if (deviceInput == null || !deviceInput.device.hasMediaType(AVMediaType.video)) {
            return true
        }
        return Camera2Engine.canAddCamera(this, deviceInput.device)
    }

    fun addInput(input: AVCaptureInput) {
        addInputWithNoConnections(input)
        addImplicitConnections()
    }

    fun addInputWithNoConnections(input: AVCaptureInput) {
        synchronized(lock) {
            if (inputList.none { it === input }) {
                inputList.add(input)
            }
        }
        changed()
    }

    fun removeInput(input: AVCaptureInput) {
        synchronized(lock) {
            inputList.removeAll { it === input }
            val removed = connectionList.filter { connection -> connection.inputPorts.any { it.input === input } }
            connectionList.removeAll { connection -> removed.any { it === connection } }
            for (connection in removed) {
                connection.session = null
            }
        }
        changed()
    }

    fun canAddOutput(output: AVCaptureOutput): Boolean {
        val owner = output.session
        if (owner != null && owner !== this) {
            return false
        }
        return synchronized(lock) { outputList.none { it === output } }
    }

    fun addOutput(output: AVCaptureOutput) {
        addOutputWithNoConnections(output)
        addImplicitConnections()
    }

    fun addOutputWithNoConnections(output: AVCaptureOutput) {
        synchronized(lock) {
            if (outputList.none { it === output }) {
                outputList.add(output)
            }
            output.session = this
        }
        changed()
    }

    fun removeOutput(output: AVCaptureOutput) {
        synchronized(lock) {
            outputList.removeAll { it === output }
            val removed = connectionList.filter { it.output === output }
            connectionList.removeAll { connection -> removed.any { it === connection } }
            for (connection in removed) {
                connection.session = null
            }
            if (output.session === this) {
                output.session = null
            }
        }
        changed()
    }

    fun canAddConnection(connection: AVCaptureConnection): Boolean {
        return synchronized(lock) {
            if (connectionList.any { it === connection }) {
                false
            } else if (connection.inputPorts.isEmpty() ||
                !connection.inputPorts.all { port -> inputList.any { it === port.input } }
            ) {
                false
            } else {
                val output = connection.output
                val layer = connection.videoPreviewLayer
                if (output != null) {
                    outputList.any { it === output } && connectionList.none { it.output === output }
                } else if (layer != null) {
                    connectionList.none { it.videoPreviewLayer === layer }
                } else {
                    false
                }
            }
        }
    }

    fun addConnection(connection: AVCaptureConnection) {
        synchronized(lock) {
            if (connectionList.none { it === connection }) {
                connectionList.add(connection)
            }
            connection.session = this
        }
        changed()
    }

    fun removeConnection(connection: AVCaptureConnection) {
        synchronized(lock) {
            connectionList.removeAll { it === connection }
            if (connection.session === this) {
                connection.session = null
            }
        }
        changed()
    }

    fun startRunning() {
        isRunning = true
        applyConfiguration()
    }

    fun stopRunning() {
        isRunning = false
        Camera2Engine.stop(this)
        AudioCaptureBridge.stop(this)
    }

    internal fun connectionsFor(output: AVCaptureOutput): List<AVCaptureConnection> {
        return synchronized(lock) { connectionList.filter { it.output === output } }
    }

    internal fun removePreviewLayerConnections(layer: AVCaptureVideoPreviewLayer) {
        val removed = synchronized(lock) {
            val matching = connectionList.filter { it.videoPreviewLayer === layer }
            connectionList.removeAll { connection -> matching.any { it === connection } }
            for (connection in matching) {
                connection.session = null
            }
            matching
        }
        if (removed.isNotEmpty()) {
            changed()
        }
    }

    internal fun firstVideoPort(): AVCaptureInputPort? {
        return synchronized(lock) {
            inputList.flatMap { it.ports }.firstOrNull { it.mediaType == AVMediaType.video }
        }
    }

    private fun changed() {
        val apply = synchronized(lock) { configurationDepth == 0 }
        if (apply) {
            applyConfiguration()
        }
    }

    private fun applyConfiguration() {
        try {
            if (isRunning) {
                Camera2Engine.sync(this)
            }
            val (audioInput, audioOutput) = synchronized(lock) {
                val input = inputList
                    .filterIsInstance<AVCaptureDeviceInput>()
                    .firstOrNull { it.device.hasMediaType(AVMediaType.audio) }
                val output = outputList.firstOrNull { it is AVCaptureAudioDataOutput }
                input to output
            }
            AudioCaptureBridge.update(this, audioInput?.device, audioOutput, isRunning)
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to apply capture session configuration", error)
        }
    }

    private fun addImplicitConnections() {
        var added = false
        synchronized(lock) {
            for (output in outputList) {
                if (connectionList.any { it.output === output }) {
                    continue
                }
                val port = inputList.flatMap { it.ports }.firstOrNull { port ->
                    if (port.mediaType == AVMediaType.video) !isAudioOutput(output) else isAudioOutput(output)
                } ?: continue
                val connection = AVCaptureConnection(inputPorts = listOf(port), output = output)
                connection.session = this
                connectionList.add(connection)
                added = true
            }
        }
        if (added) {
            changed()
        }
    }

    private fun isAudioOutput(output: AVCaptureOutput): Boolean {
        return output is AVCaptureAudioDataOutput
    }
}

class AVCaptureMultiCamSession : AVCaptureSession() {
    val isMultitaskingCameraAccessSupported = false

    var isMultitaskingCameraAccessEnabled = false

    val hardwareCost: Float = 0f

    val systemPressureCost: Float = 0f

    companion object {
        val isMultiCamSupported: Boolean
            get() = CameraCatalog.concurrentCameraIds().isNotEmpty()
    }
}

typealias AVCaptureVideoPreviewLayer = com.moblin.android.media.haishinkit.media.video.PreviewView

private val previewLayerSessions = WeakHashMap<AVCaptureVideoPreviewLayer, WeakReference<AVCaptureSession>>()

var AVCaptureVideoPreviewLayer.session: AVCaptureSession?
    get() = synchronized(previewLayerSessions) { previewLayerSessions[this]?.get() }
    set(value) {
        setPreviewLayerSession(this, value, withConnection = true)
    }

fun AVCaptureVideoPreviewLayer.setSessionWithNoConnection(session: AVCaptureSession) {
    setPreviewLayerSession(this, session, withConnection = false)
}

private fun setPreviewLayerSession(
    layer: AVCaptureVideoPreviewLayer,
    session: AVCaptureSession?,
    withConnection: Boolean,
) {
    val old = synchronized(previewLayerSessions) {
        val previous = previewLayerSessions[layer]?.get()
        if (session == null) {
            previewLayerSessions.remove(layer)
        } else {
            previewLayerSessions[layer] = WeakReference(session)
        }
        previous
    }
    if (old != null && old !== session) {
        old.removePreviewLayerConnections(layer)
    }
    if (session == null || !withConnection || old === session) {
        return
    }
    val port = session.firstVideoPort() ?: return
    val connection = AVCaptureConnection(inputPort = port, videoPreviewLayer = layer)
    if (session.canAddConnection(connection)) {
        session.addConnection(connection)
    }
}
