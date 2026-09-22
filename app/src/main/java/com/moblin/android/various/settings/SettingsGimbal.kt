package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private object UuidStringSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.moblin.android.various.settings.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
class SettingsGimbalPreset : Named {
    companion object {
        val baseName: String = localized("My preset")
    }

    @SerialName("id")
    @Serializable(with = UuidStringSerializer::class)
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("x")
    var x: Float = 0f

    @SerialName("y")
    var y: Float = 0f

    @SerialName("zoomX")
    var zoomX: Float = 1f
}

@Serializable
private data class SettingsGimbalData(
    @SerialName("zoomSpeed") val zoomSpeed: Float? = null,
    @SerialName("naturalZoom") val naturalZoom: Boolean? = null,
    @SerialName("tracking") val tracking: Boolean? = null,
    @SerialName("functionShutter") val functionShutter: SettingsControllerFunction? = null,
    @SerialName("shutterSceneId") val shutterSceneId: String? = null,
    @SerialName("shutterWidgetId") val shutterWidgetId: String? = null,
    @SerialName("shutterGimbalPresetId") val shutterGimbalPresetId: String? = null,
    @SerialName("shutterMotion") val shutterMotion: SettingsGimbalMotion? = null,
    @SerialName("shutterMacroId") val shutterMacroId: String? = null,
    @SerialName("shutterStreamDeckLayoutId") val shutterStreamDeckLayoutId: String? = null,
    @SerialName("functionFlip") val functionFlip: SettingsControllerFunction? = null,
    @SerialName("flipSceneId") val flipSceneId: String? = null,
    @SerialName("flipWidgetId") val flipWidgetId: String? = null,
    @SerialName("flipGimbalPresetId") val flipGimbalPresetId: String? = null,
    @SerialName("flipMotion") val flipMotion: SettingsGimbalMotion? = null,
    @SerialName("flipMacroId") val flipMacroId: String? = null,
    @SerialName("flipStreamDeckLayoutId") val flipStreamDeckLayoutId: String? = null,
    @SerialName("presets") val presets: List<SettingsGimbalPreset>? = null,
)

object SettingsGimbalSerializer : KSerializer<SettingsGimbal> {
    override val descriptor: SerialDescriptor = SettingsGimbalData.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsGimbal) {
        val data = SettingsGimbalData(
            zoomSpeed = value.zoomSpeed,
            naturalZoom = value.naturalZoom,
            tracking = value.tracking,
            functionShutter = value.functionShutter,
            shutterSceneId = value.functionDataShutter.sceneId?.toString(),
            shutterWidgetId = value.functionDataShutter.widgetId?.toString(),
            shutterGimbalPresetId = value.functionDataShutter.gimbalPresetId?.toString(),
            shutterMotion = value.functionDataShutter.gimbalMotion,
            shutterMacroId = value.functionDataShutter.macroId?.toString(),
            shutterStreamDeckLayoutId = value.functionDataShutter.streamDeckLayoutId?.toString(),
            functionFlip = value.functionFlip,
            flipSceneId = value.functionDataFlip.sceneId?.toString(),
            flipWidgetId = value.functionDataFlip.widgetId?.toString(),
            flipGimbalPresetId = value.functionDataFlip.gimbalPresetId?.toString(),
            flipMotion = value.functionDataFlip.gimbalMotion,
            flipMacroId = value.functionDataFlip.macroId?.toString(),
            flipStreamDeckLayoutId = value.functionDataFlip.streamDeckLayoutId?.toString(),
            presets = value.presets,
        )
        encoder.encodeSerializableValue(SettingsGimbalData.serializer(), data)
    }

    override fun deserialize(decoder: Decoder): SettingsGimbal {
        val data = decoder.decodeSerializableValue(SettingsGimbalData.serializer())
        val settings = SettingsGimbal()
        settings.zoomSpeed = data.zoomSpeed ?: SettingsGimbal.zoomSpeedDefault
        settings.naturalZoom = data.naturalZoom ?: true
        settings.tracking = data.tracking ?: true
        settings.functionShutter = data.functionShutter ?: SettingsControllerFunction.RECORD
        settings.functionDataShutter.sceneId = data.shutterSceneId?.let { UUID.fromString(it) }
        settings.functionDataShutter.widgetId = data.shutterWidgetId?.let { UUID.fromString(it) }
        settings.functionDataShutter.gimbalPresetId =
            data.shutterGimbalPresetId?.let { UUID.fromString(it) }
        settings.functionDataShutter.gimbalMotion = data.shutterMotion ?: SettingsGimbalMotion.KAPOW
        settings.functionDataShutter.macroId = data.shutterMacroId?.let { UUID.fromString(it) }
        settings.functionDataShutter.streamDeckLayoutId =
            data.shutterStreamDeckLayoutId?.let { UUID.fromString(it) }
        settings.functionFlip = data.functionFlip ?: SettingsControllerFunction.SWITCH_SCENE
        settings.functionDataFlip.sceneId = data.flipSceneId?.let { UUID.fromString(it) }
        settings.functionDataFlip.widgetId = data.flipWidgetId?.let { UUID.fromString(it) }
        settings.functionDataFlip.gimbalPresetId = data.flipGimbalPresetId?.let { UUID.fromString(it) }
        settings.functionDataFlip.gimbalMotion = data.flipMotion ?: SettingsGimbalMotion.KAPOW
        settings.functionDataFlip.macroId = data.flipMacroId?.let { UUID.fromString(it) }
        settings.functionDataFlip.streamDeckLayoutId =
            data.flipStreamDeckLayoutId?.let { UUID.fromString(it) }
        settings.presets = data.presets ?: emptyList()
        return settings
    }
}

@Serializable(with = SettingsGimbalSerializer::class)
class SettingsGimbal {
    companion object {
        const val zoomSpeedDefault: Float = 50f
    }

    var zoomSpeed: Float = zoomSpeedDefault

    var naturalZoom: Boolean = true

    var tracking: Boolean = true

    var functionShutter: SettingsControllerFunction = SettingsControllerFunction.RECORD

    var functionDataShutter: SettingsControllerFunctionData = SettingsControllerFunctionData()

    var functionFlip: SettingsControllerFunction = SettingsControllerFunction.SWITCH_SCENE

    var functionDataFlip: SettingsControllerFunctionData = SettingsControllerFunctionData()

    var presets: List<SettingsGimbalPreset> = emptyList()
}
