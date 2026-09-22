package com.moblin.android.various.settings

import com.moblin.android.various.network.DefaultTcpPorts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsHttpProxySerializer::class)
class SettingsHttpProxy(
    initialEnabled: Boolean = false,
    initialLocalNetwork: Boolean = false,
    initialPort: Int = DefaultTcpPorts.httpProxy,
) {
    val enabled = MutableStateFlow(initialEnabled)

    val localNetwork = MutableStateFlow(initialLocalNetwork)

    val port = MutableStateFlow(initialPort)
}

@Serializable
private class SettingsHttpProxyWire(
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("localNetwork") val localNetwork: Boolean = false,
    @SerialName("port") val port: Int = DefaultTcpPorts.httpProxy,
)

object SettingsHttpProxySerializer : KSerializer<SettingsHttpProxy> {
    private val wireSerializer = SettingsHttpProxyWire.serializer()

    override val descriptor: SerialDescriptor = wireSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SettingsHttpProxy) {
        encoder.encodeSerializableValue(
            wireSerializer,
            SettingsHttpProxyWire(
                enabled = value.enabled.value,
                localNetwork = value.localNetwork.value,
                port = value.port.value,
            ),
        )
    }

    override fun deserialize(decoder: Decoder): SettingsHttpProxy {
        val wire = decoder.decodeSerializableValue(wireSerializer)
        return SettingsHttpProxy(
            initialEnabled = wire.enabled,
            initialLocalNetwork = wire.localNetwork,
            initialPort = wire.port,
        )
    }
}
