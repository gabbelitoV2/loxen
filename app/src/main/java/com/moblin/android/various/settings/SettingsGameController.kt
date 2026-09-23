package com.moblin.android.various.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

private fun <T> rawValueSerializer(
    serialName: String,
    rawValue: (T) -> String,
    fromRawValue: (String) -> T?,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(rawValue(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeString()
        return fromRawValue(value) ?: throw SerializationException("Unknown $serialName raw value '$value'")
    }
}

private fun <T> caseNameSerializer(serialName: String, cases: List<T>, caseName: (T) -> String): KSerializer<T> =
    JsonObjectSerializer(
        serialName,
        { value -> JsonObject(mapOf(caseName(value) to JsonObject(emptyMap()))) },
        { container ->
            val keys = container.keys.filter { key -> cases.any { caseName(it) == key } }
            if (keys.size != 1) {
                throw SerializationException("$serialName expects exactly one case key")
            }
            if (container[keys[0]] !is JsonObject) {
                throw SerializationException("$serialName case value must be an object")
            }
            cases.first { caseName(it) == keys[0] }
        },
    )

enum class SettingsControllerFunctionSection {
    GENERAL,
    FILTERS,
}

@Serializable(with = SettingsGimbalMotion.Serializer::class)
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

    object Serializer : KSerializer<SettingsGimbalMotion> by caseNameSerializer(
        "com.moblin.android.various.settings.SettingsGimbalMotion",
        entries,
        { it.rawValue },
    )
}

@Serializable(with = SettingsControllerThumbStickFunction.Serializer::class)
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

    object Serializer : KSerializer<SettingsControllerThumbStickFunction> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsControllerThumbStickFunction",
        { it.rawValue },
        { rawValue -> entries.firstOrNull { it.rawValue == rawValue } },
    )
}

@Serializable(with = SettingsControllerFunction.Serializer::class)
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

    object Serializer : KSerializer<SettingsControllerFunction> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsControllerFunction",
        { it.rawValue },
        { rawValue -> entries.firstOrNull { it.rawValue == rawValue } },
    )
}

data class SettingsControllerFunctionData(
    var sceneId: UUID? = null,
    var widgetId: UUID? = null,
    var gimbalPresetId: UUID? = null,
    var gimbalMotion: SettingsGimbalMotion = SettingsGimbalMotion.KAPOW,
    var macroId: UUID? = null,
    var streamDeckLayoutId: UUID? = null,
)

@Serializable(with = SettingsGameControllerButton.Serializer::class)
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("text", text)
        encode("function", function)
        encode("sceneId", functionData.value.sceneId)
        encode("widgetId", functionData.value.widgetId)
        encode("gimbalPresetId", functionData.value.gimbalPresetId)
        encode("gimbalMotion", functionData.value.gimbalMotion)
        encode("macroId", functionData.value.macroId)
        encode("streamDeckLayoutId", functionData.value.streamDeckLayoutId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsGameControllerButton {
            val button = SettingsGameControllerButton()
            button.id = container.decode("id", UUID.randomUUID())
            button.name = container.decode("name", "")
            button.text = container.decode("text", "")
            button.function.value = container.decode("function", SettingsControllerFunction.UNUSED)
            button.functionData.value.sceneId = container.decode<UUID?>("sceneId", null)
            button.functionData.value.widgetId = container.decode<UUID?>("widgetId", null)
            button.functionData.value.gimbalPresetId = container.decode<UUID?>("gimbalPresetId", null)
            button.functionData.value.gimbalMotion = container.decode("gimbalMotion", SettingsGimbalMotion.KAPOW)
            button.functionData.value.macroId = container.decode<UUID?>("macroId", null)
            button.functionData.value.streamDeckLayoutId = container.decode<UUID?>("streamDeckLayoutId", null)
            return button
        }
    }

    object Serializer : KSerializer<SettingsGameControllerButton> by JsonObjectSerializer(
        "SettingsGameControllerButton",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsGameController.Serializer::class)
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("buttons", buttons)
        encode("leftThumbStickFunction", leftThumbStickFunction)
        encode("rightThumbStickFunction", rightThumbStickFunction)
    }

    companion object {
        fun decode(container: JsonObject): SettingsGameController {
            val controller = SettingsGameController()
            controller.id = container.decode("id", UUID.randomUUID())
            controller.buttons.value = container.decode(
                "buttons",
                ListSerializer(SettingsGameControllerButton.serializer()),
                emptyList(),
            )
            controller.leftThumbStickFunction.value = container.decode(
                "leftThumbStickFunction",
                SettingsControllerThumbStickFunction.UNUSED,
            )
            controller.rightThumbStickFunction.value = container.decode(
                "rightThumbStickFunction",
                SettingsControllerThumbStickFunction.UNUSED,
            )
            return controller
        }
    }

    object Serializer : KSerializer<SettingsGameController> by JsonObjectSerializer(
        "SettingsGameController",
        { it.encode() },
        { decode(it) },
    )
}
