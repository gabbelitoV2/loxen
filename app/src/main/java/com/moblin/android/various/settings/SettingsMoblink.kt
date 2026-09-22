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
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _port = MutableStateFlow(DefaultTcpPorts.moblinkStreamer)
    val port: StateFlow<Int> = _port.asStateFlow()

    object Serializer : KSerializer<SettingsMoblinkStreamer> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("SettingsMoblinkStreamer") {
                element<Boolean>("enabled")
                element<Int>("port")
            }

        override fun serialize(encoder: Encoder, value: SettingsMoblinkStreamer) {
            val composite = encoder.beginStructure(descriptor)
            composite.encodeBooleanElement(descriptor, 0, value._enabled.value)
            composite.encodeIntElement(descriptor, 1, value._port.value)
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): SettingsMoblinkStreamer {
            val composite = decoder.beginStructure(descriptor)
            val result = SettingsMoblinkStreamer()
            while (true) {
                when (val index = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> result._enabled.value = composite.decodeBooleanElement(descriptor, 0)
                    1 -> result._port.value = composite.decodeIntElement(descriptor, 1)
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
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _name = MutableStateFlow(randomName())
    val name: StateFlow<String> = _name.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _manual = MutableStateFlow(false)
    val manual: StateFlow<Boolean> = _manual.asStateFlow()

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
            composite.encodeBooleanElement(descriptor, 0, value._enabled.value)
            composite.encodeStringElement(descriptor, 1, value._name.value)
            composite.encodeStringElement(descriptor, 2, value._url.value)
            composite.encodeBooleanElement(descriptor, 3, value._manual.value)
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): SettingsMoblinkRelay {
            val composite = decoder.beginStructure(descriptor)
            val result = SettingsMoblinkRelay()
            while (true) {
                when (val index = composite.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> result._enabled.value = composite.decodeBooleanElement(descriptor, 0)
                    1 -> result._name.value = composite.decodeStringElement(descriptor, 1)
                    2 -> {
                        val url = composite.decodeStringElement(descriptor, 2)
                        result._url.value = if (isValidWebSocketUrl(url) == null) url else ""
                    }
                    3 -> result._manual.value = composite.decodeBooleanElement(descriptor, 3)
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
