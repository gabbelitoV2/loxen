package com.moblin.android.various.settings

import android.util.Size
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.streamingplatforms.kick.storeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.storeTwitchAccessTokenInKeychain
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcaseVisibility
import com.moblin.android.streamingplatforms.youtube.loadYouTubeAuthStateFromKeychain
import com.moblin.android.streamingplatforms.youtube.storeYouTubeAuthStateInKeychain
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.utils.Named
import java.net.URI
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class SettingsStreamCodec(val rawValue: String) {
    @SerialName("H.265/HEVC")
    h265hevc("H.265/HEVC"),

    @SerialName("H.264/AVC")
    h264avc("H.264/AVC");

    fun shortString(): String {
        return when (this) {
            SettingsStreamCodec.h265hevc -> "H.265"
            SettingsStreamCodec.h264avc -> "H.264"
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsStreamCodec {
            return SettingsStreamCodec.entries.firstOrNull { it.rawValue == value }
                ?: SettingsStreamCodec.h264avc
        }
    }
}

@Serializable
enum class SettingsStreamH264Profile(val rawValue: String) {
    @SerialName("Baseline")
    baseline("Baseline"),

    @SerialName("Main")
    main("Main"),

    @SerialName("High")
    high("High");

    companion object {
        fun fromRawValue(value: String): SettingsStreamH264Profile? {
            return SettingsStreamH264Profile.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsStreamRateControl(val rawValue: String) {
    @SerialName("ABR")
    abr("ABR"),

    @SerialName("CBR")
    cbr("CBR"),

    @SerialName("VBR")
    vbr("VBR");

    override fun toString(): String {
        return when (this) {
            SettingsStreamRateControl.abr -> localized("ABR (Average)")
            SettingsStreamRateControl.cbr -> localized("CBR (Constant)")
            SettingsStreamRateControl.vbr -> localized("VBR (Variable)")
        }
    }

    fun shortString(): String {
        return when (this) {
            SettingsStreamRateControl.abr -> localized("ABR")
            SettingsStreamRateControl.cbr -> localized("CBR")
            SettingsStreamRateControl.vbr -> localized("VBR")
        }
    }

    companion object {
        fun cases(): List<SettingsStreamRateControl> {
            return listOf(
                SettingsStreamRateControl.abr,
                SettingsStreamRateControl.cbr,
                SettingsStreamRateControl.vbr
            )
        }

        fun makeValid(value: SettingsStreamRateControl): SettingsStreamRateControl {
            return value
        }

        fun fromRawValue(value: String): SettingsStreamRateControl? {
            return SettingsStreamRateControl.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsStreamResolution(val rawValue: String) {
    @SerialName("4032x3024")
    r4032x3024("4032x3024"),

    @SerialName("3840x2160")
    r3840x2160("3840x2160"),

    @SerialName("2560x1440")
    r2560x1440("2560x1440"),

    @SerialName("1920x1440")
    r1920x1440("1920x1440"),

    @SerialName("1920x1080")
    r1920x1080("1920x1080"),

    @SerialName("1664x936")
    r1664x936("1664x936"),

    @SerialName("1280x720")
    r1280x720("1280x720"),

    @SerialName("1024x768")
    r1024x768("1024x768"),

    @SerialName("960x540")
    r960x540("960x540"),

    @SerialName("854x480")
    r854x480("854x480"),

    @SerialName("640x360")
    r640x360("640x360"),

    @SerialName("426x240")
    r426x240("426x240");

    fun shortString(): String {
        return when (this) {
            SettingsStreamResolution.r4032x3024 -> "3024p (4:3)"
            SettingsStreamResolution.r3840x2160 -> "4K"
            SettingsStreamResolution.r2560x1440 -> "1440p"
            SettingsStreamResolution.r1920x1440 -> "1440p (4:3)"
            SettingsStreamResolution.r1920x1080 -> "1080p"
            SettingsStreamResolution.r1664x936 -> "936p"
            SettingsStreamResolution.r1024x768 -> "768p (4:3)"
            SettingsStreamResolution.r1280x720 -> "720p"
            SettingsStreamResolution.r960x540 -> "540p"
            SettingsStreamResolution.r854x480 -> "480p"
            SettingsStreamResolution.r640x360 -> "360p"
            SettingsStreamResolution.r426x240 -> "240p"
        }
    }

    fun dimensions(portrait: Boolean): Size {
        var size = when (this) {
            SettingsStreamResolution.r4032x3024 -> Size(4032, 3024)
            SettingsStreamResolution.r3840x2160 -> Size(3840, 2160)
            SettingsStreamResolution.r2560x1440 -> Size(2560, 1440)
            SettingsStreamResolution.r1920x1440 -> Size(1920, 1440)
            SettingsStreamResolution.r1920x1080 -> Size(1920, 1080)
            SettingsStreamResolution.r1664x936 -> Size(1664, 936)
            SettingsStreamResolution.r1024x768 -> Size(1024, 768)
            SettingsStreamResolution.r1280x720 -> Size(1280, 720)
            SettingsStreamResolution.r960x540 -> Size(960, 540)
            SettingsStreamResolution.r854x480 -> Size(854, 480)
            SettingsStreamResolution.r640x360 -> Size(640, 360)
            SettingsStreamResolution.r426x240 -> Size(426, 240)
        }
        if (portrait) {
            size = Size(size.height, size.width)
        }
        return size
    }

    companion object {
        fun greaterThan(lhs: SettingsStreamResolution, rhs: SettingsStreamResolution): Boolean {
            return lhs.dimensions(portrait = false).width > rhs.dimensions(portrait = false).width
        }

        fun fromRawValue(value: String): SettingsStreamResolution? {
            return SettingsStreamResolution.entries.firstOrNull { it.rawValue == value }
        }
    }
}

val fpss: List<Int> = listOf(120, 100, 60, 50, 30, 25, 15)

@Serializable
enum class SettingsStreamSrtImplementation(val rawValue: String) {
    @SerialName("Moblin")
    moblin("Moblin"),

    @SerialName("Official")
    official("Official");

    override fun toString(): String {
        return when (this) {
            SettingsStreamSrtImplementation.moblin -> localized("Moblin")
            SettingsStreamSrtImplementation.official -> localized("Official")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsStreamSrtImplementation? {
            return SettingsStreamSrtImplementation.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsStreamAudioCodec(val rawValue: String) {
    @SerialName("AAC")
    aac("AAC"),

    @SerialName("OPUS")
    opus("OPUS");

    fun toEncoder(): AudioEncoderSettings.Format {
        return when (this) {
            SettingsStreamAudioCodec.aac -> AudioEncoderSettings.Format.aac
            SettingsStreamAudioCodec.opus -> AudioEncoderSettings.Format.opus
        }
    }

    override fun toString(): String {
        return when (this) {
            SettingsStreamAudioCodec.aac -> "AAC"
            SettingsStreamAudioCodec.opus -> "Opus"
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsStreamAudioCodec? {
            return SettingsStreamAudioCodec.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsStreamProtocol(val rawValue: String) {
    @SerialName("RTMP")
    rtmp("RTMP"),

    @SerialName("SRT")
    srt("SRT"),

    @SerialName("RIST")
    rist("RIST"),

    @SerialName("WHIP")
    whip("WHIP"),

    @SerialName("Mobcam")
    mobcam("Mobcam");

    companion object {
        fun fromRawValue(value: String): SettingsStreamProtocol {
            return SettingsStreamProtocol.entries.firstOrNull { it.rawValue == value }
                ?: SettingsStreamProtocol.rtmp
        }
    }
}

enum class SettingsStreamDetailedProtocol {
    rtmp,
    rtmps,
    srt,
    srtla,
    rist,
    whip,
    whips,
    mobcam,
}

@Serializable
class SettingsStreamSrtConnectionPriority(
    @SerialName("name")
    var name: String = "",
) {
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("priority")
    var priority: Int = 1

    @SerialName("enabled")
    var enabled: Boolean = true

    @SerialName("relayId")
    var relayId: UUID? = null

    fun clone(): SettingsStreamSrtConnectionPriority {
        val new = SettingsStreamSrtConnectionPriority(name)
        new.priority = priority
        new.enabled = enabled
        new.relayId = relayId
        return new
    }
}

@Serializable
class SettingsStreamSrtConnectionPriorities(
    @SerialName("enabled")
    var enabled: Boolean = false,

    @SerialName("priorities")
    var priorities: MutableList<SettingsStreamSrtConnectionPriority> = mutableListOf(
        SettingsStreamSrtConnectionPriority("Cellular"),
        SettingsStreamSrtConnectionPriority("WiFi")
    ),
) {
    fun clone(): SettingsStreamSrtConnectionPriorities {
        val new = SettingsStreamSrtConnectionPriorities()
        new.enabled = enabled
        new.priorities.clear()
        for (priority in priorities) {
            new.priorities.add(priority.clone())
        }
        return new
    }
}

@Serializable
enum class SettingsStreamSrtAdaptiveBitrateAlgorithm {
    belabox,
    fastIrl,
    slowIrl,
    customIrl;

    override fun toString(): String {
        return when (this) {
            SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox -> localized("BELABOX")
            SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl -> localized("Fast IRL")
            SettingsStreamSrtAdaptiveBitrateAlgorithm.slowIrl -> localized("Slow IRL")
            SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl -> localized("Custom IRL")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsStreamSrtAdaptiveBitrateAlgorithm {
            return SettingsStreamSrtAdaptiveBitrateAlgorithm.entries.firstOrNull { it.name == value }
                ?: SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox
        }
    }
}

@Serializable
class SettingsStreamSrtAdaptiveBitrateFastIrlSettings(
    @SerialName("packetsInFlight")
    var packetsInFlight: Int = 200,

    @SerialName("minimumBitrate")
    var minimumBitrate: Float = 250f,
) {
    fun clone(): SettingsStreamSrtAdaptiveBitrateFastIrlSettings {
        val new = SettingsStreamSrtAdaptiveBitrateFastIrlSettings()
        new.packetsInFlight = packetsInFlight
        new.minimumBitrate = minimumBitrate
        return new
    }
}

@Serializable
class SettingsStreamSrtAdaptiveBitrateCustomSettings(
    @SerialName("packetsInFlight")
    var packetsInFlight: Int = 200,

    @SerialName("pifDiffIncreaseFactor")
    var pifDiffIncreaseFactor: Float = 100f,

    @SerialName("rttDiffHighDecreaseFactor")
    var rttDiffHighDecreaseFactor: Float = 0.9f,

    @SerialName("rttDiffHighAllowedSpike")
    var rttDiffHighAllowedSpike: Float = 50f,

    @SerialName("rttDiffHighMinimumDecrease")
    var rttDiffHighMinimumDecrease: Float = 250f,

    @SerialName("minimumBitrate")
    var minimumBitrate: Float = 250f,
) {
    fun clone(): SettingsStreamSrtAdaptiveBitrateCustomSettings {
        val new = SettingsStreamSrtAdaptiveBitrateCustomSettings()
        new.packetsInFlight = packetsInFlight
        new.pifDiffIncreaseFactor = pifDiffIncreaseFactor
        new.rttDiffHighDecreaseFactor = rttDiffHighDecreaseFactor
        new.rttDiffHighAllowedSpike = rttDiffHighAllowedSpike
        new.rttDiffHighMinimumDecrease = rttDiffHighMinimumDecrease
        new.minimumBitrate = minimumBitrate
        return new
    }
}

@Serializable
class SettingsStreamSrtAdaptiveBitrateBelaboxSettings(
    @SerialName("minimumBitrate")
    var minimumBitrate: Float = 250f,
) {
    fun clone(): SettingsStreamSrtAdaptiveBitrateBelaboxSettings {
        val new = SettingsStreamSrtAdaptiveBitrateBelaboxSettings()
        new.minimumBitrate = minimumBitrate
        return new
    }
}

@Serializable
class SettingsStreamSrtAdaptiveBitrate(
    @SerialName("algorithm")
    var algorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm =
        SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox,

    @SerialName("fastIrlSettings")
    var fastIrlSettings: SettingsStreamSrtAdaptiveBitrateFastIrlSettings =
        SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),

    @SerialName("customSettings")
    var customSettings: SettingsStreamSrtAdaptiveBitrateCustomSettings =
        SettingsStreamSrtAdaptiveBitrateCustomSettings(),

    @SerialName("belaboxSettings")
    var belaboxSettings: SettingsStreamSrtAdaptiveBitrateBelaboxSettings =
        SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
) {
    fun clone(): SettingsStreamSrtAdaptiveBitrate {
        val new = SettingsStreamSrtAdaptiveBitrate()
        new.algorithm = algorithm
        new.fastIrlSettings = fastIrlSettings.clone()
        new.customSettings = customSettings.clone()
        new.belaboxSettings = belaboxSettings.clone()
        return new
    }
}

@Serializable
class SettingsStreamSrt(
    @SerialName("latency")
    var latency: Int = defaultSrtLatency,

    @SerialName("maximumBandwidthFollowInput")
    var maximumBandwidthFollowInput: Boolean = true,

    @SerialName("overheadBandwidth")
    var overheadBandwidth: Int = 25,

    @SerialName("adaptiveBitrateEnabled")
    var adaptiveBitrateEnabled: Boolean = true,

    @SerialName("adaptiveBitrate")
    var adaptiveBitrate: SettingsStreamSrtAdaptiveBitrate = SettingsStreamSrtAdaptiveBitrate(),

    @SerialName("connectionPriorities")
    var connectionPriorities: SettingsStreamSrtConnectionPriorities =
        SettingsStreamSrtConnectionPriorities(),

    @SerialName("mpegtsPacketsPerPacket")
    var mpegtsPacketsPerPacketRemove: Int = 7,

    @SerialName("dnsLookupStrategy")
    var dnsLookupStrategy: SettingsDnsLookupStrategy = SettingsDnsLookupStrategy.system,

    @SerialName("implementation")
    var implementation: SettingsStreamSrtImplementation = SettingsStreamSrtImplementation.moblin,

    @SerialName("bigPackets")
    var bigPackets: Boolean = true,

    @SerialName("bigPacketsMigrated")
    var bigPacketsMigrated: Boolean = false,

    @SerialName("implemenationMigrated")
    var implemenationMigrated: Boolean = false,
) {
    init {
        if (!bigPacketsMigrated) {
            bigPackets = mpegtsPacketsPerPacketRemove == 7
            bigPacketsMigrated = true
        }
        if (!implemenationMigrated) {
            if (latency < 1000) {
                implementation = SettingsStreamSrtImplementation.official
            }
            implemenationMigrated = true
        }
    }

    fun mpegtsPacketsPerPacket(): Int {
        return if (bigPackets) {
            7
        } else {
            6
        }
    }

    fun clone(): SettingsStreamSrt {
        val new = SettingsStreamSrt()
        new.latency = latency
        new.overheadBandwidth = overheadBandwidth
        new.maximumBandwidthFollowInput = maximumBandwidthFollowInput
        new.adaptiveBitrateEnabled = adaptiveBitrateEnabled
        new.adaptiveBitrate = adaptiveBitrate.clone()
        new.connectionPriorities = connectionPriorities.clone()
        new.mpegtsPacketsPerPacketRemove = mpegtsPacketsPerPacketRemove
        new.dnsLookupStrategy = dnsLookupStrategy
        new.implementation = implementation
        new.bigPackets = bigPackets
        new.bigPacketsMigrated = bigPacketsMigrated
        new.implemenationMigrated = implemenationMigrated
        return new
    }
}

@Serializable
class SettingsStreamRtmp(
    @SerialName("adaptiveBitrateEnabled")
    var adaptiveBitrateEnabled: Boolean = true,
) {
    fun clone(): SettingsStreamRtmp {
        val new = SettingsStreamRtmp()
        new.adaptiveBitrateEnabled = adaptiveBitrateEnabled
        return new
    }
}

@Serializable
class SettingsStreamRist(
    @SerialName("adaptiveBitrateEnabled")
    var adaptiveBitrateEnabled: Boolean = true,

    @SerialName("bonding")
    var bonding: Boolean = true,
) {
    fun clone(): SettingsStreamRist {
        val new = SettingsStreamRist()
        new.adaptiveBitrateEnabled = adaptiveBitrateEnabled
        new.bonding = bonding
        return new
    }
}

@Serializable
data class SettingsHttpHeader(
    @SerialName("name")
    var name: String = "",

    @SerialName("value")
    var value: String = "",
)

@Serializable
enum class SettingsStreamWhipHttpTransport {
    standard,
    remoteControl;

    override fun toString(): String {
        return when (this) {
            SettingsStreamWhipHttpTransport.standard -> localized("Standard")
            SettingsStreamWhipHttpTransport.remoteControl -> localized("Remote control")
        }
    }
}

@Serializable
class SettingsStreamWhip(
    @SerialName("headers")
    var headers: MutableList<SettingsHttpHeader> = mutableListOf(),

    @SerialName("httpTransport")
    var httpTransport: SettingsStreamWhipHttpTransport = SettingsStreamWhipHttpTransport.standard,
) {
    fun clone(): SettingsStreamWhip {
        val new = SettingsStreamWhip()
        new.headers = headers.toMutableList()
        new.httpTransport = httpTransport
        return new
    }
}

@Serializable
class SettingsStreamChat(
    @SerialName("bttvEmotes")
    var bttvEmotes: Boolean = false,

    @SerialName("ffzEmotes")
    var ffzEmotes: Boolean = false,

    @SerialName("seventvEmotes")
    var seventvEmotes: Boolean = false,
) {
    fun clone(): SettingsStreamChat {
        val new = SettingsStreamChat()
        new.bttvEmotes = bttvEmotes
        new.ffzEmotes = ffzEmotes
        new.seventvEmotes = seventvEmotes
        return new
    }
}

@Serializable
class SettingsStreamRecording(
    @SerialName("overrideStream")
    var overrideStream: Boolean = false,

    @SerialName("resolution")
    var resolution: SettingsStreamResolution = SettingsStream.defaultResolution,

    @SerialName("fps")
    var fps: Int = SettingsStream.defaultFps,

    @SerialName("videoCodec")
    var videoCodec: SettingsStreamCodec = SettingsStreamCodec.h265hevc,

    @SerialName("videoBitrate")
    var videoBitrate: Int = 0,

    @SerialName("maxKeyFrameInterval")
    var maxKeyFrameInterval: Int = 0,

    @SerialName("audioBitrate")
    var audioBitrate: Int = 128_000,

    @SerialName("autoStartRecording")
    var autoStartRecording: Boolean = false,

    @SerialName("autoStopRecording")
    var autoStopRecording: Boolean = false,

    @SerialName("cleanRecordings")
    var cleanRecordings: Boolean = false,

    @SerialName("cleanSnapshots")
    var cleanSnapshots: Boolean = false,

    @SerialName("recordingPath")
    var recordingPath: ByteArray? = null,
) {
    fun clone(): SettingsStreamRecording {
        val new = SettingsStreamRecording()
        new.overrideStream = overrideStream
        new.resolution = resolution
        new.fps = fps
        new.videoCodec = videoCodec
        new.videoBitrate = videoBitrate
        new.maxKeyFrameInterval = maxKeyFrameInterval
        new.audioBitrate = audioBitrate
        new.autoStartRecording = autoStartRecording
        new.autoStopRecording = autoStopRecording
        new.cleanRecordings = cleanRecordings
        new.cleanSnapshots = cleanSnapshots
        new.recordingPath = recordingPath
        return new
    }

    fun videoBitrateString(): String {
        return if (videoBitrate != 0) {
            formatBytesPerSecond(speed = videoBitrate.toLong())
        } else {
            localized("Auto")
        }
    }

    fun maxKeyFrameIntervalString(): String {
        return if (maxKeyFrameInterval != 0) {
            formatShortDuration(seconds = maxKeyFrameInterval)
        } else {
            localized("Auto")
        }
    }

    fun audioBitrateString(): String {
        return if (audioBitrate != 0) {
            formatBytesPerSecond(speed = audioBitrate.toLong())
        } else {
            localized("Auto")
        }
    }

    fun isDefaultRecordingPath(): Boolean {
        return recordingPath == null
    }
}

@Serializable
class SettingsStreamPreviewStream(
    @SerialName("url")
    var url: String = "",

    @SerialName("resolution")
    var resolution: SettingsStreamResolution = SettingsStreamResolution.r640x360,

    @SerialName("bitrate")
    var bitrate: Int = 500_000,
) {
    fun clone(): SettingsStreamPreviewStream {
        val new = SettingsStreamPreviewStream()
        new.url = url
        new.resolution = resolution
        new.bitrate = bitrate
        return new
    }
}

@Serializable
enum class SettingsStreamReplayTransitionType(val rawValue: String) {
    @SerialName("fade")
    fade("fade"),

    @SerialName("stingers")
    stingers("stingers"),

    @SerialName("none")
    none("none");

    override fun toString(): String {
        return when (this) {
            SettingsStreamReplayTransitionType.fade -> localized("Fade")
            SettingsStreamReplayTransitionType.stingers -> localized("Stingers")
            SettingsStreamReplayTransitionType.none -> localized("None")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsStreamReplayTransitionType {
            return SettingsStreamReplayTransitionType.entries.firstOrNull { it.rawValue == value }
                ?: SettingsStreamReplayTransitionType.fade
        }
    }
}

@Serializable
data class SettingsStreamReplayStinger(
    @SerialName("id")
    var id: UUID = UUID.randomUUID(),

    @SerialName("name")
    var name: String = "",

    @SerialName("transitionPoint")
    var transitionPoint: Double = 0.5,
) {
    fun makeFilename(): String? {
        val path = runCatching { URI("file:///$name").path ?: "" }.getOrElse { return null }
        val fileExtension = path.substringAfterLast('.', "")
        return "$id.$fileExtension"
    }
}

@Serializable
class SettingsStreamReplay(
    @SerialName("enabled")
    var enabled: Boolean = false,

    @SerialName("transitionType")
    var transitionType: SettingsStreamReplayTransitionType = SettingsStreamReplayTransitionType.fade,

    @SerialName("inStinger")
    var inStinger: SettingsStreamReplayStinger = SettingsStreamReplayStinger(),

    @SerialName("outStinger")
    var outStinger: SettingsStreamReplayStinger = SettingsStreamReplayStinger(),

    @SerialName("postTriggerDelay")
    var postTriggerDelay: Int = 3,

    @SerialName("x")
    var x: Double = 0.0,

    @SerialName("y")
    var y: Double = 0.0,

    @SerialName("size")
    var size: Double = 100.0,

    @SerialName("alignment")
    var alignment: SettingsAlignment = SettingsAlignment.topLeft,

    @SerialName("positioningLock")
    var positioningLock: Boolean = false,

    @SerialName("enterForegroundCountAtLatestUsage")
    var enterForegroundCountAtLatestUsage: Int? = null,

    @SerialName("fade")
    var fade: Boolean? = null,
) {
    @Transient
    var layout: SettingsWidgetLayout = SettingsWidgetLayout()

    init {
        if (fade != null) {
            transitionType = if (fade == true) {
                SettingsStreamReplayTransitionType.fade
            } else {
                SettingsStreamReplayTransitionType.none
            }
        }
        layout.x = x
        layout.updateXString()
        layout.y = y
        layout.updateYString()
        layout.size = size
        layout.updateSizeString()
        layout.alignment = alignment
        layout.positioningLock = positioningLock
    }

    fun clone(): SettingsStreamReplay {
        val new = SettingsStreamReplay()
        new.enabled = enabled
        new.transitionType = transitionType
        new.inStinger = inStinger.copy()
        new.outStinger = outStinger.copy()
        new.postTriggerDelay = postTriggerDelay
        new.layout = layout
        new.x = x
        new.y = y
        new.size = size
        new.alignment = alignment
        new.positioningLock = positioningLock
        new.enterForegroundCountAtLatestUsage = enterForegroundCountAtLatestUsage
        return new
    }
}

@Serializable
class SettingsStreamTwitchReward(
    @SerialName("id")
    var id: UUID = UUID.randomUUID(),

    @SerialName("rewardId")
    var rewardId: String = "",

    @SerialName("title")
    var title: String = "",

    @SerialName("alert")
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
)

const val maximumNumberOfTwitchRaidChannels = 10

@Serializable
class SettingsStreamTwitchRaidChannel(
    @SerialName("channelId")
    var channelId: String = "",

    @SerialName("channelName")
    var channelName: String = "",

    @SerialName("timestamp")
    var timestamp: Instant = Instant.now(),
) {
    val id: String
        get() = channelId

    fun clone(): SettingsStreamTwitchRaidChannel {
        val new = SettingsStreamTwitchRaidChannel(channelId, channelName)
        new.timestamp = timestamp
        return new
    }
}

fun appendTwitchRaidChannel(
    channels: List<SettingsStreamTwitchRaidChannel>,
    channelId: String,
    channelName: String,
): List<SettingsStreamTwitchRaidChannel> {
    val result = channels.filter { it.channelId != channelId }.toMutableList()
    result.add(0, SettingsStreamTwitchRaidChannel(channelId, channelName))
    return result.take(maximumNumberOfTwitchRaidChannels)
}

@Serializable
class SettingsStreamMultiStreamingDestination(
    @SerialName("name")
    override var name: String = SettingsStreamMultiStreamingDestination.baseName,

    @SerialName("url")
    var url: String = defaultRtmpStreamUrl,

    @SerialName("enabled")
    var enabled: Boolean = false,
) : Named {
    @Transient
    var id: UUID = UUID.randomUUID()

    companion object {
        val baseName: String = localized("My destination")
    }

    fun clone(): SettingsStreamMultiStreamingDestination {
        val new = SettingsStreamMultiStreamingDestination()
        new.name = name
        new.url = url
        new.enabled = enabled
        return new
    }
}

@Serializable
class SettingsStreamMultiStreaming(
    @SerialName("destinations")
    var destinations: MutableList<SettingsStreamMultiStreamingDestination> = mutableListOf(),
) {
    fun clone(): SettingsStreamMultiStreaming {
        val new = SettingsStreamMultiStreaming()
        for (destination in destinations) {
            new.destinations.add(destination.clone())
        }
        return new
    }
}

@Serializable
class SettingsTwitchAlerts(
    @SerialName("follows")
    var follows: Boolean = true,

    @SerialName("subscriptions")
    var subscriptions: Boolean = true,

    @SerialName("giftSubscriptions")
    var giftSubscriptions: Boolean = true,

    @SerialName("resubscriptions")
    var resubscriptions: Boolean = true,

    @SerialName("rewards")
    var rewards: Boolean = true,

    @SerialName("raids")
    var raids: Boolean = true,

    @SerialName("cheers")
    var cheers: Boolean = true,

    @SerialName("minimumCheerBits")
    var minimumCheerBits: Int = 0,

    @SerialName("watchStreaks")
    var watchStreaks: Boolean = true,

    @SerialName("minimumWatchStreak")
    var minimumWatchStreak: Int = 5,

    @SerialName("sharedChat")
    var sharedChat: Boolean = false,
) {
    fun clone(): SettingsTwitchAlerts {
        val new = SettingsTwitchAlerts()
        new.follows = follows
        new.subscriptions = subscriptions
        new.giftSubscriptions = giftSubscriptions
        new.resubscriptions = resubscriptions
        new.rewards = rewards
        new.raids = raids
        new.cheers = cheers
        new.minimumCheerBits = minimumCheerBits
        new.watchStreaks = watchStreaks
        new.minimumWatchStreak = minimumWatchStreak
        new.sharedChat = sharedChat
        return new
    }

    fun isBitsEnabled(amount: Int): Boolean {
        return cheers && amount >= minimumCheerBits
    }

    fun isWatchStreakEnabled(count: Int): Boolean {
        return watchStreaks && count >= minimumWatchStreak
    }
}

@Serializable
class SettingsKickAlerts(
    @SerialName("subscriptions")
    var subscriptions: Boolean = true,

    @SerialName("giftedSubscriptions")
    var giftedSubscriptions: Boolean = true,

    @SerialName("rewards")
    var rewards: Boolean = true,

    @SerialName("hosts")
    var hosts: Boolean = true,

    @SerialName("bans")
    var bans: Boolean = true,

    @SerialName("kicks")
    var kicks: Boolean = true,

    @SerialName("minimumKicks")
    var minimumKicks: Int = 0,
) {
    fun clone(): SettingsKickAlerts {
        val new = SettingsKickAlerts()
        new.subscriptions = subscriptions
        new.giftedSubscriptions = giftedSubscriptions
        new.rewards = rewards
        new.hosts = hosts
        new.bans = bans
        new.kicks = kicks
        new.minimumKicks = minimumKicks
        return new
    }

    fun isKicksEnabled(amount: Int): Boolean {
        return kicks && amount >= minimumKicks
    }
}

@Serializable
class SettingsStream(
    @SerialName("name")
    override var name: String = "My stream",

    @SerialName("id")
    var id: UUID = UUID.randomUUID(),

    @SerialName("enabled")
    var enabled: Boolean = false,

    @SerialName("url")
    var url: String = defaultStreamUrl,

    @SerialName("twitchChannelName")
    var twitchChannelName: String = "",

    @SerialName("twitchChannelId")
    var twitchChannelId: String = "",

    @SerialName("twitchShowFollows")
    var twitchShowFollows: Boolean? = null,

    @SerialName("twitchChatAlerts")
    var twitchChatAlerts: SettingsTwitchAlerts = SettingsTwitchAlerts(),

    @SerialName("twitchToastAlerts")
    var twitchToastAlerts: SettingsTwitchAlerts = SettingsTwitchAlerts(),

    @SerialName("twitchAccessToken")
    var twitchAccessToken: String = "",

    @SerialName("twitchLoggedIn")
    var twitchLoggedIn: Boolean = false,

    @SerialName("twitchWantsToBeLoggedIn")
    var twitchWantsToBeLoggedIn: Boolean = false,

    @SerialName("twitchNotLoggedInCount")
    var twitchNotLoggedInCount: Int = 0,

    @SerialName("twitchRewards")
    var twitchRewards: MutableList<SettingsStreamTwitchReward> = mutableListOf(),

    @SerialName("twitchRaidsSent")
    var twitchRaidsSent: MutableList<SettingsStreamTwitchRaidChannel> = mutableListOf(),

    @SerialName("twitchRaidsReceived")
    var twitchRaidsReceived: MutableList<SettingsStreamTwitchRaidChannel> = mutableListOf(),

    @SerialName("twitchSendMessagesTo")
    var twitchSendMessagesTo: Boolean = true,

    @SerialName("kickChannelName")
    var kickChannelName: String = "",

    @SerialName("kickChannelId")
    var kickChannelId: String? = null,

    @SerialName("kickChatroomChannelId")
    var kickChatroomChannelId: String? = null,

    @SerialName("kickSlug")
    var kickSlug: String? = null,

    @SerialName("kickAccessToken")
    var kickAccessToken: String = "",

    @SerialName("kickLoggedIn")
    var kickLoggedIn: Boolean = false,

    @SerialName("kickWantsToBeLoggedIn")
    var kickWantsToBeLoggedIn: Boolean = false,

    @SerialName("kickNotLoggedInCount")
    var kickNotLoggedInCount: Int = 0,

    @SerialName("kickSendMessagesTo")
    var kickSendMessagesTo: Boolean = true,

    @SerialName("kickChatAlerts")
    var kickChatAlerts: SettingsKickAlerts = SettingsKickAlerts(),

    @SerialName("kickToastAlerts")
    var kickToastAlerts: SettingsKickAlerts = SettingsKickAlerts(),

    @SerialName("youTubeAuthState")
    var youTubeAuthState: Any? = null,

    @SerialName("youTubeWantsToBeLoggedIn")
    var youTubeWantsToBeLoggedIn: Boolean = false,

    @SerialName("youTubeNotLoggedInCount")
    var youTubeNotLoggedInCount: Int = 0,

    @SerialName("youTubeVideoId")
    var youTubeVideoIds: String = "",

    @SerialName("youTubeHandle")
    var youTubeHandle: String = "",

    @SerialName("youTubeScheduleStreamTitle")
    var youTubeScheduleStreamTitle: String = "",

    @SerialName("youTubeScheduleStreamVisibility")
    var youTubeScheduleStreamVisibility: YouTubeApiLiveBroadcaseVisibility =
        YouTubeApiLiveBroadcaseVisibility.public,

    @SerialName("youTubeScheduleStreamAutoStop")
    var youTubeScheduleStreamAutoStop: Boolean = true,

    @SerialName("afreecaTvChannelName")
    var soopChannelName: String = "",

    @SerialName("afreecaTvStreamId")
    var soopStreamId: String = "",

    @SerialName("openStreamingPlatformUrl")
    var openStreamingPlatformUrl: String = "",

    @SerialName("openStreamingPlatformChannelId")
    var openStreamingPlatformChannelId: String = "",

    @SerialName("obsWebSocketEnabled")
    var obsWebSocketEnabled: Boolean = false,

    @SerialName("obsWebSocketUrl")
    var obsWebSocketUrl: String = "",

    @SerialName("obsWebSocketPassword")
    var obsWebSocketPassword: String = "",

    @SerialName("obsSourceName")
    var obsSourceName: String = "",

    @SerialName("obsMainScene")
    var obsMainScene: String = "",

    @SerialName("obsBrbScene")
    var obsBrbScene: String = "",

    @SerialName("obsBrbSceneVideoSourceBroken")
    var obsBrbSceneVideoSourceBroken: Boolean = false,

    @SerialName("obsAutoStartStream")
    var obsAutoStartStream: Boolean = false,

    @SerialName("obsAutoStopStream")
    var obsAutoStopStream: Boolean = false,

    @SerialName("obsAutoStartRecording")
    var obsAutoStartRecording: Boolean = false,

    @SerialName("obsAutoStopRecording")
    var obsAutoStopRecording: Boolean = false,

    @SerialName("streamingDirectlyToObs")
    var streamingDirectlyToObs: Boolean = false,

    @SerialName("discordSnapshotWebhook")
    var discordSnapshotWebhook: String = "",

    @SerialName("discordChatBotSnapshotWebhook")
    var discordChatBotSnapshotWebhook: String = "",

    @SerialName("discordSnapshotWebhookOnlyWhenLive")
    var discordSnapshotWebhookOnlyWhenLive: Boolean = true,

    @SerialName("resolution")
    var resolution: SettingsStreamResolution = SettingsStream.defaultResolution,

    @SerialName("fps")
    var fps: Int = SettingsStream.defaultFps,

    @SerialName("autoFps")
    var lowLightBoost: Boolean = false,

    @SerialName("bitrate")
    var bitrate: Int = 5_000_000,

    @SerialName("bitrateRateControl")
    var rateControl: SettingsStreamRateControl = SettingsStreamRateControl.abr,

    @SerialName("codec")
    var codec: SettingsStreamCodec = SettingsStreamCodec.h265hevc,

    @SerialName("h264Profile")
    var h264Profile: SettingsStreamH264Profile = SettingsStreamH264Profile.main,

    @SerialName("bFrames")
    var bFrames: Boolean = false,

    @SerialName("adaptiveEncoderResolution")
    var adaptiveEncoderResolution: Boolean = false,

    @SerialName("adaptiveEncoderResolutionThreashold")
    var adaptiveEncoderResolutionThreashold: Double = 1.0,

    @SerialName("adaptiveBitrate")
    var adaptiveBitrate: Boolean = true,

    @SerialName("srt")
    var srt: SettingsStreamSrt = SettingsStreamSrt(),

    @SerialName("rtmp")
    var rtmp: SettingsStreamRtmp = SettingsStreamRtmp(),

    @SerialName("rist")
    var rist: SettingsStreamRist = SettingsStreamRist(),

    @SerialName("whip")
    var whip: SettingsStreamWhip = SettingsStreamWhip(),

    @SerialName("maxKeyFrameInterval")
    var maxKeyFrameInterval: Int = 2,

    @SerialName("audioCodec")
    var audioCodec: SettingsStreamAudioCodec = SettingsStreamAudioCodec.aac,

    @SerialName("audioBitrate")
    var audioBitrate: Int = 128_000,

    @SerialName("chat")
    var chat: SettingsStreamChat = SettingsStreamChat(),

    @SerialName("recording")
    var recording: SettingsStreamRecording = SettingsStreamRecording(),

    @SerialName("realtimeIrlEnabled")
    var realtimeIrlEnabled: Boolean = false,

    @SerialName("realtimeIrlBaseUrl")
    var realtimeIrlBaseUrl: String = SettingsStream.defaultRealtimeIrlBaseUrl,

    @SerialName("realtimeIrlPushKey")
    var realtimeIrlPushKey: String = "",

    @SerialName("portrait")
    var portrait: Boolean = false,

    @SerialName("backgroundStreaming")
    var backgroundStreaming: Boolean = false,

    @SerialName("backgroundStreamingPiP")
    var backgroundStreamingPiP: Boolean = true,

    @SerialName("estimatedViewerDelay")
    var estimatedViewerDelay: Float = 8.0f,

    @SerialName("ntpPoolAddress")
    var ntpPoolAddress: String = "time.apple.com",

    @SerialName("timecodesEnabled")
    var timecodesEnabled: Boolean = false,

    @SerialName("replay")
    var replay: SettingsStreamReplay = SettingsStreamReplay(),

    @SerialName("goLiveNotificationDiscordMessage")
    var goLiveNotificationDiscordMessage: String = "",

    @SerialName("goLiveNotificationDiscordWebhookUrl")
    var goLiveNotificationDiscordWebhookUrl: String = "",

    @SerialName("goLiveNotificationMoblinWebsite")
    var goLiveNotificationMoblinWebsite: Boolean = false,

    @SerialName("multiStreaming")
    var multiStreaming: SettingsStreamMultiStreaming = SettingsStreamMultiStreaming(),

    @SerialName("previewStream")
    var previewStream: SettingsStreamPreviewStream = SettingsStreamPreviewStream(),

    @SerialName("autoGoLive")
    var autoGoLive: Boolean = false,
) : Named {
    init {
        val showFollows = twitchShowFollows
        if (showFollows != null) {
            twitchChatAlerts.follows = showFollows
            twitchToastAlerts.follows = showFollows
        }
        twitchShowFollows = null
        if (kickAccessToken.isNotEmpty()) {
            storeKickAccessTokenInKeychain(id, kickAccessToken)
            kickAccessToken = ""
        }
        val encoded = loadYouTubeAuthStateFromKeychain(id)
        if (encoded != null) {
            val decoded = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
            if (decoded != null) {
                youTubeAuthState = decodeYouTubeAuthState(decoded)
            }
        }
    }

    companion object {
        val defaultRealtimeIrlBaseUrl: String = "https://rtirl.com/api"
        val defaultResolution: SettingsStreamResolution = SettingsStreamResolution.r1920x1080
        val defaultFps: Int = 30
    }

    override fun equals(other: Any?): Boolean {
        return other is SettingsStream && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun getYouTubeVideoIds(): List<String> {
        return youTubeVideoIds.split(",").filter { it.isNotEmpty() }
    }

    fun clone(): SettingsStream {
        val new = SettingsStream(name)
        new.url = url
        new.twitchChannelName = twitchChannelName
        new.twitchChannelId = twitchChannelId
        new.twitchShowFollows = twitchShowFollows
        new.twitchRewards = twitchRewards.map { it }.toMutableList()
        new.twitchRaidsSent = twitchRaidsSent.map { it.clone() }.toMutableList()
        new.twitchRaidsReceived = twitchRaidsReceived.map { it.clone() }.toMutableList()
        new.twitchSendMessagesTo = twitchSendMessagesTo
        new.twitchChatAlerts = twitchChatAlerts.clone()
        new.twitchToastAlerts = twitchToastAlerts.clone()
        new.twitchAccessToken = twitchAccessToken
        new.twitchLoggedIn = twitchLoggedIn
        new.twitchWantsToBeLoggedIn = twitchWantsToBeLoggedIn
        new.twitchNotLoggedInCount = twitchNotLoggedInCount
        if (twitchLoggedIn) {
            storeTwitchAccessTokenInKeychain(new.id, twitchAccessToken)
        }
        new.kickChannelName = kickChannelName
        new.kickChannelId = kickChannelId
        new.kickChatroomChannelId = kickChatroomChannelId
        new.kickSlug = kickSlug
        new.kickAccessToken = kickAccessToken
        new.kickLoggedIn = kickLoggedIn
        new.kickWantsToBeLoggedIn = kickWantsToBeLoggedIn
        new.kickNotLoggedInCount = kickNotLoggedInCount
        if (kickLoggedIn) {
            storeKickAccessTokenInKeychain(new.id, kickAccessToken)
        }
        new.kickSendMessagesTo = kickSendMessagesTo
        new.kickChatAlerts = kickChatAlerts.clone()
        new.kickToastAlerts = kickToastAlerts.clone()
        new.youTubeAuthState = youTubeAuthState
        new.youTubeWantsToBeLoggedIn = youTubeWantsToBeLoggedIn
        new.youTubeNotLoggedInCount = youTubeNotLoggedInCount
        new.youTubeVideoIds = youTubeVideoIds
        new.youTubeHandle = youTubeHandle
        new.youTubeScheduleStreamTitle = youTubeScheduleStreamTitle
        new.youTubeScheduleStreamVisibility = youTubeScheduleStreamVisibility
        new.youTubeScheduleStreamAutoStop = youTubeScheduleStreamAutoStop
        new.soopChannelName = soopChannelName
        new.soopStreamId = soopStreamId
        new.openStreamingPlatformUrl = openStreamingPlatformUrl
        new.openStreamingPlatformChannelId = openStreamingPlatformChannelId
        new.obsWebSocketEnabled = obsWebSocketEnabled
        new.obsWebSocketUrl = obsWebSocketUrl
        new.obsWebSocketPassword = obsWebSocketPassword
        new.obsSourceName = obsSourceName
        new.obsBrbScene = obsBrbScene
        new.obsMainScene = obsMainScene
        new.obsBrbSceneVideoSourceBroken = obsBrbSceneVideoSourceBroken
        new.obsAutoStartStream = obsAutoStartStream
        new.obsAutoStopStream = obsAutoStopStream
        new.obsAutoStartRecording = obsAutoStartRecording
        new.obsAutoStopRecording = obsAutoStopRecording
        new.streamingDirectlyToObs = streamingDirectlyToObs
        new.discordSnapshotWebhook = discordSnapshotWebhook
        new.discordChatBotSnapshotWebhook = discordChatBotSnapshotWebhook
        new.discordSnapshotWebhookOnlyWhenLive = discordSnapshotWebhookOnlyWhenLive
        new.resolution = resolution
        new.fps = fps
        new.lowLightBoost = lowLightBoost
        new.bitrate = bitrate
        new.rateControl = rateControl
        new.codec = codec
        new.h264Profile = h264Profile
        new.bFrames = bFrames
        new.adaptiveEncoderResolution = adaptiveEncoderResolution
        new.adaptiveEncoderResolutionThreashold = adaptiveEncoderResolutionThreashold
        new.adaptiveBitrate = adaptiveBitrate
        new.srt = srt.clone()
        new.rtmp = rtmp.clone()
        new.rist = rist.clone()
        new.whip = whip.clone()
        new.maxKeyFrameInterval = maxKeyFrameInterval
        new.audioCodec = audioCodec
        new.audioBitrate = audioBitrate
        new.chat = chat.clone()
        new.recording = recording.clone()
        new.realtimeIrlEnabled = realtimeIrlEnabled
        new.realtimeIrlBaseUrl = realtimeIrlBaseUrl
        new.realtimeIrlPushKey = realtimeIrlPushKey
        new.portrait = portrait
        new.backgroundStreaming = backgroundStreaming
        new.backgroundStreamingPiP = backgroundStreamingPiP
        new.estimatedViewerDelay = estimatedViewerDelay
        new.ntpPoolAddress = ntpPoolAddress
        new.timecodesEnabled = timecodesEnabled
        new.replay = replay.clone()
        new.goLiveNotificationDiscordMessage = goLiveNotificationDiscordMessage
        new.goLiveNotificationDiscordWebhookUrl = goLiveNotificationDiscordWebhookUrl
        new.goLiveNotificationMoblinWebsite = goLiveNotificationMoblinWebsite
        new.multiStreaming = multiStreaming.clone()
        new.previewStream = previewStream.clone()
        new.autoGoLive = autoGoLive
        return new
    }

    fun getScheme(): String? {
        return runCatching { URI(url).scheme }.getOrNull()
    }

    fun getProtocol(): SettingsStreamProtocol {
        return when (getScheme()) {
            "rtmp" -> SettingsStreamProtocol.rtmp
            "rtmps" -> SettingsStreamProtocol.rtmp
            "srt" -> SettingsStreamProtocol.srt
            "srtla" -> SettingsStreamProtocol.srt
            "rist" -> SettingsStreamProtocol.rist
            "whip" -> SettingsStreamProtocol.whip
            "whips" -> SettingsStreamProtocol.whip
            "mobcam" -> SettingsStreamProtocol.mobcam
            else -> SettingsStreamProtocol.rtmp
        }
    }

    fun getDetailedProtocol(): SettingsStreamDetailedProtocol {
        return when (getScheme()) {
            "rtmp" -> SettingsStreamDetailedProtocol.rtmp
            "rtmps" -> SettingsStreamDetailedProtocol.rtmps
            "srt" -> SettingsStreamDetailedProtocol.srt
            "srtla" -> SettingsStreamDetailedProtocol.srtla
            "rist" -> SettingsStreamDetailedProtocol.rist
            "whip" -> SettingsStreamDetailedProtocol.whip
            "whips" -> SettingsStreamDetailedProtocol.whips
            "mobcam" -> SettingsStreamDetailedProtocol.mobcam
            else -> SettingsStreamDetailedProtocol.rtmp
        }
    }

    fun protocolString(): String {
        if (getProtocol() == SettingsStreamProtocol.srt && isSrtla()) {
            return "SRTLA"
        } else if (getProtocol() == SettingsStreamProtocol.rtmp && isRtmps()) {
            return "RTMPS"
        }
        return getProtocol().rawValue
    }

    fun isRtmps(): Boolean {
        return getScheme() == "rtmps"
    }

    fun isSrtla(): Boolean {
        return getScheme() == "srtla"
    }

    fun mobcamPort(): Int {
        val port = runCatching { URI(url).port }.getOrNull() ?: return DefaultTcpPorts.mobcamStream
        if (port < 0 || port > 65535) {
            return DefaultTcpPorts.mobcamStream
        }
        return port
    }

    fun isBonding(): Boolean {
        if (isSrtla()) {
            return true
        }
        if (getProtocol() == SettingsStreamProtocol.rist && rist.bonding) {
            return true
        }
        return false
    }

    fun resolutionString(): String {
        return resolution.shortString()
    }

    fun dimensions(): Size {
        return resolution.dimensions(portrait)
    }

    fun codecString(): String {
        return codec.shortString()
    }

    fun rateControlString(): String {
        return rateControl.shortString()
    }

    fun bitrateString(): String {
        var bitrate = formatBytesPerSecond(speed = bitrate.toLong())
        if (getProtocol() == SettingsStreamProtocol.srt && srt.adaptiveBitrateEnabled) {
            bitrate = "<$bitrate"
        } else if (getProtocol() == SettingsStreamProtocol.rtmp && rtmp.adaptiveBitrateEnabled) {
            bitrate = "<$bitrate"
        }
        return bitrate
    }

    fun audioBitrateString(): String {
        return formatBytesPerSecond(speed = audioBitrate.toLong())
    }

    fun audioCodecString(): String {
        return audioCodec.toString()
    }

    fun maxKeyFrameIntervalString(): String {
        return if (maxKeyFrameInterval != 0) {
            formatShortDuration(seconds = maxKeyFrameInterval)
        } else {
            localized("Auto")
        }
    }

    fun isYouTubeAuthorized(): Boolean {
        return TODO("no Android counterpart for AppAuthCore OIDAuthState")
    }

    private fun encodeYouTubeAuthState(): ByteArray? {
        val authState = youTubeAuthState ?: return null
        storeYouTubeAuthStateInKeychain(id, authState.toString())
        return TODO("no Android counterpart for NSKeyedArchiver")
    }

    private fun decodeYouTubeAuthState(encoded: ByteArray?): Any? {
        if (encoded == null) {
            return null
        }
        return TODO("no Android counterpart for NSKeyedUnarchiver")
    }
}
