package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsKeyboardKey.Serializer::class)
class SettingsKeyboardKey {
    var id: UUID = UUID.randomUUID()

    var key: String by Published("")
    var function: SettingsControllerFunction by Published(SettingsControllerFunction.UNUSED)
    var functionData: SettingsControllerFunctionData by Published(SettingsControllerFunctionData())

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("key", key)
        encode("function", function)
        encode("sceneId", functionData.sceneId)
        encode("widgetId", functionData.widgetId)
        encode("gimbalPresetId", functionData.gimbalPresetId)
        encode("gimbalMotion", functionData.gimbalMotion)
        encode("macroId", functionData.macroId)
        encode("streamDeckLayoutId", functionData.streamDeckLayoutId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsKeyboardKey {
            val key = SettingsKeyboardKey()
            key.id = container.decode("id", UUID.randomUUID())
            key.key = container.decode("key", "")
            key.function = container.decode("function", SettingsControllerFunction.UNUSED)
            key.functionData.sceneId = container.decode<UUID?>("sceneId", null)
            key.functionData.widgetId = container.decode<UUID?>("widgetId", null)
            key.functionData.gimbalPresetId = container.decode<UUID?>("gimbalPresetId", null)
            key.functionData.gimbalMotion = container.decode("gimbalMotion", SettingsGimbalMotion.KAPOW)
            key.functionData.macroId = container.decode<UUID?>("macroId", null)
            key.functionData.streamDeckLayoutId = container.decode<UUID?>("streamDeckLayoutId", null)
            return key
        }
    }

    object Serializer : KSerializer<SettingsKeyboardKey> by JsonObjectSerializer(
        "SettingsKeyboardKey",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsKeyboard.Serializer::class)
class SettingsKeyboard {
    var keys: List<SettingsKeyboardKey> by Published(emptyList())

    fun encode(): JsonObject = encodeContainer {
        encode("keys", keys)
    }

    companion object {
        fun decode(container: JsonObject): SettingsKeyboard {
            val keyboard = SettingsKeyboard()
            keyboard.keys = container.decode("keys", ListSerializer(SettingsKeyboardKey.serializer()), emptyList())
            return keyboard
        }
    }

    object Serializer : KSerializer<SettingsKeyboard> by JsonObjectSerializer(
        "SettingsKeyboard",
        { it.encode() },
        { decode(it) },
    )
}
