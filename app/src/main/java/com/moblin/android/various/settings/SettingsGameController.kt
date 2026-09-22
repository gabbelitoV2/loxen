package com.moblin.android.various.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

enum class SettingsControllerFunctionSection {
    GENERAL,
    FILTERS,
}

enum class SettingsGimbalMotion(val rawValue: String) {
    KAPOW("kapow"),
    YES("yes"),
    NO("no"),
    WAKEUP("wakeup");

    override fun toString(): String = when (this) {
        KAPOW -> localized("Kapow")
        YES -> localized("Yes")
        NO -> localized("No")
        WAKEUP -> localized("Wakeup")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsGimbalMotion =
            entries.firstOrNull { it.rawValue == rawValue } ?: KAPOW
    }
}

enum class SettingsControllerThumbStickFunction(val rawValue: String) {
    UNUSED("Unused"),
    GIMBAL_PAN_TILT("Gimbal pan and tilt");

    override fun toString(): String = when (this) {
        UNUSED -> localized("Unused")
        GIMBAL_PAN_TILT -> localized("Gimbal pan and tilt")
    }

    @Composable
    fun color(): Color = when (this) {
        UNUSED -> Color.Gray
        else -> MaterialTheme.colorScheme.primary
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsControllerThumbStickFunction =
            entries.firstOrNull { it.rawValue == rawValue } ?: UNUSED
    }
}

enum class SettingsControllerFunction(val rawValue: String) {
    UNUSED("Unused"),
    RECORD("Record"),
    STREAM("Stream"),
    ZOOM_IN("Zoom in"),
    ZOOM_OUT("Zoom out"),
    GIMBAL_UP("Gimbal up"),
    GIMBAL_DOWN("Gimbal down"),
    GIMBAL_LEFT("Gimbal left"),
    GIMBAL_RIGHT("Gimbal right"),
    GIMBAL_PRESET("Gimbal preset"),
    GIMBAL_ANIMATE("Gimbal animate"),
    GIMBAL_TRACKING("Gimbal tracking"),
    MUTE("Mute"),
    TORCH("Torch"),
    BLACK_SCREEN("Black screen"),
    SCENE("Scene"),
    SWITCH_SCENE("Switch scene"),
    WIDGET("Widget"),
    MACRO("Macro"),
    STREAM_DECK_LAYOUT("Stream deck layout"),
    INSTANT_REPLAY("Instant replay"),
    STOP_REPLAY("Stop replay"),
    SNAPSHOT("Snapshot"),
    PAUSE_TTS("Pause TTS"),
    PIXELLATE("Pixellate"),
    MOVIE("Movie"),
    GRAY_SCALE("Gray scale"),
    SEPIA("Sepia"),
    TRIPLE("Triple"),
    TWIN("Twin"),
    FOUR_THREE("4:3"),
    PINCH("Pinch"),
    WHIRLPOOL("Whirlpool"),
    POLL("Poll"),
    BLUR_FACES("Blur faces"),
    PRIVACY("Privacy"),
    BEAUTY("Beauty"),
    CAMERA_MAN("Camera man");

    override fun toString(): String = when (this) {
        UNUSED -> localized("Unused")
        RECORD -> localized("Record")
        STREAM -> localized("Stream")
        ZOOM_IN -> localized("Zoom in")
        ZOOM_OUT -> localized("Zoom out")
        GIMBAL_UP -> localized("Gimbal up")
        GIMBAL_DOWN -> localized("Gimbal down")
        GIMBAL_LEFT -> localized("Gimbal left")
        GIMBAL_RIGHT -> localized("Gimbal right")
        GIMBAL_PRESET -> localized("Gimbal preset")
        GIMBAL_ANIMATE -> localized("Gimbal animate")
        GIMBAL_TRACKING -> localized("Gimbal tracking")
        MUTE -> localized("Mute")
        TORCH -> localized("Torch")
        BLACK_SCREEN -> localized("Stealth mode")
        SCENE -> localized("Scene")
        SWITCH_SCENE -> localized("Switch scene")
        WIDGET -> localized("Widget")
        MACRO -> localized("Macro")
        STREAM_DECK_LAYOUT -> localized("Stream Deck layout")
        INSTANT_REPLAY -> localized("Instant replay")
        STOP_REPLAY -> localized("Stop replay")
        SNAPSHOT -> localized("Snapshot")
        PAUSE_TTS -> localized("Pause TTS")
        PIXELLATE -> localized("Pixellate")
        MOVIE -> localized("Movie")
        GRAY_SCALE -> localized("Gray scale")
        SEPIA -> localized("Sepia")
        TRIPLE -> localized("Triple")
        TWIN -> localized("Twin")
        FOUR_THREE -> localized("4:3")
        PINCH -> localized("Pinch")
        WHIRLPOOL -> localized("Whirlpool")
        POLL -> localized("Poll")
        BLUR_FACES -> localized("Blur faces")
        PRIVACY -> localized("Blur background")
        BEAUTY -> localized("Beauty")
        CAMERA_MAN -> localized("Camera man")
    }

    fun toString(sceneName: String?, widgetName: String?): String = when (this) {
        SCENE -> sceneName?.let { localized("$it scene") } ?: localized("Scene")
        WIDGET -> widgetName?.let { localized("$it widget") } ?: localized("Widget")
        else -> toString()
    }

    @Composable
    fun color(): Color = when (this) {
        UNUSED -> Color.Gray
        else -> MaterialTheme.colorScheme.primary
    }

    fun section(): SettingsControllerFunctionSection = when (this) {
        UNUSED,
        RECORD,
        STREAM,
        ZOOM_IN,
        ZOOM_OUT,
        GIMBAL_UP,
        GIMBAL_DOWN,
        GIMBAL_LEFT,
        GIMBAL_RIGHT,
        GIMBAL_PRESET,
        GIMBAL_ANIMATE,
        GIMBAL_TRACKING,
        MUTE,
        TORCH,
        BLACK_SCREEN,
        SCENE,
        SWITCH_SCENE,
        WIDGET,
        MACRO,
        STREAM_DECK_LAYOUT,
        INSTANT_REPLAY,
        STOP_REPLAY,
        SNAPSHOT,
        PAUSE_TTS -> SettingsControllerFunctionSection.GENERAL
        PIXELLATE,
        MOVIE,
        GRAY_SCALE,
        SEPIA,
        TRIPLE,
        TWIN,
        FOUR_THREE,
        PINCH,
        WHIRLPOOL,
        POLL,
        BLUR_FACES,
        PRIVACY,
        BEAUTY,
        CAMERA_MAN -> SettingsControllerFunctionSection.FILTERS
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsControllerFunction =
            entries.firstOrNull { it.rawValue == rawValue } ?: UNUSED
    }
}

data class SettingsControllerFunctionData(
    var sceneId: UUID? = null,
    var widgetId: UUID? = null,
    var gimbalPresetId: UUID? = null,
    var gimbalMotion: SettingsGimbalMotion = SettingsGimbalMotion.KAPOW,
    var macroId: UUID? = null,
    var streamDeckLayoutId: UUID? = null,
)

@Serializable(with = SettingsGameControllerButtonSerializer::class)
class SettingsGameControllerButton {
    var id: UUID = UUID.randomUUID()
    var name: String = ""
    var text: String = ""
    val function = MutableStateFlow(SettingsControllerFunction.UNUSED)
    val functionData = MutableStateFlow(SettingsControllerFunctionData())

    fun setFunction(value: SettingsControllerFunction) {
        function.value = value
    }

    fun setFunctionData(value: SettingsControllerFunctionData) {
        functionData.value = value
    }
}

@Serializable
data class SettingsGameControllerButtonSurrogate(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("function") val function: String? = null,
    @SerialName("sceneId") val sceneId: String? = null,
    @SerialName("widgetId") val widgetId: String? = null,
    @SerialName("gimbalPresetId") val gimbalPresetId: String? = null,
    @SerialName("gimbalMotion") val gimbalMotion: String? = null,
    @SerialName("macroId") val macroId: String? = null,
    @SerialName("streamDeckLayoutId") val streamDeckLayoutId: String? = null,
)

object SettingsGameControllerButtonSerializer : KSerializer<SettingsGameControllerButton> {
    override val descriptor: SerialDescriptor =
        SettingsGameControllerButtonSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsGameControllerButton) {
        val functionData = value.functionData.value
        val surrogate = SettingsGameControllerButtonSurrogate(
            id = value.id.toString(),
            name = value.name,
            text = value.text,
            function = value.function.value.rawValue,
            sceneId = functionData.sceneId?.toString(),
            widgetId = functionData.widgetId?.toString(),
            gimbalPresetId = functionData.gimbalPresetId?.toString(),
            gimbalMotion = functionData.gimbalMotion.rawValue,
            macroId = functionData.macroId?.toString(),
            streamDeckLayoutId = functionData.streamDeckLayoutId?.toString(),
        )
        encoder.encodeSerializableValue(SettingsGameControllerButtonSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): SettingsGameControllerButton {
        val surrogate = decoder.decodeSerializableValue(SettingsGameControllerButtonSurrogate.serializer())
        val button = SettingsGameControllerButton()
        button.id = surrogate.id?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        button.name = surrogate.name ?: ""
        button.text = surrogate.text ?: ""
        button.setFunction(
            surrogate.function?.let { SettingsControllerFunction.fromRawValue(it) }
                ?: SettingsControllerFunction.UNUSED
        )
        val functionData = SettingsControllerFunctionData()
        functionData.sceneId = surrogate.sceneId?.let { UUID.fromString(it) }
        functionData.widgetId = surrogate.widgetId?.let { UUID.fromString(it) }
        functionData.gimbalPresetId = surrogate.gimbalPresetId?.let { UUID.fromString(it) }
        functionData.gimbalMotion = surrogate.gimbalMotion?.let { SettingsGimbalMotion.fromRawValue(it) }
            ?: SettingsGimbalMotion.KAPOW
        functionData.macroId = surrogate.macroId?.let { UUID.fromString(it) }
        functionData.streamDeckLayoutId = surrogate.streamDeckLayoutId?.let { UUID.fromString(it) }
        button.setFunctionData(functionData)
        return button
    }
}

@Serializable(with = SettingsGameControllerSerializer::class)
class SettingsGameController {
    var id: UUID = UUID.randomUUID()
    val buttons = MutableStateFlow<List<SettingsGameControllerButton>>(emptyList())
    val leftThumbStickFunction = MutableStateFlow(SettingsControllerThumbStickFunction.UNUSED)
    val rightThumbStickFunction = MutableStateFlow(SettingsControllerThumbStickFunction.UNUSED)

    fun setButtons(value: List<SettingsGameControllerButton>) {
        buttons.value = value
    }

    fun setLeftThumbStickFunction(value: SettingsControllerThumbStickFunction) {
        leftThumbStickFunction.value = value
    }

    fun setRightThumbStickFunction(value: SettingsControllerThumbStickFunction) {
        rightThumbStickFunction.value = value
    }

    init {
        val defaultButtons = mutableListOf<SettingsGameControllerButton>()
        var button = SettingsGameControllerButton()
        button.name = "dpad.left.fill"
        button.text = localized("Left")
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "dpad.right.fill"
        button.text = localized("Right")
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "dpad.up.fill"
        button.text = localized("Up")
        button.setFunction(SettingsControllerFunction.ZOOM_IN)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "dpad.down.fill"
        button.text = localized("Down")
        button.setFunction(SettingsControllerFunction.ZOOM_OUT)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "a.circle"
        button.text = "A"
        button.setFunction(SettingsControllerFunction.TORCH)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "b.circle"
        button.text = "B"
        button.setFunction(SettingsControllerFunction.MUTE)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "x.circle"
        button.text = "X"
        button.setFunction(SettingsControllerFunction.BLACK_SCREEN)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "y.circle"
        button.text = "Y"
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "circle.circle"
        button.text = localized("Circle")
        button.setFunction(SettingsControllerFunction.TORCH)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "xmark.circle"
        button.text = localized("X mark")
        button.setFunction(SettingsControllerFunction.MUTE)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "square.circle"
        button.text = localized("Square")
        button.setFunction(SettingsControllerFunction.BLACK_SCREEN)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "triangle.circle"
        button.text = localized("Triangle")
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "zl.rectangle.roundedtop"
        button.text = "ZL"
        button.setFunction(SettingsControllerFunction.STREAM)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "l.rectangle.roundedbottom"
        button.text = "L"
        button.setFunction(SettingsControllerFunction.RECORD)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "zr.rectangle.roundedtop"
        button.text = "ZR"
        button.setFunction(SettingsControllerFunction.INSTANT_REPLAY)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "r.rectangle.roundedbottom"
        button.text = "R"
        button.setFunction(SettingsControllerFunction.SNAPSHOT)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "l2.rectangle.roundedtop"
        button.text = "L2"
        button.setFunction(SettingsControllerFunction.STREAM)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "l1.rectangle.roundedbottom"
        button.text = "L1"
        button.setFunction(SettingsControllerFunction.RECORD)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "r2.rectangle.roundedtop"
        button.text = "R2"
        button.setFunction(SettingsControllerFunction.PIXELLATE)
        defaultButtons.add(button)
        button = SettingsGameControllerButton()
        button.name = "r1.rectangle.roundedbottom"
        button.text = "R1"
        button.setFunction(SettingsControllerFunction.TRIPLE)
        defaultButtons.add(button)
        buttons.value = defaultButtons
    }
}

@Serializable
data class SettingsGameControllerSurrogate(
    @SerialName("id") val id: String? = null,
    @SerialName("buttons") val buttons: List<SettingsGameControllerButton>? = null,
    @SerialName("leftThumbStickFunction") val leftThumbStickFunction: String? = null,
    @SerialName("rightThumbStickFunction") val rightThumbStickFunction: String? = null,
)

object SettingsGameControllerSerializer : KSerializer<SettingsGameController> {
    override val descriptor: SerialDescriptor =
        SettingsGameControllerSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsGameController) {
        val surrogate = SettingsGameControllerSurrogate(
            id = value.id.toString(),
            buttons = value.buttons.value,
            leftThumbStickFunction = value.leftThumbStickFunction.value.rawValue,
            rightThumbStickFunction = value.rightThumbStickFunction.value.rawValue,
        )
        encoder.encodeSerializableValue(SettingsGameControllerSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): SettingsGameController {
        val surrogate = decoder.decodeSerializableValue(SettingsGameControllerSurrogate.serializer())
        val controller = SettingsGameController()
        controller.id = surrogate.id?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        controller.setButtons(surrogate.buttons ?: emptyList())
        controller.setLeftThumbStickFunction(
            surrogate.leftThumbStickFunction?.let { SettingsControllerThumbStickFunction.fromRawValue(it) }
                ?: SettingsControllerThumbStickFunction.UNUSED
        )
        controller.setRightThumbStickFunction(
            surrogate.rightThumbStickFunction?.let { SettingsControllerThumbStickFunction.fromRawValue(it) }
                ?: SettingsControllerThumbStickFunction.UNUSED
        )
        return controller
    }
}
