package com.moblin.android.platform.avfoundation

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.NotificationCenter

private const val TAG = "MoblinAudio"

const val AVAudioSessionRouteChangeReasonKey = "AVAudioSessionRouteChangeReasonKey"

class AVAudioSessionPortDescription(
    val uid: String,
    val portName: String,
    val portType: String,
    val dataSources: List<AVAudioSessionDataSourceDescription>?,
    internal val audioDeviceInfo: AudioDeviceInfo? = null,
) {
    val preferredDataSource: AVAudioSessionDataSourceDescription?
        get() = AVAudioSession.sharedInstance().preferredDataSource(this)

    val selectedDataSource: AVAudioSessionDataSourceDescription?
        get() = preferredDataSource ?: dataSources?.firstOrNull()

    fun setPreferredDataSource(dataSource: AVAudioSessionDataSourceDescription?) {
        AVAudioSession.sharedInstance().setPreferredDataSource(this, dataSource)
    }

    override fun equals(other: Any?): Boolean {
        return other is AVAudioSessionPortDescription && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "<port $portType \"$portName\" uid=$uid dataSources=${dataSources?.map { it.dataSourceName }}>"
    }
}

class AVAudioSessionDataSourceDescription(
    val dataSourceID: Int,
    val dataSourceName: String,
    val orientation: String?,
    val supportedPolarPatterns: List<String>?,
    internal val portUid: String = "",
    internal val microphoneDirection: Int = AVAudioSession.MicrophoneDirection.unspecified,
    internal val builtInMicAddress: String? = null,
) {
    val preferredPolarPattern: String?
        get() = AVAudioSession.sharedInstance().polarPattern(portUid, dataSourceID)

    val selectedPolarPattern: String?
        get() = preferredPolarPattern ?: AVAudioSession.PolarPattern.omnidirectional

    fun setPreferredPolarPattern(pattern: String?) {
        AVAudioSession.sharedInstance().setPolarPattern(portUid, dataSourceID, pattern)
    }

    override fun equals(other: Any?): Boolean {
        return other is AVAudioSessionDataSourceDescription &&
            other.portUid == portUid &&
            other.dataSourceID == dataSourceID
    }

    override fun hashCode(): Int {
        return 31 * portUid.hashCode() + dataSourceID
    }

    override fun toString(): String {
        return "<dataSource $dataSourceID \"$dataSourceName\" port=$portUid>"
    }
}

class AVAudioSessionRouteDescription(
    val inputs: List<AVAudioSessionPortDescription>,
    val outputs: List<AVAudioSessionPortDescription>,
)

internal class AudioCaptureSelection(
    val port: AVAudioSessionPortDescription,
    val dataSource: AVAudioSessionDataSourceDescription?,
    val stereo: Boolean,
)

class AVAudioSession private constructor() {
    object Category {
        const val ambient = "AVAudioSessionCategoryAmbient"
        const val soloAmbient = "AVAudioSessionCategorySoloAmbient"
        const val playback = "AVAudioSessionCategoryPlayback"
        const val record = "AVAudioSessionCategoryRecord"
        const val playAndRecord = "AVAudioSessionCategoryPlayAndRecord"
        const val multiRoute = "AVAudioSessionCategoryMultiRoute"
    }

    object Mode {
        const val default = "AVAudioSessionModeDefault"
        const val videoRecording = "AVAudioSessionModeVideoRecording"
        const val measurement = "AVAudioSessionModeMeasurement"
        const val voiceChat = "AVAudioSessionModeVoiceChat"
    }

    object CategoryOptions {
        const val mixWithOthers = 0x1
        const val duckOthers = 0x2
        const val allowBluetoothHFP = 0x4
        const val allowBluetooth = 0x4
        const val defaultToSpeaker = 0x8
        const val interruptSpokenAudioAndMixWithOthers = 0x11
        const val allowBluetoothA2DP = 0x20
        const val allowAirPlay = 0x40
        const val overrideMutedMicrophoneInterruption = 0x80
        const val bluetoothHighQualityRecording = 0x80000
    }

    object Port {
        const val builtInMic = "MicrophoneBuiltIn"
        const val headsetMic = "MicrophoneWired"
        const val usbAudio = "USBAudio"
        const val bluetoothHFP = "BluetoothHFP"
        const val bluetoothLE = "BluetoothLE"
        const val lineIn = "LineIn"
        const val continuityMicrophone = "ContinuityMicrophone"
        const val builtInSpeaker = "Speaker"
        const val builtInReceiver = "Receiver"
        const val headphones = "Headphones"
        const val bluetoothA2DP = "BluetoothA2DPOutput"
        const val HDMI = "HDMIOutput"
        const val lineOut = "LineOut"
        const val carAudio = "CarAudio"
        const val airPlay = "AirPlay"
    }

    object Orientation {
        const val top = "Top"
        const val bottom = "Bottom"
        const val front = "Front"
        const val back = "Back"
        const val left = "Left"
        const val right = "Right"
    }

    object PolarPattern {
        const val omnidirectional = "Omnidirectional"
        const val cardioid = "Cardioid"
        const val subcardioid = "Subcardioid"
        const val stereo = "Stereo"
    }

    object RouteChangeReason {
        const val unknown = 0
        const val newDeviceAvailable = 1
        const val oldDeviceUnavailable = 2
        const val categoryChange = 3
        const val override = 4
        const val wakeFromSleep = 6
        const val noSuitableRouteForCategory = 7
        const val routeConfigurationChange = 8
    }

    object MicrophoneDirection {
        const val unspecified = 0
        const val towardsUser = 1
        const val awayFromUser = 2
        const val external = 3
    }

    private val lock = Any()
    private var preferredInputUid: String? = null
    private val preferredDataSourceIds = HashMap<String, Int>()
    private val polarPatterns = HashMap<String, String>()
    private var newDeviceRouteUid: String? = null
    private var capturePortUid: String? = null
    private var knownInputUids: Set<String>? = null
    private var category = Category.soloAmbient
    private var mode = Mode.default
    private var categoryOptions = 0
    private var preferredSampleRateValue = 0.0
    private var active = false
    private var defaultCaptureDevice: AVCaptureDevice? = null
    private var deviceCallbackRegistered = false
    private val inputGainObservers = mutableListOf<(AVAudioSession) -> Unit>()

    val availableInputs: List<AVAudioSessionPortDescription>?
        get() {
            ensureDeviceCallback()
            return listInputPorts()
        }

    val preferredInput: AVAudioSessionPortDescription?
        get() {
            val uid = synchronized(lock) { preferredInputUid } ?: return null
            return listInputPorts().firstOrNull { it.uid == uid }
        }

    val currentRoute: AVAudioSessionRouteDescription
        get() = AVAudioSessionRouteDescription(
            inputs = listOfNotNull(currentRouteInput(listInputPorts())),
            outputs = listOutputPorts(),
        )

    val inputDataSources: List<AVAudioSessionDataSourceDescription>?
        get() = currentRouteInput(listInputPorts())?.dataSources

    val inputDataSource: AVAudioSessionDataSourceDescription?
        get() = currentRouteInput(listInputPorts())?.preferredDataSource

    val isInputAvailable: Boolean
        get() = listInputPorts().isNotEmpty()

    val isInputGainSettable = false

    val inputGain = 1f

    val sampleRate = 48000.0

    val preferredSampleRate: Double
        get() = synchronized(lock) { preferredSampleRateValue }

    val inputNumberOfChannels: Int
        get() = if (captureSelection()?.stereo == true) 2 else 1

    val categoryName: String
        get() = synchronized(lock) { category }

    val isActive: Boolean
        get() = synchronized(lock) { active }

    fun setPreferredInput(input: AVAudioSessionPortDescription?) {
        synchronized(lock) {
            preferredInputUid = input?.uid
            newDeviceRouteUid = null
        }
        Log.i(TAG, "Preferred input ${input?.uid ?: "none"}")
    }

    fun setInputDataSource(dataSource: AVAudioSessionDataSourceDescription?) {
        synchronized(lock) {
            if (dataSource == null) {
                val portUid = preferredInputUid ?: builtInPortUid
                preferredDataSourceIds.remove(portUid)
            } else {
                preferredDataSourceIds[dataSource.portUid] = dataSource.dataSourceID
            }
        }
        Log.i(TAG, "Input data source ${dataSource?.dataSourceName ?: "none"}")
    }

    fun setInputGain(gain: Float) {
        Log.d(TAG, "Input gain $gain is not settable")
    }

    fun setCategory(category: String, mode: String = "", options: Int = 0) {
        synchronized(lock) {
            this.category = category
            if (mode.isNotEmpty()) {
                this.mode = mode
            }
            categoryOptions = options
        }
        Log.i(TAG, "Category $category mode ${mode.ifEmpty { "-" }} options 0x${Integer.toHexString(options)}")
        handleCategoryChanged()
    }

    fun setMode(mode: String) {
        synchronized(lock) {
            this.mode = mode
        }
    }

    fun setPreferredSampleRate(rate: Double) {
        synchronized(lock) {
            preferredSampleRateValue = rate
        }
    }

    fun setPrefersNoInterruptionsFromSystemAlerts(value: Boolean) {
        Log.d(TAG, "Prefers no interruptions from system alerts $value")
    }

    fun setAllowHapticsAndSystemSoundsDuringRecording(value: Boolean) {
        Log.d(TAG, "Allow haptics and system sounds during recording $value")
    }

    fun setActive(active: Boolean) {
        synchronized(lock) {
            this.active = active
        }
        if (active) {
            ensureDeviceCallback()
        }
        Log.i(TAG, "Session active $active")
    }

    fun observeInputGain(changeHandler: (AVAudioSession) -> Unit): Any {
        synchronized(lock) {
            inputGainObservers.add(changeHandler)
        }
        return changeHandler
    }

    internal fun preferredCaptureDevice(): AVCaptureDevice? {
        synchronized(lock) {
            defaultCaptureDevice?.let {
                return it
            }
        }
        val device = try {
            AVCaptureDevice.makeAudioDevice(defaultAudioDeviceUniqueID, defaultAudioDeviceName, null, null)
        } catch (error: Exception) {
            Log.w(TAG, "Failed to create the default audio device: $error")
            return null
        }
        synchronized(lock) {
            defaultCaptureDevice = defaultCaptureDevice ?: device
            return defaultCaptureDevice
        }
    }

    internal fun preferredDataSource(port: AVAudioSessionPortDescription): AVAudioSessionDataSourceDescription? {
        val dataSourceId = synchronized(lock) { preferredDataSourceIds[port.uid] } ?: return null
        return port.dataSources?.firstOrNull { it.dataSourceID == dataSourceId }
    }

    internal fun setPreferredDataSource(
        port: AVAudioSessionPortDescription,
        dataSource: AVAudioSessionDataSourceDescription?,
    ) {
        synchronized(lock) {
            if (dataSource == null) {
                preferredDataSourceIds.remove(port.uid)
            } else {
                preferredDataSourceIds[port.uid] = dataSource.dataSourceID
            }
        }
    }

    internal fun polarPattern(portUid: String, dataSourceId: Int): String? {
        return synchronized(lock) { polarPatterns["$portUid $dataSourceId"] }
    }

    internal fun setPolarPattern(portUid: String, dataSourceId: Int, pattern: String?) {
        synchronized(lock) {
            if (pattern == null) {
                polarPatterns.remove("$portUid $dataSourceId")
            } else {
                polarPatterns["$portUid $dataSourceId"] = pattern
            }
        }
    }

    internal fun captureSelection(): AudioCaptureSelection? {
        val ports = listInputPorts()
        val preferredUid = synchronized(lock) { preferredInputUid }
        val port = ports.firstOrNull { it.uid == preferredUid }
            ?: ports.firstOrNull { it.uid == builtInPortUid }
            ?: ports.firstOrNull()
            ?: return null
        val dataSource = port.preferredDataSource ?: port.dataSources?.firstOrNull()
        val stereo = if (dataSource != null) {
            dataSource.preferredPolarPattern == PolarPattern.stereo
        } else {
            (port.audioDeviceInfo?.channelCounts?.maxOrNull() ?: 1) >= 2
        }
        return AudioCaptureSelection(port, dataSource, stereo)
    }

    internal fun captureRoutedDeviceChanged(device: AudioDeviceInfo?) {
        val ports = listInputPorts()
        val portUid = device?.let { portUidFor(it) }?.takeIf { uid -> ports.any { it.uid == uid } }
        val changed = synchronized(lock) {
            val previous = capturePortUid
            capturePortUid = portUid
            previous != null && portUid != null && previous != portUid
        }
        Log.i(TAG, "Capture routed to ${portUid ?: "unknown"} (${device?.let { describe(it) } ?: "none"})")
        if (changed) {
            postRouteChange(RouteChangeReason.override)
        }
    }

    internal fun captureStopped() {
        synchronized(lock) {
            capturePortUid = null
        }
    }

    private fun currentRouteInput(ports: List<AVAudioSessionPortDescription>): AVAudioSessionPortDescription? {
        val (newDeviceUid, captureUid, preferredUid) = synchronized(lock) {
            Triple(newDeviceRouteUid, capturePortUid, preferredInputUid)
        }
        return ports.firstOrNull { it.uid == newDeviceUid }
            ?: ports.firstOrNull { it.uid == captureUid }
            ?: ports.firstOrNull { it.uid == preferredUid }
            ?: ports.firstOrNull { it.uid == builtInPortUid }
            ?: ports.firstOrNull()
    }

    private fun listInputPorts(): List<AVAudioSessionPortDescription> {
        val devices = inputDevices()
        val allowsBluetoothInput = synchronized(lock) {
            (categoryOptions and CategoryOptions.allowBluetoothHFP) != 0
        }
        val ports = mutableListOf<AVAudioSessionPortDescription>()
        val builtInMics = devices.filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        if (builtInMics.isNotEmpty()) {
            ports.add(makeBuiltInPort(builtInMics))
        }
        val seenUids = mutableSetOf<String>()
        for (device in devices) {
            if (!allowsBluetoothInput && isBluetoothInput(device)) {
                continue
            }
            val portType = externalPortType(device.type) ?: continue
            val uid = portUidFor(device) ?: continue
            if (!seenUids.add(uid)) {
                continue
            }
            val name = device.productName?.toString()?.trim().orEmpty().ifEmpty { portType }
            ports.add(AVAudioSessionPortDescription(uid, name, portType, null, device))
        }
        return ports
    }

    private fun makeBuiltInPort(builtInMics: List<AudioDeviceInfo>): AVAudioSessionPortDescription {
        val bottomMic = builtInMics.firstOrNull { addressOf(it).equals("bottom", ignoreCase = true) }
            ?: builtInMics.first()
        val backMic = builtInMics.firstOrNull { addressOf(it).equals("back", ignoreCase = true) }
        val stereoSupported = builtInMics.any { device ->
            val counts = device.channelCounts
            counts.isEmpty() || counts.any { it >= 2 }
        } || builtInMics.size >= 2
        val supportedPatterns = if (stereoSupported) {
            listOf(PolarPattern.omnidirectional, PolarPattern.stereo)
        } else {
            listOf(PolarPattern.omnidirectional)
        }
        val dataSources = mutableListOf(
            AVAudioSessionDataSourceDescription(
                builtInBottomDataSourceId,
                "Bottom",
                Orientation.bottom,
                supportedPatterns,
                builtInPortUid,
                MicrophoneDirection.unspecified,
                addressOf(bottomMic),
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dataSources.add(
                AVAudioSessionDataSourceDescription(
                    builtInFrontDataSourceId,
                    "Front",
                    Orientation.front,
                    supportedPatterns,
                    builtInPortUid,
                    MicrophoneDirection.towardsUser,
                    addressOf(bottomMic),
                ),
            )
            dataSources.add(
                AVAudioSessionDataSourceDescription(
                    builtInBackDataSourceId,
                    "Back",
                    Orientation.back,
                    supportedPatterns,
                    builtInPortUid,
                    MicrophoneDirection.awayFromUser,
                    addressOf(backMic ?: bottomMic),
                ),
            )
        }
        return AVAudioSessionPortDescription(
            builtInPortUid,
            builtInPortName,
            Port.builtInMic,
            dataSources,
            bottomMic,
        )
    }

    internal fun builtInMicDevice(address: String?): AudioDeviceInfo? {
        val builtInMics = inputDevices().filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        return builtInMics.firstOrNull { address != null && addressOf(it) == address } ?: builtInMics.firstOrNull()
    }

    private fun listOutputPorts(): List<AVAudioSessionPortDescription> {
        val manager = audioManager() ?: return emptyList()
        val devices = try {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } catch (error: Exception) {
            return emptyList()
        }
        val ports = mutableListOf<AVAudioSessionPortDescription>()
        val seenUids = mutableSetOf<String>()
        for (device in devices) {
            val portType = outputPortType(device.type) ?: continue
            val uid = "${device.type}:${device.productName}:${addressOf(device)}"
            if (!seenUids.add(uid)) {
                continue
            }
            ports.add(AVAudioSessionPortDescription(uid, device.productName?.toString().orEmpty(), portType, null, device))
        }
        return ports
    }

    private fun inputDevices(): List<AudioDeviceInfo> {
        val manager = audioManager() ?: return emptyList()
        return try {
            manager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to list input devices: $error")
            emptyList()
        }
    }

    private fun ensureDeviceCallback() {
        val manager = synchronized(lock) {
            if (deviceCallbackRegistered) {
                return
            }
            val audioManager = audioManager() ?: return
            deviceCallbackRegistered = true
            knownInputUids = currentInputUids()
            audioManager
        }
        try {
            manager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        } catch (error: Exception) {
            Log.w(TAG, "Failed to register audio device callback: $error")
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            handleDevicesChanged(addedDevices?.toList().orEmpty(), emptyList())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            handleDevicesChanged(emptyList(), removedDevices?.toList().orEmpty())
        }
    }

    private fun handleDevicesChanged(added: List<AudioDeviceInfo>, removed: List<AudioDeviceInfo>) {
        val uids = currentInputUids()
        val reason = synchronized(lock) {
            val previous = knownInputUids ?: emptySet()
            knownInputUids = uids
            val newUids = uids - previous
            val goneUids = previous - uids
            if (newUids.isEmpty() && goneUids.isEmpty()) {
                return
            }
            val newExternalUid = newUids.firstOrNull { it != builtInPortUid }
            val routeUid = newDeviceRouteUid
            if (newExternalUid != null) {
                newDeviceRouteUid = newExternalUid
            } else if (routeUid != null && goneUids.contains(routeUid)) {
                newDeviceRouteUid = null
            }
            val captureUid = capturePortUid
            if (captureUid != null && goneUids.contains(captureUid)) {
                capturePortUid = null
            }
            if (newUids.isNotEmpty()) {
                RouteChangeReason.newDeviceAvailable
            } else {
                RouteChangeReason.oldDeviceUnavailable
            }
        }
        Log.i(
            TAG,
            "Inputs changed: added ${added.map { describe(it) }} removed ${removed.map { describe(it) }}",
        )
        postRouteChange(reason)
    }

    private fun handleCategoryChanged() {
        val uids = currentInputUids()
        val changed = synchronized(lock) {
            if (!deviceCallbackRegistered) {
                return
            }
            val previous = knownInputUids
            knownInputUids = uids
            val routeUid = newDeviceRouteUid
            if (routeUid != null && !uids.contains(routeUid)) {
                newDeviceRouteUid = null
            }
            previous != null && previous != uids
        }
        if (changed) {
            Log.i(TAG, "Inputs changed by category options: ${uids.sorted()}")
            postRouteChange(RouteChangeReason.categoryChange)
        }
    }

    private fun currentInputUids(): Set<String> {
        return listInputPorts().map { it.uid }.toSet()
    }

    private fun postRouteChange(reason: Int) {
        try {
            NotificationCenter.default.post(
                routeChangeNotification,
                this,
                mapOf(AVAudioSessionRouteChangeReasonKey to reason),
            )
        } catch (error: Exception) {
            Log.w(TAG, "Route change observer failed: $error")
        }
    }

    companion object {
        const val routeChangeNotification = "AVAudioSession.routeChangeNotification"
        const val interruptionNotification = "AVAudioSession.interruptionNotification"
        internal const val builtInPortUid = "builtin"
        internal const val builtInPortName = "Built-In Microphone"
        internal const val builtInBottomDataSourceId = 0
        internal const val builtInFrontDataSourceId = 1
        internal const val builtInBackDataSourceId = 2
        internal const val defaultAudioDeviceUniqueID = "com.moblin.audio.default"
        internal const val defaultAudioDeviceName = "Microphone"

        private val instance: AVAudioSession by lazy { AVAudioSession() }

        fun sharedInstance(): AVAudioSession {
            return instance
        }

        internal fun audioManager(): AudioManager? {
            return try {
                AppDelegate.context.getSystemService(AudioManager::class.java)
            } catch (error: Throwable) {
                null
            }
        }

        internal fun portUidFor(device: AudioDeviceInfo): String? {
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                return builtInPortUid
            }
            if (externalPortType(device.type) == null) {
                return null
            }
            return "${device.type}:${device.productName}:${addressOf(device)}"
        }

        internal fun addressOf(device: AudioDeviceInfo): String {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return ""
            }
            return device.address ?: ""
        }

        internal fun isBluetoothInput(device: AudioDeviceInfo?): Boolean {
            val type = device?.type ?: return false
            return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

        internal fun describe(device: AudioDeviceInfo): String {
            return "${device.type}:${device.productName}:${addressOf(device)}#${device.id}"
        }

        private fun externalPortType(type: Int): String? {
            return when (type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> Port.headsetMic
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                -> Port.usbAudio
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_LINE_DIGITAL,
                -> Port.lineIn
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Port.bluetoothHFP
                } else {
                    null
                }
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET
                ) {
                    Port.bluetoothLE
                } else {
                    null
                }
            }
        }

        private fun outputPortType(type: Int): String? {
            return when (type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Port.builtInSpeaker
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Port.builtInReceiver
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                -> Port.headphones
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> Port.usbAudio
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> Port.bluetoothA2DP
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Port.bluetoothHFP
                AudioDeviceInfo.TYPE_HDMI -> Port.HDMI
                AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> Port.lineOut
                else -> null
            }
        }
    }
}
