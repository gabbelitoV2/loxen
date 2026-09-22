package com.moblin.android.various.settings

import android.media.Image
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.mediaPlayerCamera
import com.moblin.android.localized
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
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

val defaultStreamUrl = "srt://my_public_ip:4000"
val defaultRtmpStreamUrl = "rtmp://my_public_ip:1935/live/foobar"
val defaultQuickButtonColor = RgbColor(red = 255 / 4, green = 255 / 4, blue = 255 / 4)
val defaultStreamButtonColor = RgbColor(red = 255, green = 59, blue = 48)
val defaultSegmentedPickerSelectedColor =
    RgbColor(red = 142, green = 142, blue = 147, opacity = 0.6)
val defaultSrtLatency: Int = 3000
val minZoomX: Float = 0.5f

private object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

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

@Serializable
class SettingsColorLut(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("type")
    var type: SettingsColorLutType = SettingsColorLutType.bundled,
    @SerialName("name")
    var name: String = "",
    @SerialName("enabled")
    var enabled: Boolean = false,
) {
    fun clone(): SettingsColorLut {
        val new = SettingsColorLut(type = type, name = name)
        new.id = id
        new.enabled = enabled
        return new
    }
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

@Serializable
class SettingsColor(
    @SerialName("space")
    var space: SettingsColorSpace = SettingsColorSpace.srgb,
    @SerialName("lutEnabled")
    var lutEnabled: Boolean = true,
    @SerialName("lut")
    @Serializable(with = UuidSerializer::class)
    var lut: UUID = UUID.randomUUID(),
    @SerialName("bundledLuts")
    var bundledLuts: List<SettingsColorLut> = allBundledLuts,
    @SerialName("diskLuts")
    var diskLuts: List<SettingsColorLut> = emptyList(),
    @SerialName("diskLutsPng")
    var diskLutsPng: List<SettingsColorLut> = emptyList(),
    @SerialName("diskLutsCube")
    var diskLutsCube: List<SettingsColorLut> = emptyList(),
) {
    fun allLuts(): List<SettingsColorLut> {
        return bundledLuts + diskLutsCube + diskLutsPng
    }
}

@Serializable
class SettingsShow(
    @SerialName("chat")
    var chat: Boolean = true,
    @SerialName("viewers")
    var viewers: Boolean = true,
    @SerialName("uptime")
    var uptime: Boolean = true,
    @SerialName("stream")
    var stream: Boolean = false,
    @SerialName("speed")
    var speed: Boolean = true,
    @SerialName("audioLevel")
    var audioLevel: Boolean = true,
    @SerialName("zoom")
    var zoom: Boolean = false,
    @SerialName("zoomPresets")
    var zoomPresets: Boolean = true,
    @SerialName("microphone")
    var microphone: Boolean = false,
    @SerialName("audioBar")
    var audioBar: Boolean = true,
    @SerialName("cameras")
    var cameras: Boolean = false,
    @SerialName("obsStatus")
    var obsStatus: Boolean = true,
    @SerialName("rtmpSpeed")
    var ingests: Boolean = true,
    @SerialName("gameController")
    var gameController: Boolean = true,
    @SerialName("location")
    var location: Boolean = false,
    @SerialName("remoteControl")
    var remoteControl: Boolean = true,
    @SerialName("browserWidgets")
    var browserWidgets: Boolean = true,
    @SerialName("bonding")
    var bonding: Boolean = true,
    @SerialName("events")
    var events: Boolean = true,
    @SerialName("djiDevices")
    var djiDevices: Boolean = true,
    @SerialName("bondingRtts")
    var bondingRtts: Boolean = false,
    @SerialName("moblink")
    var moblink: Boolean = true,
    @SerialName("catPrinter")
    var catPrinter: Boolean = true,
    @SerialName("heartRateDevice")
    var workoutDevice: Boolean = true,
    @SerialName("cpu")
    var systemMonitor: Boolean = false,
)

@Serializable
class SettingsZoomPreset(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    var name: String = "",
    @SerialName("x")
    var x: Float = 1.0f,
) {
    override fun equals(other: Any?): Boolean {
        return other is SettingsZoomPreset && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Serializable
class SettingsZoomSwitchTo(
    @SerialName("level")
    var level: Float = 1.0f,
    @SerialName("x")
    var x: Float = 1.0f,
    @SerialName("enabled")
    var enabled: Boolean = false,
)

@Serializable
class SettingsZoom(
    @SerialName("back")
    var back: MutableList<SettingsZoomPreset> = mutableListOf(),
    @SerialName("front")
    var front: MutableList<SettingsZoomPreset> = mutableListOf(),
    @SerialName("switchToBack")
    var switchToBack: SettingsZoomSwitchTo = SettingsZoomSwitchTo(),
    @SerialName("switchToFront")
    var switchToFront: SettingsZoomSwitchTo = SettingsZoomSwitchTo(),
    @SerialName("speed")
    var speed: Float = 5.0f,
    @SerialName("backgroundColor")
    var backgroundColor: RgbColor = defaultSegmentedPickerSelectedColor,
) {
    @Transient
    var backgroundColorColor: Color = backgroundColor.color()
}

@Serializable
class SettingsBitratePreset(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("bitrate")
    var bitrate: Int = 5_000_000,
)

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

@Serializable
class SettingsTesla(
    @SerialName("vin")
    var vin: String = "",
    @SerialName("privateKey")
    var privateKey: String = "",
    @SerialName("enabled")
    var enabled: Boolean = true,
    @SerialName("bluetoothPeripheralName")
    var bluetoothPeripheralName: String? = null,
    @SerialName("bluetoothPeripheralId")
    @Serializable(with = UuidSerializer::class)
    var bluetoothPeripheralId: UUID? = null,
)

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

@Serializable
class SettingsMediaPlayerFile(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    var name: String = "My video",
) {
    fun clone(): SettingsMediaPlayerFile {
        val new = SettingsMediaPlayerFile()
        new.id = id
        new.name = name
        return new
    }
}

@Serializable
class SettingsMediaPlayer(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    override var name: String = baseName,
    @SerialName("playerId")
    var playerId: String = "",
    @SerialName("autoSelectMic")
    var autoSelectMic: Boolean = true,
    @SerialName("playlist")
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

    companion object {
        val baseName: String = localized("My player")
    }
}

@Serializable
class SettingsMediaPlayers(
    @SerialName("players")
    var players: List<SettingsMediaPlayer> = emptyList(),
)

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

@Serializable
class SettingsReplay(
    @SerialName("start")
    var start: Double = 20.0,
    @SerialName("stop")
    var stop: Double = SettingsReplay.stop,
    @SerialName("speed")
    var speed: SettingsReplaySpeed = SettingsReplaySpeed.one,
) {
    companion object {
        const val stop: Double = 30.0
    }
}

@Serializable
class SettingsCyclingPowerDevice(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    override var name: String = baseName,
    @SerialName("enabled")
    var enabled: Boolean = false,
    @SerialName("bluetoothPeripheralName")
    var bluetoothPeripheralName: String? = null,
    @SerialName("bluetoothPeripheralId")
    @Serializable(with = UuidSerializer::class)
    var bluetoothPeripheralId: UUID? = null,
) : Named {
    companion object {
        val baseName: String = localized("My device")
    }
}

@Serializable
class SettingsCyclingPowerDevices(
    @SerialName("devices")
    var devices: MutableList<SettingsCyclingPowerDevice> = mutableListOf(),
)

val defaultWheelCircumference = 2105

@Serializable
class SettingsWorkoutDevice(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    override var name: String = baseName,
    @SerialName("enabled")
    var enabled: Boolean = false,
    @SerialName("bluetoothPeripheralName")
    var bluetoothPeripheralName: String? = null,
    @SerialName("bluetoothPeripheralId")
    @Serializable(with = UuidSerializer::class)
    var bluetoothPeripheralId: UUID? = null,
    @SerialName("wheelCircumference")
    var wheelCircumference: Int = defaultWheelCircumference,
) : Named {
    companion object {
        val baseName: String = localized("My device")
    }
}

@Serializable
class SettingsWorkoutDevices(
    @SerialName("devices")
    var devices: MutableList<SettingsWorkoutDevice> = mutableListOf(),
)

private val defaultRgbLightColor = RgbColor(red = 0, green = 255, blue = 0)

@Serializable
class SettingsBlackSharkCoolerDevice(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    override var name: String = baseName,
    @SerialName("enabled")
    var enabled: Boolean = false,
    @SerialName("bluetoothPeripheralName")
    var bluetoothPeripheralName: String? = null,
    @SerialName("bluetoothPeripheralId")
    @Serializable(with = UuidSerializer::class)
    var bluetoothPeripheralId: UUID? = null,
    @SerialName("rgbLightEnabled")
    var rgbLightEnabled: Boolean = false,
    @SerialName("rgbLightColor")
    var rgbLightColor: RgbColor = defaultRgbLightColor,
    @SerialName("rgbLightBrightness")
    var rgbLightBrightness: Double = 100.0,
) : Named {
    @Transient
    var rgbLightColorColor: Color = rgbLightColor.color()

    companion object {
        val baseName: String = localized("My cooler")
    }
}

@Serializable
class SettingsBlackSharkCoolerDevices(
    @SerialName("devices")
    var devices: List<SettingsBlackSharkCoolerDevice> = emptyList(),
)

@Serializable
class SettingsNetworkInterfaceName(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("interfaceName")
    var interfaceName: String = "",
    @SerialName("name")
    var name: String = "",
)

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

@Serializable
class WebBrowserBookmarkSettings(
    @SerialName("url")
    var url: String = "https://google.com",
) {
    @Transient
    var id: UUID = UUID.randomUUID()
}

@Serializable
class WebBrowserSettings(
    @SerialName("home")
    var home: String = "https://google.com",
    @SerialName("bookmarks")
    var bookmarks: List<WebBrowserBookmarkSettings> = emptyList(),
)

@Serializable
class SettingsAlertsMediaGalleryItem(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    @SerialName("name")
    var name: String = "",
)

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

@Serializable
class SettingsAlertsMediaGallery(
    @SerialName("bundledImages")
    var bundledImages: List<SettingsAlertsMediaGalleryItem> = allBundledAlertsMediaGalleryImages,
    @SerialName("customImages")
    var customImages: List<SettingsAlertsMediaGalleryItem> = emptyList(),
    @SerialName("bundledSounds")
    var bundledSounds: List<SettingsAlertsMediaGalleryItem> = allBundledAlertsMediaGallerySounds,
    @SerialName("customSounds")
    var customSounds: List<SettingsAlertsMediaGalleryItem> = emptyList(),
) {
    fun getWhiteStarImageId(): UUID {
        return bundledImages[3].id
    }

    fun getGlassesImageId(): UUID {
        return bundledImages[5].id
    }
}

@Serializable
class SettingsDisconnectProtection(
    @SerialName("liveSceneId")
    @Serializable(with = UuidSerializer::class)
    var liveSceneId: UUID? = null,
    @SerialName("fallbackSceneId")
    @Serializable(with = UuidSerializer::class)
    var fallbackSceneId: UUID? = null,
)

@Serializable
enum class SettingsWiFiAwareRole {
    sender,
    receiver;

    override fun toString(): String {
        return when (this) {
            sender -> "Sender"
            receiver -> "Receiver"
        }
    }
}

@Serializable
class SettingsWiFiAware(
    @SerialName("enabled")
    var enabled: Boolean = false,
    @SerialName("role")
    var role: SettingsWiFiAwareRole = SettingsWiFiAwareRole.sender,
)

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

@Serializable
class SettingsFace(
    @Transient
    var blurFaces: Boolean = false,
    @Transient
    var blurText: Boolean = false,
    @Transient
    var blurBackground: Boolean = false,
    @Transient
    var showMoblin: Boolean = false,
    @SerialName("privacyMode")
    var privacyMode: SettingsFacePrivacyMode = SettingsFacePrivacyMode.blur,
    @SerialName("blurStrength")
    var blurStrength: Float = 0.8f,
    @SerialName("pixellateStrength")
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

@Serializable
class SettingsBeauty(
    @SerialName("enabled")
    var enabled: Boolean = false,
    @SerialName("smoothRadius")
    var smoothnessRadius: Float = 10.0f,
    @SerialName("smoothStrength")
    var smoothnessStrength: Float = 0.65f,
    @SerialName("shapePosition")
    var shapePosition: Float = 0.5f,
    @SerialName("shapeRadius")
    var shapeRadius: Float = 0.5f,
    @SerialName("shapeStrength")
    var shapeStrength: Float = 0.5f,
) {
    @Transient
    var settings: SettingsBeautySettings = SettingsBeautySettings.smoothness
}

@Serializable
class SettingsWiFi(
    @SerialName("ssid")
    var ssid: String = "",
    @SerialName("password")
    var password: String = "",
) {
    @Transient
    val id: String
        get() = ssid
}

@Serializable
class Database(
    @SerialName("streams")
    var streams: MutableList<SettingsStream> = mutableListOf(),
    @SerialName("scenes")
    var scenes: MutableList<SettingsScene> = mutableListOf(),
    @SerialName("widgets")
    var widgets: MutableList<SettingsWidget> = mutableListOf(),
    @SerialName("show")
    var show: SettingsShow = SettingsShow(),
    @SerialName("zoom")
    var zoom: SettingsZoom = SettingsZoom(),
    @SerialName("tapToFocus")
    var tapToFocus: Boolean = false,
    @SerialName("bitratePresets")
    var bitratePresets: MutableList<SettingsBitratePreset> = mutableListOf(),
    @SerialName("iconImage")
    var iconImage: String = plainIcon.image(),
    @SerialName("videoStabilizationMode")
    var videoStabilizationMode: SettingsVideoStabilizationMode =
        SettingsVideoStabilizationMode.off,
    @SerialName("chat")
    var chat: SettingsChat = SettingsChat(),
    @SerialName("mic")
    var mic: SettingsMic = getDefaultMic(),
    @SerialName("mics")
    var mics: SettingsMics = SettingsMics(),
    @SerialName("debug")
    @Contextual
    var debug: SettingsDebug = SettingsDebug(),
    @SerialName("quickButtons")
    var quickButtonsGeneral: SettingsQuickButtons = SettingsQuickButtons(),
    @SerialName("globalButtons")
    var quickButtons: MutableList<SettingsQuickButton> = mutableListOf(),
    @SerialName("rtmpServer")
    var rtmpServer: SettingsRtmpServer = SettingsRtmpServer(),
    @SerialName("networkInterfaceNames")
    var networkInterfaceNames: MutableList<SettingsNetworkInterfaceName> = mutableListOf(),
    @SerialName("lowBitrateWarning")
    var lowBitrateWarning: Boolean = true,
    @SerialName("vibrate")
    var vibrate: Boolean = false,
    @SerialName("gameControllers")
    var gameControllers: MutableList<SettingsGameController> =
        mutableListOf(SettingsGameController()),
    @SerialName("remoteControl")
    var remoteControl: SettingsRemoteControl = SettingsRemoteControl(),
    @SerialName("startStopRecordingConfirmations")
    var startStopRecordingConfirmations: Boolean = true,
    @SerialName("color")
    var color: SettingsColor = SettingsColor(),
    @SerialName("mirrorFrontCameraOnStream")
    var mirrorFrontCameraOnStream: Boolean = true,
    @SerialName("streamButtonColor")
    var streamButtonColor: RgbColor = defaultStreamButtonColor,
    @SerialName("location")
    var location: SettingsLocation = SettingsLocation(),
    @SerialName("watch")
    var watch: JsonObject = JsonObject(emptyMap()),
    @SerialName("audio")
    var audio: SettingsAudio = SettingsAudio(),
    @SerialName("macros")
    var macros: SettingsMacros = SettingsMacros(),
    @SerialName("webBrowser")
    var webBrowser: WebBrowserSettings = WebBrowserSettings(),
    @SerialName("deepLinkCreator")
    var deepLinkCreator: DeepLinkCreator = DeepLinkCreator(),
    @SerialName("srtlaServer")
    var srtlaServer: SettingsSrtlaServer = SettingsSrtlaServer(),
    @SerialName("mediaPlayers")
    var mediaPlayers: SettingsMediaPlayers = SettingsMediaPlayers(),
    @SerialName("showAllSettings")
    var showAllSettings: Boolean = false,
    @SerialName("portrait")
    var portrait: Boolean = false,
    @SerialName("djiDevices")
    var djiDevices: SettingsDjiDevices = SettingsDjiDevices(),
    @SerialName("alertsMediaGallery")
    var alertsMediaGallery: SettingsAlertsMediaGallery = SettingsAlertsMediaGallery(),
    @SerialName("catPrinters")
    var catPrinters: SettingsCatPrinters = SettingsCatPrinters(),
    @SerialName("verboseStatuses")
    var verboseStatuses: Boolean = false,
    @SerialName("scoreboardPlayers")
    var scoreboardPlayers: MutableList<SettingsWidgetScoreboardPlayer> = mutableListOf(),
    @SerialName("keyboard")
    var keyboard: SettingsKeyboard = SettingsKeyboard(),
    @SerialName("tesla")
    var tesla: SettingsTesla = SettingsTesla(),
    @SerialName("srtlaRelay")
    var srtlaRelay: SettingsMoblink = SettingsMoblink(),
    @SerialName("pixellateStrength")
    var pixellateStrength: Float = 0.3f,
    @SerialName("moblink")
    var moblink: SettingsMoblink = SettingsMoblink(),
    @SerialName("sceneSwitchTransition")
    var sceneSwitchTransition: SettingsSceneSwitchTransition =
        SettingsSceneSwitchTransition.blur,
    @SerialName("forceSceneSwitchTransition")
    var forceSceneSwitchTransition: Boolean = false,
    @SerialName("alwaysAttachCameraPreview")
    var alwaysAttachCameraPreview: Boolean = false,
    @SerialName("alwaysAttachPhotoShoot")
    var alwaysAttachPhotoShoot: Boolean = false,
    @SerialName("cameraControlsEnabled")
    var cameraControlsEnabled: Boolean = false,
    @SerialName("externalDisplayContent")
    var externalDisplayContent: SettingsExternalDisplayContent =
        SettingsExternalDisplayContent.stream,
    @SerialName("cyclingPowerDevices")
    var cyclingPowerDevices: SettingsCyclingPowerDevices = SettingsCyclingPowerDevices(),
    @SerialName("cyclingPowerDevicesMigrated")
    var cyclingPowerDevicesMigrated: Boolean = false,
    @SerialName("heartRateDevices")
    var workoutDevices: SettingsWorkoutDevices = SettingsWorkoutDevices(),
    @SerialName("phoneCoolerDevices")
    var blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices =
        SettingsBlackSharkCoolerDevices(),
    @SerialName("remoteSceneId")
    @Serializable(with = UuidSerializer::class)
    var remoteSceneId: UUID? = null,
    @SerialName("sceneNumericInput")
    var sceneNumericInput: Boolean = false,
    @SerialName("savedWifiNetworks")
    var savedWifiNetworks: List<SettingsWiFi> = emptyList(),
    @SerialName("goPro")
    var goPro: SettingsGoPro = SettingsGoPro(),
    @SerialName("replay")
    var replay: SettingsReplay = SettingsReplay(),
    @SerialName("portraitVideoOffsetFromTop")
    var portraitVideoOffsetFromTop: Double = 0.0,
    @SerialName("autoSceneSwitchers")
    var autoSceneSwitchers: SettingsAutoSceneSwitchers = SettingsAutoSceneSwitchers(),
    @SerialName("fixedHorizon")
    var fixedHorizon: Boolean = false,
    @SerialName("whirlpoolAngle")
    var whirlpoolAngle: Float = (PI / 2).toFloat(),
    @SerialName("pinchScale")
    var pinchScale: Float = 0.5f,
    @SerialName("selfieStick")
    var selfieStick: SettingsSelfieStick = SettingsSelfieStick(),
    @SerialName("bigButtons")
    var bigButtons: Boolean = false,
    @SerialName("verticalButtons")
    var verticalButtons: Boolean = false,
    @SerialName("bigAudioLevelMeter")
    var bigAudioLevelMeter: Boolean = false,
    @SerialName("ristServer")
    var ristServer: SettingsRistServer = SettingsRistServer(),
    @SerialName("disconnectProtection")
    var disconnectProtection: SettingsDisconnectProtection = SettingsDisconnectProtection(),
    @SerialName("rtspClient")
    var rtspClient: SettingsRtspClient = SettingsRtspClient(),
    @SerialName("srtClient")
    var srtClient: SettingsSrtClient = SettingsSrtClient(),
    @SerialName("whipServer")
    var whipServer: SettingsWhipServer = SettingsWhipServer(),
    @SerialName("whepClient")
    var whepClient: SettingsWhepClient = SettingsWhepClient(),
    @SerialName("navigation")
    var navigation: SettingsNavigation = SettingsNavigation(),
    @SerialName("wiFiAware")
    var wiFiAware: SettingsWiFiAware = SettingsWiFiAware(),
    @SerialName("face")
    var face: SettingsFace = SettingsFace(),
    @SerialName("beauty")
    var beauty: SettingsBeauty = SettingsBeauty(),
    @SerialName("talkBack")
    var talkback: SettingsTalkback = SettingsTalkback(),
    @SerialName("gimbal")
    var gimbal: SettingsGimbal = SettingsGimbal(),
    @SerialName("scoreboardSizeMigrated")
    var scoreboardSizeMigrated: Boolean = false,
    @SerialName("streamDecks")
    var streamDecks: SettingsStreamDecks = SettingsStreamDecks(),
    @SerialName("graphicsImplementation")
    var graphicsImplementation: SettingsGraphicsImplementation =
        SettingsGraphicsImplementation.coreImage,
    @SerialName("graphicsHighQualityDownsampling")
    var graphicsHighQualityDownsampling: Boolean = false,
    @SerialName("ingestsSoftwareVideoDecoding")
    var ingestsSoftwareVideoDecoding: Boolean = false,
    @SerialName("torchLevel")
    var torchLevel: Float = 1.0f,
    @SerialName("appMode")
    var appMode: SettingsAppMode = SettingsAppMode.streaming,
    @SerialName("httpProxy")
    var httpProxy: SettingsHttpProxy = SettingsHttpProxy(),
) {
    @Transient
    var streamButtonColorColor: Color = defaultStreamButtonColor.color()

    fun getSavedWiFiNetwork(ssid: String): SettingsWiFi? {
        return savedWifiNetworks.firstOrNull { it.ssid == ssid }
    }

    fun getHighestBitratePreset(): Int {
        return bitratePresets.sortedByDescending { it.bitrate }.firstOrNull()?.bitrate ?: 5_000_000
    }

    fun toJsonString(): String {
        return json.encodeToString(this)
    }

    private fun applyDecodeMigrations(root: JsonObject) {
        if (debug.preferStereoMicToBeRemoved) {
            audio._preferStereoMic.value = true
            debug.preferStereoMicToBeRemoved = false
        }
        if (!root.containsKey("moblink")) {
            moblink = srtlaRelay
        }
        if (!cyclingPowerDevicesMigrated) {
            for (cyclingPowerDevice in cyclingPowerDevices.devices) {
                val alreadyThere = workoutDevices.devices.any {
                    it.bluetoothPeripheralId == cyclingPowerDevice.bluetoothPeripheralId
                }
                if (!alreadyThere) {
                    val workoutDevice = SettingsWorkoutDevice()
                    workoutDevice.id = cyclingPowerDevice.id
                    workoutDevice.name = cyclingPowerDevice.name
                    workoutDevice.enabled = cyclingPowerDevice.enabled
                    workoutDevice.bluetoothPeripheralName = cyclingPowerDevice.bluetoothPeripheralName
                    workoutDevice.bluetoothPeripheralId = cyclingPowerDevice.bluetoothPeripheralId
                    workoutDevices.devices.add(workoutDevice)
                }
            }
            cyclingPowerDevicesMigrated = true
        }
        if (!root.containsKey("face")) {
            face = debug.faceToBeRemoved
        }
        if (!root.containsKey("graphicsHighQualityDownsampling")) {
            graphicsHighQualityDownsampling = debug.highQualityDownsamplingToBeRemoved
        }
        if (!root.containsKey("httpProxy") && debug.httpProxyToBeRemoved) {
            httpProxy.enabled.value = true
        }
        if (!scoreboardSizeMigrated) {
            for (widget in widgets) {
                if (widget.type != SettingsWidgetType.scoreboard) {
                    continue
                }
                for (scene in scenes) {
                    for (sceneWidget in scene.widgets) {
                        if (sceneWidget.widgetId == widget.id) {
                            sceneWidget.layout.size = defaultScoreboardSize
                        }
                    }
                }
            }
            scoreboardSizeMigrated = true
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }

        fun fromString(settings: String): Database {
            val root = json.parseToJsonElement(settings).jsonObject
            val database = json.decodeFromJsonElement<Database>(root)
            database.applyDecodeMigrations(root)
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
    return emptyList()
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
