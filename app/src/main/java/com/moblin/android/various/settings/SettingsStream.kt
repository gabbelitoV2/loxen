package com.moblin.android.various.settings

import android.util.Size
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.platform.codable.DataSerializer
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

private fun <T> settingsStreamRawValueSerializer(
    serialName: String,
    values: List<T>,
    rawValue: (T) -> String,
    fallback: T,
): KSerializer<T> {
    return object : KSerializer<T> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: T) {
            encoder.encodeString(rawValue(value))
        }

        override fun deserialize(decoder: Decoder): T {
            val text = decoder.decodeString()
            return values.firstOrNull { rawValue(it) == text } ?: fallback
        }
    }
}

@Serializable(with = SettingsStreamCodec.Serializer::class)
enum class SettingsStreamCodec(val rawValue: String) {
    h265hevc("H.265/HEVC"),
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

    object Serializer : KSerializer<SettingsStreamCodec> by settingsStreamRawValueSerializer(
        "com.moblin.android.various.settings.SettingsStreamCodec",
        SettingsStreamCodec.entries,
        { it.rawValue },
        SettingsStreamCodec.h264avc,
    )
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

@Serializable(with = SettingsStreamProtocol.Serializer::class)
enum class SettingsStreamProtocol(val rawValue: String) {
    rtmp("RTMP"),
    srt("SRT"),
    rist("RIST"),
    whip("WHIP"),
    mobcam("Mobcam");

    companion object {
        fun fromRawValue(value: String): SettingsStreamProtocol {
            return SettingsStreamProtocol.entries.firstOrNull { it.rawValue == value }
                ?: SettingsStreamProtocol.rtmp
        }
    }

    object Serializer : KSerializer<SettingsStreamProtocol> by settingsStreamRawValueSerializer(
        "com.moblin.android.various.settings.SettingsStreamProtocol",
        SettingsStreamProtocol.entries,
        { it.rawValue },
        SettingsStreamProtocol.rtmp,
    )
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

@Serializable(with = SettingsStreamSrtConnectionPriority.Serializer::class)
class SettingsStreamSrtConnectionPriority(
    var name: String = "",
) {
    var id: UUID = UUID.randomUUID()
    var priority: Int = 1
    var enabled: Boolean = true
    var relayId: UUID? = null

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("priority", priority)
        encode("enabled", enabled)
        encode("relayId", relayId)
    }

    fun clone(): SettingsStreamSrtConnectionPriority {
        val new = SettingsStreamSrtConnectionPriority(name)
        new.priority = priority
        new.enabled = enabled
        new.relayId = relayId
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtConnectionPriority {
            val connectionPriority = SettingsStreamSrtConnectionPriority()
            connectionPriority.id = container.decode("id", UUID.randomUUID())
            connectionPriority.name = container.decode("name", "")
            connectionPriority.priority = container.decode("priority", 1)
            connectionPriority.enabled = container.decode("enabled", true)
            connectionPriority.relayId = container.decode<UUID?>("relayId", null)
            return connectionPriority
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtConnectionPriority> by JsonObjectSerializer(
        "SettingsStreamSrtConnectionPriority",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtConnectionPriorities.Serializer::class)
class SettingsStreamSrtConnectionPriorities(
    var enabled: Boolean = false,
    var priorities: MutableList<SettingsStreamSrtConnectionPriority> = mutableListOf(
        SettingsStreamSrtConnectionPriority("Cellular"),
        SettingsStreamSrtConnectionPriority("WiFi")
    ),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("priorities", priorities)
    }

    fun clone(): SettingsStreamSrtConnectionPriorities {
        val new = SettingsStreamSrtConnectionPriorities()
        new.enabled = enabled
        new.priorities.clear()
        for (priority in priorities) {
            new.priorities.add(priority.clone())
        }
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtConnectionPriorities {
            val connectionPriorities = SettingsStreamSrtConnectionPriorities()
            connectionPriorities.enabled = container.decodeIfPresent<Boolean>("enabled")
                ?: throw SerializationException("Missing key 'enabled'")
            connectionPriorities.priorities = (
                container.decodeIfPresent(
                    "priorities",
                    ListSerializer(SettingsStreamSrtConnectionPriority.serializer()),
                ) ?: throw SerializationException("Missing key 'priorities'")
                ).toMutableList()
            return connectionPriorities
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtConnectionPriorities> by JsonObjectSerializer(
        "SettingsStreamSrtConnectionPriorities",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtAdaptiveBitrateAlgorithm.Serializer::class)
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

    fun encode(): JsonObject = JsonObject(mapOf(name to JsonObject(emptyMap())))

    companion object {
        fun fromRawValue(value: String): SettingsStreamSrtAdaptiveBitrateAlgorithm {
            return SettingsStreamSrtAdaptiveBitrateAlgorithm.entries.firstOrNull { it.name == value }
                ?: SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox
        }

        fun decode(container: JsonObject): SettingsStreamSrtAdaptiveBitrateAlgorithm {
            return if (container.containsKey("belabox")) {
                SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox
            } else if (container.containsKey("fastIrl")) {
                SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl
            } else if (container.containsKey("slowIrl")) {
                SettingsStreamSrtAdaptiveBitrateAlgorithm.slowIrl
            } else if (container.containsKey("customIrl")) {
                SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl
            } else {
                SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox
            }
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtAdaptiveBitrateAlgorithm> by JsonObjectSerializer(
        "SettingsStreamSrtAdaptiveBitrateAlgorithm",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtAdaptiveBitrateFastIrlSettings.Serializer::class)
class SettingsStreamSrtAdaptiveBitrateFastIrlSettings(
    var packetsInFlight: Int = 200,
    var minimumBitrate: Float = 250f,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("packetsInFlight", packetsInFlight)
        encode("minimumBitrate", minimumBitrate)
    }

    fun clone(): SettingsStreamSrtAdaptiveBitrateFastIrlSettings {
        val new = SettingsStreamSrtAdaptiveBitrateFastIrlSettings()
        new.packetsInFlight = packetsInFlight
        new.minimumBitrate = minimumBitrate
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtAdaptiveBitrateFastIrlSettings {
            val settings = SettingsStreamSrtAdaptiveBitrateFastIrlSettings()
            settings.packetsInFlight = container.decode("packetsInFlight", 200)
            settings.minimumBitrate = container.decode("minimumBitrate", 250f)
            return settings
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtAdaptiveBitrateFastIrlSettings> by JsonObjectSerializer(
        "SettingsStreamSrtAdaptiveBitrateFastIrlSettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtAdaptiveBitrateCustomSettings.Serializer::class)
class SettingsStreamSrtAdaptiveBitrateCustomSettings(
    var packetsInFlight: Int = 200,
    var pifDiffIncreaseFactor: Float = 100f,
    var rttDiffHighDecreaseFactor: Float = 0.9f,
    var rttDiffHighAllowedSpike: Float = 50f,
    var rttDiffHighMinimumDecrease: Float = 250f,
    var minimumBitrate: Float = 250f,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("packetsInFlight", packetsInFlight)
        encode("pifDiffIncreaseFactor", pifDiffIncreaseFactor)
        encode("rttDiffHighDecreaseFactor", rttDiffHighDecreaseFactor)
        encode("rttDiffHighAllowedSpike", rttDiffHighAllowedSpike)
        encode("rttDiffHighMinimumDecrease", rttDiffHighMinimumDecrease)
        encode("minimumBitrate", minimumBitrate)
    }

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

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtAdaptiveBitrateCustomSettings {
            val settings = SettingsStreamSrtAdaptiveBitrateCustomSettings()
            settings.packetsInFlight = container.decode("packetsInFlight", 200)
            settings.pifDiffIncreaseFactor = container.decode("pifDiffIncreaseFactor", 100f)
            settings.rttDiffHighDecreaseFactor = container.decode("rttDiffHighDecreaseFactor", 0.9f)
            settings.rttDiffHighAllowedSpike = container.decode("rttDiffHighAllowedSpike", 50f)
            settings.rttDiffHighMinimumDecrease = container.decode("rttDiffHighMinimumDecrease", 250f)
            settings.minimumBitrate = container.decode("minimumBitrate", 250f)
            return settings
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtAdaptiveBitrateCustomSettings> by JsonObjectSerializer(
        "SettingsStreamSrtAdaptiveBitrateCustomSettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtAdaptiveBitrateBelaboxSettings.Serializer::class)
class SettingsStreamSrtAdaptiveBitrateBelaboxSettings(
    var minimumBitrate: Float = 250f,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("minimumBitrate", minimumBitrate)
    }

    fun clone(): SettingsStreamSrtAdaptiveBitrateBelaboxSettings {
        val new = SettingsStreamSrtAdaptiveBitrateBelaboxSettings()
        new.minimumBitrate = minimumBitrate
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtAdaptiveBitrateBelaboxSettings {
            val settings = SettingsStreamSrtAdaptiveBitrateBelaboxSettings()
            settings.minimumBitrate = container.decode("minimumBitrate", 250f)
            return settings
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtAdaptiveBitrateBelaboxSettings> by JsonObjectSerializer(
        "SettingsStreamSrtAdaptiveBitrateBelaboxSettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrtAdaptiveBitrate.Serializer::class)
class SettingsStreamSrtAdaptiveBitrate(
    algorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm =
        SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox,
    var fastIrlSettings: SettingsStreamSrtAdaptiveBitrateFastIrlSettings =
        SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),
    var customSettings: SettingsStreamSrtAdaptiveBitrateCustomSettings =
        SettingsStreamSrtAdaptiveBitrateCustomSettings(),
    var belaboxSettings: SettingsStreamSrtAdaptiveBitrateBelaboxSettings =
        SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
) {
    var algorithm: SettingsStreamSrtAdaptiveBitrateAlgorithm by Published(algorithm)

    fun encode(): JsonObject = encodeContainer {
        encode("algorithm", algorithm)
        encode("fastIrlSettings", fastIrlSettings)
        encode("customSettings", customSettings)
        encode("belaboxSettings", belaboxSettings)
    }

    fun clone(): SettingsStreamSrtAdaptiveBitrate {
        val new = SettingsStreamSrtAdaptiveBitrate()
        new.algorithm = algorithm
        new.fastIrlSettings = fastIrlSettings.clone()
        new.customSettings = customSettings.clone()
        new.belaboxSettings = belaboxSettings.clone()
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrtAdaptiveBitrate {
            val adaptiveBitrate = SettingsStreamSrtAdaptiveBitrate()
            adaptiveBitrate.algorithm = container.decode("algorithm", SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox)
            adaptiveBitrate.fastIrlSettings = container.decode(
                "fastIrlSettings",
                SettingsStreamSrtAdaptiveBitrateFastIrlSettings.serializer(),
                SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),
            )
            adaptiveBitrate.customSettings = container.decode(
                "customSettings",
                SettingsStreamSrtAdaptiveBitrateCustomSettings.serializer(),
                SettingsStreamSrtAdaptiveBitrateCustomSettings(),
            )
            adaptiveBitrate.belaboxSettings = container.decode(
                "belaboxSettings",
                SettingsStreamSrtAdaptiveBitrateBelaboxSettings.serializer(),
                SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
            )
            return adaptiveBitrate
        }
    }

    object Serializer : KSerializer<SettingsStreamSrtAdaptiveBitrate> by JsonObjectSerializer(
        "SettingsStreamSrtAdaptiveBitrate",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamSrt.Serializer::class)
class SettingsStreamSrt(
    latency: Int = defaultSrtLatency,
    maximumBandwidthFollowInput: Boolean = true,
    var overheadBandwidth: Int = 25,
    adaptiveBitrateEnabled: Boolean = true,
    var adaptiveBitrate: SettingsStreamSrtAdaptiveBitrate = SettingsStreamSrtAdaptiveBitrate(),
    var connectionPriorities: SettingsStreamSrtConnectionPriorities =
        SettingsStreamSrtConnectionPriorities(),
    var mpegtsPacketsPerPacketRemove: Int = 7,
    dnsLookupStrategy: SettingsDnsLookupStrategy = SettingsDnsLookupStrategy.system,
    implementation: SettingsStreamSrtImplementation = SettingsStreamSrtImplementation.moblin,
    bigPackets: Boolean = true,
    var bigPacketsMigrated: Boolean = false,
    var implemenationMigrated: Boolean = false,
) {
    var latency: Int by Published(latency)
    var maximumBandwidthFollowInput: Boolean by Published(maximumBandwidthFollowInput)
    var adaptiveBitrateEnabled: Boolean by Published(adaptiveBitrateEnabled)
    var dnsLookupStrategy: SettingsDnsLookupStrategy by Published(dnsLookupStrategy)
    var implementation: SettingsStreamSrtImplementation by Published(implementation)
    var bigPackets: Boolean by Published(bigPackets)

    fun encode(): JsonObject = encodeContainer {
        encode("latency", latency)
        encode("maximumBandwidthFollowInput", maximumBandwidthFollowInput)
        encode("overheadBandwidth", overheadBandwidth)
        encode("adaptiveBitrateEnabled", adaptiveBitrateEnabled)
        encode("adaptiveBitrate", adaptiveBitrate)
        encode("connectionPriorities", connectionPriorities)
        encode("mpegtsPacketsPerPacket", mpegtsPacketsPerPacketRemove)
        encode("dnsLookupStrategy", dnsLookupStrategy)
        encode("implementation", implementation)
        encode("bigPackets", bigPackets)
        encode("bigPacketsMigrated", bigPacketsMigrated)
        encode("implemenationMigrated", implemenationMigrated)
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

    companion object {
        fun decode(container: JsonObject): SettingsStreamSrt {
            val srt = SettingsStreamSrt()
            srt.latency = container.decode("latency", defaultSrtLatency)
            srt.maximumBandwidthFollowInput = container.decode("maximumBandwidthFollowInput", true)
            srt.overheadBandwidth = container.decode("overheadBandwidth", 25)
            srt.adaptiveBitrateEnabled = container.decode("adaptiveBitrateEnabled", true)
            srt.adaptiveBitrate = container.decode(
                "adaptiveBitrate",
                SettingsStreamSrtAdaptiveBitrate.serializer(),
                SettingsStreamSrtAdaptiveBitrate(),
            )
            srt.connectionPriorities = container.decode(
                "connectionPriorities",
                SettingsStreamSrtConnectionPriorities.serializer(),
                SettingsStreamSrtConnectionPriorities(),
            )
            srt.mpegtsPacketsPerPacketRemove = container.decode("mpegtsPacketsPerPacket", 7)
            srt.dnsLookupStrategy = container.decode("dnsLookupStrategy", SettingsDnsLookupStrategy.system)
            srt.implementation = container.decode("implementation", SettingsStreamSrtImplementation.moblin)
            srt.bigPackets = container.decode("bigPackets", true)
            srt.bigPacketsMigrated = container.decode("bigPacketsMigrated", false)
            if (!srt.bigPacketsMigrated) {
                srt.bigPackets = srt.mpegtsPacketsPerPacketRemove == 7
                srt.bigPacketsMigrated = true
            }
            srt.implemenationMigrated = container.decode("implemenationMigrated", false)
            if (!srt.implemenationMigrated) {
                if (srt.latency < 1000) {
                    srt.implementation = SettingsStreamSrtImplementation.official
                }
                srt.implemenationMigrated = true
            }
            return srt
        }
    }

    object Serializer : KSerializer<SettingsStreamSrt> by JsonObjectSerializer(
        "SettingsStreamSrt",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamRtmp.Serializer::class)
class SettingsStreamRtmp(
    var adaptiveBitrateEnabled: Boolean = true,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("adaptiveBitrateEnabled", adaptiveBitrateEnabled)
    }

    fun clone(): SettingsStreamRtmp {
        val new = SettingsStreamRtmp()
        new.adaptiveBitrateEnabled = adaptiveBitrateEnabled
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamRtmp {
            val rtmp = SettingsStreamRtmp()
            rtmp.adaptiveBitrateEnabled = container.decodeIfPresent<Boolean>("adaptiveBitrateEnabled")
                ?: throw SerializationException("Missing key 'adaptiveBitrateEnabled'")
            return rtmp
        }
    }

    object Serializer : KSerializer<SettingsStreamRtmp> by JsonObjectSerializer(
        "SettingsStreamRtmp",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamRist.Serializer::class)
class SettingsStreamRist(
    var adaptiveBitrateEnabled: Boolean = true,
    var bonding: Boolean = true,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("adaptiveBitrateEnabled", adaptiveBitrateEnabled)
        encode("bonding", bonding)
    }

    fun clone(): SettingsStreamRist {
        val new = SettingsStreamRist()
        new.adaptiveBitrateEnabled = adaptiveBitrateEnabled
        new.bonding = bonding
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamRist {
            val rist = SettingsStreamRist()
            rist.adaptiveBitrateEnabled = container.decodeIfPresent<Boolean>("adaptiveBitrateEnabled")
                ?: throw SerializationException("Missing key 'adaptiveBitrateEnabled'")
            rist.bonding = container.decodeIfPresent<Boolean>("bonding")
                ?: throw SerializationException("Missing key 'bonding'")
            return rist
        }
    }

    object Serializer : KSerializer<SettingsStreamRist> by JsonObjectSerializer(
        "SettingsStreamRist",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsHttpHeader.Serializer::class)
data class SettingsHttpHeader(
    val name: String = "",
    val value: String = "",
) {
    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("value", value)
    }

    companion object {
        fun decode(container: JsonObject): SettingsHttpHeader {
            return SettingsHttpHeader(
                name = container.decodeIfPresent<String>("name")
                    ?: throw SerializationException("Missing key 'name'"),
                value = container.decodeIfPresent<String>("value")
                    ?: throw SerializationException("Missing key 'value'"),
            )
        }
    }

    object Serializer : KSerializer<SettingsHttpHeader> by JsonObjectSerializer(
        "SettingsHttpHeader",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamWhipHttpTransport.Serializer::class)
enum class SettingsStreamWhipHttpTransport {
    standard,
    remoteControl;

    override fun toString(): String {
        return when (this) {
            SettingsStreamWhipHttpTransport.standard -> localized("Standard")
            SettingsStreamWhipHttpTransport.remoteControl -> localized("Remote control")
        }
    }

    fun encode(): JsonObject = JsonObject(mapOf(name to JsonObject(emptyMap())))

    companion object {
        fun decode(container: JsonObject): SettingsStreamWhipHttpTransport {
            val cases = container.keys.mapNotNull { key ->
                SettingsStreamWhipHttpTransport.entries.firstOrNull { it.name == key }
            }
            if (cases.size != 1) {
                throw SerializationException("Expected exactly one case")
            }
            val case = cases[0]
            if (container[case.name] !is JsonObject) {
                throw SerializationException("Expected an object for case '${case.name}'")
            }
            return case
        }
    }

    object Serializer : KSerializer<SettingsStreamWhipHttpTransport> by JsonObjectSerializer(
        "SettingsStreamWhipHttpTransport",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamWhip.Serializer::class)
class SettingsStreamWhip(
    headers: MutableList<SettingsHttpHeader> = mutableListOf(),
    httpTransport: SettingsStreamWhipHttpTransport = SettingsStreamWhipHttpTransport.standard,
) {
    var headers: MutableList<SettingsHttpHeader> by PublishedList(headers)
    var httpTransport: SettingsStreamWhipHttpTransport by Published(httpTransport)

    fun encode(): JsonObject = encodeContainer {
        encode("headers", headers)
        encode("httpTransport", httpTransport)
    }

    fun clone(): SettingsStreamWhip {
        val new = SettingsStreamWhip()
        new.headers = headers.map { it.copy() }.toMutableList()
        new.httpTransport = httpTransport
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamWhip {
            val whip = SettingsStreamWhip()
            whip.headers = container.decode(
                "headers",
                ListSerializer(SettingsHttpHeader.serializer()),
                emptyList(),
            ).toMutableList()
            whip.httpTransport = container.decode("httpTransport", SettingsStreamWhipHttpTransport.standard)
            return whip
        }
    }

    object Serializer : KSerializer<SettingsStreamWhip> by JsonObjectSerializer(
        "SettingsStreamWhip",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamChat.Serializer::class)
class SettingsStreamChat(
    var bttvEmotes: Boolean = false,
    var ffzEmotes: Boolean = false,
    var seventvEmotes: Boolean = false,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("bttvEmotes", bttvEmotes)
        encode("ffzEmotes", ffzEmotes)
        encode("seventvEmotes", seventvEmotes)
    }

    fun clone(): SettingsStreamChat {
        val new = SettingsStreamChat()
        new.bttvEmotes = bttvEmotes
        new.ffzEmotes = ffzEmotes
        new.seventvEmotes = seventvEmotes
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamChat {
            val chat = SettingsStreamChat()
            chat.bttvEmotes = container.decodeIfPresent<Boolean>("bttvEmotes")
                ?: throw SerializationException("Missing key 'bttvEmotes'")
            chat.ffzEmotes = container.decodeIfPresent<Boolean>("ffzEmotes")
                ?: throw SerializationException("Missing key 'ffzEmotes'")
            chat.seventvEmotes = container.decodeIfPresent<Boolean>("seventvEmotes")
                ?: throw SerializationException("Missing key 'seventvEmotes'")
            return chat
        }
    }

    object Serializer : KSerializer<SettingsStreamChat> by JsonObjectSerializer(
        "SettingsStreamChat",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamRecording.Serializer::class)
class SettingsStreamRecording(
    overrideStream: Boolean = false,
    resolution: SettingsStreamResolution = SettingsStream.defaultResolution,
    fps: Int = SettingsStream.defaultFps,
    videoCodec: SettingsStreamCodec = SettingsStreamCodec.h265hevc,
    videoBitrate: Int = 0,
    maxKeyFrameInterval: Int = 0,
    audioBitrate: Int = 128_000,
    autoStartRecording: Boolean = false,
    autoStopRecording: Boolean = false,
    cleanRecordings: Boolean = false,
    cleanSnapshots: Boolean = false,
    recordingPath: ByteArray? = null,
) {
    var overrideStream: Boolean by Published(overrideStream)
    var resolution: SettingsStreamResolution by Published(resolution)
    var fps: Int by Published(fps)
    var videoCodec: SettingsStreamCodec by Published(videoCodec)
    var videoBitrate: Int by Published(videoBitrate)
    var maxKeyFrameInterval: Int by Published(maxKeyFrameInterval)
    var audioBitrate: Int by Published(audioBitrate)
    var autoStartRecording: Boolean by Published(autoStartRecording)
    var autoStopRecording: Boolean by Published(autoStopRecording)
    var cleanRecordings: Boolean by Published(cleanRecordings)
    var cleanSnapshots: Boolean by Published(cleanSnapshots)
    var recordingPath: ByteArray? by Published(recordingPath)

    fun encode(): JsonObject = encodeContainer {
        encode("overrideStream", overrideStream)
        encode("resolution", resolution)
        encode("fps", fps)
        encode("videoCodec", videoCodec)
        encode("videoBitrate", videoBitrate.toUInt())
        encode("maxKeyFrameInterval", maxKeyFrameInterval)
        encode("audioBitrate", audioBitrate.toUInt())
        encode("autoStartRecording", autoStartRecording)
        encode("autoStopRecording", autoStopRecording)
        encode("cleanRecordings", cleanRecordings)
        encode("cleanSnapshots", cleanSnapshots)
        encode("recordingPath", recordingPath, DataSerializer.nullable)
    }

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

    companion object {
        fun decode(container: JsonObject): SettingsStreamRecording {
            val recording = SettingsStreamRecording()
            recording.overrideStream = container.decode("overrideStream", false)
            recording.resolution = container.decode("resolution", SettingsStream.defaultResolution)
            recording.fps = container.decode("fps", SettingsStream.defaultFps)
            recording.videoCodec = container.decode("videoCodec", SettingsStreamCodec.h265hevc)
            recording.videoBitrate = container.decode<UInt>("videoBitrate", 0u).toInt()
            recording.maxKeyFrameInterval = container.decode("maxKeyFrameInterval", 0)
            recording.audioBitrate = container.decode<UInt>("audioBitrate", 128_000u).toInt()
            recording.autoStartRecording = container.decode("autoStartRecording", false)
            recording.autoStopRecording = container.decode("autoStopRecording", false)
            recording.cleanRecordings = container.decode("cleanRecordings", false)
            recording.cleanSnapshots = container.decode("cleanSnapshots", false)
            recording.recordingPath = container.decode("recordingPath", DataSerializer.nullable, null)
            return recording
        }
    }

    object Serializer : KSerializer<SettingsStreamRecording> by JsonObjectSerializer(
        "SettingsStreamRecording",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamPreviewStream.Serializer::class)
class SettingsStreamPreviewStream(
    url: String = "",
    resolution: SettingsStreamResolution = SettingsStreamResolution.r640x360,
    bitrate: Int = 500_000,
) {
    var url: String by Published(url)
    var resolution: SettingsStreamResolution by Published(resolution)
    var bitrate: Int by Published(bitrate)

    fun encode(): JsonObject = encodeContainer {
        encode("url", url)
        encode("resolution", resolution)
        encode("bitrate", bitrate.toUInt())
    }

    fun clone(): SettingsStreamPreviewStream {
        val new = SettingsStreamPreviewStream()
        new.url = url
        new.resolution = resolution
        new.bitrate = bitrate
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamPreviewStream {
            val previewStream = SettingsStreamPreviewStream()
            previewStream.url = container.decode("url", "")
            previewStream.resolution = container.decode("resolution", SettingsStreamResolution.r640x360)
            previewStream.bitrate = container.decode<UInt>("bitrate", 500_000u).toInt()
            return previewStream
        }
    }

    object Serializer : KSerializer<SettingsStreamPreviewStream> by JsonObjectSerializer(
        "SettingsStreamPreviewStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamReplayTransitionType.Serializer::class)
enum class SettingsStreamReplayTransitionType(val rawValue: String) {
    fade("fade"),
    stingers("stingers"),
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

    object Serializer : KSerializer<SettingsStreamReplayTransitionType> by settingsStreamRawValueSerializer(
        "com.moblin.android.various.settings.SettingsStreamReplayTransitionType",
        SettingsStreamReplayTransitionType.entries,
        { it.rawValue },
        SettingsStreamReplayTransitionType.fade,
    )
}

@Serializable(with = SettingsStreamReplayStinger.Serializer::class)
data class SettingsStreamReplayStinger(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val transitionPoint: Double = 0.5,
) {
    fun makeFilename(): String? {
        val path = name.substringBefore('#').substringBefore('?').substringAfterLast('/')
        val fileExtension = path.substringAfterLast('.', "")
        return "$id.$fileExtension"
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("transitionPoint", transitionPoint)
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamReplayStinger {
            return SettingsStreamReplayStinger(
                id = container.decodeIfPresent<UUID>("id")
                    ?: throw SerializationException("Missing key 'id'"),
                name = container.decodeIfPresent<String>("name")
                    ?: throw SerializationException("Missing key 'name'"),
                transitionPoint = container.decodeIfPresent<Double>("transitionPoint")
                    ?: throw SerializationException("Missing key 'transitionPoint'"),
            )
        }
    }

    object Serializer : KSerializer<SettingsStreamReplayStinger> by JsonObjectSerializer(
        "SettingsStreamReplayStinger",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamReplay.Serializer::class)
class SettingsStreamReplay(
    enabled: Boolean = false,
    transitionType: SettingsStreamReplayTransitionType = SettingsStreamReplayTransitionType.fade,
    inStinger: SettingsStreamReplayStinger = SettingsStreamReplayStinger(),
    outStinger: SettingsStreamReplayStinger = SettingsStreamReplayStinger(),
    postTriggerDelay: Int = 3,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var size: Double = 100.0,
    var alignment: SettingsAlignment = SettingsAlignment.topLeft,
    var positioningLock: Boolean = false,
    var enterForegroundCountAtLatestUsage: Int? = null,
    var fade: Boolean? = null,
) {
    var enabled: Boolean by Published(enabled)
    var transitionType: SettingsStreamReplayTransitionType by Published(transitionType)
    var inStinger: SettingsStreamReplayStinger by Published(inStinger)
    var outStinger: SettingsStreamReplayStinger by Published(outStinger)
    var postTriggerDelay: Int by Published(postTriggerDelay)
    var layout: SettingsWidgetLayout by Published(SettingsWidgetLayout())

    init {
        if (fade != null) {
            this.transitionType = if (fade == true) {
                SettingsStreamReplayTransitionType.fade
            } else {
                SettingsStreamReplayTransitionType.none
            }
        }
        layout = SettingsWidgetLayout(
            x = x,
            xString = x.toString(),
            y = y,
            yString = y.toString(),
            size = size,
            sizeString = size.toString(),
            alignment = alignment,
            positioningLock = positioningLock,
        )
    }

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("transitionType", transitionType)
        encode("inStinger", inStinger)
        encode("outStinger", outStinger)
        encode("postTriggerDelay", postTriggerDelay)
        encode("x", layout.x)
        encode("y", layout.y)
        encode("size", layout.size)
        encode("alignment", layout.alignment)
        encode("positioningLock", layout.positioningLock)
        encode("enterForegroundCountAtLatestUsage", enterForegroundCountAtLatestUsage)
    }

    fun clone(): SettingsStreamReplay {
        val new = SettingsStreamReplay()
        new.enabled = enabled
        new.transitionType = transitionType
        new.inStinger = inStinger.copy()
        new.outStinger = outStinger.copy()
        new.postTriggerDelay = postTriggerDelay
        new.layout = SettingsWidgetLayout(
            x = layout.x,
            xString = layout.xString,
            y = layout.y,
            yString = layout.yString,
            size = layout.size,
            sizeString = layout.sizeString,
            alignment = layout.alignment,
            positioningLock = layout.positioningLock,
        )
        new.x = x
        new.y = y
        new.size = size
        new.alignment = alignment
        new.positioningLock = positioningLock
        new.enterForegroundCountAtLatestUsage = enterForegroundCountAtLatestUsage
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamReplay {
            val replay = SettingsStreamReplay()
            replay.enabled = container.decode("enabled", false)
            val fade = container.decodeIfPresent<Boolean>("fade")
            if (fade != null) {
                if (fade) {
                    replay.transitionType = SettingsStreamReplayTransitionType.fade
                } else {
                    replay.transitionType = SettingsStreamReplayTransitionType.none
                }
            } else {
                replay.transitionType = container.decode("transitionType", SettingsStreamReplayTransitionType.fade)
            }
            replay.inStinger = container.decode(
                "inStinger",
                SettingsStreamReplayStinger.serializer(),
                SettingsStreamReplayStinger(),
            )
            replay.outStinger = container.decode(
                "outStinger",
                SettingsStreamReplayStinger.serializer(),
                SettingsStreamReplayStinger(),
            )
            replay.postTriggerDelay = container.decode("postTriggerDelay", 3)
            val x = container.decode("x", 0.0)
            val y = container.decode("y", 0.0)
            val size = container.decode("size", 100.0)
            replay.layout = SettingsWidgetLayout(
                x = x,
                xString = x.toString(),
                y = y,
                yString = y.toString(),
                size = size,
                sizeString = size.toString(),
                alignment = container.decode("alignment", SettingsAlignment.topLeft),
                positioningLock = container.decode("positioningLock", false),
            )
            replay.enterForegroundCountAtLatestUsage = container.decode<Int?>(
                "enterForegroundCountAtLatestUsage",
                null,
            )
            return replay
        }
    }

    object Serializer : KSerializer<SettingsStreamReplay> by JsonObjectSerializer(
        "SettingsStreamReplay",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamTwitchReward.Serializer::class)
class SettingsStreamTwitchReward(
    var id: UUID = UUID.randomUUID(),
    var rewardId: String = "",
    var title: String = "",
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("rewardId", rewardId)
        encode("title", title)
        encode("alert", alert)
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamTwitchReward {
            val reward = SettingsStreamTwitchReward()
            reward.id = container.decodeIfPresent<UUID>("id")
                ?: throw SerializationException("Missing key 'id'")
            reward.rewardId = container.decodeIfPresent<String>("rewardId")
                ?: throw SerializationException("Missing key 'rewardId'")
            reward.title = container.decodeIfPresent<String>("title")
                ?: throw SerializationException("Missing key 'title'")
            reward.alert = container.decodeIfPresent("alert", SettingsWidgetAlertsAlert.serializer())
                ?: throw SerializationException("Missing key 'alert'")
            return reward
        }
    }

    object Serializer : KSerializer<SettingsStreamTwitchReward> by JsonObjectSerializer(
        "SettingsStreamTwitchReward",
        { it.encode() },
        { decode(it) },
    )
}

const val maximumNumberOfTwitchRaidChannels = 10

@Serializable(with = SettingsStreamTwitchRaidChannel.Serializer::class)
class SettingsStreamTwitchRaidChannel(
    var channelId: String = "",
    var channelName: String = "",
    var timestamp: Instant = Instant.now(),
) {
    val id: String
        get() = channelId

    fun encode(): JsonObject = encodeContainer {
        encode("channelId", channelId)
        encode("channelName", channelName)
        encode("timestamp", timestamp)
    }

    fun clone(): SettingsStreamTwitchRaidChannel {
        val new = SettingsStreamTwitchRaidChannel(channelId, channelName)
        new.timestamp = timestamp
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamTwitchRaidChannel {
            val channel = SettingsStreamTwitchRaidChannel()
            channel.channelId = container.decode("channelId", "")
            channel.channelName = container.decode("channelName", "")
            channel.timestamp = container.decode("timestamp", Instant.now())
            return channel
        }
    }

    object Serializer : KSerializer<SettingsStreamTwitchRaidChannel> by JsonObjectSerializer(
        "SettingsStreamTwitchRaidChannel",
        { it.encode() },
        { decode(it) },
    )
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

@Serializable(with = SettingsStreamMultiStreamingDestination.Serializer::class)
class SettingsStreamMultiStreamingDestination(
    name: String = SettingsStreamMultiStreamingDestination.baseName,
    url: String = defaultRtmpStreamUrl,
    enabled: Boolean = false,
) : Named {
    override var name: String by Published(name)
    var url: String by Published(url)
    var enabled: Boolean by Published(enabled)
    var id: UUID = UUID.randomUUID()

    companion object {
        val baseName: String = localized("My destination")

        fun decode(container: JsonObject): SettingsStreamMultiStreamingDestination {
            val destination = SettingsStreamMultiStreamingDestination()
            destination.name = container.decode("name", baseName)
            destination.url = container.decode("url", defaultRtmpStreamUrl)
            destination.enabled = container.decode("enabled", false)
            return destination
        }
    }

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("url", url)
        encode("enabled", enabled)
    }

    fun clone(): SettingsStreamMultiStreamingDestination {
        val new = SettingsStreamMultiStreamingDestination()
        new.name = name
        new.url = url
        new.enabled = enabled
        return new
    }

    object Serializer : KSerializer<SettingsStreamMultiStreamingDestination> by JsonObjectSerializer(
        "SettingsStreamMultiStreamingDestination",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamMultiStreaming.Serializer::class)
class SettingsStreamMultiStreaming(
    destinations: MutableList<SettingsStreamMultiStreamingDestination> = mutableListOf(),
) {
    var destinations: MutableList<SettingsStreamMultiStreamingDestination> by PublishedList(destinations)

    fun encode(): JsonObject = encodeContainer {
        encode("destinations", destinations)
    }

    fun clone(): SettingsStreamMultiStreaming {
        val new = SettingsStreamMultiStreaming()
        for (destination in destinations) {
            new.destinations.add(destination.clone())
        }
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamMultiStreaming {
            val multiStreaming = SettingsStreamMultiStreaming()
            multiStreaming.destinations = container.decode(
                "destinations",
                ListSerializer(SettingsStreamMultiStreamingDestination.serializer()),
                emptyList(),
            ).toMutableList()
            return multiStreaming
        }
    }

    object Serializer : KSerializer<SettingsStreamMultiStreaming> by JsonObjectSerializer(
        "SettingsStreamMultiStreaming",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsTwitchAlerts.Serializer::class)
class SettingsTwitchAlerts(
    follows: Boolean = true,
    subscriptions: Boolean = true,
    giftSubscriptions: Boolean = true,
    resubscriptions: Boolean = true,
    rewards: Boolean = true,
    raids: Boolean = true,
    cheers: Boolean = true,
    minimumCheerBits: Int = 0,
    watchStreaks: Boolean = true,
    minimumWatchStreak: Int = 5,
    sharedChat: Boolean = false,
) {
    var follows: Boolean by Published(follows)
    var subscriptions: Boolean by Published(subscriptions)
    var giftSubscriptions: Boolean by Published(giftSubscriptions)
    var resubscriptions: Boolean by Published(resubscriptions)
    var rewards: Boolean by Published(rewards)
    var raids: Boolean by Published(raids)
    var cheers: Boolean by Published(cheers)
    var minimumCheerBits: Int by Published(minimumCheerBits)
    var watchStreaks: Boolean by Published(watchStreaks)
    var minimumWatchStreak: Int by Published(minimumWatchStreak)
    var sharedChat: Boolean by Published(sharedChat)

    fun encode(): JsonObject = encodeContainer {
        encode("follows", follows)
        encode("subscriptions", subscriptions)
        encode("giftSubscriptions", giftSubscriptions)
        encode("resubscriptions", resubscriptions)
        encode("rewards", rewards)
        encode("raids", raids)
        encode("cheers", cheers)
        encode("minimumCheerBits", minimumCheerBits)
        encode("watchStreaks", watchStreaks)
        encode("minimumWatchStreak", minimumWatchStreak)
        encode("sharedChat", sharedChat)
    }

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

    companion object {
        fun decode(container: JsonObject): SettingsTwitchAlerts {
            val alerts = SettingsTwitchAlerts()
            alerts.follows = container.decode("follows", true)
            alerts.subscriptions = container.decode("subscriptions", true)
            alerts.giftSubscriptions = container.decode("giftSubscriptions", true)
            alerts.resubscriptions = container.decode("resubscriptions", true)
            alerts.rewards = container.decode("rewards", true)
            alerts.raids = container.decode("raids", true)
            alerts.cheers = container.decode("cheers", true)
            alerts.minimumCheerBits = container.decode("minimumCheerBits", 0)
            alerts.watchStreaks = container.decode("watchStreaks", true)
            alerts.minimumWatchStreak = container.decode("minimumWatchStreak", 5)
            alerts.sharedChat = container.decode("sharedChat", false)
            return alerts
        }
    }

    object Serializer : KSerializer<SettingsTwitchAlerts> by JsonObjectSerializer(
        "SettingsTwitchAlerts",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsKickAlerts.Serializer::class)
class SettingsKickAlerts(
    subscriptions: Boolean = true,
    giftedSubscriptions: Boolean = true,
    rewards: Boolean = true,
    hosts: Boolean = true,
    bans: Boolean = true,
    kicks: Boolean = true,
    minimumKicks: Int = 0,
) {
    var subscriptions: Boolean by Published(subscriptions)
    var giftedSubscriptions: Boolean by Published(giftedSubscriptions)
    var rewards: Boolean by Published(rewards)
    var hosts: Boolean by Published(hosts)
    var bans: Boolean by Published(bans)
    var kicks: Boolean by Published(kicks)
    var minimumKicks: Int by Published(minimumKicks)

    fun encode(): JsonObject = encodeContainer {
        encode("subscriptions", subscriptions)
        encode("giftedSubscriptions", giftedSubscriptions)
        encode("rewards", rewards)
        encode("hosts", hosts)
        encode("bans", bans)
        encode("kicks", kicks)
        encode("minimumKicks", minimumKicks)
    }

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

    companion object {
        fun decode(container: JsonObject): SettingsKickAlerts {
            val alerts = SettingsKickAlerts()
            alerts.subscriptions = container.decode("subscriptions", true)
            alerts.giftedSubscriptions = container.decode("giftedSubscriptions", true)
            alerts.rewards = container.decode("rewards", true)
            alerts.hosts = container.decode("hosts", true)
            alerts.bans = container.decode("bans", true)
            alerts.kicks = container.decode("kicks", true)
            alerts.minimumKicks = container.decode("minimumKicks", 0)
            return alerts
        }
    }

    object Serializer : KSerializer<SettingsKickAlerts> by JsonObjectSerializer(
        "SettingsKickAlerts",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStream.Serializer::class)
class SettingsStream(
    name: String = "My stream",
    var id: UUID = UUID.randomUUID(),
    var enabled: Boolean = false,
    url: String = defaultStreamUrl,
    twitchChannelName: String = "",
    var twitchChannelId: String = "",
    var twitchShowFollows: Boolean? = null,
    var twitchChatAlerts: SettingsTwitchAlerts = SettingsTwitchAlerts(),
    var twitchToastAlerts: SettingsTwitchAlerts = SettingsTwitchAlerts(),
    var twitchAccessToken: String = "",
    var twitchLoggedIn: Boolean = false,
    var twitchWantsToBeLoggedIn: Boolean = false,
    var twitchNotLoggedInCount: Int = 0,
    var twitchRewards: MutableList<SettingsStreamTwitchReward> = mutableListOf(),
    twitchRaidsSent: MutableList<SettingsStreamTwitchRaidChannel> = mutableListOf(),
    twitchRaidsReceived: MutableList<SettingsStreamTwitchRaidChannel> = mutableListOf(),
    twitchSendMessagesTo: Boolean = true,
    kickChannelName: String = "",
    kickChannelId: String? = null,
    kickChatroomChannelId: String? = null,
    kickSlug: String? = null,
    var kickAccessToken: String = "",
    kickLoggedIn: Boolean = false,
    var kickWantsToBeLoggedIn: Boolean = false,
    var kickNotLoggedInCount: Int = 0,
    kickSendMessagesTo: Boolean = true,
    var kickChatAlerts: SettingsKickAlerts = SettingsKickAlerts(),
    var kickToastAlerts: SettingsKickAlerts = SettingsKickAlerts(),
    youTubeAuthState: com.moblin.android.platform.appauth.OIDAuthState? = null,
    var youTubeWantsToBeLoggedIn: Boolean = false,
    var youTubeNotLoggedInCount: Int = 0,
    youTubeVideoIds: String = "",
    youTubeHandle: String = "",
    youTubeScheduleStreamTitle: String = "",
    youTubeScheduleStreamVisibility: YouTubeApiLiveBroadcaseVisibility =
        YouTubeApiLiveBroadcaseVisibility.public,
    youTubeScheduleStreamAutoStop: Boolean = true,
    soopChannelName: String = "",
    var soopStreamId: String = "",
    var openStreamingPlatformUrl: String = "",
    var openStreamingPlatformChannelId: String = "",
    obsWebSocketEnabled: Boolean = false,
    var obsWebSocketUrl: String = "",
    var obsWebSocketPassword: String = "",
    obsSourceName: String = "",
    var obsMainScene: String = "",
    var obsBrbScene: String = "",
    var obsBrbSceneVideoSourceBroken: Boolean = false,
    var obsAutoStartStream: Boolean = false,
    var obsAutoStopStream: Boolean = false,
    var obsAutoStartRecording: Boolean = false,
    var obsAutoStopRecording: Boolean = false,
    streamingDirectlyToObs: Boolean = false,
    var discordSnapshotWebhook: String = "",
    var discordChatBotSnapshotWebhook: String = "",
    discordSnapshotWebhookOnlyWhenLive: Boolean = true,
    resolution: SettingsStreamResolution = SettingsStream.defaultResolution,
    fps: Int = SettingsStream.defaultFps,
    lowLightBoost: Boolean = false,
    bitrate: Int = 5_000_000,
    rateControl: SettingsStreamRateControl = SettingsStreamRateControl.abr,
    codec: SettingsStreamCodec = SettingsStreamCodec.h265hevc,
    h264Profile: SettingsStreamH264Profile = SettingsStreamH264Profile.main,
    bFrames: Boolean = false,
    adaptiveEncoderResolution: Boolean = false,
    adaptiveEncoderResolutionThreashold: Double = 1.0,
    var adaptiveBitrate: Boolean = true,
    var srt: SettingsStreamSrt = SettingsStreamSrt(),
    var rtmp: SettingsStreamRtmp = SettingsStreamRtmp(),
    var rist: SettingsStreamRist = SettingsStreamRist(),
    var whip: SettingsStreamWhip = SettingsStreamWhip(),
    maxKeyFrameInterval: Int = 2,
    audioCodec: SettingsStreamAudioCodec = SettingsStreamAudioCodec.aac,
    var audioBitrate: Int = 128_000,
    var chat: SettingsStreamChat = SettingsStreamChat(),
    var recording: SettingsStreamRecording = SettingsStreamRecording(),
    realtimeIrlEnabled: Boolean = false,
    realtimeIrlBaseUrl: String = SettingsStream.defaultRealtimeIrlBaseUrl,
    realtimeIrlPushKey: String = "",
    portrait: Boolean = false,
    backgroundStreaming: Boolean = false,
    backgroundStreamingPiP: Boolean = true,
    estimatedViewerDelay: Float = 8.0f,
    ntpPoolAddress: String = "time.apple.com",
    timecodesEnabled: Boolean = false,
    var replay: SettingsStreamReplay = SettingsStreamReplay(),
    goLiveNotificationDiscordMessage: String = "",
    goLiveNotificationDiscordWebhookUrl: String = "",
    goLiveNotificationMoblinWebsite: Boolean = false,
    multiStreaming: SettingsStreamMultiStreaming = SettingsStreamMultiStreaming(),
    var previewStream: SettingsStreamPreviewStream = SettingsStreamPreviewStream(),
    autoGoLive: Boolean = false,
) : Named {
    override var name: String by Published(name)
    var url: String by Published(url)
    var twitchChannelName: String by Published(twitchChannelName)
    var twitchRaidsSent: MutableList<SettingsStreamTwitchRaidChannel> by PublishedList(twitchRaidsSent)
    var twitchRaidsReceived: MutableList<SettingsStreamTwitchRaidChannel> by PublishedList(twitchRaidsReceived)
    var twitchSendMessagesTo: Boolean by Published(twitchSendMessagesTo)
    var kickChannelName: String by Published(kickChannelName)
    var kickChannelId: String? by Published(kickChannelId)
    var kickChatroomChannelId: String? by Published(kickChatroomChannelId)
    var kickSlug: String? by Published(kickSlug)
    var kickLoggedIn: Boolean by Published(kickLoggedIn)
    var kickSendMessagesTo: Boolean by Published(kickSendMessagesTo)
    var youTubeAuthState: com.moblin.android.platform.appauth.OIDAuthState? by Published(youTubeAuthState)
    var youTubeVideoIds: String by Published(youTubeVideoIds)
    var youTubeHandle: String by Published(youTubeHandle)
    var youTubeScheduleStreamTitle: String by Published(youTubeScheduleStreamTitle)
    var youTubeScheduleStreamVisibility: YouTubeApiLiveBroadcaseVisibility by Published(youTubeScheduleStreamVisibility)
    var youTubeScheduleStreamAutoStop: Boolean by Published(youTubeScheduleStreamAutoStop)
    var soopChannelName: String by Published(soopChannelName)
    var obsWebSocketEnabled: Boolean by Published(obsWebSocketEnabled)
    var obsSourceName: String by Published(obsSourceName)
    var streamingDirectlyToObs: Boolean by Published(streamingDirectlyToObs)
    var discordSnapshotWebhookOnlyWhenLive: Boolean by Published(discordSnapshotWebhookOnlyWhenLive)
    var resolution: SettingsStreamResolution by Published(resolution)
    var fps: Int by Published(fps)
    var lowLightBoost: Boolean by Published(lowLightBoost)
    var bitrate: Int by Published(bitrate)
    var rateControl: SettingsStreamRateControl by Published(rateControl)
    var codec: SettingsStreamCodec by Published(codec)
    var h264Profile: SettingsStreamH264Profile by Published(h264Profile)
    var bFrames: Boolean by Published(bFrames)
    var adaptiveEncoderResolution: Boolean by Published(adaptiveEncoderResolution)
    var adaptiveEncoderResolutionThreashold: Double by Published(adaptiveEncoderResolutionThreashold)
    var maxKeyFrameInterval: Int by Published(maxKeyFrameInterval)
    var audioCodec: SettingsStreamAudioCodec by Published(audioCodec)
    var realtimeIrlEnabled: Boolean by Published(realtimeIrlEnabled)
    var realtimeIrlBaseUrl: String by Published(realtimeIrlBaseUrl)
    var realtimeIrlPushKey: String by Published(realtimeIrlPushKey)
    var portrait: Boolean by Published(portrait)
    var backgroundStreaming: Boolean by Published(backgroundStreaming)
    var backgroundStreamingPiP: Boolean by Published(backgroundStreamingPiP)
    var estimatedViewerDelay: Float by Published(estimatedViewerDelay)
    var ntpPoolAddress: String by Published(ntpPoolAddress)
    var timecodesEnabled: Boolean by Published(timecodesEnabled)
    var goLiveNotificationDiscordMessage: String by Published(goLiveNotificationDiscordMessage)
    var goLiveNotificationDiscordWebhookUrl: String by Published(goLiveNotificationDiscordWebhookUrl)
    var goLiveNotificationMoblinWebsite: Boolean by Published(goLiveNotificationMoblinWebsite)
    var multiStreaming: SettingsStreamMultiStreaming by Published(multiStreaming)
    var autoGoLive: Boolean by Published(autoGoLive)

    companion object {
        val defaultRealtimeIrlBaseUrl: String = "https://rtirl.com/api"
        val defaultResolution: SettingsStreamResolution = SettingsStreamResolution.r1920x1080
        val defaultFps: Int = 30

        fun decode(container: JsonObject): SettingsStream {
            val stream = SettingsStream()
            stream.name = container.decode("name", "My stream")
            stream.id = container.decode("id", UUID.randomUUID())
            stream.enabled = container.decode("enabled", false)
            stream.url = container.decode("url", defaultStreamUrl)
            stream.twitchChannelName = container.decode("twitchChannelName", "")
            stream.twitchChannelId = container.decode("twitchChannelId", "")
            stream.twitchShowFollows = container.decode<Boolean?>("twitchShowFollows", null)
            stream.twitchAccessToken = container.decode("twitchAccessToken", "")
            stream.twitchLoggedIn = container.decode("twitchLoggedIn", false)
            stream.twitchWantsToBeLoggedIn = container.decode("twitchWantsToBeLoggedIn", stream.twitchLoggedIn)
            stream.twitchNotLoggedInCount = container.decode("twitchNotLoggedInCount", 0)
            stream.twitchRewards = container.decode(
                "twitchRewards",
                ListSerializer(SettingsStreamTwitchReward.serializer()),
                emptyList(),
            ).toMutableList()
            stream.twitchRaidsSent = container.decode(
                "twitchRaidsSent",
                ListSerializer(SettingsStreamTwitchRaidChannel.serializer()),
                emptyList(),
            ).toMutableList()
            stream.twitchRaidsReceived = container.decode(
                "twitchRaidsReceived",
                ListSerializer(SettingsStreamTwitchRaidChannel.serializer()),
                emptyList(),
            ).toMutableList()
            stream.twitchSendMessagesTo = container.decode("twitchSendMessagesTo", true)
            stream.twitchChatAlerts = container.decode(
                "twitchChatAlerts",
                SettingsTwitchAlerts.serializer(),
                SettingsTwitchAlerts(),
            )
            stream.twitchToastAlerts = container.decode(
                "twitchToastAlerts",
                SettingsTwitchAlerts.serializer(),
                SettingsTwitchAlerts(),
            )
            val twitchShowFollows = stream.twitchShowFollows
            if (twitchShowFollows != null) {
                stream.twitchChatAlerts.follows = twitchShowFollows
                stream.twitchToastAlerts.follows = twitchShowFollows
            }
            stream.twitchShowFollows = null
            stream.kickChannelName = container.decode("kickChannelName", "")
            stream.kickChannelId = container.decode<String?>("kickChannelId", null)
            stream.kickChatroomChannelId = container.decode<String?>("kickChatroomChannelId", null)
            stream.kickSlug = container.decode<String?>("kickSlug", null)
            stream.kickAccessToken = container.decode("kickAccessToken", "")
            if (stream.kickAccessToken.isNotEmpty()) {
                storeKickAccessTokenInKeychain(stream.id, stream.kickAccessToken)
                stream.kickAccessToken = ""
            }
            stream.kickLoggedIn = container.decode("kickLoggedIn", false)
            stream.kickWantsToBeLoggedIn = container.decode("kickWantsToBeLoggedIn", stream.kickLoggedIn)
            stream.kickNotLoggedInCount = container.decode("kickNotLoggedInCount", 0)
            stream.kickSendMessagesTo = container.decode("kickSendMessagesTo", true)
            stream.kickChatAlerts = container.decode(
                "kickChatAlerts",
                SettingsKickAlerts.serializer(),
                SettingsKickAlerts(),
            )
            stream.kickToastAlerts = container.decode(
                "kickToastAlerts",
                SettingsKickAlerts.serializer(),
                SettingsKickAlerts(),
            )
            val encoded = loadYouTubeAuthStateFromKeychain(stream.id)
            if (encoded != null) {
                stream.youTubeAuthState = stream.decodeYouTubeAuthState(
                    runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
                )
            }
            stream.youTubeVideoIds = container.decode("youTubeVideoId", "")
            stream.youTubeWantsToBeLoggedIn = container.decode(
                "youTubeWantsToBeLoggedIn",
                stream.youTubeAuthState != null,
            )
            stream.youTubeNotLoggedInCount = container.decode("youTubeNotLoggedInCount", 0)
            stream.youTubeHandle = container.decode("youTubeHandle", "")
            stream.youTubeScheduleStreamTitle = container.decode("youTubeScheduleStreamTitle", "")
            stream.youTubeScheduleStreamVisibility = container.decode(
                "youTubeScheduleStreamVisibility",
                YouTubeApiLiveBroadcaseVisibility.public,
            )
            stream.youTubeScheduleStreamAutoStop = container.decode("youTubeScheduleStreamAutoStop", true)
            stream.soopChannelName = container.decode("afreecaTvChannelName", "")
            stream.soopStreamId = container.decode("afreecaTvStreamId", "")
            stream.openStreamingPlatformUrl = container.decode("openStreamingPlatformUrl", "")
            stream.openStreamingPlatformChannelId = container.decode("openStreamingPlatformChannelId", "")
            stream.obsWebSocketEnabled = container.decode("obsWebSocketEnabled", false)
            stream.obsWebSocketUrl = container.decode("obsWebSocketUrl", "")
            stream.obsWebSocketPassword = container.decode("obsWebSocketPassword", "")
            stream.obsSourceName = container.decode("obsSourceName", "")
            stream.obsMainScene = container.decode("obsMainScene", "")
            stream.obsBrbScene = container.decode("obsBrbScene", "")
            stream.obsBrbSceneVideoSourceBroken = container.decode("obsBrbSceneVideoSourceBroken", false)
            stream.obsAutoStartStream = container.decode("obsAutoStartStream", false)
            stream.obsAutoStopStream = container.decode("obsAutoStopStream", false)
            stream.obsAutoStartRecording = container.decode("obsAutoStartRecording", false)
            stream.obsAutoStopRecording = container.decode("obsAutoStopRecording", false)
            stream.streamingDirectlyToObs = container.decode("streamingDirectlyToObs", false)
            stream.discordSnapshotWebhook = container.decode("discordSnapshotWebhook", "")
            stream.discordChatBotSnapshotWebhook = container.decode("discordChatBotSnapshotWebhook", "")
            stream.discordSnapshotWebhookOnlyWhenLive = container.decode(
                "discordSnapshotWebhookOnlyWhenLive",
                true,
            )
            stream.resolution = container.decode("resolution", defaultResolution)
            stream.fps = container.decode("fps", defaultFps)
            stream.lowLightBoost = container.decode("autoFps", false)
            stream.bitrate = container.decode<UInt>("bitrate", 5_000_000u).toInt()
            stream.rateControl = SettingsStreamRateControl.makeValid(
                container.decode("bitrateRateControl", SettingsStreamRateControl.abr)
            )
            stream.codec = container.decode("codec", SettingsStreamCodec.h265hevc)
            stream.h264Profile = container.decode("h264Profile", SettingsStreamH264Profile.main)
            stream.bFrames = container.decode("bFrames", false)
            stream.adaptiveEncoderResolution = container.decode("adaptiveEncoderResolution", false)
            stream.adaptiveEncoderResolutionThreashold = container.decode(
                "adaptiveEncoderResolutionThreashold",
                1.0,
            )
            stream.adaptiveBitrate = container.decode("adaptiveBitrate", true)
            stream.srt = container.decode("srt", SettingsStreamSrt.serializer(), SettingsStreamSrt())
            stream.rtmp = container.decode("rtmp", SettingsStreamRtmp.serializer(), SettingsStreamRtmp())
            stream.rist = container.decode("rist", SettingsStreamRist.serializer(), SettingsStreamRist())
            stream.whip = container.decode("whip", SettingsStreamWhip.serializer(), SettingsStreamWhip())
            stream.maxKeyFrameInterval = container.decode("maxKeyFrameInterval", 2)
            stream.audioCodec = container.decode("audioCodec", SettingsStreamAudioCodec.aac)
            stream.audioBitrate = container.decode("audioBitrate", 128_000)
            stream.chat = container.decode("chat", SettingsStreamChat.serializer(), SettingsStreamChat())
            stream.recording = container.decode(
                "recording",
                SettingsStreamRecording.serializer(),
                SettingsStreamRecording(),
            )
            stream.realtimeIrlEnabled = container.decode("realtimeIrlEnabled", false)
            stream.realtimeIrlBaseUrl = container.decode("realtimeIrlBaseUrl", defaultRealtimeIrlBaseUrl)
            stream.realtimeIrlPushKey = container.decode("realtimeIrlPushKey", "")
            stream.portrait = container.decode("portrait", false)
            stream.backgroundStreaming = container.decode("backgroundStreaming", false)
            stream.backgroundStreamingPiP = container.decode("backgroundStreamingPiP", true)
            stream.estimatedViewerDelay = container.decode("estimatedViewerDelay", 8.0f)
            stream.ntpPoolAddress = container.decode("ntpPoolAddress", "time.apple.com")
            stream.timecodesEnabled = container.decode("timecodesEnabled", false)
            stream.replay = container.decode("replay", SettingsStreamReplay.serializer(), SettingsStreamReplay())
            stream.goLiveNotificationDiscordMessage = container.decode(
                "goLiveNotificationDiscordMessage",
                "",
            )
            stream.goLiveNotificationDiscordWebhookUrl = container.decode(
                "goLiveNotificationDiscordWebhookUrl",
                "",
            )
            stream.goLiveNotificationMoblinWebsite = container.decode("goLiveNotificationMoblinWebsite", false)
            stream.multiStreaming = container.decode(
                "multiStreaming",
                SettingsStreamMultiStreaming.serializer(),
                SettingsStreamMultiStreaming(),
            )
            stream.previewStream = container.decode(
                "previewStream",
                SettingsStreamPreviewStream.serializer(),
                SettingsStreamPreviewStream(),
            )
            stream.autoGoLive = container.decode("autoGoLive", false)
            return stream
        }
    }

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("id", id)
        encode("enabled", enabled)
        encode("url", url)
        encode("twitchChannelName", twitchChannelName)
        encode("twitchChannelId", twitchChannelId)
        encode("twitchShowFollows", twitchShowFollows)
        encode("twitchAccessToken", twitchAccessToken)
        encode("twitchLoggedIn", twitchLoggedIn)
        encode("twitchWantsToBeLoggedIn", twitchWantsToBeLoggedIn)
        encode("twitchNotLoggedInCount", twitchNotLoggedInCount)
        encode("twitchRewards", twitchRewards)
        encode("twitchRaidsSent", twitchRaidsSent)
        encode("twitchRaidsReceived", twitchRaidsReceived)
        encode("twitchSendMessagesTo", twitchSendMessagesTo)
        encode("twitchChatAlerts", twitchChatAlerts)
        encode("twitchToastAlerts", twitchToastAlerts)
        encode("kickChannelName", kickChannelName)
        encode("kickChannelId", kickChannelId)
        encode("kickChatroomChannelId", kickChatroomChannelId)
        encode("kickSlug", kickSlug)
        encode("kickAccessToken", kickAccessToken)
        encode("kickLoggedIn", kickLoggedIn)
        encode("kickWantsToBeLoggedIn", kickWantsToBeLoggedIn)
        encode("kickNotLoggedInCount", kickNotLoggedInCount)
        encode("kickSendMessagesTo", kickSendMessagesTo)
        encode("kickChatAlerts", kickChatAlerts)
        encode("kickToastAlerts", kickToastAlerts)
        val encoded = encodeYouTubeAuthState()
        if (encoded != null) {
            storeYouTubeAuthStateInKeychain(id, Base64.getEncoder().encodeToString(encoded))
        }
        encode("youTubeVideoId", youTubeVideoIds)
        encode("youTubeWantsToBeLoggedIn", youTubeWantsToBeLoggedIn)
        encode("youTubeNotLoggedInCount", youTubeNotLoggedInCount)
        encode("youTubeHandle", youTubeHandle)
        encode("youTubeScheduleStreamTitle", youTubeScheduleStreamTitle)
        encode("youTubeScheduleStreamVisibility", youTubeScheduleStreamVisibility)
        encode("youTubeScheduleStreamAutoStop", youTubeScheduleStreamAutoStop)
        encode("afreecaTvChannelName", soopChannelName)
        encode("afreecaTvStreamId", soopStreamId)
        encode("openStreamingPlatformUrl", openStreamingPlatformUrl)
        encode("openStreamingPlatformChannelId", openStreamingPlatformChannelId)
        encode("obsWebSocketEnabled", obsWebSocketEnabled)
        encode("obsWebSocketUrl", obsWebSocketUrl)
        encode("obsWebSocketPassword", obsWebSocketPassword)
        encode("obsSourceName", obsSourceName)
        encode("obsMainScene", obsMainScene)
        encode("obsBrbScene", obsBrbScene)
        encode("obsBrbSceneVideoSourceBroken", obsBrbSceneVideoSourceBroken)
        encode("obsAutoStartStream", obsAutoStartStream)
        encode("obsAutoStopStream", obsAutoStopStream)
        encode("obsAutoStartRecording", obsAutoStartRecording)
        encode("obsAutoStopRecording", obsAutoStopRecording)
        encode("streamingDirectlyToObs", streamingDirectlyToObs)
        encode("discordSnapshotWebhook", discordSnapshotWebhook)
        encode("discordChatBotSnapshotWebhook", discordChatBotSnapshotWebhook)
        encode("discordSnapshotWebhookOnlyWhenLive", discordSnapshotWebhookOnlyWhenLive)
        encode("resolution", resolution)
        encode("fps", fps)
        encode("autoFps", lowLightBoost)
        encode("bitrate", bitrate.toUInt())
        encode("bitrateRateControl", rateControl)
        encode("codec", codec)
        encode("h264Profile", h264Profile)
        encode("bFrames", bFrames)
        encode("adaptiveEncoderResolution", adaptiveEncoderResolution)
        encode("adaptiveEncoderResolutionThreashold", adaptiveEncoderResolutionThreashold)
        encode("adaptiveBitrate", adaptiveBitrate)
        encode("srt", srt)
        encode("rtmp", rtmp)
        encode("rist", rist)
        encode("whip", whip)
        encode("maxKeyFrameInterval", maxKeyFrameInterval)
        encode("audioCodec", audioCodec)
        encode("audioBitrate", audioBitrate)
        encode("chat", chat)
        encode("recording", recording)
        encode("realtimeIrlEnabled", realtimeIrlEnabled)
        encode("realtimeIrlBaseUrl", realtimeIrlBaseUrl)
        encode("realtimeIrlPushKey", realtimeIrlPushKey)
        encode("portrait", portrait)
        encode("backgroundStreaming", backgroundStreaming)
        encode("backgroundStreamingPiP", backgroundStreamingPiP)
        encode("estimatedViewerDelay", estimatedViewerDelay)
        encode("ntpPoolAddress", ntpPoolAddress)
        encode("timecodesEnabled", timecodesEnabled)
        encode("replay", replay)
        encode("goLiveNotificationDiscordMessage", goLiveNotificationDiscordMessage)
        encode("goLiveNotificationDiscordWebhookUrl", goLiveNotificationDiscordWebhookUrl)
        encode("goLiveNotificationMoblinWebsite", goLiveNotificationMoblinWebsite)
        encode("multiStreaming", multiStreaming)
        encode("previewStream", previewStream)
        encode("autoGoLive", autoGoLive)
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
        return youTubeAuthState?.isAuthorized == true
    }

    private fun encodeYouTubeAuthState(): ByteArray? {
        return youTubeAuthState?.archivedData()
    }

    private fun decodeYouTubeAuthState(encoded: ByteArray?): com.moblin.android.platform.appauth.OIDAuthState? {
        if (encoded == null) {
            return null
        }
        return com.moblin.android.platform.appauth.OIDAuthState.unarchivedObject(from = encoded)
    }

    object Serializer : KSerializer<SettingsStream> by JsonObjectSerializer(
        "SettingsStream",
        { it.encode() },
        { decode(it) },
    )
}
