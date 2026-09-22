package com.moblin.android.various.settings

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsKeyboardKeySerializer::class)
class SettingsKeyboardKey {
    var id: UUID = UUID.randomUUID()

    private val _key = MutableStateFlow("")

    var key: String
        get() = _key.value
        set(value) {
            _key.value = value
        }

    private val function = MutableStateFlow(SettingsControllerFunction.unused)

    var function: SettingsControllerFunction
        get() = function.value
        set(value) {
            function.value = value
        }

    private val functionData = MutableStateFlow(SettingsControllerFunctionData())

    var functionData: SettingsControllerFunctionData
        get() = functionData.value
        set(value) {
            functionData.value = value
        }
}

object SettingsKeyboardKeySerializer : KSerializer<SettingsKeyboardKey> {
    private val surrogate = SettingsKeyboardKeyData.serializer()

    override val descriptor: SerialDescriptor = surrogate.descriptor

    override fun serialize(encoder: Encoder, value: SettingsKeyboardKey) {
        encoder.encodeSerializableValue(surrogate, value.toSettingsKeyboardKeyData())
    }

    override fun deserialize(decoder: Decoder): SettingsKeyboardKey {
        return decoder.decodeSerializableValue(surrogate).toSettingsKeyboardKey()
    }
}

@Serializable
private class SettingsKeyboardKeyData(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("key") val key: String = "",
    @SerialName("function") val function: String =
        encodeControllerFunctionRawValue(SettingsControllerFunction.unused),
    @SerialName("sceneId") val sceneId: String? = null,
    @SerialName("widgetId") val widgetId: String? = null,
    @SerialName("gimbalPresetId") val gimbalPresetId: String? = null,
    @SerialName("gimbalMotion") val gimbalMotion: String =
        encodeGimbalMotionRawValue(SettingsGimbalMotion.kapow),
    @SerialName("macroId") val macroId: String? = null,
    @SerialName("streamDeckLayoutId") val streamDeckLayoutId: String? = null
)

@Serializable(with = SettingsKeyboardSerializer::class)
class SettingsKeyboard {
    private val _keys = MutableStateFlow<List<SettingsKeyboardKey>>(emptyList())

    var keys: List<SettingsKeyboardKey>
        get() = _keys.value
        set(value) {
            _keys.value = value
        }
}

object SettingsKeyboardSerializer : KSerializer<SettingsKeyboard> {
    private val surrogate = SettingsKeyboardData.serializer()

    override val descriptor: SerialDescriptor = surrogate.descriptor

    override fun serialize(encoder: Encoder, value: SettingsKeyboard) {
        encoder.encodeSerializableValue(
            surrogate,
            SettingsKeyboardData(keys = value.keys.map { it.toSettingsKeyboardKeyData() })
        )
    }

    override fun deserialize(decoder: Decoder): SettingsKeyboard {
        val data = decoder.decodeSerializableValue(surrogate)
        val result = SettingsKeyboard()
        result.keys = data.keys.map { it.toSettingsKeyboardKey() }
        return result
    }
}

@Serializable
private class SettingsKeyboardData(
    @SerialName("keys") val keys: List<SettingsKeyboardKeyData> = emptyList()
)

private fun SettingsKeyboardKey.toSettingsKeyboardKeyData(): SettingsKeyboardKeyData {
    val functionData = functionData
    return SettingsKeyboardKeyData(
        id = id.toString(),
        key = key,
        function = encodeControllerFunctionRawValue(function),
        sceneId = functionData.sceneId?.toString(),
        widgetId = functionData.widgetId?.toString(),
        gimbalPresetId = functionData.gimbalPresetId?.toString(),
        gimbalMotion = encodeGimbalMotionRawValue(functionData.gimbalMotion),
        macroId = functionData.macroId?.toString(),
        streamDeckLayoutId = functionData.streamDeckLayoutId?.toString()
    )
}

private fun SettingsKeyboardKeyData.toSettingsKeyboardKey(): SettingsKeyboardKey {
    val result = SettingsKeyboardKey()
    result.id = UUID.fromString(id)
    result.key = key
    result.function = decodeControllerFunctionRawValue(function)
    val functionData = SettingsControllerFunctionData()
    functionData.sceneId = sceneId?.let { UUID.fromString(it) }
    functionData.widgetId = widgetId?.let { UUID.fromString(it) }
    functionData.gimbalPresetId = gimbalPresetId?.let { UUID.fromString(it) }
    functionData.gimbalMotion = decodeGimbalMotionRawValue(gimbalMotion)
    functionData.macroId = macroId?.let { UUID.fromString(it) }
    functionData.streamDeckLayoutId = streamDeckLayoutId?.let { UUID.fromString(it) }
    result.functionData = functionData
    return result
}

private fun encodeControllerFunctionRawValue(value: SettingsControllerFunction): String {
    return value.rawValue.toString()
}

private fun decodeControllerFunctionRawValue(value: String): SettingsControllerFunction {
    return SettingsControllerFunction.entries.firstOrNull { it.rawValue.toString() == value }
        ?: SettingsControllerFunction.unused
}

private fun encodeGimbalMotionRawValue(value: SettingsGimbalMotion): String {
    return value.rawValue.toString()
}

private fun decodeGimbalMotionRawValue(value: String): SettingsGimbalMotion {
    return SettingsGimbalMotion.entries.firstOrNull { it.rawValue.toString() == value }
        ?: SettingsGimbalMotion.kapow
}
