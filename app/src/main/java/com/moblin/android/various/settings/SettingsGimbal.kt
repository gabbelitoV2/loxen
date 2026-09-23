package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsGimbalPreset.Serializer::class)
class SettingsGimbalPreset : Named {
    var id: UUID = UUID.randomUUID()

    override var name: String = baseName

    var x: Float = 0f

    var y: Float = 0f

    var zoomX: Float = 1f

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("x", x)
        encode("y", y)
        encode("zoomX", zoomX)
    }

    companion object {
        val baseName: String = localized("My preset")

        fun decode(container: JsonObject): SettingsGimbalPreset {
            val preset = SettingsGimbalPreset()
            preset.id = container.decode("id", UUID.randomUUID())
            preset.name = container.decode("name", SettingsGimbalPreset.baseName)
            preset.x = container.decode("x", 0.0f)
            preset.y = container.decode("y", 0.0f)
            preset.zoomX = container.decode("zoomX", 1f)
            return preset
        }
    }

    object Serializer : KSerializer<SettingsGimbalPreset> by JsonObjectSerializer(
        "SettingsGimbalPreset",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsGimbal.Serializer::class)
class SettingsGimbal {
    var zoomSpeed: Float = zoomSpeedDefault

    var naturalZoom: Boolean = true

    var tracking: Boolean = true

    var functionShutter: SettingsControllerFunction = SettingsControllerFunction.RECORD

    var functionDataShutter: SettingsControllerFunctionData = SettingsControllerFunctionData()

    var functionFlip: SettingsControllerFunction = SettingsControllerFunction.SWITCH_SCENE

    var functionDataFlip: SettingsControllerFunctionData = SettingsControllerFunctionData()

    var presets: List<SettingsGimbalPreset> = emptyList()

    fun encode(): JsonObject = encodeContainer {
        encode("zoomSpeed", zoomSpeed)
        encode("naturalZoom", naturalZoom)
        encode("tracking", tracking)
        encode("functionShutter", functionShutter)
        encode("shutterSceneId", functionDataShutter.sceneId)
        encode("shutterWidgetId", functionDataShutter.widgetId)
        encode("shutterGimbalPresetId", functionDataShutter.gimbalPresetId)
        encode("shutterMotion", functionDataShutter.gimbalMotion)
        encode("shutterMacroId", functionDataShutter.macroId)
        encode("shutterStreamDeckLayoutId", functionDataShutter.streamDeckLayoutId)
        encode("functionFlip", functionFlip)
        encode("flipSceneId", functionDataFlip.sceneId)
        encode("flipWidgetId", functionDataFlip.widgetId)
        encode("flipGimbalPresetId", functionDataFlip.gimbalPresetId)
        encode("flipMotion", functionDataFlip.gimbalMotion)
        encode("flipMacroId", functionDataFlip.macroId)
        encode("flipStreamDeckLayoutId", functionDataFlip.streamDeckLayoutId)
        encode("presets", presets, ListSerializer(SettingsGimbalPreset.serializer()))
    }

    companion object {
        const val zoomSpeedDefault: Float = 50f

        fun decode(container: JsonObject): SettingsGimbal {
            val gimbal = SettingsGimbal()
            gimbal.zoomSpeed = container.decode("zoomSpeed", zoomSpeedDefault)
            gimbal.naturalZoom = container.decode("naturalZoom", true)
            gimbal.tracking = container.decode("tracking", true)
            gimbal.functionShutter = container.decode("functionShutter", SettingsControllerFunction.RECORD)
            gimbal.functionDataShutter.sceneId = container.decode<UUID?>("shutterSceneId", null)
            gimbal.functionDataShutter.widgetId = container.decode<UUID?>("shutterWidgetId", null)
            gimbal.functionDataShutter.gimbalPresetId = container.decode<UUID?>("shutterGimbalPresetId", null)
            gimbal.functionDataShutter.gimbalMotion = container.decode("shutterMotion", SettingsGimbalMotion.KAPOW)
            gimbal.functionDataShutter.macroId = container.decode<UUID?>("shutterMacroId", null)
            gimbal.functionDataShutter.streamDeckLayoutId = container.decode<UUID?>("shutterStreamDeckLayoutId", null)
            gimbal.functionFlip = container.decode("functionFlip", SettingsControllerFunction.SWITCH_SCENE)
            gimbal.functionDataFlip.sceneId = container.decode<UUID?>("flipSceneId", null)
            gimbal.functionDataFlip.widgetId = container.decode<UUID?>("flipWidgetId", null)
            gimbal.functionDataFlip.gimbalPresetId = container.decode<UUID?>("flipGimbalPresetId", null)
            gimbal.functionDataFlip.gimbalMotion = container.decode("flipMotion", SettingsGimbalMotion.KAPOW)
            gimbal.functionDataFlip.macroId = container.decode<UUID?>("flipMacroId", null)
            gimbal.functionDataFlip.streamDeckLayoutId = container.decode<UUID?>("flipStreamDeckLayoutId", null)
            gimbal.presets = container.decode("presets", ListSerializer(SettingsGimbalPreset.serializer()), emptyList())
            return gimbal
        }
    }

    object Serializer : KSerializer<SettingsGimbal> by JsonObjectSerializer(
        "SettingsGimbal",
        { it.encode() },
        { decode(it) },
    )
}
