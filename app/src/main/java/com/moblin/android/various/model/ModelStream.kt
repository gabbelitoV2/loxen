package com.moblin.android.various.model

import android.graphics.Bitmap
import android.media.MediaCodecInfo
import android.os.Build
import android.os.SystemClock
import com.moblin.android.platform.log.Log
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.formatShortDuration
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
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.various.settings.SettingsColorSpace
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamH264Profile
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.SettingsStreamWhipHttpTransport
import com.moblin.android.various.settings.defaultStreamUrl
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.various.storages.ThermalState
import com.moblin.android.various.utils.tryGetToastSubTitle
import com.moblin.android.various.utils.uploadImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.moblin.android.platform.swiftui.PublishedStateFlow as MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import com.moblin.android.media.haishinkit.codec.video.numberOfFailedEncodings

private const val TAG = "ModelStream"

private val mainScope = CoroutineScope(Dispatchers.Main)

private val lowPowerBitrate: Int = 2_000_000

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
    if (delayed && !isLive.value) {
        return
    }
    if (stream.value.url == defaultStreamUrl) {
        makeErrorToast(
            title = localized("Please enter your stream URL in stream settings before going live."),
            subTitle = localized("Configure it in Settings → Streams → ${stream.value.name} → URL."),
        )
        return
    }
    if (database.location.resetWhenGoingLive) {
        resetLocationData()
    }
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.GO_LIVE))
    setIsLive(value = true)
    streaming = true
    streamTotalBytes = 0L
    updateScreenAutoOff()
    startNetStream()
    startFetchingYouTubeChatVideoId()
    reloadViewers()
    if (stream.value.recording.autoStartRecording) {
        startRecording()
    }
    if (stream.value.obsAutoStartStream) {
        obsStartStream()
    }
    if (stream.value.obsAutoStartRecording) {
        obsStartRecording()
    }
    val historyStream = StreamingHistoryStream(settings = stream.value.clone())
    streamingHistoryStream = historyStream
    historyStream.updateHighestThermalState(
        thermalState = ThermalState.entries.firstOrNull {
            it == ThermalState.from(from = statusOther.thermalState.value)
        } ?: ThermalState.NOMINAL,
    )
    historyStream.updateLowestBatteryLevel(level = battery.level.value)
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
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.END))
    streamTotalBytes += media.streamTotal()
    streaming = false
    if (stream.value.recording.autoStopRecording) {
        stopRecording()
    }
    if (stopObsStreamIfEnabled && stream.value.obsAutoStopStream) {
        obsStopStream()
    }
    if (stopObsRecordingIfEnabled && stream.value.obsAutoStopRecording) {
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
    return isGoLiveNotificationDiscordConfigured() || stream.value.goLiveNotificationMoblinWebsite
}

private fun Model.isGoLiveNotificationDiscordConfigured(): Boolean {
    if (stream.value.goLiveNotificationDiscordMessage.isEmpty()) {
        return false
    }
    if (stream.value.goLiveNotificationDiscordWebhookUrl.isEmpty()) {
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
        val discordUrl = stream.value.goLiveNotificationDiscordWebhookUrl
        if (discordUrl.isNotEmpty()) {
            pending += 1
            media.takeSnapshot(age = 0.0f) { image, _, _ ->
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
        message = stream.value.goLiveNotificationDiscordMessage,
    ) { _ ->
        onCompleted()
    }
}

fun Model.startNetStream() {
    streamState = StreamState.connecting
    latestLowBitrateTime = Instant.now()
    moblink.streamer?.stopTunnels()
    when (stream.value.getProtocol()) {
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
    val rtmp = stream.value.rtmp
    media.rtmpStartStream(
        url = stream.value.url,
        targetBitrate = getBitrate().toInt(),
        adaptiveBitrateEnabled = rtmp.adaptiveBitrateEnabled,
    )
    updateAdaptiveBitrateRtmpIfEnabled()
}

private fun Model.startNetStreamSrt() {
    val srt = stream.value.srt
    com.moblin.android.media.haishinkit.mpeg.payloadSize = srt.mpegtsPacketsPerPacket() * MpegTsPacket.size
    previousSrtDroppedPacketsTotal = 0
    media.srtStartStream(
        isSrtla = stream.value.isSrtla(),
        url = stream.value.url,
        reconnectTime = 5.0,
        targetBitrate = getBitrate().toInt(),
        adaptiveBitrateAlgorithm = if (srt.adaptiveBitrateEnabled) {
            srt.adaptiveBitrate.algorithm
        } else {
            null
        },
        latency = srt.latency,
        experimental = database.debug.enhancedMoblinSrt.value,
        overheadBandwidth = srt.overheadBandwidth,
        maximumBandwidthFollowInput = srt.maximumBandwidthFollowInput,
        mpegtsPacketsPerPacket = srt.mpegtsPacketsPerPacket(),
        packetPadding = database.debug.packetPadding.value,
        networkInterfaceNames = database.networkInterfaceNames,
        connectionPriorities = srt.connectionPriorities,
        dnsLookupStrategy = srt.dnsLookupStrategy,
    )
    updateAdaptiveBitrateSrt(srt = srt)
}

private fun Model.startNetStreamRist() {
    val rist = stream.value.rist
    media.ristStartStream(
        url = stream.value.url,
        bonding = rist.bonding,
        targetBitrate = getBitrate().toInt(),
        adaptiveBitrateEnabled = rist.adaptiveBitrateEnabled,
    )
    updateAdaptiveBitrateRistIfEnabled()
}

private fun Model.startNetStreamWhip() {
    media.whipStartStream(
        url = stream.value.url,
        headers = stream.value.whip.headers,
        videoCodec = stream.value.codec,
        audioCodec = stream.value.audioCodec,
        videoBitrate = stream.value.bitrate.toDouble(),
    )
}

private fun Model.startNetStreamMobcam() {
    media.mobcamStartStream(port = stream.value.mobcamPort(), deviceName = Build.MODEL)
}

fun Model.startPreviewStream() {
    if (isPreviewStreaming.value) {
        return
    }
    if (stream.value.previewStream.url.isEmpty()) {
        makeErrorToast(title = localized("Preview stream not configured"))
        return
    }
    media.startPreviewStream(
        url = stream.value.previewStream.url,
        resolution = stream.value.previewStream.resolution,
        bitrate = stream.value.previewStream.bitrate,
    )
    setIsPreviewStreaming(value = true)
}

fun Model.stopPreviewStream() {
    if (!isPreviewStreaming.value) {
        return
    }
    media.stopPreviewStream()
    setIsPreviewStreaming(value = false)
}

fun Model.togglePreviewStream() {
    if (isPreviewStreaming.value) {
        stopPreviewStream()
    } else {
        startPreviewStream()
    }
}

fun Model.setIsPreviewStreaming(value: Boolean) {
    isPreviewStreaming.value = value
    setQuickButton(type = SettingsQuickButtonType.previewStream, isOn = value)
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
    bonding.statistics.value = noValue
}

fun Model.setCurrentStream(stream: SettingsStream) {
    this.stream.value = stream
    stream.enabled = true
    for (ostream in database.streams) {
        if (ostream.id != stream.id) {
            ostream.enabled = false
        }
    }
    currentStreamId.value = stream.id
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
    setColorRange()
    setNetStream()
    setStreamResolution()
    setStreamFps()
    setColorSpace()
    setStreamCodec()
    setStreamAdaptiveResolution()
    setStreamKeyFrameInterval()
    setStreamBitrate(stream = stream.value)
    setStreamRateControl(stream = stream.value)
    setGraphicsImplementation()
    setAudioStreamBitrate(stream = stream.value)
    setAudioStreamFormat(format = stream.value.audioCodec.toEncoder())
    setAudioChannelsMap(
        channelsMap = mapOf(
            0 to database.audio.outputToInputChannelsMap.channel1,
            1 to database.audio.outputToInputChannelsMap.channel2,
        ),
    )
    setAudioGain(gainDb = database.audio.gainDb.value)
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
    cameraPreviewView.setDevices(ids = emptyList<UUID>(), widgets = emptyMap())
    media.setNetStream(
        proto = stream.value.getProtocol(),
        portrait = stream.value.portrait,
        timecodesEnabled = isTimecodesEnabled(),
        builtinAudioDelay = database.debug.builtinAudioAndVideoDelay.value,
        attachDefaultAudio = !isChatPhone(),
        destinations = stream.value.multiStreaming.destinations,
        srtImplementation = stream.value.srt.implementation,
        limitAdaptiveBitrateByTransportBitrate = stream.value.rateControl != SettingsStreamRateControl.cbr,
        colorRange = stream.value.colorRange,
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
    val processor = media.processor
    if (processor == null) {
        this.processor = null
        return
    }
    processorControlQueue.launch {
        processor.setDrawable(drawable = streamPreviewView)
        processor.setExternalDisplayDrawable(drawable = externalDisplayStreamPreviewView)
        mainScope.launch {
            this@attachStream.processor = processor
        }
        processor.startRunning()
    }
}

fun Model.setStreamResolution() {
    val resolution: SettingsStreamResolution = if (stream.value.recording.overrideStream) {
        if (stream.value.recording.resolution > stream.value.resolution) {
            stream.value.recording.resolution
        } else {
            stream.value.resolution
        }
    } else {
        stream.value.resolution
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
        canvas = resolution.dimensions(portrait = stream.value.portrait),
        stream = stream.value.resolution.dimensions(portrait = stream.value.portrait),
    )
}

private fun Model.setStreamCodec() {
    when (stream.value.codec) {
        SettingsStreamCodec.h264avc -> {
            when (stream.value.h264Profile) {
                SettingsStreamH264Profile.baseline -> {
                    media.setVideoProfile(profile = com.moblin.android.platform.videotoolbox.kVTProfileLevel_H264_Baseline_AutoLevel)
                }
                SettingsStreamH264Profile.main -> {
                    media.setVideoProfile(profile = com.moblin.android.platform.videotoolbox.kVTProfileLevel_H264_Main_AutoLevel)
                }
                SettingsStreamH264Profile.high -> {
                    media.setVideoProfile(profile = com.moblin.android.platform.videotoolbox.kVTProfileLevel_H264_High_AutoLevel)
                }
            }
        }
        SettingsStreamCodec.h265hevc -> {
            if (database.color.space == SettingsColorSpace.hlgBt2020) {
                media.setVideoProfile(profile = com.moblin.android.platform.videotoolbox.kVTProfileLevel_HEVC_Main10_AutoLevel)
            } else {
                media.setVideoProfile(profile = com.moblin.android.platform.videotoolbox.kVTProfileLevel_HEVC_Main_AutoLevel)
            }
        }
    }
    media.setAllowFrameReordering(value = stream.value.bFrames)
}

private fun Model.setStreamAdaptiveResolution() {
    media.setStreamAdaptiveResolution(
        value = stream.value.adaptiveEncoderResolution,
        thresholdsFactor = stream.value.adaptiveEncoderResolutionThreashold,
    )
}

private fun Model.setStreamKeyFrameInterval() {
    media.setStreamKeyFrameInterval(seconds = stream.value.maxKeyFrameInterval)
}

fun Model.isStreamConfigured(): Boolean {
    return stream.value != fallbackStream
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
        val elapsed = now - startTime.toEpochMilli()
        streamUptime.uptime.value = formatShortDuration(seconds = (elapsed / 1000).toInt())
    } else if (streamUptime.uptime.value != noValue) {
        streamUptime.uptime.value = noValue
    }
}

private fun Model.makeYouAreLiveToast() {
    makeToast(title = localized("🎉 You are LIVE at ${stream.value.name} 🎉"))
}

fun Model.makeStreamEndedToast(subTitle: String? = null, onTapped: (() -> Unit)? = null) {
    makeToast(title = localized("🤟 Stream ended 🤟"), subTitle = subTitle, onTapped = onTapped)
}

fun Model.makeNotLoggedInToToast(platform: Platform) {
    makeErrorToast(
        title = localized("Not logged in to ${platform.displayName()}"),
        subTitle = localized("Please login again"),
    )
}

private fun Model.makeConnectFailureToast(subTitle: String) {
    makeErrorToast(
        title = failedToConnectMessage(stream.value.name),
        subTitle = subTitle,
        vibrate = true,
    )
}

private fun Model.makeFffffToast(subTitle: String) {
    makeErrorToast(
        title = fffffMessage,
        font = FontFamily.Default,
        subTitle = subTitle,
        vibrate = true,
    )
}

private fun Model.onConnected() {
    makeYouAreLiveToast()
    streamStartTime = Instant.now()
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
        streamTotalBytes += media.streamTotal()
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
    if (bonding.statistics.value != noValue) {
        bonding.statistics.value = noValue
    }
}

private fun Model.handleBondingStatistics(connections: List<BondingConnection>) {
    bonding.statisticsFormatter.format(connections)?.let { (message, rtts, percentages) ->
        bonding.statistics.value = message
        bonding.rtts.value = rtts
        bonding.pieChartPercentages.value = percentages
    }
}

fun Model.updateSpeed(now: Long) {
    if (isLive.value) {
        val speed = media.getVideoStreamBitrate(bitrate = stream.value.bitrate).toLong()
        checkLowBitrate(speed = speed, now = now)
        streamingHistoryStream?.updateBitrate(bitrate = speed)
        val speedMbpsOneDecimal = String.format(Locale.US, "%.1f", speed.toDouble() / 1_000_000)
        if (speedMbpsOneDecimal != bitrate.speedMbpsOneDecimal.value) {
            bitrate.speedMbpsOneDecimal.value = speedMbpsOneDecimal
        }
        val speedString = formatBytesPerSecond(speed = speed)
        val total = sizeFormatter.string(fromByteCount = media.streamTotal())
        val numberOfDestinations = media.getNumberOfDestinations()
        val speedAndTotal = if (numberOfDestinations == 1) {
            localized("$speedString ($total)")
        } else {
            localized("$speedString x$numberOfDestinations ($total)")
        }
        if (speedAndTotal != bitrate.speedAndTotal.value) {
            bitrate.speedAndTotal.value = speedAndTotal
        }
        val bitrateStatusIconColor: Color? = if (speed < stream.value.bitrate.toLong() / 5) {
            Color.Red
        } else if (speed < stream.value.bitrate.toLong() / 2) {
            Color(0xFFFFA500)
        } else {
            null
        }
        if (bitrateStatusIconColor != bitrate.statusIconColor.value) {
            bitrate.statusIconColor.value = bitrateStatusIconColor
        }
        if (isWatchLocal()) {
            sendSpeedAndTotalToWatch(speedAndTotal = bitrate.speedAndTotal.value)
        }
    } else if (bitrate.speedAndTotal.value != noValue) {
        bitrate.speedMbpsOneDecimal.value = noValue
        bitrate.speedAndTotal.value = noValue
        if (isWatchLocal()) {
            sendSpeedAndTotalToWatch(speedAndTotal = bitrate.speedAndTotal.value)
        }
    }
}

private fun Model.updateCameraControls() {
    media.setCameraControls(enabled = database.cameraControlsEnabled)
}

fun Model.setCameraControlsEnabled() {
    cameraControlEnabled.value = database.cameraControlsEnabled
    media.setCameraControls(enabled = database.cameraControlsEnabled)
}

fun Model.updateSrtlaPriorities() {
    media.setConnectionPriorities(connectionPriorities = stream.value.srt.connectionPriorities.clone())
}

private fun Model.checkLowBitrate(speed: Long, now: Long) {
    if (!database.lowBitrateWarning) {
        return
    }
    if (streamState != StreamState.connected) {
        return
    }
    if (speed < 500_000 && now > latestLowBitrateTime.toEpochMilli() + 15_000) {
        makeWarningToast(title = lowBitrateMessage, vibrate = true)
        latestLowBitrateTime = Instant.now()
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
    } else if (getSrtClientStream(id = cameraId) != null) {
        isNetwork = true
    } else {
        val ristStream = getRistStream(id = cameraId)
        if (ristStream != null) {
            isNetwork = true
            ristStream.connected = true
        } else if (getWhipStream(id = cameraId) != null) {
            isNetwork = true
        } else if (getWhepStream(id = cameraId) != null) {
            isNetwork = true
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
    } else if (getSrtClientStream(id = cameraId) != null) {
        isNetwork = true
    } else {
        val ristStream = getRistStream(id = cameraId)
        if (ristStream != null) {
            isNetwork = true
            ristStream.connected = false
        } else if (getWhipStream(id = cameraId) != null) {
            isNetwork = true
        } else if (getWhepStream(id = cameraId) != null) {
            isNetwork = true
        }
    }
    if (isNetwork) {
        markMicAsDisconnected(id = "$cameraId 0")
        switchMicIfNeededAfterNetworkCameraChange()
    }
    if (isCurrentScenesVideoSourceNetwork(cameraId = cameraId)) {
        updateAutoSceneSwitcherVideoSourceDisconnected()
    }
    updateDisconnectProtectionVideoSourceDisconnected()
    updateVideoPreviews()
}

private fun Model.handleNoTorch() {
    if (!streamOverlay.isFrontCameraSelected.value) {
        makeErrorToast(
            title = localized("Torch unavailable in this scene."),
            subTitle = localized("Normally only available for built-in cameras."),
        )
    }
}

fun Model.startStreamIfAutoGoLive() {
    if (!stream.value.autoGoLive || stream.value.getProtocol() != SettingsStreamProtocol.mobcam || isLive.value) {
        return
    }
    startStream()
}

fun Model.toggleStream() {
    if (isLive.value) {
        stopStream()
    } else {
        startStream()
    }
}

fun Model.setIsLive(value: Boolean) {
    isLive.value = value
    updateLiveActivity()
    updateMacStatusItem()
    updatePictureInPicture()
    if (isWatchLocal()) {
        sendIsLiveToWatch(isLive = isLive.value)
    }
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(streaming = isLive.value))
}

fun Model.setStreamFps(fps: Int? = null) {
    if (isChatPhone()) {
        media.setFps(fps = 1, preferAutoFps = false)
    } else {
        media.setFps(fps = fps ?: stream.value.fps, preferAutoFps = stream.value.lowLightBoost)
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

fun Model.getBitratePresetByBitrate(bitrate: Int): SettingsBitratePreset? {
    return database.bitratePresets.firstOrNull { it.bitrate == bitrate.toInt() }
}

fun Model.setBitrate(bitrate: Int) {
    if (bitrate != stream.value.bitrate) {
        stream.value.bitrate = bitrate
    }
    if (stream.value.enabled) {
        setStreamBitrate(stream = stream.value)
    }
    val preset = getBitratePresetByBitrate(bitrate = bitrate) ?: return
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(bitrate = preset.id))
}

private fun Model.getBitrate(): Int {
    return if (statusTopRight.isLowPowerMode.value) {
        lowPowerBitrate
    } else {
        stream.value.bitrate
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
        if (newBitrateStatusColor != bitrate.statusColor.value) {
            bitrate.statusColor.value = newBitrateStatusColor
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
        overlay = database.debug.debugOverlay.value,
        relaxed = relaxedBitrate,
    )
    result?.let { (lines, actions) ->
        latestDebugLines = lines
        latestDebugActions = actions
    }
}

fun Model.updateDebugOverlay() {
    if (database.debug.debugOverlay.value) {
        debugOverlay.debugLines.value = latestDebugLines + latestDebugActions
        if (Log.isLoggable(TAG, Log.DEBUG) && isLive.value) {
            Log.d(TAG, latestDebugLines.joinToString(separator = ", "))
        }
    } else if (debugOverlay.debugLines.value.isNotEmpty()) {
        debugOverlay.debugLines.value = emptyList()
    }
}

private fun Model.setColorRange() {
    for (mediaPlayer in mediaPlayers.values) {
        mediaPlayer.setPixelFormatType(stream.value.colorRange)
    }
    reloadIngests()
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

fun Model.mediaOnPhotoTaken() {
    mainScope.launch {
        handlePhotoTaken()
    }
}

fun Model.mediaOnFps(fps: Int) {
    mainScope.launch {
        currentFps = fps
        updateStatusStreamText()
    }
}

fun Model.mediaMoblinkStreamerDestinationAddress(address: String, port: Int) {
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
        when (stream.value.whip.httpTransport) {
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
