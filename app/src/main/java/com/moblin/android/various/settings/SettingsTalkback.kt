package com.moblin.android.various.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsTalkbackSerializer::class)
class SettingsTalkback {
    val enabled = MutableStateFlow(false)

    val micId = MutableStateFlow("")

    enum class CodingKeys(val rawValue: String) {
        enabled("enabled"),
        micId("micId");

        companion object {
            fun fromRawValue(rawValue: String): CodingKeys? =
                entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled.value = value
    }

    fun setMicId(value: String) {
        micId.value = value
    }
}

object SettingsTalkbackSerializer : KSerializer<SettingsTalkback> {
    private val encodedSerializer = EncodedSettings.serializer()

    override val descriptor: SerialDescriptor = encodedSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SettingsTalkback) {
        encoder.encodeSerializableValue(
            encodedSerializer,
            EncodedSettings(
                enabled = value.enabled.value,
                micId = value.micId.value
            )
        )
    }

    override fun deserialize(decoder: Decoder): SettingsTalkback {
        val decoded = decoder.decodeSerializableValue(encodedSerializer)
        return SettingsTalkback().apply {
            setEnabled(decoded.enabled)
            setMicId(decoded.micId)
        }
    }
}

@Serializable
@SerialName("SettingsTalkback")
private data class EncodedSettings(
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("micId") val micId: String = ""
)
