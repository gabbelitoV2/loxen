package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsSelfieStick.Serializer::class)
class SettingsSelfieStick(
    enabled: Boolean = false,
    function: SettingsControllerFunction = SettingsControllerFunction.SWITCH_SCENE,
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

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("function", function)
        encode("sceneId", functionData.value.sceneId)
        encode("widgetId", functionData.value.widgetId)
        encode("gimbalPresetId", functionData.value.gimbalPresetId)
        encode("gimbalMotion", functionData.value.gimbalMotion)
        encode("macroId", functionData.value.macroId)
        encode("streamDeckLayoutId", functionData.value.streamDeckLayoutId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsSelfieStick {
            val selfieStick = SettingsSelfieStick()
            selfieStick.enabled.value = container.decode("enabled", false)
            selfieStick.function.value = container.decode("function", SettingsControllerFunction.SWITCH_SCENE)
            selfieStick.functionData.value = SettingsControllerFunctionData(
                sceneId = container.decode<UUID?>("sceneId", null),
                widgetId = container.decode<UUID?>("widgetId", null),
                gimbalPresetId = container.decode<UUID?>("gimbalPresetId", null),
                gimbalMotion = container.decode("gimbalMotion", SettingsGimbalMotion.KAPOW),
                macroId = container.decode<UUID?>("macroId", null),
                streamDeckLayoutId = container.decode<UUID?>("streamDeckLayoutId", null),
            )
            return selfieStick
        }
    }

    object Serializer : KSerializer<SettingsSelfieStick> by JsonObjectSerializer(
        "SettingsSelfieStick",
        { it.encode() },
        { decode(it) },
    )
}
