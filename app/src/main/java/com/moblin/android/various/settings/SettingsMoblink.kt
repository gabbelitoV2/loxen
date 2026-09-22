package com.moblin.android.various.settings

import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.utils.randomName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsMoblinkStreamer.Serializer::class)
class SettingsMoblinkStreamer {
    val enabled = MutableStateFlow(false)

    val port = MutableStateFlow(DefaultTcpPorts.moblinkStreamer)

    object Serializer : KSerializer<SettingsMoblinkStreamer> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("SettingsMoblinkStreamer") {
                element<Boolean>("enabled")
                element<Int>("port")
            }

        override fun serialize(encoder: Encoder, value: SettingsMoblinkStreamer) {
            val composite = encoder.beginStructure(descriptor)
            composite.encodeBooleanElement(descriptor, 0, value.enabled.value)
            composite.encodeIntElement(descriptor, 1, value.port.value)
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): SettingsMoblinkStreamer {
            val composite = decoder.beginStructure(descriptor)
            val result = SettingsMoblinkStreamer()
            while (true) {
                when (val index = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> result.enabled.value = composite.decodeBooleanElement(descriptor, 0)
                    1 -> result.port.value = composite.decodeIntElement(descriptor, 1)
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            composite.endStructure(descriptor)
            return result
        }
    }
}

@Serializable(with = SettingsMoblinkRelay.Serializer::class)
class SettingsMoblinkRelay {
    val enabled = MutableStateFlow(false)

    val name = MutableStateFlow(randomName())

    val url = MutableStateFlow("")

    val manual = MutableStateFlow(false)

    object Serializer : KSerializer<SettingsMoblinkRelay> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("SettingsMoblinkRelay") {
                element<Boolean>("enabled")
                element<String>("name")
                element<String>("url")
                element<Boolean>("manual")
            }

        override fun serialize(encoder: Encoder, value: SettingsMoblinkRelay) {
            val composite = encoder.beginStructure(descriptor)
            composite.encodeBooleanElement(descriptor, 0, value.enabled.value)
            composite.encodeStringElement(descriptor, 1, value.name.value)
            composite.encodeStringElement(descriptor, 2, value.url.value)
            composite.encodeBooleanElement(descriptor, 3, value.manual.value)
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): SettingsMoblinkRelay {
            val composite = decoder.beginStructure(descriptor)
            val result = SettingsMoblinkRelay()
            while (true) {
                when (val index = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> result.enabled.value = composite.decodeBooleanElement(descriptor, 0)
                    1 -> result.name.value = composite.decodeStringElement(descriptor, 1)
                    2 -> {
                        val url = composite.decodeStringElement(descriptor, 2)
                        result.url.value = if (isValidWebSocketUrl(url) == null) url else ""
                    }
                    3 -> result.manual.value = composite.decodeBooleanElement(descriptor, 3)
                    else -> throw SerializationException("Unexpected index $index")
                }
            }
            composite.endStructure(descriptor)
            return result
        }
    }
}

@Serializable
class SettingsMoblink {
    @SerialName("server")
    var streamer: SettingsMoblinkStreamer = SettingsMoblinkStreamer()

    @SerialName("client")
    var relay: SettingsMoblinkRelay = SettingsMoblinkRelay()

    @SerialName("password")
    var password: String = "1234"
}
