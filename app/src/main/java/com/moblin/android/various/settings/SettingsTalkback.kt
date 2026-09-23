package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsTalkback.Serializer::class)
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

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("micId", micId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsTalkback {
            val talkback = SettingsTalkback()
            talkback.enabled.value = container.decode("enabled", false)
            talkback.micId.value = container.decode("micId", "")
            return talkback
        }
    }

    object Serializer : KSerializer<SettingsTalkback> by JsonObjectSerializer(
        "SettingsTalkback",
        { it.encode() },
        { decode(it) },
    )
}
