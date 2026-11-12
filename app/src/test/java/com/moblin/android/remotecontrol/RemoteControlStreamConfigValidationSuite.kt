package com.moblin.android.remotecontrol

import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.settings.SettingsStreamH264Profile
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.SettingsStreamSrtAdaptiveBitrateAlgorithm
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriority
import com.moblin.android.various.settings.SettingsStreamSrtImplementation
import com.moblin.android.various.settings.SettingsStreamWhipHttpTransport
import com.moblin.android.various.settings.defaultRtmpStreamUrl
import org.junit.Test
import java.util.UUID
import kotlin.math.pow
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun modified(stream: SettingsStream, edit: (SettingsStream) -> Unit = {}): SettingsStream {
    val copy = SettingsStream.decode(stream.encode())
    edit(copy)
    return copy
}

private fun destination(
    name: String,
    url: String = defaultRtmpStreamUrl,
): SettingsStreamMultiStreamingDestination {
    val destination = SettingsStreamMultiStreamingDestination()
    destination.name = name
    destination.url = url
    return destination
}

private val idle = RemoteControlStreamState()
private val live = RemoteControlStreamState(isLive = true)
private val recording = RemoteControlStreamState(isRecording = true)

@RunWith(RobolectricTestRunner::class)
class RemoteControlStreamConfigValidationSuite {
    class Fixture {
        val main = SettingsStream(name = "Main")
        val other = SettingsStream(name = "Other")
        val streams: List<SettingsStream>

        init {
            main.enabled = true
            main.url = "srtla://example.com:5000"
            main.multiStreaming.destinations = mutableListOf(destination("First"), destination("Second"))
            main.whip.headers = mutableListOf(SettingsHttpHeader(name = "Authorization", value = "Bearer secret"))
            main.obsBrbScene = "BRB"
            val relay = SettingsStreamSrtConnectionPriority(name = "Relay")
            relay.relayId = UUID.randomUUID()
            main.srt.connectionPriorities.priorities.add(relay)
            streams = listOf(main, other)
        }

        fun stream(name: String): SettingsStream {
            return streams.first { it.name == name }
        }
    }

    data class StreamCase(
        val testDescription: String,
        val stream: String = "Main",
        val state: RemoteControlStreamState = RemoteControlStreamState(),
        val edit: (SettingsStream) -> Unit,
    )

    @Test
    fun echoIsAccepted() {
        val states = listOf(
            idle,
            live,
            recording,
            RemoteControlStreamState(isLive = true, isRecording = true),
        )
        for (state in states) {
            val fixture = Fixture()
            assertTrue(
                isValidRemoteControlStreamUpdate(
                    stream = fixture.main,
                    new = modified(fixture.main),
                    streams = fixture.streams,
                    state = state,
                    creating = false,
                ),
            )
        }
    }

    @Test
    fun updateIsRejected() {
        val testCases = listOf(
            StreamCase("Empty name") { it.name = "" },
            StreamCase("Name of other stream") { it.name = "Other" },
            StreamCase("Malformed URL") { it.url = "no url" },
            StreamCase("RTMP URL without host") { it.url = "rtmp:///live/key" },
            StreamCase("URL with unsupported scheme") { it.url = "ftp://example.com" },
            StreamCase("URL while live", "Main", live) { it.url = "srt://example.com:4000" },
            StreamCase("URL of other stream while live", "Other", live) { it.url = "srt://example.com:4000" },
            StreamCase("URL of other stream while recording", "Other", recording) {
                it.url = "srt://example.com:4000"
            },
            StreamCase("Portrait while recording", "Main", recording) { it.portrait = true },
            StreamCase("Resolution while live", "Main", live) {
                it.resolution = SettingsStreamResolution.r1280x720
            },
            StreamCase("Resolution while recording", "Main", recording) {
                it.resolution = SettingsStreamResolution.r1280x720
            },
            StreamCase("FPS while recording", "Main", recording) { it.fps = 60 },
            StreamCase("FPS not in list") { it.fps = 24 },
            StreamCase("Codec while live", "Main", live) { it.codec = SettingsStreamCodec.h264avc },
            StreamCase("Profile while live", "Main", live) { it.h264Profile = SettingsStreamH264Profile.high },
            StreamCase("Rate control while live", "Main", live) { it.rateControl = SettingsStreamRateControl.cbr },
            StreamCase("Bitrate too low") { it.bitrate = 10000 },
            StreamCase("Key frame interval while live", "Main", live) { it.maxKeyFrameInterval = 1 },
            StreamCase("Key frame interval too big") { it.maxKeyFrameInterval = 11 },
            StreamCase("B-frames while live", "Main", live) { it.bFrames = true },
            StreamCase("Color range while recording", "Main", recording) {
                it.colorRange = SettingsStreamColorRange.limited
            },
            StreamCase("Adaptive resolution while live", "Main", live) { it.adaptiveEncoderResolution = true },
            StreamCase("Adaptive resolution threshold too big") { it.adaptiveEncoderResolutionThreashold = 4.0 },
            StreamCase("Timecodes with RTMP") {
                it.url = "rtmp://example.com/live/key"
                it.timecodesEnabled = true
            },
            StreamCase("Timecodes with H.264") {
                it.codec = SettingsStreamCodec.h264avc
                it.timecodesEnabled = true
            },
            StreamCase("Timecodes while live", "Main", live) { it.timecodesEnabled = true },
            StreamCase("NTP pool address with H.264") {
                it.codec = SettingsStreamCodec.h264avc
                it.ntpPoolAddress = "pool.ntp.org"
            },
            StreamCase("Audio codec while live", "Main", live) { it.audioCodec = SettingsStreamAudioCodec.opus },
            StreamCase("Audio bitrate while live", "Main", live) { it.audioBitrate = 64000 },
            StreamCase("Audio bitrate not multiple of 32 Kbps") { it.audioBitrate = 100_000 },
            StreamCase("Audio bitrate too big") { it.audioBitrate = 352_000 },
            StreamCase("Estimated viewer delay too big") { it.estimatedViewerDelay = 16f },
            StreamCase("Estimated viewer delay negative") { it.estimatedViewerDelay = -1f },
            StreamCase("SRT latency too big") { it.srt.latency = 70000 },
            StreamCase("SRT latency while live", "Main", live) { it.srt.latency = 2000 },
            StreamCase("SRT adaptive bitrate while live", "Main", live) { it.srt.adaptiveBitrateEnabled = false },
            StreamCase("SRT overhead bandwidth too small") { it.srt.overheadBandwidth = 4 },
            StreamCase("SRT overhead bandwidth too big") { it.srt.overheadBandwidth = 101 },
            StreamCase("SRT big packets while live", "Main", live) { it.srt.bigPackets = false },
            StreamCase("SRT DNS lookup strategy while live", "Main", live) {
                it.srt.dnsLookupStrategy = SettingsDnsLookupStrategy.ipv4
            },
            StreamCase("SRT implementation while live", "Main", live) {
                it.srt.implementation = SettingsStreamSrtImplementation.official
            },
            StreamCase("Fast IRL packets in flight") {
                it.srt.adaptiveBitrate.fastIrlSettings.packetsInFlight = 100
            },
            StreamCase("BELABOX minimum bitrate") {
                it.srt.adaptiveBitrate.belaboxSettings.minimumBitrate = 10f
            },
            StreamCase("Custom IRL decrease factor") {
                it.srt.adaptiveBitrate.customSettings.rttDiffHighDecreaseFactor = 0.5f
            },
            StreamCase("Connection priority too big") { it.srt.connectionPriorities.priorities[0].priority = 11 },
            StreamCase("Connection priority too small") {
                it.srt.connectionPriorities.priorities[0].priority = 0
            },
            StreamCase("Deleted cellular connection priority") {
                it.srt.connectionPriorities.priorities.removeAt(0)
            },
            StreamCase("Deleted WiFi connection priority") {
                it.srt.connectionPriorities.priorities.removeAt(1)
            },
            StreamCase("Unknown connection priority") {
                it.srt.connectionPriorities.priorities.add(SettingsStreamSrtConnectionPriority(name = "New"))
            },
            StreamCase("Duplicated connection priority") {
                it.srt.connectionPriorities.priorities.add(it.srt.connectionPriorities.priorities[0])
            },
            StreamCase("RTMP adaptive bitrate while live", "Main", live) { it.rtmp.adaptiveBitrateEnabled = false },
            StreamCase("RIST bonding while live", "Main", live) { it.rist.bonding = false },
            StreamCase("WHIP bearer token while live", "Main", live) {
                it.whip.headers[0] = it.whip.headers[0].copy(value = "Bearer other")
            },
            StreamCase("WHIP header without bearer") {
                it.whip.headers[0] = it.whip.headers[0].copy(value = "other")
            },
            StreamCase("WHIP other header") {
                it.whip.headers.add(SettingsHttpHeader(name = "X-Foo", value = "bar"))
            },
            StreamCase("WHIP second authorization header") {
                it.whip.headers.add(SettingsHttpHeader(name = "Authorization", value = "Bearer other"))
            },
            StreamCase("WHIP HTTP transport while live", "Main", live) {
                it.whip.httpTransport = SettingsStreamWhipHttpTransport.remoteControl
            },
            StreamCase("Destination URL without host") {
                it.multiStreaming.destinations[0].url = "rtmps:///live/key"
            },
            StreamCase("Destination SRT URL") {
                it.multiStreaming.destinations[0].url = "srt://example.com:4000"
            },
            StreamCase("Destination URL while live", "Main", live) {
                it.multiStreaming.destinations[0].url = "rtmp://example.com/live/key"
            },
            StreamCase("Destination enabled while recording", "Main", recording) {
                it.multiStreaming.destinations[0].enabled = true
            },
            StreamCase("Destination added while live", "Main", live) {
                it.multiStreaming.destinations.add(destination("Third"))
            },
            StreamCase("Destination deleted while recording", "Main", recording) {
                it.multiStreaming.destinations.removeAt(it.multiStreaming.destinations.size - 1)
            },
            StreamCase("Destination empty name") { it.multiStreaming.destinations[0].name = "" },
            StreamCase("Destination duplicated name") { it.multiStreaming.destinations[0].name = "Second" },
            StreamCase("Added destination with invalid URL") {
                it.multiStreaming.destinations.add(destination("Third", "no url"))
            },
            StreamCase("Recording override while recording", "Main", recording) {
                it.recording.overrideStream = true
            },
            StreamCase("Recording resolution while live", "Main", live) {
                it.recording.resolution = SettingsStreamResolution.r1280x720
            },
            StreamCase("Recording video codec while recording", "Main", recording) {
                it.recording.videoCodec = SettingsStreamCodec.h264avc
            },
            StreamCase("Recording video bitrate too big") { it.recording.videoBitrate = 60_000_000 },
            StreamCase("Recording key frame interval too big") { it.recording.maxKeyFrameInterval = 11 },
            StreamCase("Recording audio bitrate not multiple of 32 Kbps") { it.recording.audioBitrate = 100_000 },
            StreamCase("Recording audio bitrate while recording", "Main", recording) {
                it.recording.audioBitrate = 64000
            },
            StreamCase("Preview stream RTMP URL") { it.previewStream.url = "rtmp://example.com/live/key" },
            StreamCase("Preview stream resolution") {
                it.previewStream.resolution = SettingsStreamResolution.r1920x1080
            },
            StreamCase("Preview stream bitrate") { it.previewStream.bitrate = 3_000_000 },
            StreamCase("Replay post trigger delay") { it.replay.postTriggerDelay = 6 },
            StreamCase("Replay x") { it.replay.layout = it.replay.layout.copy(x = 101.0) },
            StreamCase("Replay size") { it.replay.layout = it.replay.layout.copy(size = 0.0) },
            StreamCase("Replay stinger transition point") {
                it.replay.inStinger = it.replay.inStinger.copy(transitionPoint = 2.0)
            },
            StreamCase("OBS WebSocket URL") { it.obsWebSocketUrl = "http://example.com" },
            StreamCase("OBS BRB scene when video source broken without BRB scene") {
                it.obsBrbScene = ""
                it.obsBrbSceneVideoSourceBroken = true
            },
            StreamCase("Open Streaming Platform URL") { it.openStreamingPlatformUrl = "https://example.com" },
            StreamCase("RealtimeIRL base URL") { it.realtimeIrlBaseUrl = "ws://example.com" },
            StreamCase("Twitch minimum watch streak") { it.twitchChatAlerts.minimumWatchStreak = 2 },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val stream = fixture.stream(testCase.stream)
            assertFalse(
                isValidRemoteControlStreamUpdate(
                    stream = stream,
                    new = modified(stream, testCase.edit),
                    streams = fixture.streams,
                    state = testCase.state,
                    creating = false,
                ),
                testCase.testDescription,
            )
        }
    }

    @Test
    fun updateIsAccepted() {
        val testCases = listOf(
            StreamCase("New name") { it.name = "Renamed" },
            StreamCase("URL") { it.url = "rtmp://example.com/live/key" },
            StreamCase("URL that needs cleaning") { it.url = "SRT://example.com: 4000" },
            StreamCase("Portrait") { it.portrait = true },
            StreamCase("Portrait of other stream while live", "Other", live) { it.portrait = true },
            StreamCase("Resolution of other stream while live", "Other", live) {
                it.resolution = SettingsStreamResolution.r1280x720
            },
            StreamCase("Codec while recording", "Main", recording) { it.codec = SettingsStreamCodec.h264avc },
            StreamCase("Bitrate while live", "Main", live) { it.bitrate = 3_000_000 },
            StreamCase("Low light boost while live", "Main", live) { it.lowLightBoost = true },
            StreamCase("Key frame interval while recording", "Main", recording) { it.maxKeyFrameInterval = 0 },
            StreamCase("Timecodes") { it.timecodesEnabled = true },
            StreamCase("Timecodes when changing to RIST and H.265") {
                it.url = "rist://example.com:2030"
                it.timecodesEnabled = true
            },
            StreamCase("Audio bitrate") { it.audioBitrate = 320_000 },
            StreamCase("SRT latency while recording", "Main", recording) { it.srt.latency = 2000 },
            StreamCase("Fast IRL settings while live", "Main", live) {
                it.srt.adaptiveBitrate.algorithm = SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl
                it.srt.adaptiveBitrate.fastIrlSettings.packetsInFlight = 700
                it.srt.adaptiveBitrate.fastIrlSettings.minimumBitrate = 2000f
            },
            StreamCase("Custom IRL settings") {
                val custom = it.srt.adaptiveBitrate.customSettings
                custom.pifDiffIncreaseFactor = 5f
                custom.rttDiffHighDecreaseFactor = 0.5f.pow(0.2f)
                custom.rttDiffHighMinimumDecrease = 400f
                custom.packetsInFlight = 5000
                custom.rttDiffHighAllowedSpike = 25f
                custom.minimumBitrate = 50f
            },
            StreamCase("Connection priorities while live", "Main", live) {
                it.srt.connectionPriorities.enabled = true
                it.srt.connectionPriorities.priorities[0].priority = 10
                it.srt.connectionPriorities.priorities[1].enabled = false
            },
            StreamCase("Deleted relay connection priority") {
                it.srt.connectionPriorities.priorities.removeAt(2)
            },
            StreamCase("WHIP bearer token") {
                it.whip.headers[0] = it.whip.headers[0].copy(value = "Bearer other")
            },
            StreamCase("WHIP bearer token while recording", "Main", recording) {
                it.whip.headers[0] = it.whip.headers[0].copy(value = "Bearer other")
            },
            StreamCase("WHIP added bearer token", "Other") {
                it.whip.headers = mutableListOf(SettingsHttpHeader(name = "Authorization", value = "Bearer token"))
            },
            StreamCase("Destination URL") {
                it.multiStreaming.destinations[0].url = "rtmps://example.com/live/key"
            },
            StreamCase("Destination enabled") { it.multiStreaming.destinations[0].enabled = true },
            StreamCase("Destination added") { it.multiStreaming.destinations.add(destination("Third")) },
            StreamCase("Destination deleted") { it.multiStreaming.destinations.removeAt(0) },
            StreamCase("Destination renamed while live", "Main", live) {
                it.multiStreaming.destinations[0].name = "Renamed"
            },
            StreamCase("Recording video codec while live", "Main", live) {
                it.recording.videoCodec = SettingsStreamCodec.h264avc
            },
            StreamCase("Recording audio bitrate auto") { it.recording.audioBitrate = 0 },
            StreamCase("Recording video bitrate") { it.recording.videoBitrate = 50_000_000 },
            StreamCase("Clean recordings while recording", "Main", recording) {
                it.recording.cleanRecordings = true
            },
            StreamCase("Preview stream URL") { it.previewStream.url = "whips://example.com/live" },
            StreamCase("Preview stream resolution and bitrate") {
                it.previewStream.resolution = SettingsStreamResolution.r854x480
                it.previewStream.bitrate = 2_000_000
            },
            StreamCase("Replay while live", "Main", live) {
                it.replay.enabled = true
                it.replay.layout = it.replay.layout.copy(x = 100.0)
                it.replay.layout = it.replay.layout.copy(size = 1.0)
                it.replay.postTriggerDelay = 5
                it.replay.outStinger = it.replay.outStinger.copy(transitionPoint = 1.0)
            },
            StreamCase("Empty OBS WebSocket URL") { it.obsWebSocketUrl = "" },
            StreamCase("OBS WebSocket URL") { it.obsWebSocketUrl = "wss://example.com:4455" },
            StreamCase("OBS BRB scene when video source broken") { it.obsBrbSceneVideoSourceBroken = true },
            StreamCase("Open Streaming Platform URL") { it.openStreamingPlatformUrl = "ws://example.com:5443/ws" },
            StreamCase("RealtimeIRL base URL") { it.realtimeIrlBaseUrl = "https://example.com/api" },
            StreamCase("Empty RealtimeIRL base URL") { it.realtimeIrlBaseUrl = "" },
            StreamCase("Estimated viewer delay") { it.estimatedViewerDelay = 15f },
            StreamCase("Platforms while live", "Main", live) {
                it.twitchChannelName = "twitch"
                it.kickChannelName = "kick"
                it.youTubeVideoIds = "a, b"
                it.twitchToastAlerts.minimumWatchStreak = 25
                it.kickChatAlerts.minimumKicks = 100
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val stream = fixture.stream(testCase.stream)
            assertTrue(
                isValidRemoteControlStreamUpdate(
                    stream = stream,
                    new = modified(stream, testCase.edit),
                    streams = fixture.streams,
                    state = testCase.state,
                    creating = false,
                ),
                testCase.testDescription,
            )
        }
    }

    @Test
    fun createIsAccepted() {
        val testCases = listOf(
            StreamCase("URL while live", "Other", live) { it.url = "rtmp://example.com/live/key" },
            StreamCase("Resolution while live", "Other", live) {
                it.resolution = SettingsStreamResolution.r1280x720
            },
            StreamCase("Destination", "Other", recording) {
                it.multiStreaming.destinations =
                    mutableListOf(destination("First", "rtmp://example.com/live/key"))
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val new = modified(SettingsStream(name = "New"), testCase.edit)
            val stream = SettingsStream(name = new.name)
            stream.id = new.id
            stream.srt.connectionPriorities.priorities = new.srt.connectionPriorities.priorities
            assertTrue(
                isValidRemoteControlStreamUpdate(
                    stream = stream,
                    new = new,
                    streams = fixture.streams,
                    state = testCase.state,
                    creating = true,
                ),
                testCase.testDescription,
            )
        }
    }

    @Test
    fun createIsRejected() {
        val testCases = listOf(
            StreamCase("Empty name") { it.name = "" },
            StreamCase("Name of other stream") { it.name = "Main" },
            StreamCase("Malformed URL") { it.url = "no url" },
            StreamCase("FPS not in list") { it.fps = 24 },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val new = modified(SettingsStream(name = "New"), testCase.edit)
            val stream = SettingsStream(name = new.name)
            stream.id = new.id
            stream.srt.connectionPriorities.priorities = new.srt.connectionPriorities.priorities
            assertFalse(
                isValidRemoteControlStreamUpdate(
                    stream = stream,
                    new = new,
                    streams = fixture.streams,
                    state = testCase.state,
                    creating = true,
                ),
                testCase.testDescription,
            )
        }
    }
}
