package com.moblin.android.various.settings

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsSelfieStickSerializer::class)
class SettingsSelfieStick(
    enabled: Boolean = false,
    function: SettingsControllerFunction = SettingsControllerFunction.switchScene,
    functionData: SettingsControllerFunctionData = SettingsControllerFunctionData(),
) {
    val enabled = MutableStateFlow(enabled)

    val function = MutableStateFlow(function)

    val functionData = MutableStateFlow(functionData)

    enum class CodingKeys {
        enabled,
        function,
        sceneId,
        widgetId,
        gimbalPresetId,
        gimbalMotion,
        macroId,
        streamDeckLayoutId
    }
}

object SettingsSelfieStickSerializer : KSerializer<SettingsSelfieStick> {
    private val shapeSerializer: KSerializer<SettingsSelfieStickShape> =
        SettingsSelfieStickShape.serializer()

    override val descriptor: SerialDescriptor = shapeSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SettingsSelfieStick) {
        val functionData = value.functionData.value
        val shape = SettingsSelfieStickShape(
            enabled = value.enabled.value,
            function = value.function.value.rawValue,
            sceneId = functionData.sceneId?.toString(),
            widgetId = functionData.widgetId?.toString(),
            gimbalPresetId = functionData.gimbalPresetId?.toString(),
            gimbalMotion = functionData.gimbalMotion.rawValue,
            macroId = functionData.macroId?.toString(),
            streamDeckLayoutId = functionData.streamDeckLayoutId?.toString(),
        )
        encoder.encodeSerializableValue(shapeSerializer, shape)
    }

    override fun deserialize(decoder: Decoder): SettingsSelfieStick {
        val shape = decoder.decodeSerializableValue(shapeSerializer)
        val functionData = SettingsControllerFunctionData()
        functionData.sceneId = shape.sceneId.toUUIDOrNull()
        functionData.widgetId = shape.widgetId.toUUIDOrNull()
        functionData.gimbalPresetId = shape.gimbalPresetId.toUUIDOrNull()
        functionData.gimbalMotion = shape.gimbalMotion
            ?.let { SettingsGimbalMotion.fromRawValue(it) }
            ?: SettingsGimbalMotion.kapow
        functionData.macroId = shape.macroId.toUUIDOrNull()
        functionData.streamDeckLayoutId = shape.streamDeckLayoutId.toUUIDOrNull()
        return SettingsSelfieStick(
            enabled = shape.enabled,
            function = shape.function
                ?.let { SettingsControllerFunction.fromRawValue(it) }
                ?: SettingsControllerFunction.switchScene,
            functionData = functionData,
        )
    }
}

@Serializable
private data class SettingsSelfieStickShape(
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("function") val function: String? = null,
    @SerialName("sceneId") val sceneId: String? = null,
    @SerialName("widgetId") val widgetId: String? = null,
    @SerialName("gimbalPresetId") val gimbalPresetId: String? = null,
    @SerialName("gimbalMotion") val gimbalMotion: String? = null,
    @SerialName("macroId") val macroId: String? = null,
    @SerialName("streamDeckLayoutId") val streamDeckLayoutId: String? = null,
)

private fun String?.toUUIDOrNull(): UUID? =
    this?.let { runCatching { UUID.fromString(it) }.getOrNull() }
