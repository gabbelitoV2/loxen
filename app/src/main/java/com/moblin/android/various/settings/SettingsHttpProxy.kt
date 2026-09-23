package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.network.DefaultTcpPorts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsHttpProxy.Serializer::class)
class SettingsHttpProxy(
    initialEnabled: Boolean = false,
    initialLocalNetwork: Boolean = false,
    initialPort: Int = DefaultTcpPorts.httpProxy,
) {
    val enabled = MutableStateFlow(initialEnabled)

    val localNetwork = MutableStateFlow(initialLocalNetwork)

    val port = MutableStateFlow(initialPort)

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("localNetwork", localNetwork)
        encode("port", port)
    }

    companion object {
        fun decode(container: JsonObject): SettingsHttpProxy {
            val httpProxy = SettingsHttpProxy()
            httpProxy.enabled.value = container.decode("enabled", false)
            httpProxy.localNetwork.value = container.decode("localNetwork", false)
            httpProxy.port.value = container.decode("port", DefaultTcpPorts.httpProxy) { it in 0..65535 }
            return httpProxy
        }
    }

    object Serializer : KSerializer<SettingsHttpProxy> by JsonObjectSerializer(
        "SettingsHttpProxy",
        { it.encode() },
        { decode(it) },
    )
}
