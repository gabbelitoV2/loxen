package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsQuickButtonType.Serializer::class)
enum class SettingsQuickButtonType(val rawValue: String) {
    unknown("Unknown"),
    torch("Torch"),
    mute("Mute"),
    bitrate("Bitrate"),
    mic("Mic"),
    chat("Chat"),
    blackScreen("Black screen"),
    record("Record"),
    image("Image"),
    movie("Movie"),
    grayScale("Gray scale"),
    sepia("Sepia"),
    triple("Triple"),
    twin("Twin"),
    pixellate("Pixellate"),
    stream("Stream"),
    grid("Grid"),
    cameraLevel("Camera level"),
    obs("OBS"),
    remote("Remote"),
    draw("Draw"),
    localOverlays("Local overlays"),
    browser("Browser"),
    cameraPreview("Camera preview"),
    fourThree("4:3"),
    crt("CRT"),
    poll("Poll"),
    snapshot("Snapshot"),
    widgets("Widgets"),
    luts("LUTs"),
    workout("Workout"),
    moderation("Moderation"),
    predefinedMessages("Predefined messages"),
    skipCurrentTts("Skip current TTS"),
    streamMarker("Stream marker"),
    reloadBrowserWidgets("Reload browser widgets"),
    interactiveChat("Interactive chat"),
    lockScreen("Lock screen"),
    djiDevices("DJI devices"),
    portrait("Portrait"),
    goPro("GoPro"),
    replay("Replay"),
    connectionPriorities("Connection priorities"),
    instantReplay("Instant replay"),
    pinch("Pinch"),
    whirlpool("Whirlpool"),
    autoSceneSwitcher("Auto scene switcher"),
    pauseTts("Pause TTS"),
    live("Live"),
    navigation("Navigation"),
    blurFaces("Blur faces"),
    blurText("Blur text"),
    privacy("Privacy"),
    moblinInMouth("Moblin in mouth"),
    glasses("Glasses"),
    sparkle("Sparkle"),
    beauty("Beauty filter"),
    cameraMan("Camera man"),
    videoPreview("Video preview"),
    interactiveBrowserWidgets("Interactive browser widgets"),
    macros("Macros"),
    gimbalTracking("Gimbal tracking"),
    previewStream("Preview stream"),
    photoShoot("Photo shoot");

    override fun toString(): String {
        return when (this) {
            unknown -> localized("Unknown")
            torch -> localized("Torch")
            mute -> localized("Mute")
            live -> localized("Stream")
            mic -> localized("Mic")
            record -> localized("Record")
            snapshot -> localized("Snapshot")
            widgets -> localized("Scene widgets")
            localOverlays -> localized("Local overlays")
            blackScreen -> localized("Stealth mode")
            chat -> localized("Chat")
            bitrate -> localized("Bitrate")
            browser -> localized("Browser")
            draw -> localized("Draw")
            poll -> localized("Poll")
            pinch -> localized("Pinch")
            whirlpool -> localized("Whirlpool")
            blurFaces -> localized("Blur faces")
            privacy -> localized("Blur background")
            blurText -> localized("Blur text")
            glasses -> localized("Glasses")
            sparkle -> localized("Sparkle")
            movie -> localized("Movie")
            fourThree -> localized("4:3")
            crt -> localized("CRT")
            pixellate -> localized("Pixellate")
            grayScale -> localized("Gray scale")
            sepia -> localized("Sepia")
            triple -> localized("Triple")
            twin -> localized("Twin")
            moblinInMouth -> localized("Moblin in mouth")
            cameraMan -> localized("Camera man")
            beauty -> localized("Beauty")
            luts -> localized("LUTs")
            obs -> localized("OBS")
            remote -> localized("Remote")
            replay -> localized("Replay")
            instantReplay -> localized("Instant replay")
            djiDevices -> localized("DJI devices")
            goPro -> localized("GoPro")
            interactiveChat -> localized("Scrollable chat")
            autoSceneSwitcher -> localized("Auto scene switcher")
            lockScreen -> localized("Lock screen")
            image -> localized("Camera")
            cameraPreview -> localized("Camera preview")
            stream -> localized("Switch stream")
            grid -> localized("Grid")
            cameraLevel -> localized("Camera level")
            workout -> localized("Workout")
            skipCurrentTts -> localized("Skip current TTS")
            pauseTts -> localized("Pause TTS")
            moderation -> localized("Moderation")
            predefinedMessages -> localized("Predefined messages")
            streamMarker -> localized("Stream marker")
            navigation -> localized("Navigation")
            reloadBrowserWidgets -> localized("Reload browser widgets")
            portrait -> localized("Portrait")
            connectionPriorities -> localized("Connection priorities")
            videoPreview -> localized("Video preview")
            interactiveBrowserWidgets -> localized("Interactive browser widgets")
            macros -> localized("Macros")
            gimbalTracking -> localized("Gimbal tracking")
            previewStream -> localized("Preview stream")
            photoShoot -> localized("Photo shoot")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsQuickButtonType {
            val v = if (value == "Pause chat") "Chat" else value
            return entries.firstOrNull { it.rawValue == v } ?: unknown
        }

        fun filters(): List<SettingsQuickButtonType> {
            return listOf(
                movie,
                fourThree,
                crt,
                grayScale,
                sepia,
                triple,
                twin,
                pixellate,
                whirlpool,
                pinch,
                blurFaces,
                privacy,
                moblinInMouth,
                beauty,
                cameraMan,
                poll,
            )
        }
    }

    object Serializer : KSerializer<SettingsQuickButtonType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "com.moblin.android.various.settings.SettingsQuickButtonType",
            PrimitiveKind.STRING,
        )

        override fun serialize(encoder: Encoder, value: SettingsQuickButtonType) {
            encoder.encodeString(value.rawValue)
        }

        override fun deserialize(decoder: Decoder): SettingsQuickButtonType {
            return fromRawValue(decoder.decodeString())
        }
    }
}

@Serializable(with = SettingsQuickButton.Serializer::class)
class SettingsQuickButton {
    var id: UUID = UUID.randomUUID()
    var name: String
    var type: SettingsQuickButtonType
    var imageOn: String
    var imageOff: String
    val isOn = MutableStateFlow(false)
    val enabled = MutableStateFlow(true)
    var backgroundColor: RgbColor = defaultQuickButtonColor
    val color = MutableStateFlow(defaultQuickButtonColor.color())
    val page = MutableStateFlow(1)

    constructor(
        type: SettingsQuickButtonType,
        imageOn: String,
        imageOff: String? = null,
        isOn: Boolean = false,
        page: Int = 1,
    ) {
        name = type.toString()
        this.type = type
        this.imageOn = imageOn
        this.imageOff = imageOff ?: imageOn
        this.isOn.value = isOn
        this.page.value = page
    }

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("id", id)
        encode("type", type)
        encode("systemImageNameOn", imageOn)
        encode("systemImageNameOff", imageOff)
        encode("isOn", isOn)
        encode("enabled", enabled)
        encode("backgroundColor", backgroundColor)
        encode("page", page)
    }

    companion object {
        fun decode(container: JsonObject): SettingsQuickButton {
            val button = SettingsQuickButton(type = SettingsQuickButtonType.unknown, imageOn = "")
            button.name = container.decode("name", "")
            button.id = container.decode("id", UUID.randomUUID())
            button.type = container.decode("type", SettingsQuickButtonType.unknown)
            button.imageOn = container.decode("systemImageNameOn", "")
            button.imageOff = container.decode("systemImageNameOff", "")
            button.isOn.value = container.decode("isOn", false)
            button.enabled.value = container.decode("enabled", true)
            button.backgroundColor = container.decode("backgroundColor", defaultQuickButtonColor)
            button.color.value = button.backgroundColor.color()
            button.page.value = container.decode("page", 1)
            return button
        }
    }

    object Serializer : KSerializer<SettingsQuickButton> by JsonObjectSerializer(
        "SettingsQuickButton",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsQuickButtons.Serializer::class)
class SettingsQuickButtons {
    val twoColumns = MutableStateFlow(true)
    val bigButtons = MutableStateFlow(false)
    val showName = MutableStateFlow(true)
    val enableScroll = MutableStateFlow(true)
    val stealthModeShowChat = MutableStateFlow(false)
    val stealthModeShowStatus = MutableStateFlow(false)
    var backgroundImageCropX: Double = 0.0
    var backgroundImageCropY: Double = 0.0
    var backgroundImageCropWidth: Double = 1.0
    var backgroundImageCropHeight: Double = 1.0
    val backgroundImageOpacity = MutableStateFlow(1.0)

    fun encode(): JsonObject = encodeContainer {
        encode("twoColumns", twoColumns)
        encode("bigButtons", bigButtons)
        encode("showName", showName)
        encode("enableScroll", enableScroll)
        encode("blackScreenShowChat", stealthModeShowChat)
        encode("blackScreenShowStatus", stealthModeShowStatus)
        encode("backgroundImageCropX", backgroundImageCropX)
        encode("backgroundImageCropY", backgroundImageCropY)
        encode("backgroundImageCropWidth", backgroundImageCropWidth)
        encode("backgroundImageCropHeight", backgroundImageCropHeight)
        encode("backgroundImageOpacity", backgroundImageOpacity)
    }

    constructor()

    companion object {
        fun decode(container: JsonObject): SettingsQuickButtons {
            val quickButtons = SettingsQuickButtons()
            quickButtons.twoColumns.value = container.decode("twoColumns", true)
            quickButtons.bigButtons.value = container.decode("bigButtons", false)
            quickButtons.showName.value = container.decode("showName", true)
            quickButtons.enableScroll.value = container.decode("enableScroll", true)
            quickButtons.stealthModeShowChat.value = container.decode("blackScreenShowChat", false)
            quickButtons.stealthModeShowStatus.value = container.decode("blackScreenShowStatus", false)
            quickButtons.backgroundImageCropX = container.decode("backgroundImageCropX", 0.0)
            quickButtons.backgroundImageCropY = container.decode("backgroundImageCropY", 0.0)
            quickButtons.backgroundImageCropWidth = container.decode("backgroundImageCropWidth", 1.0)
            quickButtons.backgroundImageCropHeight = container.decode("backgroundImageCropHeight", 1.0)
            quickButtons.backgroundImageOpacity.value = container.decode("backgroundImageOpacity", 1.0)
            return quickButtons
        }
    }

    object Serializer : KSerializer<SettingsQuickButtons> by JsonObjectSerializer(
        "SettingsQuickButtons",
        { it.encode() },
        { decode(it) },
    )
}

private fun RgbColor.color(): Color =
    Color(
        red = red.toFloat() / 255f,
        green = green.toFloat() / 255f,
        blue = blue.toFloat() / 255f,
    )
