package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsQuickButtonTypeSerializer::class)
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
}

object SettingsQuickButtonTypeSerializer : KSerializer<SettingsQuickButtonType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SettingsQuickButtonType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettingsQuickButtonType) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): SettingsQuickButtonType {
        return SettingsQuickButtonType.fromRawValue(decoder.decodeString())
    }
}

@Serializable
private class SettingsQuickButtonSurrogate(
    @SerialName("name") val name: String = "",
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("type") val type: SettingsQuickButtonType = SettingsQuickButtonType.unknown,
    @SerialName("systemImageNameOn") val systemImageNameOn: String = "",
    @SerialName("systemImageNameOff") val systemImageNameOff: String = "",
    @SerialName("isOn") val isOn: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("backgroundColor") val backgroundColor: RgbColor = defaultQuickButtonColor,
    @SerialName("page") val page: Int = 1,
)

object SettingsQuickButtonSerializer : KSerializer<SettingsQuickButton> {
    override val descriptor: SerialDescriptor = SettingsQuickButtonSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsQuickButton) {
        val surrogate = SettingsQuickButtonSurrogate(
            name = value.name,
            id = value.id.toString(),
            type = value.type,
            systemImageNameOn = value.imageOn,
            systemImageNameOff = value.imageOff,
            isOn = value.isOn.value,
            enabled = value.enabled.value,
            backgroundColor = value.backgroundColor,
            page = value.page.value,
        )
        SettingsQuickButtonSurrogate.serializer().serialize(encoder, surrogate)
    }

    override fun deserialize(decoder: Decoder): SettingsQuickButton {
        val surrogate = SettingsQuickButtonSurrogate.serializer().deserialize(decoder)
        val button = SettingsQuickButton(
            type = surrogate.type,
            imageOn = surrogate.systemImageNameOn,
            imageOff = surrogate.systemImageNameOff,
            isOn = surrogate.isOn,
            page = surrogate.page,
        )
        button.id = runCatching { UUID.fromString(surrogate.id) }.getOrDefault(UUID.randomUUID())
        button.name = surrogate.name
        button.enabled.value = surrogate.enabled
        button.backgroundColor = surrogate.backgroundColor
        button.color.value = surrogate.backgroundColor.color()
        return button
    }
}

@Serializable(with = SettingsQuickButtonSerializer::class)
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
}

@Serializable
private class SettingsQuickButtonsSurrogate(
    @SerialName("twoColumns") val twoColumns: Boolean = true,
    @SerialName("bigButtons") val bigButtons: Boolean = false,
    @SerialName("showName") val showName: Boolean = true,
    @SerialName("enableScroll") val enableScroll: Boolean = true,
    @SerialName("blackScreenShowChat") val blackScreenShowChat: Boolean = false,
    @SerialName("blackScreenShowStatus") val blackScreenShowStatus: Boolean = false,
    @SerialName("backgroundImageCropX") val backgroundImageCropX: Double = 0.0,
    @SerialName("backgroundImageCropY") val backgroundImageCropY: Double = 0.0,
    @SerialName("backgroundImageCropWidth") val backgroundImageCropWidth: Double = 1.0,
    @SerialName("backgroundImageCropHeight") val backgroundImageCropHeight: Double = 1.0,
    @SerialName("backgroundImageOpacity") val backgroundImageOpacity: Double = 1.0,
)

object SettingsQuickButtonsSerializer : KSerializer<SettingsQuickButtons> {
    override val descriptor: SerialDescriptor = SettingsQuickButtonsSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsQuickButtons) {
        val surrogate = SettingsQuickButtonsSurrogate(
            twoColumns = value.twoColumns.value,
            bigButtons = value.bigButtons.value,
            showName = value.showName.value,
            enableScroll = value.enableScroll.value,
            blackScreenShowChat = value.stealthModeShowChat.value,
            blackScreenShowStatus = value.stealthModeShowStatus.value,
            backgroundImageCropX = value.backgroundImageCropX,
            backgroundImageCropY = value.backgroundImageCropY,
            backgroundImageCropWidth = value.backgroundImageCropWidth,
            backgroundImageCropHeight = value.backgroundImageCropHeight,
            backgroundImageOpacity = value.backgroundImageOpacity.value,
        )
        SettingsQuickButtonsSurrogate.serializer().serialize(encoder, surrogate)
    }

    override fun deserialize(decoder: Decoder): SettingsQuickButtons {
        val surrogate = SettingsQuickButtonsSurrogate.serializer().deserialize(decoder)
        val quickButtons = SettingsQuickButtons()
        quickButtons.twoColumns.value = surrogate.twoColumns
        quickButtons.bigButtons.value = surrogate.bigButtons
        quickButtons.showName.value = surrogate.showName
        quickButtons.enableScroll.value = surrogate.enableScroll
        quickButtons.stealthModeShowChat.value = surrogate.blackScreenShowChat
        quickButtons.stealthModeShowStatus.value = surrogate.blackScreenShowStatus
        quickButtons.backgroundImageCropX = surrogate.backgroundImageCropX
        quickButtons.backgroundImageCropY = surrogate.backgroundImageCropY
        quickButtons.backgroundImageCropWidth = surrogate.backgroundImageCropWidth
        quickButtons.backgroundImageCropHeight = surrogate.backgroundImageCropHeight
        quickButtons.backgroundImageOpacity.value = surrogate.backgroundImageOpacity
        return quickButtons
    }
}

@Serializable(with = SettingsQuickButtonsSerializer::class)
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

    constructor()
}
