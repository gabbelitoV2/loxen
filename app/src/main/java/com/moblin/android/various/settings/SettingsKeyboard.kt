package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsKeyboardKey.Serializer::class)
class SettingsKeyboardKey {
    var id: UUID = UUID.randomUUID()

    private val _key = MutableStateFlow("")

    var key: String
        get() = _key.value
        set(value) {
            _key.value = value
        }

    private val _function = MutableStateFlow(SettingsControllerFunction.UNUSED)

    var function: SettingsControllerFunction
        get() = _function.value
        set(value) {
            _function.value = value
        }

    private val _functionData = MutableStateFlow(SettingsControllerFunctionData())

    var functionData: SettingsControllerFunctionData
        get() = _functionData.value
        set(value) {
            _functionData.value = value
        }

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
    private val _keys = MutableStateFlow<List<SettingsKeyboardKey>>(emptyList())

    var keys: List<SettingsKeyboardKey>
        get() = _keys.value
        set(value) {
            _keys.value = value
        }

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
