package com.moblin.android.various.model

import android.graphics.Bitmap
import android.media.MediaCodecInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.noValue
import com.moblin.android.common.various.sizeFormatter
import com.moblin.android.common.various.uptimeFormatter
import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsPacket
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.BondingConnection
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamWhipHttpTransport
import com.moblin.android.various.settings.defaultStreamUrl
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.various.storages.ThermalState
import com.moblin.android.various.utils.tryGetToastSubTitle
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

private const val TAG = "ModelStream"

private val mainScope = CoroutineScope(Dispatchers.Main)

private val lowPowerBitrate: UInt = 2_000_000u

val fffffMessage = localized("😢 FFFFF 😢")

val lowBitrateMessage = localized("Low bitrate")

val lowBatteryMessage = localized("Low battery")

class CreateStreamWizard {
    var platform: WizardPlatform = WizardPlatform.custom
    var networkSetup: WizardNetworkSetup = WizardNetworkSetup.direct
    var customProtocol: WizardCustomProtocol = WizardCustomProtocol.none
    val twitchStream = SettingsStream(name = "")
    var twitchAccessToken = ""
    var twitchLoggedIn: Boolean = false
    val kickStream = SettingsStream(name = "")
    var kickAccessToken = ""
    var kickLoggedIn: Boolean = false
    val youTubeStream = SettingsStream(name = "")
    private val _presenting = MutableStateFlow(false)
    var presenting: Boolean
        get() = _presenting.value
        set(value) {
            _presenting.value = value
        }
    private val _presentingSetup = MutableStateFlow(false)
    var presentingSetup: Boolean
        get() = _presentingSetup.value
        set(value) {
            _presentingSetup.value = value
        }
    private val _showTwitchAuth = MutableStateFlow(false)
    var showTwitchAuth: Boolean
        get() = _showTwitchAuth.value
        set(value) {
            _showTwitchAuth.value = value
        }
    private val _showKickAuth = MutableStateFlow(false)
    var showKickAuth: Boolean
        get() = _showKickAuth.value
        set(value) {
            _showKickAuth.value = value
        }
    private val _name = MutableStateFlow("")
    var name: String
        get() = _name.value
        set(value) {
            _name.value = value
        }
    private val _backgroundStreaming = MutableStateFlow(false)
    var backgroundStreaming: Boolean
        get() = _backgroundStreaming.value
        set(value) {
            _backgroundStreaming.value = value
        }
    private val _autoGoLive = MutableStateFlow(false)
    var autoGoLive: Boolean
        get() = _autoGoLive.value
        set(value) {
            _autoGoLive.value = value
        }
    private val _goLiveNotificationMoblinWebsite = MutableStateFlow(false)
    var goLiveNotificationMoblinWebsite: Boolean
        get() = _goLiveNotificationMoblinWebsite.value
        set(value) {
            _goLiveNotificationMoblinWebsite.value = value
        }
    private val _twitchChannelName = MutableStateFlow("")
    var twitchChannelName: String
        get() = _twitchChannelName.value
        set(value) {
            _twitchChannelName.value = value
        }
    private val _twitchChannelId = MutableStateFlow("")
    var twitchChannelId: String
        get() = _twitchChannelId.value
        set(value) {
            _twitchChannelId.value = value
        }
    private val _kickChannelName = MutableStateFlow("")
    var kickChannelName: String
        get() = _kickChannelName.value
        set(value) {
            _kickChannelName.value = value
        }
    var kickChannelId: String? = null
    var kickSlug: String? = null
    var kickChatroomChannelId: String? = null
    private val _youTubeHandle = MutableStateFlow("")
    var youTubeHandle: String
        get() = _youTubeHandle.value
        set(value) {
            _youTubeHandle.value = value
        }
    private val _soopChannelName = MutableStateFlow("")
    var soopChannelName: String
        get() = _soopChannelName.value
        set(value) {
            _soopChannelName.value = value
        }
    private val _soopStreamId = MutableStateFlow("")
    var soopStreamId: String
        get() = _soopStreamId.value
        set(value) {
            _soopStreamId.value = value
        }
    private val _obsAddress = MutableStateFlow("")
    var obsAddress: String
        get() = _obsAddress.value
        set(value) {
            _obsAddress.value = value
        }
    private val _obsPort = MutableStateFlow("")
    var obsPort: String
        get() = _obsPort.value
        set(value) {
            _obsPort.value = value
        }
    private val _obsRemoteControlEnabled = MutableStateFlow(false)
    var obsRemoteControlEnabled: Boolean
        get() = _obsRemoteControlEnabled.value
        set(value) {
            _obsRemoteControlEnabled.value = value
        }
    private val _obsRemoteControlUrl = MutableStateFlow("")
    var obsRemoteControlUrl: String
        get() = _obsRemoteControlUrl.value
        set(value) {
            _obsRemoteControlUrl.value = value
        }
    private val _obsRemoteControlPassword = MutableStateFlow("")
    var obsRemoteControlPassword: String
        get() = _obsRemoteControlPassword.value
        set(value) {
            _obsRemoteControlPassword.value = value
        }
    private val _obsRemoteControlSourceName = MutableStateFlow("")
    var obsRemoteControlSourceName: String
        get() = _obsRemoteControlSourceName.value
        set(value) {
            _obsRemoteControlSourceName.value = value
        }
    private val _obsRemoteControlMainScene = MutableStateFlow("")
    var obsRemoteControlMainScene: String
        get() = _obsRemoteControlMainScene.value
        set(value) {
            _obsRemoteControlMainScene.value = value
        }
    private val _obsRemoteControlBrbScene = MutableStateFlow("")
    var obsRemoteControlBrbScene: String
        get() = _obsRemoteControlBrbScene.value
        set(value) {
            _obsRemoteControlBrbScene.value = value
        }
    private val _directIngest = MutableStateFlow("")
    var directIngest: String
        get() = _directIngest.value
        set(value) {
            _directIngest.value = value
        }
    private val _directStreamKey = MutableStateFlow("")
    var directStreamKey: String
        get() = _directStreamKey.value
        set(value) {
            _directStreamKey.value = value
        }
    private val _belaboxUrl = MutableStateFlow("")
    var belaboxUrl: String
        get() = _belaboxUrl.value
        set(value) {
            _belaboxUrl.value = value
        }
    private val _customSrtUrl = MutableStateFlow("")
    var customSrtUrl: String
        get() = _customSrtUrl.value
        set(value) {
            _customSrtUrl.value = value
        }
    private val _customSrtStreamId = MutableStateFlow("")
    var customSrtStreamId: String
        get() = _customSrtStreamId.value
        set(value) {
            _customSrtStreamId.value = value
        }
    private val _customRtmpUrl = MutableStateFlow("")
    var customRtmpUrl: String
        get() = _customRtmpUrl.value
        set(value) {
            _customRtmpUrl.value = value
        }
    private val _customRtmpStreamKey = MutableStateFlow("")
    var customRtmpStreamKey: String
        get() = _customRtmpStreamKey.value
        set(value) {
            _customRtmpStreamKey.value = value
        }
    private val _customRistUrl = MutableStateFlow("")
    var customRistUrl: String
        get() = _customRistUrl.value
        set(value) {
            _customRistUrl.value = value
        }
    private val _customWhipUrl = MutableStateFlow("")
    var customWhipUrl: String
        get() = _customWhipUrl.value
        set(value) {
            _customWhipUrl.value = value
        }
}

enum class StreamState {
    connecting,
    connected,
    disconnected,
}

fun failedToConnectMessage(name: String): String {
    return localized("😢 Failed to connect to $name 😢")
}

fun Model.startStream(delayed: Boolean = false) {
    Log.i(TAG, "stream: Start")
    if (isChatPhone()) {
        return
    }
    if (streaming) {
        return
    }
    if (delayed && !isLive) {
        return
    }
    if (stream.url == defaultStreamUrl) {
        makeErrorToast(
            title = localized("Please enter your stream URL in stream settings before going live."),
            subTitle = localized("Configure it in Settings → Streams → ${stream.name} → URL."),
        )
        return
    }
    if (database.location.resetWhenGoingLive) {
        resetLocationData()
    }
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.goLive))
    setIsLive(value = true)
    streaming = true
    streamTotalBytes = 0u
    updateScreenAutoOff()
    startNetStream()
    startFetchingYouTubeChatVideoId()
    reloadViewers()
    if (stream.recording.autoStartRecording) {
        startRecording()
    }
    if (stream.obsAutoStartStream) {
        obsStartStream()
    }
    if (stream.obsAutoStartRecording) {
        obsStartRecording()
    }
    val historyStream = StreamingHistoryStream(settings = stream.clone())
    streamingHistoryStream = historyStream
    historyStream.updateHighestThermalState(thermalState = ThermalState.from(statusOther.thermalState))
    historyStream.updateLowestBatteryLevel(level = battery.level)
}

fun Model.stopStream(
    stopObsStreamIfEnabled: Boolean = true,
    stopObsRecordingIfEnabled: Boolean = true,
): Boolean {
    setIsLive(value = false)
    updateScreenAutoOff()
    realtimeIrl?.stop()
    stopFetchingYouTubeChatVideoId()
    if (!streaming) {
        return false
    }
    Log.i(TAG, "stream: Stop")
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.end))
    streamTotalBytes += media.streamTotal().toULong()
    streaming = false
    if (stream.recording.autoStopRecording) {
        stopRecording()
    }
    if (stopObsStreamIfEnabled && stream.obsAutoStopStream) {
        obsStopStream()
    }
    if (stopObsRecordingIfEnabled && stream.obsAutoStopRecording) {
        obsStopRecording()
    }
    stopNetStream()
    makeStreamEndedToast()
    streamState = StreamState.disconnected
    streamingHistoryStream?.let { historyStream ->
        historyStream.stopTime = Instant.now()
        historyStream.totalBytes = streamTotalBytes
        streamingHistory.append(stream = historyStream)
        streamingHistory.store()
    }
    return true
}

fun Model.isGoLiveNotificationConfigured(): Boolean {
    return isGoLiveNotificationDiscordConfigured() || stream.goLiveNotificationMoblinWebsite
}

private fun Model.isGoLiveNotificationDiscordConfigured(): Boolean {
    if (stream.goLiveNotificationDiscordMessage.isEmpty()) {
        return false
    }
    if (stream.goLiveNotificationDiscordWebhookUrl.isEmpty()) {
        return false
    }
    return true
}

fun Model.sendGoLiveNotification(onCompleted: (() -> Unit)? = null) {
    val sendToDiscord = isGoLiveNotificationDiscordConfigured()
    var pending = 1
    val completeOne: () -> Unit = {
        pending -= 1
        if (pending == 0) {
            onCompleted?.invoke()
        }
    }
    if (sendToDiscord) {
        val discordUrl = stream.goLiveNotificationDiscordWebhookUrl
        if (discordUrl.isNotEmpty()) {
            pending += 1
            media.takeSnapshot(age = 0.0) { image, _, _ ->
                val out = ByteArrayOutputStream()
                if (!image.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                    completeOne()
                    return@takeSnapshot
                }
                tryUploadGoLiveNotificationToDiscord(out.toByteArray(), discordUrl, completeOne)
            }
        }
    }
    sendLiveToMoblinWebsite(onCompleted = completeOne)
}

private fun Model.tryUploadGoLiveNotificationToDiscord(
    image: ByteArray,
    url: String,
    onCompleted: () -> Unit,
) {
    uploadImage(
        url = url,
        paramName = "snapshot",
        fileName = "snapshot.jpg",
        image = image,
        message = stream.goLiveNotificationDiscordMessage,
    ) { _ ->
        onCompleted()
    }
}

fun Model.startNetStream() {
    streamState = StreamState.connecting
    latestLowBitrateTime = SystemClock.elapsedRealtime()
    moblink.streamer?.stopTunnels()
    when (stream.getProtocol()) {
        SettingsStreamProtocol.rtmp -> startNetStreamRtmp()
        SettingsStreamProtocol.srt -> startNetStreamSrt()
        SettingsStreamProtocol.rist -> startNetStreamRist()
        SettingsStreamProtocol.whip -> startNetStreamWhip()
        SettingsStreamProtocol.mobcam -> startNetStreamMobcam()
    }
    updateSpeed(now = SystemClock.elapsedRealtime())
    streamBecameBrokenTime = null
}

private fun Model.startNetStreamRtmp() {
    val rtmp = stream.rtmp
    media.rtmpStartStream(
        url = stream.url,
        targetBitrate = getBitrate(),
        adaptiveBitrate = rtmp.adaptiveBitrateEnabled,
    )
    updateAdaptiveBitrateRtmpIfEnabled()
}

private fun Model.startNetStreamSrt() {
    val srt = stream.srt
    payloadSize = srt.mpegtsPacketsPerPacket() * MpegTsPacket.size
    previousSrtDroppedPacketsTotal = 0
    media.srtStartStream(
        isSrtla = stream.isSrtla(),
        url = stream.url,
        reconnectTime = 5.0,
        targetBitrate = getBitrate(),
        adaptiveBitrateAlgorithm = if (srt.adaptiveBitrateEnabled) {
            srt.adaptiveBitrate.algorithm
        } else {
            null
        },
        latency = srt.latency,
        experimental = database.debug.enhancedMoblinSrt,
        overheadBandwidth = srt.overheadBandwidth,
        maximumBandwidthFollowInput = srt.maximumBandwidthFollowInput,
        mpegtsPacketsPerPacket = srt.mpegtsPacketsPerPacket(),
        packetPadding = database.debug.packetPadding,
        networkInterfaceNames = database.networkInterfaceNames,
        connectionPriorities = srt.connectionPriorities,
        dnsLookupStrategy = srt.dnsLookupStrategy,
    )
    updateAdaptiveBitrateSrt(srt = srt)
}

private fun Model.startNetStreamRist() {
    val rist = stream.rist
    media.ristStartStream(
        url = stream.url,
        bonding = rist.bonding,
        targetBitrate = getBitrate(),
        adaptiveBitrate = rist.adaptiveBitrateEnabled,
    )
    updateAdaptiveBitrateRistIfEnabled()
}

private fun Model.startNetStreamWhip() {
    media.whipStartStream(
        url = stream.url,
        headers = stream.whip.headers,
        videoCodec = stream.codec,
        audioCodec = stream.audioCodec,
        videoBitrate = stream.bitrate.toDouble(),
    )
}

private fun Model.startNetStreamMobcam() {
    media.mobcamStartStream(port = stream.mobcamPort(), deviceName = Build.MODEL)
}

fun Model.startPreviewStream() {
    if (isPreviewStreaming) {
        return
    }
    if (stream.previewStream.url.isEmpty()) {
        makeErrorToast(title = localized("Preview stream not configured"))
        return
    }
    media.startPreviewStream(
        url = stream.previewStream.url,
        resolution = stream.previewStream.resolution,
        bitrate = stream.previewStream.bitrate,
    )
    setIsPreviewStreaming(value = true)
}

fun Model.stopPreviewStream() {
    if (!isPreviewStreaming) {
        return
    }
    media.stopPreviewStream()
    setIsPreviewStreaming(value = false)
}

fun Model.togglePreviewStream() {
    if (isPreviewStreaming) {
        stopPreviewStream()
    } else {
        startPreviewStream()
    }
}

fun Model.setIsPreviewStreaming(value: Boolean) {
    isPreviewStreaming = value
    setQuickButton(type = QuickButtonType.previewStream, isOn = value)
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(previewStream = value))
}

fun Model.stopNetStream() {
    moblink.streamer?.stopTunnels()
    reconnectTimer.stop()
    media.rtmpStopStream()
    media.srtStopStream()
    media.ristStopStream()
    media.whipStopStream()
    media.mobcamStopStream()
    streamStartTime = null
    updateStreamUptime(now = SystemClock.elapsedRealtime())
    updateSpeed(now = SystemClock.elapsedRealtime())
    updateAudioLevel()
    bonding.statistics = noValue
}

fun Model.setCurrentStream(stream: SettingsStream) {
    this.stream = stream
    stream.enabled = true
    for (ostream in database.streams) {
        if (ostream.id != stream.id) {
            ostream.enabled = false
        }
    }
    currentStreamId = stream.id
    updateOrientationLock()
    updateStatusStreamText()
    reloadCameraLevel()
}

fun Model.setCurrentStream(streamId: UUID): Boolean {
    val stream = findStream(id = streamId) ?: return false
    setCurrentStream(stream = stream)
    return true
}

fun Model.setCurrentStream() {
    setCurrentStream(stream = database.streams.firstOrNull { it.enabled } ?: fallbackStream)
}

fun Model.findStream(id: UUID): SettingsStream? {
    return database.streams.firstOrNull { stream ->
        stream.id == id
    }
}

fun Model.reloadStream() {
    cameraPosition = null
    stopRecorderIfNeeded(forceStop = true)
    stopStream()
    setNetStream()
    setStreamResolution()
    setStreamFps()
    setColorSpace()
    setStreamCodec()
    setStreamAdaptiveResolution()
    setStreamKeyFrameInterval()
    setStreamBitrate(stream = stream)
    setStreamRateControl(stream = stream)
    setGraphicsImplementation()
    setAudioStreamBitrate(stream = stream)
    setAudioStreamFormat(format = stream.audioCodec.toEncoder())
    setAudioChannelsMap(
        channelsMap = mapOf(
            0 to database.audio.outputToInputChannelsMap.channel1,
            1 to database.audio.outputToInputChannelsMap.channel2,
        ),
    )
    setAudioGain(gainDb = database.audio.gainDb)
    updateMicDelay()
    startRecorderIfNeeded()
    reloadConnections()
    resetChat()
    reloadLocation()
    reloadIngests()
    updateStatusStreamText()
    updateKickChannelInfoIfNeeded()
    updatePictureInPicture()
}

fun Model.reloadStreamIfEnabled(stream: SettingsStream) {
    if (stream.enabled) {
        reloadStream()
        resetSelectedScene(changeScene = false)
        updateOrientation()
    }
}

private fun Model.setNetStream() {
    cameraPreviewView.setDevices(ids = emptyList())
    media.setNetStream(
        proto = stream.getProtocol(),
        portrait = stream.portrait,
        timecodesEnabled = isTimecodesEnabled(),
        builtinAudioDelay = database.debug.builtinAudioAndVideoDelay,
        attachDefaultAudio = !isChatPhone(),
        destinations = stream.multiStreaming.destinations,
        srtImplementation = stream.srt.implementation,
        limitAdaptiveBitrateByTransportBitrate = stream.rateControl != SettingsStreamRateControl.cbr,
    )
    updateTorch()
    updateMute()
    attachStream()
    setLowFpsImage()
    setSceneSwitchTransition()
    setCleanSnapshots()
    setCleanRecordings()
    setCleanExternalDisplay()
    updateCameraControls()
    updateTalkback()
}

private fun Model.attachStream() {
    val processor = media.getProcessor()
    if (processor == null) {
        this.processor = null
        return
    }
    CoroutineScope(processorControlQueue).launch {
        processor.setDrawable(drawable = streamPreviewView)
        processor.setExternalDisplayDrawable(drawable = externalDisplayStreamPreviewView)
        mainScope.launch {
            this@attachStream.processor = processor
        }
        processor.startRunning()
    }
}

fun Model.setStreamResolution() {
    val resolution: SettingsStreamResolution = if (stream.recording.overrideStream) {
        if (stream.recording.resolution > stream.resolution) {
            stream.recording.resolution
        } else {
            stream.resolution
        }
    } else {
        stream.resolution
    }
    val captureSize: Size = when (resolution) {
        SettingsStreamResolution.r4032x3024 -> Size(4032, 3024)
        SettingsStreamResolution.r3840x2160 -> Size(3840, 2160)
        SettingsStreamResolution.r2560x1440 -> Size(3840, 2160)
        SettingsStreamResolution.r1920x1440 -> Size(1920, 1440)
        SettingsStreamResolution.r1920x1080 -> Size(1920, 1080)
        SettingsStreamResolution.r1664x936 -> Size(1920, 1080)
        SettingsStreamResolution.r1024x768 -> Size(1024, 768)
        SettingsStreamResolution.r1280x720 -> Size(1280, 720)
        SettingsStreamResolution.r960x540 -> Size(960, 540)
        SettingsStreamResolution.r854x480 -> Size(960, 540)
        SettingsStreamResolution.r640x360 -> Size(960, 540)
        SettingsStreamResolution.r426x240 -> Size(960, 540)
    }
    media.setVideoSize(
        capture = captureSize,
        canvas = resolution.dimensions(portrait = stream.portrait).toSize(),
        stream = stream.resolution.dimensions(portrait = stream.portrait),
    )
}

private fun Model.setStreamCodec() {
    when (stream.codec) {
        SettingsStreamCodec.h264avc -> {
            when (stream.h264Profile) {
                SettingsStreamH264Profile.baseline -> {
                    media.setVideoProfile(profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                }
                SettingsStreamH264Profile.main -> {
                    media.setVideoProfile(profile = MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
                }
                SettingsStreamH264Profile.high -> {
                    media.setVideoProfile(profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                }
            }
        }
        SettingsStreamCodec.h265hevc -> {
            if (database.color.space == SettingsColorSpace.hlgBt2020) {
                media.setVideoProfile(profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
            } else {
                media.setVideoProfile(profile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain)
            }
        }
    }
    media.setAllowFrameReordering(value = stream.bFrames)
}

private fun Model.setStreamAdaptiveResolution() {
    media.setStreamAdaptiveResolution(
        value = stream.adaptiveEncoderResolution,
        thresholdsFactor = stream.adaptiveEncoderResolutionThreashold,
    )
}

private fun Model.setStreamKeyFrameInterval() {
    media.setStreamKeyFrameInterval(seconds = stream.maxKeyFrameInterval)
}

fun Model.isStreamConfigured(): Boolean {
    return stream != fallbackStream
}

fun Model.isStreamConnected(): Boolean {
    return streamState == StreamState.connected
}

fun Model.isStreaming(): Boolean {
    return streaming
}

fun Model.updateStreamUptime(now: Long) {
    val startTime = streamStartTime
    if (startTime != null && isStreamConnected()) {
        val elapsed = now - startTime
        streamUptime.uptime = uptimeFormatter.format((elapsed / 1000).toDouble())
    } else if (streamUptime.uptime != noValue) {
        streamUptime.uptime = noValue
    }
}

private fun Model.makeYouAreLiveToast() {
    makeToast(title = localized("🎉 You are LIVE at ${stream.name} 🎉"))
}

fun Model.makeStreamEndedToast(subTitle: String? = null, onTapped: (() -> Unit)? = null) {
    makeToast(title = localized("🤟 Stream ended 🤟"), subTitle = subTitle, onTapped = onTapped)
}

fun Model.makeNotLoggedInToToast(platform: Platform) {
    makeErrorToast(
        title = localized("Not logged in to ${platform.name()}"),
        subTitle = localized("Please login again"),
    )
}

private fun Model.makeConnectFailureToast(subTitle: String) {
    makeErrorToast(
        title = failedToConnectMessage(stream.name),
        subTitle = subTitle,
        vibrate = true,
    )
}

private fun Model.makeFffffToast(subTitle: String) {
    makeErrorToast(
        title = fffffMessage,
        font = FontWeight.Bold,
        subTitle = subTitle,
        vibrate = true,
    )
}

private fun Model.onConnected() {
    makeYouAreLiveToast()
    streamStartTime = SystemClock.elapsedRealtime()
    streamState = StreamState.connected
    updateStreamUptime(now = SystemClock.elapsedRealtime())
}

private fun Model.onDisconnected(reason: String) {
    if (!streaming) {
        return
    }
    Log.i(TAG, "stream: Disconnected with reason: $reason")
    val subTitle = localized("Attempting again in 5 seconds.")
    if (streamState == StreamState.connected) {
        streamTotalBytes += media.streamTotal().toULong()
        makeFffffToast(subTitle = subTitle)
    } else if (streamState == StreamState.connecting) {
        makeConnectFailureToast(subTitle = subTitle)
    }
    streamState = StreamState.disconnected
    stopNetStream()
    reconnectTimer.startSingleShot(timeout = 5.0) {
        Log.i(TAG, "stream: Reconnecting")
        startNetStream()
    }
}

private fun Model.onMobcamDisconnected(reason: String) {
    if (!streaming) {
        return
    }
    Log.i(TAG, "stream: Mobcam disconnected with reason: $reason")
    streamState = StreamState.connecting
    streamStartTime = null
    updateStreamUptime(now = SystemClock.elapsedRealtime())
    updateSpeed(now = SystemClock.elapsedRealtime())
}

fun Model.updateBondingStatistics() {
    if (isStreamConnected()) {
        media.srtlaConnectionStatistics()?.let { connections ->
            handleBondingStatistics(connections = connections)
            return
        }
        media.ristBondingStatistics()?.let { connections ->
            handleBondingStatistics(connections = connections)
            return
        }
    }
    if (bonding.statistics != noValue) {
        bonding.statistics = noValue
    }
}

private fun Model.handleBondingStatistics(connections: List<BondingConnection>) {
    bonding.statisticsFormatter.format(connections)?.let { (message, rtts, percentages) ->
        bonding.statistics = message
        bonding.rtts = rtts
        bonding.pieChartPercentages = percentages
    }
}

fun Model.updateSpeed(now: Long) {
    if (isLive) {
        val speed = media.getVideoStreamBitrate(bitrate = stream.bitrate).toLong()
        checkLowBitrate(speed = speed, now = now)
        streamingHistoryStream?.updateBitrate(bitrate = speed)
        val speedMbpsOneDecimal = String.format(Locale.US, "%.1f", speed.toDouble() / 1_000_000)
        if (speedMbpsOneDecimal != bitrate.speedMbpsOneDecimal) {
            bitrate.speedMbpsOneDecimal = speedMbpsOneDecimal
        }
        val speedString = formatBytesPerSecond(speed = speed)
        val total = sizeFormatter.format(media.streamTotal())
        val numberOfDestinations = media.getNumberOfDestinations()
        val speedAndTotal = if (numberOfDestinations == 1) {
            localized("$speedString ($total)")
        } else {
            localized("$speedString x$numberOfDestinations ($total)")
        }
        if (speedAndTotal != bitrate.speedAndTotal) {
            bitrate.speedAndTotal = speedAndTotal
        }
        val bitrateStatusIconColor: Color? = if (speed < stream.bitrate.toLong() / 5) {
            Color.Red
        } else if (speed < stream.bitrate.toLong() / 2) {
            Color(0xFFFFA500)
        } else {
            null
        }
        if (bitrateStatusIconColor != bitrate.statusIconColor) {
            bitrate.statusIconColor = bitrateStatusIconColor
        }
        if (isWatchLocal()) {
            sendSpeedAndTotalToWatch(speedAndTotal = bitrate.speedAndTotal)
        }
    } else if (bitrate.speedAndTotal != noValue) {
        bitrate.speedMbpsOneDecimal = noValue
        bitrate.speedAndTotal = noValue
        if (isWatchLocal()) {
            sendSpeedAndTotalToWatch(speedAndTotal = bitrate.speedAndTotal)
        }
    }
}

private fun Model.updateCameraControls() {
    media.setCameraControls(enabled = database.cameraControlsEnabled)
}

fun Model.setCameraControlsEnabled() {
    cameraControlEnabled = database.cameraControlsEnabled
    media.setCameraControls(enabled = database.cameraControlsEnabled)
}

fun Model.updateSrtlaPriorities() {
    media.setConnectionPriorities(connectionPriorities = stream.srt.connectionPriorities.clone())
}

private fun Model.checkLowBitrate(speed: Long, now: Long) {
    if (!database.lowBitrateWarning) {
        return
    }
    if (streamState != StreamState.connected) {
        return
    }
    if (speed < 500_000 && now > latestLowBitrateTime + 15_000) {
        makeWarningToast(title = lowBitrateMessage, vibrate = true)
        latestLowBitrateTime = now
    }
}

private fun Model.handleLowFpsImage(image: ByteArray, frameNumber: ULong) {
    if (frameNumber % lowFpsImageFps.toULong() == 0UL) {
        if (isWatchLocal()) {
            sendPreviewToWatch(image = image)
        }
    }
    sendPreviewToRemoteControlAssistant(preview = image)
    sendPreviewToRemoteControlWeb(preview = image)
}

private fun Model.handleEncoderResolutionChanged(resolution: Size) {
    val dimension = minOf(resolution.width, resolution.height)
    if (dimension == 2160) {
        currentResolution = "4K"
    } else {
        currentResolution = "${dimension}p"
    }
    updateStatusStreamText()
}

private fun Model.handleBufferedVideoReady(cameraId: UUID) {
    activeBufferedVideoIds.add(cameraId)
    var isNetwork = false
    if (getRtmpStream(id = cameraId) != null) {
        isNetwork = true
    } else if (getSrtlaStream(id = cameraId) != null) {
        isNetwork = true
    } else {
        val ristStream = getRistStream(id = cameraId)
        if (ristStream != null) {
            isNetwork = true
            ristStream.connected = true
        }
    }
    if (isNetwork) {
        markMicAsConnected(id = "$cameraId 0")
        switchMicIfNeededAfterNetworkCameraChange()
    }
    updateDisconnectProtectionVideoSourceConnected()
    updateVideoPreviews()
}

private fun Model.handleBufferedVideoRemoved(cameraId: UUID) {
    activeBufferedVideoIds.remove(cameraId)
    var isNetwork = false
    if (getRtmpStream(id = cameraId) != null) {
        isNetwork = true
    } else if (getSrtlaStream(id = cameraId) != null) {
        isNetwork = true
    } else {
        val ristStream = getRistStream(id = cameraId)
        if (ristStream != null) {
            isNetwork = true
            ristStream.connected = false
        }
    }
    if (isNetwork) {
        markMicAsDisconnected(id = "$cameraId 0")
        switchMicIfNeededAfterNetworkCameraChange()
        if (isCurrentScenesVideoSourceNetwork(cameraId = cameraId)) {
            updateAutoSceneSwitcherVideoSourceDisconnected()
        }
    }
    updateDisconnectProtectionVideoSourceDisconnected()
    updateVideoPreviews()
}

private fun Model.handleNoTorch() {
    if (!streamOverlay.isFrontCameraSelected) {
        makeErrorToast(
            title = localized("Torch unavailable in this scene."),
            subTitle = localized("Normally only available for built-in cameras."),
        )
    }
}

fun Model.startStreamIfAutoGoLive() {
    if (!stream.autoGoLive || stream.getProtocol() != SettingsStreamProtocol.mobcam || isLive) {
        return
    }
    startStream()
}

fun Model.toggleStream() {
    if (isLive) {
        stopStream()
    } else {
        startStream()
    }
}

fun Model.setIsLive(value: Boolean) {
    isLive = value
    updateLiveActivity()
    updateMacStatusItem()
    updatePictureInPicture()
    if (isWatchLocal()) {
        sendIsLiveToWatch(isLive = isLive)
    }
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(streaming = isLive))
}

fun Model.setStreamFps(fps: Int? = null) {
    if (isChatPhone()) {
        media.setFps(fps = 1, preferAutoFps = false)
    } else {
        media.setFps(fps = fps ?: stream.fps, preferAutoFps = stream.lowLightBoost)
    }
}

fun Model.setStreamBitrate(stream: SettingsStream) {
    media.setVideoStreamBitrate(bitrate = stream.bitrate)
    updateStatusStreamText()
}

fun Model.setStreamRateControl(stream: SettingsStream) {
    media.setVideoStreamRateControl(rateControl = stream.rateControl)
}

fun Model.setGraphicsImplementation() {
    media.setGraphicsImplementation(database.graphicsImplementation)
}

fun Model.getBitratePresetByBitrate(bitrate: UInt): SettingsBitratePreset? {
    return database.bitratePresets.firstOrNull { it.bitrate == bitrate }
}

fun Model.setBitrate(bitrate: UInt) {
    if (bitrate != stream.bitrate) {
        stream.bitrate = bitrate
    }
    if (stream.enabled) {
        setStreamBitrate(stream = stream)
    }
    val preset = getBitratePresetByBitrate(bitrate = bitrate) ?: return
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(bitrate = preset.id))
}

private fun Model.getBitrate(): UInt {
    return if (statusTopRight.isLowPowerMode) {
        lowPowerBitrate
    } else {
        stream.bitrate
    }
}

fun Model.setAudioStreamBitrate(stream: SettingsStream) {
    media.setAudioStreamBitrate(bitrate = stream.audioBitrate)
    updateStatusStreamText()
}

fun Model.setAudioStreamFormat(format: AudioEncoderSettings.Format) {
    media.setAudioStreamFormat(format = format)
    updateStatusStreamText()
}

fun Model.setAudioChannelsMap(channelsMap: Map<Int, Int>) {
    media.setAudioChannelsMap(channelsMap = channelsMap)
}

fun Model.setAudioGain(gainDb: Float) {
    media.setAudioGain(gain = 10.0.pow(gainDb / 20.0).toFloat())
}

fun Model.isShowingStatusStream(): Boolean {
    return database.show.stream && isStreamConfigured() && !isChatPhone()
}

fun Model.updateBitrateStatus() {
    try {
        val newBitrateStatusColor: Color = if (media.srtDroppedPacketsTotal >
            previousBitrateStatusColorSrtDroppedPacketsTotal
        ) {
            Color.Red
        } else if (numberOfFailedEncodings > previousBitrateStatusNumberOfFailedEncodings) {
            Color.Red
        } else {
            Color.White
        }
        if (newBitrateStatusColor != bitrate.statusColor) {
            bitrate.statusColor = newBitrateStatusColor
        }
    } finally {
        previousBitrateStatusColorSrtDroppedPacketsTotal = media.srtDroppedPacketsTotal
        previousBitrateStatusNumberOfFailedEncodings = numberOfFailedEncodings
    }
}

fun Model.updateAdaptiveBitrate() {
    if (!streaming) {
        return
    }
    val result = media.updateAdaptiveBitrate(
        overlay = database.debug.debugOverlay,
        relaxed = relaxedBitrate,
    )
    result?.let { (lines, actions) ->
        latestDebugLines = lines
        latestDebugActions = actions
    }
}

fun Model.updateDebugOverlay() {
    if (database.debug.debugOverlay) {
        debugOverlay.debugLines = latestDebugLines + latestDebugActions
        if (Log.isLoggable(TAG, Log.DEBUG) && isLive) {
            Log.d(TAG, latestDebugLines.joinToString(separator = ", "))
        }
    } else if (debugOverlay.debugLines.isNotEmpty()) {
        debugOverlay.debugLines = emptyList()
    }
}

fun Model.setPixelFormat() {
    for ((format, type) in pixelFormats.zip(pixelFormatTypes)) {
        if (database.debug.pixelFormat == format) {
            Log.i(TAG, "Setting pixel format $format")
            pixelFormatType = type
        }
    }
}

fun Model.mediaOnSrtConnected() {
    mainScope.launch {
        onConnected()
    }
}

fun Model.mediaOnSrtDisconnected(reason: String) {
    mainScope.launch {
        onDisconnected(reason = reason)
    }
}

fun Model.mediaOnRtmpConnected() {
    mainScope.launch {
        onConnected()
    }
}

fun Model.mediaOnRtmpDisconnected(message: String) {
    mainScope.launch {
        onDisconnected(reason = "RTMP disconnected with message $message")
    }
}

fun Model.mediaOnRtmpDestinationConnected(destination: String) {
    mainScope.launch {
        makeToast(title = localized("🎉 You are LIVE at multi stream $destination 🎉"))
    }
}

fun Model.mediaOnRtmpDestinationDisconnected(destination: String) {
    mainScope.launch {
        makeErrorToast(
            title = localized("😢 Multi stream $destination failed 😢"),
            subTitle = localized("Attempting again in 5 seconds."),
        )
    }
}

fun Model.mediaOnRistConnected() {
    mainScope.launch {
        onConnected()
    }
}

fun Model.mediaOnRistDisconnected() {
    mainScope.launch {
        onDisconnected(reason = "RIST disconnected")
    }
}

fun Model.mediaOnWhipConnected() {
    mainScope.launch {
        onConnected()
    }
}

fun Model.mediaOnWhipDisconnected(reason: String) {
    mainScope.launch {
        onDisconnected(reason = reason)
    }
}

fun Model.mediaOnMobcamConnected() {
    mainScope.launch {
        onConnected()
    }
}

fun Model.mediaOnMobcamDisconnected(reason: String) {
    mainScope.launch {
        onMobcamDisconnected(reason = reason)
    }
}

fun Model.mediaOnAudioBuffer(sampleBuffer: MediaSample) {
    mainScope.launch {
        speechToText?.append(sampleBuffer = sampleBuffer)
    }
}

fun Model.mediaOnLowFpsImage(lowFpsImage: ByteArray?, frameNumber: ULong) {
    val image = lowFpsImage ?: return
    mainScope.launch {
        handleLowFpsImage(image = image, frameNumber = frameNumber)
    }
}

fun Model.mediaOnAttachCameraError() {
    makeErrorToastMain(
        title = localized("Camera capture setup error"),
        subTitle = videoCaptureError(),
    )
}

fun Model.mediaOnCaptureSessionError(message: String) {
    makeErrorToastMain(title = message, subTitle = videoCaptureError())
}

fun Model.mediaOnBufferedVideoReady(cameraId: UUID) {
    mainScope.launch {
        handleBufferedVideoReady(cameraId = cameraId)
    }
}

fun Model.mediaOnBufferedVideoRemoved(cameraId: UUID) {
    mainScope.launch {
        handleBufferedVideoRemoved(cameraId = cameraId)
    }
}

fun Model.mediaOnEncoderResolutionChanged(resolution: Size) {
    mainScope.launch {
        handleEncoderResolutionChanged(resolution = resolution)
    }
}

fun Model.mediaOnRecorderInitSegment(data: ByteArray) {
    mainScope.launch {
        replayBuffer.setInitSegment(data = data)
    }
}

fun Model.mediaOnRecorderDataSegment(segment: RecorderDataSegment) {
    mainScope.launch {
        replayBuffer.appendDataSegment(segment = segment)
    }
}

fun Model.mediaOnRecorderFinished() {}

fun Model.mediaOnNoTorch() {
    mainScope.launch {
        handleNoTorch()
    }
}

fun Model.mediaOnFps(fps: Int) {
    mainScope.launch {
        currentFps = fps
        updateStatusStreamText()
    }
}

fun Model.mediaMoblinkStreamerDestinationAddress(address: String, port: UShort) {
    mainScope.launch {
        moblink.streamer?.startTunnels(address = address, port = port)
    }
}

fun Model.mediaMoblinkStreamerRestartTunnel(relayId: UUID) {
    mainScope.launch {
        moblink.streamer?.restartTunnel(relayId = relayId)
    }
}

fun Model.mediaSetZoomX(x: Float) {
    setZoomX(x = x)
}

fun Model.mediaSetExposureBias(bias: Float) {
    setExposureBias(bias = bias)
}

fun Model.mediaSelectedFps(auto: Boolean) {
    mainScope.launch {
        lowLightBoost = auto
        updateStatusStreamText()
    }
}

fun Model.mediaError(error: Throwable) {
    makeErrorToastMain(
        title = error.localizedMessage ?: "",
        subTitle = tryGetToastSubTitle(error = error),
    )
}

fun Model.mediaOnWhipPerform(
    request: Request,
    queue: CoroutineDispatcher,
    completion: ((ByteArray?, Response?, Throwable?) -> Unit)?,
) {
    mainScope.launch {
        when (stream.whip.httpTransport) {
            SettingsStreamWhipHttpTransport.standard -> {
                httpRequest(request = request, queue = queue, completion = completion)
            }
            SettingsStreamWhipHttpTransport.remoteControl -> {
                val assistant = remoteControlAssistant
                if (assistant == null) {
                    CoroutineScope(queue).launch {
                        completion?.invoke(null, null, null)
                    }
                } else {
                    val headers = request.headers.map { (name, value) ->
                        SettingsHttpHeader(name = name, value = value)
                    }
                    val body = request.body?.let { requestBody ->
                        val buffer = Buffer()
                        requestBody.writeTo(buffer)
                        buffer.readByteArray()
                    } ?: ByteArray(0)
                    assistant.whipPerform(
                        url = request.url.toString(),
                        method = request.method,
                        headers = headers,
                        body = body,
                    ) { data, response, error ->
                        CoroutineScope(queue).launch {
                            completion?.invoke(data, response, error)
                        }
                    }
                }
            }
        }
    }
}

private fun videoCaptureError(): String {
    return localized("Try to use single or low-energy cameras.")
}
