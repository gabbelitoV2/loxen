package com.moblin.android.various.settings

import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.mediaPlayerCamera
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.UUIDSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.streamingplatforms.kick.loadKickAccessTokenFromKeychain
import com.moblin.android.streamingplatforms.twitch.loadTwitchAccessTokenFromKeychain
import com.moblin.android.various.model.CameraId
import com.moblin.android.various.model.controlBarBackgroundImagePath
import com.moblin.android.various.model.defaultScoreboardSize
import com.moblin.android.various.model.faceBackgroundImagePath
import com.moblin.android.various.model.plainIcon
import com.moblin.android.various.model.stealthModeImagePath
import com.moblin.android.various.storages.SimpleStringStorage
import com.moblin.android.various.storages.alertsStorageDirectory
import com.moblin.android.various.storages.imagesStorageDirectory
import com.moblin.android.various.storages.mediaPlayerStorageDirectory
import com.moblin.android.various.storages.pngTuberStorageDirectory
import com.moblin.android.various.storages.replayTransitionsStorageDirectory
import com.moblin.android.various.storages.vTuberStorageDirectory
import com.moblin.android.various.utils.Named
import com.moblin.android.various.utils.bestBackCameraId
import com.moblin.android.various.utils.bestFrontCameraId
import com.moblin.android.various.utils.createAndGetDirectory
import com.moblin.android.various.utils.defaultBackCameraPosition
import com.moblin.android.various.utils.formatFilenameDateAndTime
import com.moblin.android.various.utils.hasUltraWideBackCamera
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPhone
import com.moblin.android.videoeffects.FaceEffectPrivacyMode
import com.moblin.android.videoeffects.FaceEffectSettings
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import com.moblin.android.platform.Cameras

val defaultStreamUrl = "srt://my_public_ip:4000"
val defaultRtmpStreamUrl = "rtmp://my_public_ip:1935/live/foobar"
val defaultQuickButtonColor = RgbColor(red = 255 / 4, green = 255 / 4, blue = 255 / 4)
val defaultStreamButtonColor = RgbColor(red = 255, green = 59, blue = 48)
val defaultSegmentedPickerSelectedColor =
    RgbColor(red = 142, green = 142, blue = 147, opacity = 0.6)
val defaultSrtLatency: Int = 3000
val minZoomX: Float = 0.5f

sealed class SettingsCameraId {
    data class Back(val id: CameraId) : SettingsCameraId()
    data class Front(val id: CameraId) : SettingsCameraId()
    data class Rtmp(val id: UUID) : SettingsCameraId()
    data class Srtla(val id: UUID) : SettingsCameraId()
    data class Srt(val id: UUID) : SettingsCameraId()
    data class Rist(val id: UUID) : SettingsCameraId()
    data class Rtsp(val id: UUID) : SettingsCameraId()
    data class Whip(val id: UUID) : SettingsCameraId()
    data class Whep(val id: UUID) : SettingsCameraId()
    data class MediaPlayer(val id: UUID) : SettingsCameraId()
    data class External(val id: CameraId, val name: String) : SettingsCameraId()
    data object ScreenCapture : SettingsCameraId()
    data object None : SettingsCameraId()
    data object BackTripleLowEnergy : SettingsCameraId()
    data object BackDualLowEnergy : SettingsCameraId()
    data object BackWideDualLowEnergy : SettingsCameraId()
}

@Serializable
enum class SettingsColorLutType(val rawValue: String) {
    bundled("bundled"),
    disk("disk"),
    diskCube("diskCube");

    companion object {
        fun fromRawValue(value: String): SettingsColorLutType? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsColorLut.Serializer::class)
class SettingsColorLut(
    var id: UUID = UUID.randomUUID(),
    var type: SettingsColorLutType = SettingsColorLutType.bundled,
    var name: String = "",
    var enabled: Boolean = false,
) {
    fun clone(): SettingsColorLut {
        val new = SettingsColorLut(type = type, name = name)
        new.id = id
        new.enabled = enabled
        return new
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("type", type)
        encode("name", name)
        encode("enabled", enabled)
    }

    companion object {
        fun decode(container: JsonObject): SettingsColorLut {
            val lut = SettingsColorLut()
            lut.id = container.decode("id", UUID.randomUUID())
            lut.type = container.decode("type", SettingsColorLutType.bundled)
            lut.name = container.decode("name", "")
            lut.enabled = container.decode("enabled", false)
            return lut
        }
    }

    object Serializer : KSerializer<SettingsColorLut> by JsonObjectSerializer(
        "SettingsColorLut",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsColorSpace(val rawValue: String) {
    @SerialName("Standard RGB")
    srgb("Standard RGB"),

    @SerialName("P3 D65")
    p3D65("P3 D65"),

    @SerialName("HLG BT2020")
    hlgBt2020("HLG BT2020"),

    @SerialName("Apple Log")
    appleLog("Apple Log");

    companion object {
        fun fromRawValue(value: String): SettingsColorSpace? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

private val allBundledLuts = listOf(
    SettingsColorLut(type = SettingsColorLutType.bundled, name = "Apple Log To Rec 709"),
    SettingsColorLut(type = SettingsColorLutType.bundled, name = "Moblin Meme"),
)

@Serializable(with = SettingsColor.Serializer::class)
class SettingsColor(
    var space: SettingsColorSpace = SettingsColorSpace.srgb,
    var lutEnabled: Boolean = true,
    var lut: UUID = UUID.randomUUID(),
    var bundledLuts: List<SettingsColorLut> = allBundledLuts,
    var diskLuts: List<SettingsColorLut> = emptyList(),
    var diskLutsPng: List<SettingsColorLut> = emptyList(),
    var diskLutsCube: List<SettingsColorLut> = emptyList(),
) {
    fun allLuts(): List<SettingsColorLut> {
        return bundledLuts + diskLutsCube + diskLutsPng
    }

    fun encode(): JsonObject = encodeContainer {
        encode("space", space)
        encode("lutEnabled", lutEnabled)
        encode("lut", lut)
        encode("bundledLuts", bundledLuts, ListSerializer(SettingsColorLut.serializer()))
        encode("diskLuts", diskLuts, ListSerializer(SettingsColorLut.serializer()))
        encode("diskLutsPng", diskLutsPng, ListSerializer(SettingsColorLut.serializer()))
        encode("diskLutsCube", diskLutsCube, ListSerializer(SettingsColorLut.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsColor {
            val color = SettingsColor()
            color.space = container.decode("space", SettingsColorSpace.srgb)
            color.lutEnabled = container.decode("lutEnabled", true)
            color.lut = container.decode("lut", UUID.randomUUID())
            color.bundledLuts = container.decode("bundledLuts", ListSerializer(SettingsColorLut.serializer()), emptyList())
            color.diskLuts = container.decode("diskLuts", ListSerializer(SettingsColorLut.serializer()), emptyList())
            color.diskLutsPng = container.decode("diskLutsPng", ListSerializer(SettingsColorLut.serializer()), emptyList())
            color.diskLutsCube = container.decode(
                "diskLutsCube",
                ListSerializer(SettingsColorLut.serializer()),
                emptyList(),
            )
            return color
        }
    }

    object Serializer : KSerializer<SettingsColor> by JsonObjectSerializer(
        "SettingsColor",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsShow.Serializer::class)
class SettingsShow(
    var chat: Boolean = true,
    var viewers: Boolean = true,
    var uptime: Boolean = true,
    var stream: Boolean = false,
    var speed: Boolean = true,
    var audioLevel: Boolean = true,
    var zoom: Boolean = false,
    var zoomPresets: Boolean = true,
    var microphone: Boolean = false,
    var audioBar: Boolean = true,
    var cameras: Boolean = false,
    var obsStatus: Boolean = true,
    var ingests: Boolean = true,
    var gameController: Boolean = true,
    var location: Boolean = false,
    var remoteControl: Boolean = true,
    var browserWidgets: Boolean = true,
    var bonding: Boolean = true,
    var events: Boolean = true,
    var djiDevices: Boolean = true,
    var bondingRtts: Boolean = false,
    var moblink: Boolean = true,
    var catPrinter: Boolean = true,
    var workoutDevice: Boolean = true,
    var systemMonitor: Boolean = false,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("chat", chat)
        encode("viewers", viewers)
        encode("uptime", uptime)
        encode("stream", stream)
        encode("speed", speed)
        encode("audioLevel", audioLevel)
        encode("zoom", zoom)
        encode("zoomPresets", zoomPresets)
        encode("microphone", microphone)
        encode("audioBar", audioBar)
        encode("cameras", cameras)
        encode("obsStatus", obsStatus)
        encode("rtmpSpeed", ingests)
        encode("gameController", gameController)
        encode("location", location)
        encode("remoteControl", remoteControl)
        encode("browserWidgets", browserWidgets)
        encode("bonding", bonding)
        encode("events", events)
        encode("djiDevices", djiDevices)
        encode("bondingRtts", bondingRtts)
        encode("moblink", moblink)
        encode("catPrinter", catPrinter)
        encode("heartRateDevice", workoutDevice)
        encode("cpu", systemMonitor)
    }

    companion object {
        fun decode(container: JsonObject): SettingsShow {
            val show = SettingsShow()
            show.chat = container.decode("chat", true)
            show.viewers = container.decode("viewers", true)
            show.uptime = container.decode("uptime", true)
            show.stream = container.decode("stream", false)
            show.speed = container.decode("speed", true)
            show.audioLevel = container.decode("audioLevel", true)
            show.zoom = container.decode("zoom", false)
            show.zoomPresets = container.decode("zoomPresets", true)
            show.microphone = container.decode("microphone", false)
            show.audioBar = container.decode("audioBar", true)
            show.cameras = container.decode("cameras", false)
            show.obsStatus = container.decode("obsStatus", true)
            show.ingests = container.decode("rtmpSpeed", true)
            show.gameController = container.decode("gameController", true)
            show.location = container.decode("location", false)
            show.remoteControl = container.decode("remoteControl", true)
            show.browserWidgets = container.decode("browserWidgets", true)
            show.bonding = container.decode("bonding", true)
            show.events = container.decode("events", true)
            show.djiDevices = container.decode("djiDevices", true)
            show.bondingRtts = container.decode("bondingRtts", false)
            show.moblink = container.decode("moblink", true)
            show.catPrinter = container.decode("catPrinter", true)
            show.workoutDevice = container.decode("heartRateDevice", true)
            show.systemMonitor = container.decode("cpu", false)
            return show
        }
    }

    object Serializer : KSerializer<SettingsShow> by JsonObjectSerializer(
        "SettingsShow",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsZoomPreset.Serializer::class)
class SettingsZoomPreset(
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var x: Float = 1.0f,
) {
    override fun equals(other: Any?): Boolean {
        return other is SettingsZoomPreset && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("x", x)
    }

    companion object {
        fun decode(container: JsonObject): SettingsZoomPreset {
            val preset = SettingsZoomPreset()
            preset.id = container.decode("id", UUID.randomUUID())
            preset.name = container.decode("name", "")
            preset.x = container.decode("x", 1.0f)
            return preset
        }
    }

    object Serializer : KSerializer<SettingsZoomPreset> by JsonObjectSerializer(
        "SettingsZoomPreset",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsZoomSwitchTo.Serializer::class)
class SettingsZoomSwitchTo(
    var level: Float = 1.0f,
    var x: Float = 1.0f,
    var enabled: Boolean = false,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("level", level)
        encode("x", x)
        encode("enabled", enabled)
    }

    companion object {
        fun decode(container: JsonObject): SettingsZoomSwitchTo {
            val switchTo = SettingsZoomSwitchTo()
            switchTo.level = container.decode("level", 1.0f)
            switchTo.x = container.decode("x", 1.0f)
            switchTo.enabled = container.decode("enabled", false)
            return switchTo
        }
    }

    object Serializer : KSerializer<SettingsZoomSwitchTo> by JsonObjectSerializer(
        "SettingsZoomSwitchTo",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsZoom.Serializer::class)
class SettingsZoom(
    var back: MutableList<SettingsZoomPreset> = mutableListOf(),
    var front: MutableList<SettingsZoomPreset> = mutableListOf(),
    var switchToBack: SettingsZoomSwitchTo = SettingsZoomSwitchTo(),
    var switchToFront: SettingsZoomSwitchTo = SettingsZoomSwitchTo(),
    var speed: Float = 5.0f,
    var backgroundColor: RgbColor = defaultSegmentedPickerSelectedColor,
) {
    var backgroundColorColor: Color = backgroundColor.color()

    fun encode(): JsonObject = encodeContainer {
        encode("back", back, ListSerializer(SettingsZoomPreset.serializer()))
        encode("front", front, ListSerializer(SettingsZoomPreset.serializer()))
        encode("switchToBack", switchToBack, SettingsZoomSwitchTo.serializer())
        encode("switchToFront", switchToFront, SettingsZoomSwitchTo.serializer())
        encode("speed", speed)
        encode("backgroundColor", backgroundColor)
    }

    companion object {
        fun decode(container: JsonObject): SettingsZoom {
            val zoom = SettingsZoom()
            zoom.back = container.decode("back", ListSerializer(SettingsZoomPreset.serializer()), emptyList())
                .toMutableList()
            zoom.front = container.decode("front", ListSerializer(SettingsZoomPreset.serializer()), emptyList())
                .toMutableList()
            zoom.switchToBack = container.decode(
                "switchToBack",
                SettingsZoomSwitchTo.serializer(),
                SettingsZoomSwitchTo(),
            )
            zoom.switchToFront = container.decode(
                "switchToFront",
                SettingsZoomSwitchTo.serializer(),
                SettingsZoomSwitchTo(),
            )
            zoom.speed = container.decode("speed", 5.0f)
            zoom.backgroundColor = container.decode(
                "backgroundColor",
                defaultSegmentedPickerSelectedColor,
            )
            zoom.backgroundColorColor = zoom.backgroundColor.color()
            return zoom
        }
    }

    object Serializer : KSerializer<SettingsZoom> by JsonObjectSerializer(
        "SettingsZoom",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsBitratePreset.Serializer::class)
class SettingsBitratePreset(
    var id: UUID = UUID.randomUUID(),
    var bitrate: Int = 5_000_000,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("bitrate", bitrate)
    }

    companion object {
        fun decode(container: JsonObject): SettingsBitratePreset {
            val preset = SettingsBitratePreset()
            preset.id = container.decode("id", UUID.randomUUID())
            preset.bitrate = container.decode("bitrate", 5_000_000) { it >= 0 }
            return preset
        }
    }

    object Serializer : KSerializer<SettingsBitratePreset> by JsonObjectSerializer(
        "SettingsBitratePreset",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsVideoStabilizationMode(val rawValue: String) {
    @SerialName("Off")
    off("Off"),

    @SerialName("Standard")
    standard("Standard"),

    @SerialName("Cinematic")
    cinematic("Cinematic"),

    @SerialName("Cinematic extended enhanced")
    cinematicExtendedEnhanced("Cinematic extended enhanced");

    override fun toString(): String {
        return when (this) {
            off -> localized("Off")
            standard -> localized("Standard")
            cinematic -> localized("Cinematic")
            cinematicExtendedEnhanced -> localized("Cinematic extended enhanced")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsVideoStabilizationMode? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

val videoStabilizationModes = SettingsVideoStabilizationMode.entries.toList()

@Serializable(with = SettingsTesla.Serializer::class)
class SettingsTesla(
    var vin: String = "",
    var privateKey: String = "",
    var enabled: Boolean = true,
    var bluetoothPeripheralName: String? = null,
    var bluetoothPeripheralId: UUID? = null,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("vin", vin)
        encode("privateKey", privateKey)
        encode("enabled", enabled)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsTesla {
            val tesla = SettingsTesla()
            tesla.vin = container.decode("vin", "")
            tesla.privateKey = container.decode("privateKey", "")
            tesla.enabled = container.decode("enabled", true)
            tesla.bluetoothPeripheralName = container.decode<String?>("bluetoothPeripheralName", null)
            tesla.bluetoothPeripheralId = container.decode<UUID?>("bluetoothPeripheralId", null)
            return tesla
        }
    }

    object Serializer : KSerializer<SettingsTesla> by JsonObjectSerializer(
        "SettingsTesla",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsDnsLookupStrategy(val rawValue: String) {
    @SerialName("System")
    system("System"),

    @SerialName("IPv4")
    ipv4("IPv4"),

    @SerialName("IPv6")
    ipv6("IPv6"),

    @SerialName("IPv4 and IPv6")
    ipv4AndIpv6("IPv4 and IPv6");

    companion object {
        fun fromRawValue(value: String): SettingsDnsLookupStrategy? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsMediaPlayerFile.Serializer::class)
class SettingsMediaPlayerFile(
    var id: UUID = UUID.randomUUID(),
    var name: String = "My video",
) {
    fun clone(): SettingsMediaPlayerFile {
        val new = SettingsMediaPlayerFile()
        new.id = id
        new.name = name
        return new
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMediaPlayerFile {
            val file = SettingsMediaPlayerFile()
            file.id = container.decode("id", UUID.randomUUID())
            file.name = container.decode("name", "My video")
            return file
        }
    }

    object Serializer : KSerializer<SettingsMediaPlayerFile> by JsonObjectSerializer(
        "SettingsMediaPlayerFile",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMediaPlayer.Serializer::class)
class SettingsMediaPlayer(
    var id: UUID = UUID.randomUUID(),
    override var name: String = baseName,
    var playerId: String = "",
    var autoSelectMic: Boolean = true,
    var playlist: MutableList<SettingsMediaPlayerFile> = mutableListOf(),
) : Named {
    fun camera(): String {
        return mediaPlayerCamera(name)
    }

    fun clone(): SettingsMediaPlayer {
        val new = SettingsMediaPlayer()
        new.id = id
        new.name = name
        new.playerId = playerId
        new.autoSelectMic = autoSelectMic
        for (file in playlist) {
            new.playlist.add(file.clone())
        }
        return new
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("playerId", playerId)
        encode("autoSelectMic", autoSelectMic)
        encode("playlist", playlist, ListSerializer(SettingsMediaPlayerFile.serializer()))
    }

    companion object {
        val baseName: String = localized("My player")

        fun decode(container: JsonObject): SettingsMediaPlayer {
            val player = SettingsMediaPlayer()
            player.id = container.decode("id", UUID.randomUUID())
            player.name = container.decode("name", baseName)
            player.playerId = container.decode("playerId", "")
            player.autoSelectMic = container.decode("autoSelectMic", true)
            player.playlist = container.decode(
                "playlist",
                ListSerializer(SettingsMediaPlayerFile.serializer()),
                emptyList(),
            ).toMutableList()
            return player
        }
    }

    object Serializer : KSerializer<SettingsMediaPlayer> by JsonObjectSerializer(
        "SettingsMediaPlayer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMediaPlayers.Serializer::class)
class SettingsMediaPlayers(
    var players: List<SettingsMediaPlayer> = emptyList(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("players", players, ListSerializer(SettingsMediaPlayer.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsMediaPlayers {
            val mediaPlayers = SettingsMediaPlayers()
            mediaPlayers.players = container.decode(
                "players",
                ListSerializer(SettingsMediaPlayer.serializer()),
                emptyList(),
            )
            return mediaPlayers
        }
    }

    object Serializer : KSerializer<SettingsMediaPlayers> by JsonObjectSerializer(
        "SettingsMediaPlayers",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsReplaySpeed(val rawValue: String) {
    @SerialName("0.5x")
    oneHalf("0.5x"),

    @SerialName("1x")
    one("1x");

    fun toNumber(): Double {
        return when (this) {
            oneHalf -> 0.5
            one -> 1.0
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsReplaySpeed? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsReplay.Serializer::class)
class SettingsReplay(
    var start: Double = 20.0,
    var stop: Double = SettingsReplay.stop,
    var speed: SettingsReplaySpeed = SettingsReplaySpeed.one,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("start", start)
        encode("stop", stop)
        encode("speed", speed)
    }

    companion object {
        const val stop: Double = 30.0

        fun decode(container: JsonObject): SettingsReplay {
            val replay = SettingsReplay()
            replay.start = container.decode("start", 20.0)
            replay.stop = container.decode("stop", SettingsReplay.stop)
            replay.speed = container.decode("speed", SettingsReplaySpeed.one)
            return replay
        }
    }

    object Serializer : KSerializer<SettingsReplay> by JsonObjectSerializer(
        "SettingsReplay",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsCyclingPowerDevice.Serializer::class)
class SettingsCyclingPowerDevice(
    var id: UUID = UUID.randomUUID(),
    override var name: String = "",
    var enabled: Boolean = false,
    var bluetoothPeripheralName: String? = null,
    var bluetoothPeripheralId: UUID? = null,
) : Named {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("enabled", enabled)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
    }

    companion object {
        val baseName: String = localized("My device")

        fun decode(container: JsonObject): SettingsCyclingPowerDevice {
            val device = SettingsCyclingPowerDevice()
            device.id = container.decode("id", UUID.randomUUID())
            device.name = container.decode("name", baseName)
            device.enabled = container.decode("enabled", false)
            device.bluetoothPeripheralName = container.decodeIfPresent<String>("bluetoothPeripheralName")
            device.bluetoothPeripheralId = container.decodeIfPresent<UUID>("bluetoothPeripheralId")
            return device
        }
    }

    object Serializer : KSerializer<SettingsCyclingPowerDevice> by JsonObjectSerializer(
        "SettingsCyclingPowerDevice",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsCyclingPowerDevices.Serializer::class)
class SettingsCyclingPowerDevices(
    var devices: MutableList<SettingsCyclingPowerDevice> = mutableListOf(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsCyclingPowerDevice.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsCyclingPowerDevices {
            val devices = SettingsCyclingPowerDevices()
            devices.devices = container.decode(
                "devices",
                ListSerializer(SettingsCyclingPowerDevice.serializer()),
                emptyList(),
            ).toMutableList()
            return devices
        }
    }

    object Serializer : KSerializer<SettingsCyclingPowerDevices> by JsonObjectSerializer(
        "SettingsCyclingPowerDevices",
        { it.encode() },
        { decode(it) },
    )
}

val defaultWheelCircumference = 2105

@Serializable(with = SettingsWorkoutDevice.Serializer::class)
class SettingsWorkoutDevice(
    var id: UUID = UUID.randomUUID(),
    override var name: String = baseName,
    var enabled: Boolean = false,
    var bluetoothPeripheralName: String? = null,
    var bluetoothPeripheralId: UUID? = null,
    var wheelCircumference: Int = defaultWheelCircumference,
) : Named {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("enabled", enabled)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
        encode("wheelCircumference", wheelCircumference)
    }

    companion object {
        val baseName: String = localized("My device")

        fun decode(container: JsonObject): SettingsWorkoutDevice {
            val device = SettingsWorkoutDevice()
            device.id = container.decode("id", UUID.randomUUID())
            device.name = container.decode("name", baseName)
            device.enabled = container.decode("enabled", false)
            device.bluetoothPeripheralName = container.decodeIfPresent<String>("bluetoothPeripheralName")
            device.bluetoothPeripheralId = container.decodeIfPresent<UUID>("bluetoothPeripheralId")
            device.wheelCircumference = container.decode("wheelCircumference", defaultWheelCircumference)
            return device
        }
    }

    object Serializer : KSerializer<SettingsWorkoutDevice> by JsonObjectSerializer(
        "SettingsWorkoutDevice",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWorkoutDevices.Serializer::class)
class SettingsWorkoutDevices(
    var devices: MutableList<SettingsWorkoutDevice> = mutableListOf(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsWorkoutDevice.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWorkoutDevices {
            val devices = SettingsWorkoutDevices()
            devices.devices = container.decode(
                "devices",
                ListSerializer(SettingsWorkoutDevice.serializer()),
                emptyList(),
            ).toMutableList()
            return devices
        }
    }

    object Serializer : KSerializer<SettingsWorkoutDevices> by JsonObjectSerializer(
        "SettingsWorkoutDevices",
        { it.encode() },
        { decode(it) },
    )
}

private val defaultRgbLightColor = RgbColor(red = 0, green = 255, blue = 0)

@Serializable(with = SettingsBlackSharkCoolerDevice.Serializer::class)
class SettingsBlackSharkCoolerDevice(
    var id: UUID = UUID.randomUUID(),
    override var name: String = baseName,
    var enabled: Boolean = false,
    var bluetoothPeripheralName: String? = null,
    var bluetoothPeripheralId: UUID? = null,
    var rgbLightEnabled: Boolean = false,
    var rgbLightColor: RgbColor = defaultRgbLightColor,
    var rgbLightBrightness: Double = 100.0,
) : Named {
    var rgbLightColorColor: Color = rgbLightColor.color()

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("enabled", enabled)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
        encode("rgbLightEnabled", rgbLightEnabled)
        encode("rgbLightColor", rgbLightColor)
        encode("rgbLightBrightness", rgbLightBrightness)
    }

    companion object {
        val baseName: String = localized("My cooler")

        fun decode(container: JsonObject): SettingsBlackSharkCoolerDevice {
            val device = SettingsBlackSharkCoolerDevice()
            device.id = container.decode("id", UUID.randomUUID())
            device.name = container.decode("name", baseName)
            device.enabled = container.decode("enabled", false)
            device.bluetoothPeripheralName = container.decodeIfPresent<String>("bluetoothPeripheralName")
            device.bluetoothPeripheralId = container.decodeIfPresent<UUID>("bluetoothPeripheralId")
            device.rgbLightEnabled = container.decode("rgbLightEnabled", false)
            device.rgbLightColor = container.decode("rgbLightColor", defaultRgbLightColor)
            device.rgbLightColorColor = device.rgbLightColor.color()
            device.rgbLightBrightness = container.decode("rgbLightBrightness", 100.0)
            return device
        }
    }

    object Serializer : KSerializer<SettingsBlackSharkCoolerDevice> by JsonObjectSerializer(
        "SettingsBlackSharkCoolerDevice",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsBlackSharkCoolerDevices.Serializer::class)
class SettingsBlackSharkCoolerDevices(
    var devices: List<SettingsBlackSharkCoolerDevice> = emptyList(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsBlackSharkCoolerDevice.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsBlackSharkCoolerDevices {
            val devices = SettingsBlackSharkCoolerDevices()
            devices.devices = container.decode(
                "devices",
                ListSerializer(SettingsBlackSharkCoolerDevice.serializer()),
                emptyList(),
            )
            return devices
        }
    }

    object Serializer : KSerializer<SettingsBlackSharkCoolerDevices> by JsonObjectSerializer(
        "SettingsBlackSharkCoolerDevices",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsNetworkInterfaceName.Serializer::class)
class SettingsNetworkInterfaceName(
    var id: UUID = UUID.randomUUID(),
    var interfaceName: String = "",
    var name: String = "",
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("interfaceName", interfaceName)
        encode("name", name)
    }

    companion object {
        fun decode(container: JsonObject): SettingsNetworkInterfaceName {
            val networkInterfaceName = SettingsNetworkInterfaceName()
            networkInterfaceName.id = codableJson.decodeFromJsonElement(UUIDSerializer, container.getValue("id"))
            networkInterfaceName.interfaceName = codableJson.decodeFromJsonElement(
                String.serializer(),
                container.getValue("interfaceName"),
            )
            networkInterfaceName.name = codableJson.decodeFromJsonElement(String.serializer(), container.getValue("name"))
            return networkInterfaceName
        }
    }

    object Serializer : KSerializer<SettingsNetworkInterfaceName> by JsonObjectSerializer(
        "SettingsNetworkInterfaceName",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsExternalDisplayContent(val rawValue: String) {
    @SerialName("Stream")
    stream("Stream"),

    @SerialName("Clean stream")
    cleanStream("Clean stream"),

    @SerialName("Chat")
    chat("Chat"),

    @SerialName("Mirror")
    mirror("Mirror");

    override fun toString(): String {
        return when (this) {
            stream -> localized("Stream")
            cleanStream -> localized("Clean stream")
            chat -> localized("Chat")
            mirror -> localized("Mirror")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsExternalDisplayContent? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsAppMode(val rawValue: String) {
    streaming("streaming"),
    chatPhone("chatPhone");

    override fun toString(): String {
        return when (this) {
            streaming -> localized("Streaming")
            chatPhone -> localized("Chat phone")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsAppMode? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = WebBrowserBookmarkSettings.Serializer::class)
class WebBrowserBookmarkSettings(
    var url: String = "https://google.com",
) {
    var id: UUID = UUID.randomUUID()

    fun encode(): JsonObject = encodeContainer {
        encode("url", url)
    }

    companion object {
        fun decode(container: JsonObject): WebBrowserBookmarkSettings {
            val bookmark = WebBrowserBookmarkSettings()
            bookmark.url = container.decode("url", "https://google.com")
            return bookmark
        }
    }

    object Serializer : KSerializer<WebBrowserBookmarkSettings> by JsonObjectSerializer(
        "WebBrowserBookmarkSettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = WebBrowserSettings.Serializer::class)
class WebBrowserSettings(
    var home: String = "https://google.com",
    var bookmarks: List<WebBrowserBookmarkSettings> = emptyList(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("home", home)
        encode("bookmarks", bookmarks, ListSerializer(WebBrowserBookmarkSettings.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): WebBrowserSettings {
            val webBrowser = WebBrowserSettings()
            webBrowser.home = container.decode("home", "https://google.com")
            webBrowser.bookmarks = container.decode(
                "bookmarks",
                ListSerializer(WebBrowserBookmarkSettings.serializer()),
                emptyList(),
            )
            return webBrowser
        }
    }

    object Serializer : KSerializer<WebBrowserSettings> by JsonObjectSerializer(
        "WebBrowserSettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsAlertsMediaGalleryItem.Serializer::class)
class SettingsAlertsMediaGalleryItem(
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
    }

    companion object {
        fun decode(container: JsonObject): SettingsAlertsMediaGalleryItem {
            val item = SettingsAlertsMediaGalleryItem()
            item.id = container.decode("id", UUID.randomUUID())
            item.name = container.decode("name", "")
            return item
        }
    }

    object Serializer : KSerializer<SettingsAlertsMediaGalleryItem> by JsonObjectSerializer(
        "SettingsAlertsMediaGalleryItem",
        { it.encode() },
        { decode(it) },
    )
}

private val allBundledAlertsMediaGalleryImages = listOf(
    SettingsAlertsMediaGalleryItem(name = "Moblin pixels"),
    SettingsAlertsMediaGalleryItem(name = "Moblin party"),
    SettingsAlertsMediaGalleryItem(name = "Moblin trillionaire"),
    SettingsAlertsMediaGalleryItem(name = "White star"),
    SettingsAlertsMediaGalleryItem(name = "Angry"),
    SettingsAlertsMediaGalleryItem(name = "Sunglasses"),
    SettingsAlertsMediaGalleryItem(name = "Salty"),
    SettingsAlertsMediaGalleryItem(name = "-100"),
)

private val allBundledAlertsMediaGallerySounds = listOf(
    SettingsAlertsMediaGalleryItem(name = "Notification 2"),
    SettingsAlertsMediaGalleryItem(name = "Boing"),
    SettingsAlertsMediaGalleryItem(name = "Cash register"),
    SettingsAlertsMediaGalleryItem(name = "Dingaling"),
    SettingsAlertsMediaGalleryItem(name = "Level up"),
    SettingsAlertsMediaGalleryItem(name = "Notification"),
    SettingsAlertsMediaGalleryItem(name = "SFX magic"),
    SettingsAlertsMediaGalleryItem(name = "Whoosh"),
    SettingsAlertsMediaGalleryItem(name = "Coin dropping"),
    SettingsAlertsMediaGalleryItem(name = "Fart"),
    SettingsAlertsMediaGalleryItem(name = "Fart 2"),
    SettingsAlertsMediaGalleryItem(name = "Bad chili fart"),
    SettingsAlertsMediaGalleryItem(name = "Perfect fart"),
    SettingsAlertsMediaGalleryItem(name = "Silence"),
)

@Serializable(with = SettingsAlertsMediaGallery.Serializer::class)
class SettingsAlertsMediaGallery(
    var bundledImages: List<SettingsAlertsMediaGalleryItem> = allBundledAlertsMediaGalleryImages,
    var customImages: List<SettingsAlertsMediaGalleryItem> = emptyList(),
    var bundledSounds: List<SettingsAlertsMediaGalleryItem> = allBundledAlertsMediaGallerySounds,
    var customSounds: List<SettingsAlertsMediaGalleryItem> = emptyList(),
) {
    fun getWhiteStarImageId(): UUID {
        return bundledImages[3].id
    }

    fun getGlassesImageId(): UUID {
        return bundledImages[5].id
    }

    fun encode(): JsonObject = encodeContainer {
        encode("bundledImages", bundledImages, ListSerializer(SettingsAlertsMediaGalleryItem.serializer()))
        encode("customImages", customImages, ListSerializer(SettingsAlertsMediaGalleryItem.serializer()))
        encode("bundledSounds", bundledSounds, ListSerializer(SettingsAlertsMediaGalleryItem.serializer()))
        encode("customSounds", customSounds, ListSerializer(SettingsAlertsMediaGalleryItem.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsAlertsMediaGallery {
            val gallery = SettingsAlertsMediaGallery()
            gallery.bundledImages = container.decode(
                "bundledImages",
                ListSerializer(SettingsAlertsMediaGalleryItem.serializer()),
                allBundledAlertsMediaGalleryImages,
            )
            gallery.customImages = container.decode(
                "customImages",
                ListSerializer(SettingsAlertsMediaGalleryItem.serializer()),
                emptyList(),
            )
            gallery.bundledSounds = container.decode(
                "bundledSounds",
                ListSerializer(SettingsAlertsMediaGalleryItem.serializer()),
                allBundledAlertsMediaGallerySounds,
            )
            gallery.customSounds = container.decode(
                "customSounds",
                ListSerializer(SettingsAlertsMediaGalleryItem.serializer()),
                emptyList(),
            )
            return gallery
        }
    }

    object Serializer : KSerializer<SettingsAlertsMediaGallery> by JsonObjectSerializer(
        "SettingsAlertsMediaGallery",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsDisconnectProtection.Serializer::class)
class SettingsDisconnectProtection(
    var liveSceneId: UUID? = null,
    var fallbackSceneId: UUID? = null,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("liveSceneId", liveSceneId)
        encode("fallbackSceneId", fallbackSceneId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsDisconnectProtection {
            val protection = SettingsDisconnectProtection()
            protection.liveSceneId = if (container["liveSceneId"] is JsonNull) {
                null
            } else {
                container.decode<UUID?>("liveSceneId", UUID.randomUUID())
            }
            protection.fallbackSceneId = if (container["fallbackSceneId"] is JsonNull) {
                null
            } else {
                container.decode<UUID?>("fallbackSceneId", UUID.randomUUID())
            }
            return protection
        }
    }

    object Serializer : KSerializer<SettingsDisconnectProtection> by JsonObjectSerializer(
        "SettingsDisconnectProtection",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWiFiAwareRole.Serializer::class)
enum class SettingsWiFiAwareRole {
    sender,
    receiver;

    override fun toString(): String {
        return when (this) {
            sender -> "Sender"
            receiver -> "Receiver"
        }
    }

    fun encode(): JsonObject = encodeContainer {
        encode(name, JsonObject(emptyMap()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWiFiAwareRole {
            val roles = container.keys.mapNotNull { key -> entries.firstOrNull { it.name == key } }
            if (roles.size != 1) {
                throw SerializationException("Invalid number of keys found, expected one")
            }
            val role = roles[0]
            if (container[role.name] !is JsonObject) {
                throw SerializationException("Expected an object for case '${role.name}'")
            }
            return role
        }
    }

    object Serializer : KSerializer<SettingsWiFiAwareRole> by JsonObjectSerializer(
        "SettingsWiFiAwareRole",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWiFiAware.Serializer::class)
class SettingsWiFiAware(
    var enabled: Boolean = false,
    var role: SettingsWiFiAwareRole = SettingsWiFiAwareRole.sender,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("role", role)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWiFiAware {
            val wiFiAware = SettingsWiFiAware()
            wiFiAware.enabled = container.decode("enabled", false)
            wiFiAware.role = container.decode("role", SettingsWiFiAwareRole.sender)
            return wiFiAware
        }
    }

    object Serializer : KSerializer<SettingsWiFiAware> by JsonObjectSerializer(
        "SettingsWiFiAware",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsFacePrivacyMode(val rawValue: String) {
    blur("blur"),
    pixellate("pixellate"),
    backgroundImage("backgroundImage"),
    icon("icon");

    override fun toString(): String {
        return when (this) {
            blur -> localized("Blur")
            pixellate -> localized("Pixellate")
            backgroundImage -> localized("Background image")
            icon -> localized("Icon")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsFacePrivacyMode? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsFace.Serializer::class)
class SettingsFace(
    var blurFaces: Boolean = false,
    var blurText: Boolean = false,
    var blurBackground: Boolean = false,
    var showMoblin: Boolean = false,
    var privacyMode: SettingsFacePrivacyMode = SettingsFacePrivacyMode.blur,
    var blurStrength: Float = 0.8f,
    var pixellateStrength: Float = 0.3f,
) {
    fun toEffectSettings(backgroundImage: Image?, iconImage: Image?): FaceEffectSettings {
        val faceEffectPrivacyMode: FaceEffectPrivacyMode = when (privacyMode) {
            SettingsFacePrivacyMode.blur -> FaceEffectPrivacyMode.Blur(blurStrength)
            SettingsFacePrivacyMode.pixellate -> FaceEffectPrivacyMode.Pixellate(pixellateStrength)
            SettingsFacePrivacyMode.backgroundImage ->
                FaceEffectPrivacyMode.BackgroundImage(backgroundImage)
            SettingsFacePrivacyMode.icon -> FaceEffectPrivacyMode.Icon(iconImage)
        }
        return FaceEffectSettings(
            blurFaces = blurFaces,
            blurText = blurText,
            blurBackground = blurBackground,
            showMouth = showMoblin,
            privacyMode = faceEffectPrivacyMode,
        )
    }

    fun encode(): JsonObject = encodeContainer {
        encode("privacyMode", privacyMode)
        encode("blurStrength", blurStrength)
        encode("pixellateStrength", pixellateStrength)
    }

    companion object {
        fun decode(container: JsonObject): SettingsFace {
            val face = SettingsFace()
            face.blurFaces = false
            face.blurText = false
            face.blurBackground = false
            face.showMoblin = false
            face.privacyMode = container.decode("privacyMode", SettingsFacePrivacyMode.blur)
            face.blurStrength = container.decode("blurStrength", 0.8f)
            face.pixellateStrength = container.decode("pixellateStrength", 0.3f)
            return face
        }
    }

    object Serializer : KSerializer<SettingsFace> by JsonObjectSerializer(
        "SettingsFace",
        { it.encode() },
        { decode(it) },
    )
}

enum class SettingsBeautySettings {
    smoothness,
    shape;

    override fun toString(): String {
        return when (this) {
            smoothness -> localized("Smoothness")
            shape -> localized("Shape")
        }
    }
}

@Serializable(with = SettingsBeauty.Serializer::class)
class SettingsBeauty(
    var enabled: Boolean = false,
    var smoothnessRadius: Float = 10.0f,
    var smoothnessStrength: Float = 0.65f,
    var shapePosition: Float = 0.5f,
    var shapeRadius: Float = 0.5f,
    var shapeStrength: Float = 0.5f,
) {
    var settings: SettingsBeautySettings = SettingsBeautySettings.smoothness

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("smoothRadius", smoothnessRadius)
        encode("smoothStrength", smoothnessStrength)
        encode("shapePosition", shapePosition)
        encode("shapeRadius", shapeRadius)
        encode("shapeStrength", shapeStrength)
    }

    companion object {
        fun decode(container: JsonObject): SettingsBeauty {
            val beauty = SettingsBeauty()
            beauty.enabled = container.decode("enabled", false)
            beauty.smoothnessRadius = container.decode("smoothRadius", 10.0f)
            beauty.smoothnessStrength = container.decode("smoothStrength", 0.65f)
            beauty.shapePosition = container.decode("shapePosition", 0.5f)
            beauty.shapeRadius = container.decode("shapeRadius", 0.5f)
            beauty.shapeStrength = container.decode("shapeStrength", 0.5f)
            return beauty
        }
    }

    object Serializer : KSerializer<SettingsBeauty> by JsonObjectSerializer(
        "SettingsBeauty",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWiFi.Serializer::class)
class SettingsWiFi(
    var ssid: String = "",
    var password: String = "",
) {
    val id: String
        get() = ssid

    fun encode(): JsonObject = encodeContainer {
        encode("ssid", ssid)
        encode("password", password)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWiFi {
            val wiFi = SettingsWiFi()
            wiFi.ssid = container.decode("ssid", "")
            wiFi.password = container.decode("password", "")
            return wiFi
        }
    }

    object Serializer : KSerializer<SettingsWiFi> by JsonObjectSerializer(
        "SettingsWiFi",
        { it.encode() },
        { decode(it) },
    )
}

private fun normalizeWatchSettingsChat(container: JsonObject): JsonObject = encodeContainer {
    encode("fontSize", container.decode("fontSize", 17.0f))
    encode("timestampEnabled", container.decode("timestampEnabled", true))
    encode("notificationOnMessage", container.decode("notificationOnMessage", false))
    encode("notificationRate", container.decode("notificationRate", 30))
    encode("badges", container.decode("badges", true))
}

private fun normalizeWatchSettingsShow(container: JsonObject): JsonObject = encodeContainer {
    encode("thermalState", container.decode("thermalState", true))
    encode("audioLevel", container.decode("audioLevel", true))
    encode("speed", container.decode("speed", true))
}

private fun normalizeWatchSettings(container: JsonObject): JsonObject = encodeContainer {
    encode("chat", normalizeWatchSettingsChat(container.decode("chat", JsonObject(emptyMap()))))
    encode("show", normalizeWatchSettingsShow(container.decode("show", JsonObject(emptyMap()))))
    encode("viaRemoteControl", container.decode("viaRemoteControl", false))
}

@Serializable(with = Database.Serializer::class)
class Database(
    var streams: MutableList<SettingsStream> = mutableListOf(),
    var scenes: MutableList<SettingsScene> = mutableListOf(),
    var widgets: MutableList<SettingsWidget> = mutableListOf(),
    var show: SettingsShow = SettingsShow(),
    var zoom: SettingsZoom = SettingsZoom(),
    var tapToFocus: Boolean = false,
    var bitratePresets: MutableList<SettingsBitratePreset> = mutableListOf(),
    var iconImage: String = plainIcon.image(),
    var videoStabilizationMode: SettingsVideoStabilizationMode =
        SettingsVideoStabilizationMode.off,
    var chat: SettingsChat = SettingsChat(),
    var mic: SettingsMic = getDefaultMic(),
    var mics: SettingsMics = SettingsMics(),
    var debug: SettingsDebug = SettingsDebug(),
    var quickButtonsGeneral: SettingsQuickButtons = SettingsQuickButtons(),
    var quickButtons: MutableList<SettingsQuickButton> = mutableListOf(),
    var rtmpServer: SettingsRtmpServer = SettingsRtmpServer(),
    var networkInterfaceNames: MutableList<SettingsNetworkInterfaceName> = mutableListOf(),
    var lowBitrateWarning: Boolean = true,
    var vibrate: Boolean = false,
    var gameControllers: MutableList<SettingsGameController> =
        mutableListOf(SettingsGameController()),
    var remoteControl: SettingsRemoteControl = SettingsRemoteControl(),
    var startStopRecordingConfirmations: Boolean = true,
    var color: SettingsColor = SettingsColor(),
    var mirrorFrontCameraOnStream: Boolean = true,
    var streamButtonColor: RgbColor = defaultStreamButtonColor,
    var location: SettingsLocation = SettingsLocation(),
    var watch: JsonObject = JsonObject(emptyMap()),
    var audio: SettingsAudio = SettingsAudio(),
    var macros: SettingsMacros = SettingsMacros(),
    var webBrowser: WebBrowserSettings = WebBrowserSettings(),
    var deepLinkCreator: DeepLinkCreator = DeepLinkCreator(),
    var srtlaServer: SettingsSrtlaServer = SettingsSrtlaServer(),
    var mediaPlayers: SettingsMediaPlayers = SettingsMediaPlayers(),
    var showAllSettings: Boolean = false,
    var portrait: Boolean = false,
    var djiDevices: SettingsDjiDevices = SettingsDjiDevices(),
    var alertsMediaGallery: SettingsAlertsMediaGallery = SettingsAlertsMediaGallery(),
    var catPrinters: SettingsCatPrinters = SettingsCatPrinters(),
    var verboseStatuses: Boolean = false,
    var scoreboardPlayers: MutableList<SettingsWidgetScoreboardPlayer> = mutableListOf(),
    var keyboard: SettingsKeyboard = SettingsKeyboard(),
    var tesla: SettingsTesla = SettingsTesla(),
    var srtlaRelay: SettingsMoblink = SettingsMoblink(),
    var pixellateStrength: Float = 0.3f,
    var moblink: SettingsMoblink = SettingsMoblink(),
    var sceneSwitchTransition: SettingsSceneSwitchTransition =
        SettingsSceneSwitchTransition.blur,
    var forceSceneSwitchTransition: Boolean = false,
    var alwaysAttachCameraPreview: Boolean = false,
    var alwaysAttachPhotoShoot: Boolean = false,
    var cameraControlsEnabled: Boolean = false,
    var externalDisplayContent: SettingsExternalDisplayContent =
        SettingsExternalDisplayContent.stream,
    var cyclingPowerDevices: SettingsCyclingPowerDevices = SettingsCyclingPowerDevices(),
    var cyclingPowerDevicesMigrated: Boolean = false,
    var workoutDevices: SettingsWorkoutDevices = SettingsWorkoutDevices(),
    var blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices =
        SettingsBlackSharkCoolerDevices(),
    var remoteSceneId: UUID? = null,
    var sceneNumericInput: Boolean = false,
    var savedWifiNetworks: List<SettingsWiFi> = emptyList(),
    var goPro: SettingsGoPro = SettingsGoPro(),
    var replay: SettingsReplay = SettingsReplay(),
    var portraitVideoOffsetFromTop: Double = 0.0,
    var autoSceneSwitchers: SettingsAutoSceneSwitchers = SettingsAutoSceneSwitchers(),
    var fixedHorizon: Boolean = false,
    var whirlpoolAngle: Float = (PI / 2).toFloat(),
    var pinchScale: Float = 0.5f,
    var selfieStick: SettingsSelfieStick = SettingsSelfieStick(),
    var bigButtons: Boolean = false,
    var verticalButtons: Boolean = false,
    var bigAudioLevelMeter: Boolean = false,
    var ristServer: SettingsRistServer = SettingsRistServer(),
    var disconnectProtection: SettingsDisconnectProtection = SettingsDisconnectProtection(),
    var rtspClient: SettingsRtspClient = SettingsRtspClient(),
    var srtClient: SettingsSrtClient = SettingsSrtClient(),
    var whipServer: SettingsWhipServer = SettingsWhipServer(),
    var whepClient: SettingsWhepClient = SettingsWhepClient(),
    var navigation: SettingsNavigation = SettingsNavigation(),
    var wiFiAware: SettingsWiFiAware = SettingsWiFiAware(),
    var face: SettingsFace = SettingsFace(),
    var beauty: SettingsBeauty = SettingsBeauty(),
    var talkback: SettingsTalkback = SettingsTalkback(),
    var gimbal: SettingsGimbal = SettingsGimbal(),
    var scoreboardSizeMigrated: Boolean = false,
    var streamDecks: SettingsStreamDecks = SettingsStreamDecks(),
    var graphicsImplementation: SettingsGraphicsImplementation =
        SettingsGraphicsImplementation.coreImage,
    var graphicsHighQualityDownsampling: Boolean = false,
    var ingestsSoftwareVideoDecoding: Boolean = false,
    var torchLevel: Float = 1.0f,
    var appMode: SettingsAppMode = SettingsAppMode.streaming,
    var httpProxy: SettingsHttpProxy = SettingsHttpProxy(),
) {
    var streamButtonColorColor: Color = defaultStreamButtonColor.color()

    fun getSavedWiFiNetwork(ssid: String): SettingsWiFi? {
        return savedWifiNetworks.firstOrNull { it.ssid == ssid }
    }

    fun getHighestBitratePreset(): Int {
        return bitratePresets.sortedByDescending { it.bitrate }.firstOrNull()?.bitrate ?: 5_000_000
    }

    fun toJsonString(): String {
        return codableJson.encodeToString(Serializer, this)
    }

    fun encode(): JsonObject = encodeContainer {
        encode("streams", streams, ListSerializer(SettingsStream.serializer()))
        encode("scenes", scenes, ListSerializer(SettingsScene.serializer()))
        encode("widgets", widgets, ListSerializer(SettingsWidget.serializer()))
        encode("show", show, SettingsShow.serializer())
        encode("zoom", zoom, SettingsZoom.serializer())
        encode("tapToFocus", tapToFocus)
        encode("bitratePresets", bitratePresets, ListSerializer(SettingsBitratePreset.serializer()))
        encode("iconImage", iconImage)
        encode("videoStabilizationMode", videoStabilizationMode)
        encode("chat", chat, SettingsChat.serializer())
        encode("mic", mic)
        encode("mics", mics, SettingsMics.serializer())
        encode("debug", debug, SettingsDebug.serializer())
        encode("quickButtons", quickButtonsGeneral, SettingsQuickButtons.serializer())
        encode("globalButtons", quickButtons, ListSerializer(SettingsQuickButton.serializer()))
        encode("rtmpServer", rtmpServer, SettingsRtmpServer.serializer())
        encode(
            "networkInterfaceNames",
            networkInterfaceNames,
            ListSerializer(SettingsNetworkInterfaceName.serializer()),
        )
        encode("lowBitrateWarning", lowBitrateWarning)
        encode("vibrate", vibrate)
        encode("gameControllers", gameControllers, ListSerializer(SettingsGameController.serializer()))
        encode("remoteControl", remoteControl, SettingsRemoteControl.serializer())
        encode("startStopRecordingConfirmations", startStopRecordingConfirmations)
        encode("color", color, SettingsColor.serializer())
        encode("mirrorFrontCameraOnStream", mirrorFrontCameraOnStream)
        encode("streamButtonColor", streamButtonColor)
        encode("location", location, SettingsLocation.serializer())
        encode("watch", normalizeWatchSettings(watch))
        encode("audio", audio, SettingsAudio.serializer())
        encode("macros", macros, SettingsMacros.serializer())
        encode("webBrowser", webBrowser, WebBrowserSettings.serializer())
        encode("deepLinkCreator", deepLinkCreator, DeepLinkCreator.serializer())
        encode("srtlaServer", srtlaServer, SettingsSrtlaServer.serializer())
        encode("mediaPlayers", mediaPlayers, SettingsMediaPlayers.serializer())
        encode("showAllSettings", showAllSettings)
        encode("portrait", portrait)
        encode("djiDevices", djiDevices, SettingsDjiDevices.serializer())
        encode("alertsMediaGallery", alertsMediaGallery, SettingsAlertsMediaGallery.serializer())
        encode("catPrinters", catPrinters, SettingsCatPrinters.serializer())
        encode("verboseStatuses", verboseStatuses)
        encode("scoreboardPlayers", scoreboardPlayers, ListSerializer(SettingsWidgetScoreboardPlayer.serializer()))
        encode("keyboard", keyboard, SettingsKeyboard.serializer())
        encode("tesla", tesla, SettingsTesla.serializer())
        encode("srtlaRelay", srtlaRelay, SettingsMoblink.serializer())
        encode("pixellateStrength", pixellateStrength)
        encode("moblink", moblink, SettingsMoblink.serializer())
        encode("sceneSwitchTransition", sceneSwitchTransition)
        encode("forceSceneSwitchTransition", forceSceneSwitchTransition)
        encode("alwaysAttachCameraPreview", alwaysAttachCameraPreview)
        encode("alwaysAttachPhotoShoot", alwaysAttachPhotoShoot)
        encode("cameraControlsEnabled", cameraControlsEnabled)
        encode("externalDisplayContent", externalDisplayContent)
        encode("cyclingPowerDevices", cyclingPowerDevices, SettingsCyclingPowerDevices.serializer())
        encode("cyclingPowerDevicesMigrated", cyclingPowerDevicesMigrated)
        encode("heartRateDevices", workoutDevices, SettingsWorkoutDevices.serializer())
        encode("phoneCoolerDevices", blackSharkCoolerDevices, SettingsBlackSharkCoolerDevices.serializer())
        encode("remoteSceneId", remoteSceneId)
        encode("sceneNumericInput", sceneNumericInput)
        encode("goPro", goPro, SettingsGoPro.serializer())
        encode("replay", replay, SettingsReplay.serializer())
        encode("portraitVideoOffsetFromTop", portraitVideoOffsetFromTop)
        encode("autoSceneSwitchers", autoSceneSwitchers, SettingsAutoSceneSwitchers.serializer())
        encode("fixedHorizon", fixedHorizon)
        encode("whirlpoolAngle", whirlpoolAngle)
        encode("pinchScale", pinchScale)
        encode("selfieStick", selfieStick, SettingsSelfieStick.serializer())
        encode("bigButtons", bigButtons)
        encode("verticalButtons", verticalButtons)
        encode("bigAudioLevelMeter", bigAudioLevelMeter)
        encode("ristServer", ristServer, SettingsRistServer.serializer())
        encode("disconnectProtection", disconnectProtection, SettingsDisconnectProtection.serializer())
        encode("rtspClient", rtspClient, SettingsRtspClient.serializer())
        encode("srtClient", srtClient, SettingsSrtClient.serializer())
        encode("whipServer", whipServer, SettingsWhipServer.serializer())
        encode("whepClient", whepClient, SettingsWhepClient.serializer())
        encode("navigation", navigation, SettingsNavigation.serializer())
        encode("wiFiAware", wiFiAware, SettingsWiFiAware.serializer())
        encode("face", face, SettingsFace.serializer())
        encode("beauty", beauty, SettingsBeauty.serializer())
        encode("talkBack", talkback, SettingsTalkback.serializer())
        encode("gimbal", gimbal, SettingsGimbal.serializer())
        encode("scoreboardSizeMigrated", scoreboardSizeMigrated)
        encode("savedWifiNetworks", savedWifiNetworks, ListSerializer(SettingsWiFi.serializer()))
        encode("streamDecks", streamDecks, SettingsStreamDecks.serializer())
        encode("graphicsImplementation", graphicsImplementation)
        encode("graphicsHighQualityDownsampling", graphicsHighQualityDownsampling)
        encode("ingestsSoftwareVideoDecoding", ingestsSoftwareVideoDecoding)
        encode("torchLevel", torchLevel)
        encode("appMode", appMode)
        encode("httpProxy", httpProxy, SettingsHttpProxy.serializer())
    }

    companion object {
        fun decode(container: JsonObject): Database {
            val database = Database()
            database.streams = container.decode("streams", ListSerializer(SettingsStream.serializer()), emptyList())
                .toMutableList()
            database.scenes = container.decode("scenes", ListSerializer(SettingsScene.serializer()), emptyList())
                .toMutableList()
            database.widgets = container.decode("widgets", ListSerializer(SettingsWidget.serializer()), emptyList())
                .toMutableList()
            database.show = container.decode("show", SettingsShow.serializer(), SettingsShow())
            database.zoom = container.decode("zoom", SettingsZoom.serializer(), SettingsZoom())
            database.tapToFocus = container.decode("tapToFocus", false)
            database.bitratePresets = container.decode(
                "bitratePresets",
                ListSerializer(SettingsBitratePreset.serializer()),
                emptyList(),
            ).toMutableList()
            database.iconImage = container.decode("iconImage", plainIcon.image())
            database.videoStabilizationMode = container.decode(
                "videoStabilizationMode",
                SettingsVideoStabilizationMode.off,
            )
            database.chat = container.decode("chat", SettingsChat.serializer(), SettingsChat())
            database.mic = container.decode("mic", getDefaultMic())
            database.mics = container.decode("mics", SettingsMics.serializer(), SettingsMics())
            database.debug = container.decode("debug", SettingsDebug.serializer(), SettingsDebug())
            database.quickButtonsGeneral = container.decode(
                "quickButtons",
                SettingsQuickButtons.serializer(),
                SettingsQuickButtons(),
            )
            database.quickButtons = container.decode(
                "globalButtons",
                ListSerializer(SettingsQuickButton.serializer()),
                emptyList(),
            ).toMutableList()
            database.rtmpServer = container.decode("rtmpServer", SettingsRtmpServer.serializer(), SettingsRtmpServer())
            database.networkInterfaceNames = container.decode(
                "networkInterfaceNames",
                ListSerializer(SettingsNetworkInterfaceName.serializer()),
                emptyList(),
            ).toMutableList()
            database.lowBitrateWarning = container.decode("lowBitrateWarning", true)
            database.vibrate = container.decode("vibrate", false)
            database.gameControllers = container.decode(
                "gameControllers",
                ListSerializer(SettingsGameController.serializer()),
                listOf(SettingsGameController()),
            ).toMutableList()
            database.remoteControl = container.decode(
                "remoteControl",
                SettingsRemoteControl.serializer(),
                SettingsRemoteControl(),
            )
            database.startStopRecordingConfirmations = container.decode("startStopRecordingConfirmations", true)
            database.color = container.decode("color", SettingsColor.serializer(), SettingsColor())
            database.mirrorFrontCameraOnStream = container.decode("mirrorFrontCameraOnStream", true)
            database.streamButtonColor = container.decode("streamButtonColor", defaultStreamButtonColor)
            database.streamButtonColorColor = database.streamButtonColor.color()
            database.location = container.decode("location", SettingsLocation.serializer(), SettingsLocation())
            database.watch = normalizeWatchSettings(container.decode("watch", JsonObject(emptyMap())))
            database.audio = container.decode("audio", SettingsAudio.serializer(), SettingsAudio())
            if (database.debug.preferStereoMicToBeRemoved) {
                database.audio._preferStereoMic.value = true
                database.debug.preferStereoMicToBeRemoved = false
            }
            database.macros = container.decode("macros", SettingsMacros.serializer(), SettingsMacros())
            database.webBrowser = container.decode("webBrowser", WebBrowserSettings.serializer(), WebBrowserSettings())
            database.deepLinkCreator = container.decode(
                "deepLinkCreator",
                DeepLinkCreator.serializer(),
                DeepLinkCreator(),
            )
            database.srtlaServer = container.decode(
                "srtlaServer",
                SettingsSrtlaServer.serializer(),
                SettingsSrtlaServer(),
            )
            database.mediaPlayers = container.decode(
                "mediaPlayers",
                SettingsMediaPlayers.serializer(),
                SettingsMediaPlayers(),
            )
            database.showAllSettings = container.decode("showAllSettings", false)
            database.portrait = container.decode("portrait", false)
            database.djiDevices = container.decode("djiDevices", SettingsDjiDevices.serializer(), SettingsDjiDevices())
            database.alertsMediaGallery = container.decode(
                "alertsMediaGallery",
                SettingsAlertsMediaGallery.serializer(),
                SettingsAlertsMediaGallery(),
            )
            database.catPrinters = container.decode(
                "catPrinters",
                SettingsCatPrinters.serializer(),
                SettingsCatPrinters(),
            )
            database.verboseStatuses = container.decode("verboseStatuses", false)
            database.scoreboardPlayers = container.decode(
                "scoreboardPlayers",
                ListSerializer(SettingsWidgetScoreboardPlayer.serializer()),
                emptyList(),
            ).toMutableList()
            database.keyboard = container.decode("keyboard", SettingsKeyboard.serializer(), SettingsKeyboard())
            database.tesla = container.decode("tesla", SettingsTesla.serializer(), SettingsTesla())
            database.srtlaRelay = container.decode("srtlaRelay", SettingsMoblink.serializer(), SettingsMoblink())
            database.pixellateStrength = container.decode("pixellateStrength", 0.3f)
            database.moblink = container.decode("moblink", SettingsMoblink.serializer(), database.srtlaRelay)
            database.sceneSwitchTransition = container.decode(
                "sceneSwitchTransition",
                SettingsSceneSwitchTransition.blur,
            )
            database.forceSceneSwitchTransition = container.decode("forceSceneSwitchTransition", false)
            database.alwaysAttachCameraPreview = container.decode("alwaysAttachCameraPreview", false)
            database.alwaysAttachPhotoShoot = container.decode("alwaysAttachPhotoShoot", false)
            database.cameraControlsEnabled = container.decode("cameraControlsEnabled", false)
            database.externalDisplayContent = container.decode(
                "externalDisplayContent",
                SettingsExternalDisplayContent.stream,
            )
            database.cyclingPowerDevices = container.decode(
                "cyclingPowerDevices",
                SettingsCyclingPowerDevices.serializer(),
                SettingsCyclingPowerDevices(),
            )
            database.cyclingPowerDevicesMigrated = container.decode("cyclingPowerDevicesMigrated", false)
            database.workoutDevices = container.decode(
                "heartRateDevices",
                SettingsWorkoutDevices.serializer(),
                SettingsWorkoutDevices(),
            )
            if (!database.cyclingPowerDevicesMigrated) {
                for (cyclingPowerDevice in database.cyclingPowerDevices.devices) {
                    val alreadyThere = database.workoutDevices.devices.any {
                        it.bluetoothPeripheralId == cyclingPowerDevice.bluetoothPeripheralId
                    }
                    if (alreadyThere) {
                        continue
                    }
                    val workoutDevice = SettingsWorkoutDevice()
                    workoutDevice.id = cyclingPowerDevice.id
                    workoutDevice.name = cyclingPowerDevice.name
                    workoutDevice.enabled = cyclingPowerDevice.enabled
                    workoutDevice.bluetoothPeripheralName = cyclingPowerDevice.bluetoothPeripheralName
                    workoutDevice.bluetoothPeripheralId = cyclingPowerDevice.bluetoothPeripheralId
                    database.workoutDevices.devices.add(workoutDevice)
                }
                database.cyclingPowerDevicesMigrated = true
            }
            database.blackSharkCoolerDevices = container.decode(
                "phoneCoolerDevices",
                SettingsBlackSharkCoolerDevices.serializer(),
                SettingsBlackSharkCoolerDevices(),
            )
            database.remoteSceneId = container.decodeIfPresent<UUID>("remoteSceneId")
            database.sceneNumericInput = container.decode("sceneNumericInput", false)
            database.goPro = container.decode("goPro", SettingsGoPro.serializer(), SettingsGoPro())
            database.replay = container.decode("replay", SettingsReplay.serializer(), SettingsReplay())
            database.portraitVideoOffsetFromTop = container.decode("portraitVideoOffsetFromTop", 0.0)
            database.autoSceneSwitchers = container.decode(
                "autoSceneSwitchers",
                SettingsAutoSceneSwitchers.serializer(),
                SettingsAutoSceneSwitchers(),
            )
            database.fixedHorizon = container.decode("fixedHorizon", false)
            database.whirlpoolAngle = container.decode("whirlpoolAngle", (PI / 2).toFloat())
            database.pinchScale = container.decode("pinchScale", 0.5f)
            database.selfieStick = container.decode(
                "selfieStick",
                SettingsSelfieStick.serializer(),
                SettingsSelfieStick(),
            )
            database.bigButtons = container.decode("bigButtons", false)
            database.verticalButtons = container.decode("verticalButtons", false)
            database.bigAudioLevelMeter = container.decode("bigAudioLevelMeter", false)
            database.ristServer = container.decode("ristServer", SettingsRistServer.serializer(), SettingsRistServer())
            database.disconnectProtection = container.decode(
                "disconnectProtection",
                SettingsDisconnectProtection.serializer(),
                SettingsDisconnectProtection(),
            )
            database.rtspClient = container.decode("rtspClient", SettingsRtspClient.serializer(), SettingsRtspClient())
            database.srtClient = container.decode("srtClient", SettingsSrtClient.serializer(), SettingsSrtClient())
            database.whipServer = container.decode("whipServer", SettingsWhipServer.serializer(), SettingsWhipServer())
            database.whepClient = container.decode("whepClient", SettingsWhepClient.serializer(), SettingsWhepClient())
            database.navigation = container.decode(
                "navigation",
                SettingsNavigation.serializer(),
                SettingsNavigation(),
            )
            database.wiFiAware = container.decode("wiFiAware", SettingsWiFiAware.serializer(), SettingsWiFiAware())
            database.face = container.decodeIfPresent("face", SettingsFace.serializer())
                ?: database.debug.faceToBeRemoved
            database.beauty = container.decode("beauty", SettingsBeauty.serializer(), SettingsBeauty())
            database.talkback = container.decode("talkBack", SettingsTalkback.serializer(), SettingsTalkback())
            database.gimbal = container.decode("gimbal", SettingsGimbal.serializer(), SettingsGimbal())
            database.scoreboardSizeMigrated = container.decode("scoreboardSizeMigrated", false)
            database.savedWifiNetworks = container.decode(
                "savedWifiNetworks",
                ListSerializer(SettingsWiFi.serializer()),
                emptyList(),
            )
            if (!database.scoreboardSizeMigrated) {
                for (widget in database.widgets) {
                    if (widget.type != SettingsWidgetType.scoreboard) {
                        continue
                    }
                    for (scene in database.scenes) {
                        for (sceneWidget in scene.widgets) {
                            if (sceneWidget.widgetId == widget.id) {
                                sceneWidget.layout.size = defaultScoreboardSize
                            }
                        }
                    }
                }
                database.scoreboardSizeMigrated = true
            }
            database.streamDecks = container.decode(
                "streamDecks",
                SettingsStreamDecks.serializer(),
                SettingsStreamDecks(),
            )
            database.graphicsImplementation = container.decode(
                "graphicsImplementation",
                SettingsGraphicsImplementation.coreImage,
            )
            database.graphicsHighQualityDownsampling = container.decode(
                "graphicsHighQualityDownsampling",
                database.debug.highQualityDownsamplingToBeRemoved,
            )
            database.ingestsSoftwareVideoDecoding = container.decode("ingestsSoftwareVideoDecoding", false)
            database.torchLevel = container.decode("torchLevel", 1.0f)
            database.appMode = container.decode("appMode", SettingsAppMode.streaming)
            val httpProxyDefault = SettingsHttpProxy()
            httpProxyDefault.enabled.value = database.debug.httpProxyToBeRemoved
            database.httpProxy = container.decode("httpProxy", SettingsHttpProxy.serializer(), httpProxyDefault)
            return database
        }

        fun fromString(settings: String): Database {
            val database = codableJson.decodeFromString(Serializer, settings)
            if (database.zoom.back.isEmpty()) {
                addDefaultBackZoomPresets(database)
            }
            if (database.zoom.front.isEmpty()) {
                addDefaultFrontZoomPresets(database)
            }
            if (database.bitratePresets.isEmpty()) {
                addDefaultBitratePresets(database)
            }
            addMissingQuickButtons(database)
            for (button in database.quickButtons) {
                if (button.type != SettingsQuickButtonType.interactiveChat &&
                    button.type != SettingsQuickButtonType.cameraPreview &&
                    button.type != SettingsQuickButtonType.interactiveBrowserWidgets
                ) {
                    button.isOn.value = false
                }
            }
            addMissingDeepLinkQuickButtons(database)
            addMissingBundledLuts(database)
            addMissingGoPro(database)
            return database
        }
    }

    object Serializer : KSerializer<Database> by JsonObjectSerializer(
        "Database",
        { it.encode() },
        { decode(it) },
    )
}

private fun addDefaultScenes(database: Database) {
    if (isMac()) {
        var scene = SettingsScene(name = localized("Screen"))
        scene.videoSource.cameraPosition =
            TODO()
        database.scenes.add(scene)
        if (bestFrontCameraId.isNotEmpty()) {
            scene = SettingsScene(name = localized("Front"))
            scene.videoSource.cameraPosition = SettingsSceneCameraPosition.front
            scene.videoSource.frontCameraId = bestFrontCameraId
            database.scenes.add(scene)
        }
    } else {
        var scene = SettingsScene(name = localized("Back"))
        scene.videoSource.cameraPosition = defaultBackCameraPosition
        scene.videoSource.backCameraId = bestBackCameraId
        database.scenes.add(scene)
        scene = SettingsScene(name = localized("Front"))
        scene.videoSource.cameraPosition = SettingsSceneCameraPosition.front
        scene.videoSource.frontCameraId = bestFrontCameraId
        database.scenes.add(scene)
    }
}

private fun addDefaultZoomPresets(database: Database) {
    database.zoom = SettingsZoom()
    addDefaultBackZoomPresets(database)
    addDefaultFrontZoomPresets(database)
}

private fun backCameraVirtualDeviceSwitchOverVideoZoomFactors(): List<Float>? {
    return Cameras.backCameraSwitchOverZoomFactors()
}

private fun backCameraZoomFactorScale(hasUltraWideCamera: Boolean): Float {
    return 1.0f
}

private fun addDefaultBackZoomPresets(database: Database) {
    val zoomFactors = backCameraVirtualDeviceSwitchOverVideoZoomFactors()
    if (zoomFactors != null) {
        val hasUltraWideCamera = hasUltraWideBackCamera
        val scale = backCameraZoomFactorScale(hasUltraWideCamera)
        val xs = mutableListOf<Float>()
        if (hasUltraWideCamera) {
            xs.add(0.5f)
        } else {
            xs.add(1.0f)
        }
        for (factor in zoomFactors) {
            val x = (factor * scale).roundToInt().toFloat()
            val prevX = xs.lastOrNull()
            if (prevX != null) {
                if ((x / prevX) >= 4) {
                    xs.add(2 * prevX)
                }
            }
            xs.add(x)
        }
        xs.add(2 * xs.last())
        database.zoom.back = mutableListOf()
        for (x in xs) {
            val nameX = if (x < 1) formatOneDecimal(x) else x.toInt().toString()
            database.zoom.back.add(
                SettingsZoomPreset(
                    id = UUID.randomUUID(),
                    name = "${nameX}x",
                    x = x,
                ),
            )
        }
    } else {
        database.zoom.back = mutableListOf(
            SettingsZoomPreset(id = UUID.randomUUID(), name = "0.5x", x = 0.5f),
            SettingsZoomPreset(id = UUID.randomUUID(), name = "1x", x = 1.0f),
            SettingsZoomPreset(id = UUID.randomUUID(), name = "2x", x = 2.0f),
            SettingsZoomPreset(id = UUID.randomUUID(), name = "4x", x = 4.0f),
            SettingsZoomPreset(id = UUID.randomUUID(), name = "8x", x = 8.0f),
        )
    }
}

private fun addDefaultFrontZoomPresets(database: Database) {
    database.zoom.front = mutableListOf(
        SettingsZoomPreset(id = UUID.randomUUID(), name = "0.5x", x = 0.5f),
        SettingsZoomPreset(id = UUID.randomUUID(), name = "1x", x = 1.0f),
        SettingsZoomPreset(id = UUID.randomUUID(), name = "2x", x = 2.0f),
        SettingsZoomPreset(id = UUID.randomUUID(), name = "4x", x = 4.0f),
        SettingsZoomPreset(id = UUID.randomUUID(), name = "8x", x = 8.0f),
    )
}

private fun addDefaultBitratePresets(database: Database) {
    database.bitratePresets = mutableListOf(
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 15_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 12_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 9_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 7_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 6_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 5_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 4_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 3_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 2_000_000),
        SettingsBitratePreset(id = UUID.randomUUID(), bitrate = 1_000_000),
    )
}

private fun updateQuickButton(database: Database, button: SettingsQuickButton) {
    val existingButton = database.quickButtons.firstOrNull { it.type == button.type }
    if (existingButton != null) {
        existingButton.name = button.name
        existingButton.imageOn = button.imageOn
        existingButton.imageOff = button.imageOff
    } else {
        database.quickButtons.add(button)
    }
}

private fun quickButtonPageOne(): Int {
    return 1
}

private fun quickButtonPageTwo(): Int {
    return 2
}

private fun quickButtonPageThree(): Int {
    return 3
}

private fun addMissingQuickButtonsPageOne(database: Database) {
    val page = quickButtonPageOne()
    var button = SettingsQuickButton(
        type = SettingsQuickButtonType.torch,
        imageOn = "flashlight.on.fill",
        imageOff = "flashlight.off.fill",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.mute,
        imageOn = "mic.slash",
        imageOff = "mic",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.live,
        imageOn = "dot.radiowaves.left.and.right",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.mic,
        imageOn = "music.mic",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.record,
        imageOn = "record.circle.fill",
        imageOff = "record.circle",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.snapshot,
        imageOn = "camera.aperture",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.widgets,
        imageOn = "photo.on.rectangle.fill",
        imageOff = "photo.on.rectangle",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.localOverlays,
        imageOn = "square.stack.3d.up.slash.fill",
        imageOff = "square.stack.3d.up.slash",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.blackScreen,
        imageOn = "sunset.fill",
        imageOff = "sunset",
        page = page,
    )
    updateQuickButton(database, button)
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.chat,
        imageOn = "message.fill",
        imageOff = "message",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.bitrate,
        imageOn = "speedometer",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.browser,
        imageOn = "globe",
        page = page,
    )
    updateQuickButton(database, button)
}

private fun addMissingQuickButtonsPageTwo(database: Database) {
    val page = quickButtonPageTwo()
    var button = SettingsQuickButton(
        type = SettingsQuickButtonType.draw,
        imageOn = "pencil.line",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.poll,
        imageOn = "chart.bar.xaxis",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.pinch,
        imageOn = "hand.pinch.fill",
        imageOff = "hand.pinch",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.whirlpool,
        imageOn = "tornado",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.blurFaces,
        imageOn = "face.dashed",
        imageOff = "face.dashed",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.privacy,
        imageOn = "circle.rectangle.dashed",
        imageOff = "circle.rectangle.dashed",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.blurText,
        imageOn = "text.redaction",
        imageOff = "text.redaction",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.glasses,
        imageOn = "sunglasses",
        imageOff = "sunglasses",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.sparkle,
        imageOn = "eye",
        imageOff = "eye",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.movie,
        imageOn = "film.fill",
        imageOff = "film",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.fourThree,
        imageOn = "square.fill",
        imageOff = "square",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.crt,
        imageOn = "tv",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.pixellate,
        imageOn = "squareshape.split.2x2",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.grayScale,
        imageOn = "moon.fill",
        imageOff = "moon",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.sepia,
        imageOn = "moonphase.waxing.crescent.inverse",
        imageOff = "moonphase.waning.crescent",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.triple,
        imageOn = "person.3.fill",
        imageOff = "person.3",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.twin,
        imageOn = "person.2.fill",
        imageOff = "person.2",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.moblinInMouth,
        imageOn = "mouth",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.cameraMan,
        imageOn = "video.fill",
        imageOff = "video",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.beauty,
        imageOn = "wand.and.stars",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.luts,
        imageOn = "camera.filters",
        page = page,
    )
    updateQuickButton(database, button)
}

private fun addMissingQuickButtonsPageThree(database: Database) {
    val page = quickButtonPageThree()
    var button = SettingsQuickButton(
        type = SettingsQuickButtonType.obs,
        imageOn = "xserve",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.remote,
        imageOn = "appletvremote.gen1.fill",
        imageOff = "appletvremote.gen1",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.replay,
        imageOn = "play.fill",
        imageOff = "play",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.instantReplay,
        imageOn = "memories",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.djiDevices,
        imageOn = "appletvremote.gen1.fill",
        imageOff = "appletvremote.gen1",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.goPro,
        imageOn = "appletvremote.gen1.fill",
        imageOff = "appletvremote.gen1",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.interactiveChat,
        imageOn = "arrow.up.message.fill",
        imageOff = "arrow.up.message",
        isOn = true,
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.autoSceneSwitcher,
        imageOn = "autostartstop",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.lockScreen,
        imageOn = "lock.fill",
        imageOff = "lock",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.image,
        imageOn = "camera.fill",
        imageOff = "camera",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.cameraPreview,
        imageOn = "camera.rotate.fill",
        imageOff = "camera.rotate",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.stream,
        imageOn = "arrow.left.arrow.right",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.grid,
        imageOn = "grid",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.cameraLevel,
        imageOn = "level.fill",
        imageOff = "level",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.workout,
        imageOn = "figure.run",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.skipCurrentTts,
        imageOn = "waveform.slash",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.pauseTts,
        imageOn = "waveform.badge.xmark",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.moderation,
        imageOn = "shield",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.predefinedMessages,
        imageOn = "list.bullet",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.streamMarker,
        imageOn = "bookmark.fill",
        imageOff = "bookmark",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.navigation,
        imageOn = "arrow.trianglehead.turn.up.right.circle",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.reloadBrowserWidgets,
        imageOn = "arrow.clockwise",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.portrait,
        imageOn = "rectangle.portrait.rotate",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.connectionPriorities,
        imageOn = "phone.connection.fill",
        imageOff = "phone.connection",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.videoPreview,
        imageOn = "person.2.crop.square.stack",
        imageOff = "person.2.crop.square.stack",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.interactiveBrowserWidgets,
        imageOn = "hand.tap.fill",
        imageOff = "hand.tap",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.macros,
        imageOn = "increase.indent",
        imageOff = "increase.indent",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.gimbalTracking,
        imageOn = "iphone.dock.motorized.viewfinder",
        imageOff = "iphone.dock.motorized.viewfinder",
        isOn = true,
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.previewStream,
        imageOn = "video.circle.fill",
        imageOff = "video.circle",
        page = page,
    )
    updateQuickButton(database, button)
    button = SettingsQuickButton(
        type = SettingsQuickButtonType.photoShoot,
        imageOn = "person.crop.square.badge.camera.fill",
        imageOff = "person.crop.square.badge.camera",
        page = page,
    )
    updateQuickButton(database, button)
}

private fun addMissingQuickButtons(database: Database) {
    addMissingQuickButtonsPageOne(database)
    addMissingQuickButtonsPageTwo(database)
    addMissingQuickButtonsPageThree(database)
    database.quickButtons = database.quickButtons.filter { button ->
        if (button.type == SettingsQuickButtonType.unknown) {
            return@filter false
        }
        if (button.type == SettingsQuickButtonType.workout && !isPhone()) {
            return@filter false
        }
        if (button.type == SettingsQuickButtonType.portrait && isMac()) {
            return@filter false
        }
        true
    }.toMutableList()
}

private fun addMissingDeepLinkQuickButtons(database: Database) {
    val quickButtons = database.deepLinkCreator.quickButtons
    for (quickButton in database.quickButtons) {
        val button = DeepLinkCreatorQuickButton()
        val buttonExists = quickButtons.buttons.any { quickButton.type == it.type }
        if (!buttonExists) {
            button.type = quickButton.type
            button.page = quickButton.page.value
            quickButtons.buttons.add(button)
        }
    }
    quickButtons.buttons = quickButtons.buttons
        .filter { it.type != SettingsQuickButtonType.unknown }
        .toMutableList()
}

private fun addMissingBundledLuts(database: Database) {
    val bundledLuts = mutableListOf<SettingsColorLut>()
    for (lut in allBundledLuts) {
        val existingLut = database.color.bundledLuts.firstOrNull { it.name == lut.name }
        if (existingLut != null) {
            bundledLuts.add(existingLut)
        } else {
            bundledLuts.add(lut)
        }
    }
    database.color.bundledLuts = bundledLuts
}

private fun addMissingGoPro(database: Database) {
    val goPro = database.goPro
    if (goPro.launchLiveStream.isEmpty()) {
        goPro.launchLiveStream = mutableListOf(SettingsGoProLaunchLiveStream())
        goPro.selectedLaunchLiveStream = goPro.launchLiveStream.firstOrNull()?.id
    }
}

private fun updateBundledAlertsMediaGallery(database: Database) {
    val bundledImages = mutableListOf<SettingsAlertsMediaGalleryItem>()
    for (image in allBundledAlertsMediaGalleryImages) {
        val existingImage = database.alertsMediaGallery.bundledImages
            .firstOrNull { it.name == image.name }
        if (existingImage != null) {
            bundledImages.add(existingImage)
        } else {
            bundledImages.add(image)
        }
    }
    database.alertsMediaGallery.bundledImages = bundledImages
    val bundledSounds = mutableListOf<SettingsAlertsMediaGalleryItem>()
    for (sound in allBundledAlertsMediaGallerySounds) {
        val existingSound = database.alertsMediaGallery.bundledSounds
            .firstOrNull { it.name == sound.name }
        if (existingSound != null) {
            bundledSounds.add(existingSound)
        } else {
            bundledSounds.add(sound)
        }
    }
    database.alertsMediaGallery.bundledSounds = bundledSounds
}

private fun addScenesToGameController(database: Database) {
    var button = database.gameControllers[0].buttons.value[0]
    button.function.value = SettingsControllerFunction.SWITCH_SCENE
    button.functionData.value.sceneId = database.scenes[0].id
    if (database.scenes.size > 1) {
        button = database.gameControllers[0].buttons.value[1]
        button.function.value = SettingsControllerFunction.SWITCH_SCENE
        button.functionData.value.sceneId = database.scenes[1].id
    }
}

fun getDefaultMic(): SettingsMic {
    if (isMac()) {
        return SettingsMic.bottom
    }
    return SettingsMic.bottom
}

private fun createDefault(): Database {
    val database = Database()
    addDefaultScenes(database)
    addDefaultZoomPresets(database)
    addDefaultBitratePresets(database)
    addMissingQuickButtons(database)
    addMissingDeepLinkQuickButtons(database)
    addScenesToGameController(database)
    addMissingBundledLuts(database)
    return database
}

private const val settingsJsonName = "settings.json"

private val exportDirectories = listOf(
    mediaPlayerStorageDirectory,
    pngTuberStorageDirectory,
    imagesStorageDirectory,
    alertsStorageDirectory,
    vTuberStorageDirectory,
    replayTransitionsStorageDirectory,
)

private val exportFiles = listOf(
    stealthModeImagePath,
    faceBackgroundImagePath,
    controlBarBackgroundImagePath,
)

private val storage = SimpleStringStorage(key = "settings")

private val mainScope = CoroutineScope(Dispatchers.Main)
private val ioScope = CoroutineScope(Dispatchers.IO)

private fun writeZipFile(zip: ZipOutputStream, filename: String, file: File) {
    zip.putNextEntry(ZipEntry(filename))
    file.inputStream().use { input ->
        input.copyTo(zip)
    }
    zip.closeEntry()
}

class Settings {
    private var realDatabase = Database()

    val database: Database
        get() = realDatabase

    fun load() {
        try {
            tryLoadAndMigrate(storage.get())
        } catch (e: Exception) {
            Log.i("Settings", "settings: Failed to load with error $e. Using default.")
            realDatabase = createDefault()
        }
    }

    private fun tryLoadAndMigrate(settings: String) {
        realDatabase = Database.fromString(settings)
        addSensitiveData(realDatabase)
        migrateFromOlderVersions()
    }

    fun store() {
        try {
            val database = extractSensitiveData(realDatabase)
            storage.set(realDatabase.toJsonString())
            insertSensitiveData(realDatabase, database)
        } catch (e: Exception) {
            Log.i("Settings", "settings: Failed to store.")
        }
    }

    fun reset() {
        removeFilesAndFolders()
        realDatabase = createDefault()
        store()
    }

    fun importFromFile(url: URI, onCompleted: (String?) -> Unit) {
        removeFilesAndFolders()
        val root = createAndGetDirectory()
        ioScope.launch {
            val settingsJson = File(root, settingsJsonName)
            settingsJson.delete()
            try {
                ZipFile(File(url.path)).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val target = File(root, entry.name)
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(target).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                mainScope.launch {
                    onCompleted(e.localizedMessage)
                }
                return@launch
            }
            mainScope.launch {
                try {
                    val settings = settingsJson.readBytes()
                    tryLoadAndMigrate(String(settings, Charsets.UTF_8))
                    store()
                    onCompleted(null)
                } catch (e: Exception) {
                    onCompleted(e.localizedMessage)
                }
            }
        }
    }

    fun importFromClipboard(settings: String, onCompleted: (String?) -> Unit) {
        removeFilesAndFolders()
        try {
            tryLoadAndMigrate(settings)
            store()
            onCompleted(null)
        } catch (e: Exception) {
            onCompleted(e.localizedMessage)
        }
    }

    fun exportToFile(onCompleted: (URI?) -> Unit) {
        store()
        val settingsJson = storage.get().toByteArray()
        val name = Build.MODEL
        ioScope.launch {
            val url = File(
                File(System.getProperty("java.io.tmpdir") ?: "."),
                "${name}_${formatFilenameDateAndTime()}.moblinSettings",
            )
            url.delete()
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(url))).use { zip ->
                    zip.putNextEntry(ZipEntry(settingsJsonName))
                    zip.write(settingsJson)
                    zip.closeEntry()
                    val prefixCount = createAndGetDirectory().canonicalPath.length + 1
                    for (fileUrl in exportFiles) {
                        if (fileUrl.exists()) {
                            val relativeFilePath = fileUrl.canonicalPath.drop(prefixCount)
                            writeZipFile(zip, relativeFilePath, fileUrl)
                        }
                    }
                    for (directory in exportDirectories) {
                        val directoryUrl = createAndGetDirectory(directory)
                        directoryUrl.walkTopDown().filter { it.isFile }.forEach { fileUrl ->
                            val relativeFilePath = fileUrl.canonicalPath.drop(prefixCount)
                            writeZipFile(zip, relativeFilePath, fileUrl)
                        }
                    }
                }
                mainScope.launch {
                    onCompleted(url.toURI())
                }
            } catch (e: Exception) {
                mainScope.launch {
                    onCompleted(null)
                }
            }
        }
    }

    private fun removeFilesAndFolders() {
        for (file in exportFiles) {
            file.delete()
        }
    }

    private fun addSensitiveData(database: Database) {
        for (stream in database.streams) {
            val twitchAccessToken = loadTwitchAccessTokenFromKeychain(stream.id)
            if (twitchAccessToken != null) {
                stream.twitchAccessToken = twitchAccessToken
            }
            val kickAccessToken = loadKickAccessTokenFromKeychain(stream.id)
            if (kickAccessToken != null) {
                stream.kickAccessToken = kickAccessToken
            }
        }
    }

    private fun extractSensitiveData(fromDatabase: Database): Database {
        val toDatabase = Database()
        for (fromStream in fromDatabase.streams) {
            val toStream = SettingsStream(name = "")
            toStream.twitchAccessToken = fromStream.twitchAccessToken
            fromStream.twitchAccessToken = ""
            toStream.kickAccessToken = fromStream.kickAccessToken
            fromStream.kickAccessToken = ""
            toDatabase.streams.add(toStream)
        }
        return toDatabase
    }

    private fun insertSensitiveData(toDatabase: Database, fromDatabase: Database) {
        for ((index, fromStream) in fromDatabase.streams.withIndex()) {
            if (index >= toDatabase.streams.size) {
                break
            }
            toDatabase.streams[index].twitchAccessToken = fromStream.twitchAccessToken
            toDatabase.streams[index].kickAccessToken = fromStream.kickAccessToken
        }
    }

    private fun migrateFromOlderVersions() {
        updateBundledAlertsMediaGallery(realDatabase)
        val newButtons = realDatabase.quickButtons
        if (realDatabase.quickButtons.size != newButtons.size) {
            realDatabase.quickButtons = newButtons
            store()
        }
        for (widget in realDatabase.widgets) {
            if (widget.videoSource.cropX > 1.0) {
                widget.videoSource.cropX = 0.0
                store()
            }
        }
        for (widget in realDatabase.widgets) {
            if (widget.videoSource.cropY > 1.0) {
                widget.videoSource.cropY = 0.0
                store()
            }
        }
        for (widget in realDatabase.widgets) {
            if (widget.videoSource.cropWidth > 1.0) {
                widget.videoSource.cropWidth = 1.0
                store()
            }
        }
        for (widget in realDatabase.widgets) {
            if (widget.videoSource.cropHeight > 1.0) {
                widget.videoSource.cropHeight = 1.0
                store()
            }
        }
        for (scene in realDatabase.scenes) {
            for (sceneWidget in scene.widgets) {
                if (sceneWidget.migrated) {
                    continue
                }
                sceneWidget.migrated = true
                store()
                val widget = realDatabase.widgets.firstOrNull { it.id == sceneWidget.widgetId }
                    ?: continue
                if (widget.type != SettingsWidgetType.text) {
                    continue
                }
                if (widget.text.verticalAlignment == SettingsVerticalAlignment.bottom &&
                    widget.text.horizontalAlignment == SettingsHorizontalAlignment.trailing
                ) {
                    sceneWidget.layout.alignment = SettingsAlignment.bottomRight
                    sceneWidget.layout.x = 100 - sceneWidget.layout.x
                    sceneWidget.layout.updateXString()
                    sceneWidget.layout.y = 100 - sceneWidget.layout.y
                    sceneWidget.layout.updateYString()
                } else if (widget.text.verticalAlignment == SettingsVerticalAlignment.top &&
                    widget.text.horizontalAlignment == SettingsHorizontalAlignment.trailing
                ) {
                    sceneWidget.layout.alignment = SettingsAlignment.topRight
                    sceneWidget.layout.x = 100 - sceneWidget.layout.x
                    sceneWidget.layout.updateXString()
                } else if (widget.text.verticalAlignment == SettingsVerticalAlignment.bottom &&
                    widget.text.horizontalAlignment == SettingsHorizontalAlignment.leading
                ) {
                    sceneWidget.layout.alignment = SettingsAlignment.bottomLeft
                    sceneWidget.layout.y = 100 - sceneWidget.layout.y
                    sceneWidget.layout.updateYString()
                }
            }
        }
        for (scene in realDatabase.scenes) {
            for (sceneWidget in scene.widgets) {
                if (sceneWidget.migrated2) {
                    continue
                }
                sceneWidget.migrated2 = true
                store()
                val widget = realDatabase.widgets.firstOrNull { it.id == sceneWidget.widgetId }
                    ?: continue
                if (widget.type != SettingsWidgetType.browser) {
                    continue
                }
                val stream = database.streams.firstOrNull { it.enabled }
                    ?: continue
                val resolution = stream.resolution.dimensions(portrait = stream.portrait)
                val width = (100.0 * widget.browser.width / resolution.width)
                    .coerceIn(1.0, 100.0)
                val height = (100.0 * widget.browser.height / resolution.height)
                    .coerceIn(1.0, 100.0)
                sceneWidget.layout.size = maxOf(width, height)
                sceneWidget.layout.updateSizeString()
            }
        }
    }
}

private fun RgbColor.color(): Color = Color(
    red = red.toFloat() / 255.0f,
    green = green.toFloat() / 255.0f,
    blue = blue.toFloat() / 255.0f,
    alpha = (opacity ?: 1.0).toFloat(),
)
