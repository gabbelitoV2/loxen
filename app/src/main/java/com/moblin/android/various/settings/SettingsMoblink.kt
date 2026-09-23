package com.moblin.android.various.settings

import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.utils.randomName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsMoblinkStreamer.Serializer::class)
class SettingsMoblinkStreamer {
    val enabled = MutableStateFlow(false)

    val port = MutableStateFlow(DefaultTcpPorts.moblinkStreamer)

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMoblinkStreamer {
            val streamer = SettingsMoblinkStreamer()
            streamer.enabled.value = container.decode("enabled", false)
            streamer.port.value = container.decode("port", DefaultTcpPorts.moblinkStreamer.toUShort()).toInt()
            return streamer
        }
    }

    object Serializer : KSerializer<SettingsMoblinkStreamer> by JsonObjectSerializer(
        "SettingsMoblinkStreamer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMoblinkRelay.Serializer::class)
class SettingsMoblinkRelay {
    val enabled = MutableStateFlow(false)

    val name = MutableStateFlow(randomName())

    val url = MutableStateFlow("")

    val manual = MutableStateFlow(false)

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("name", name)
        encode("url", url)
        encode("manual", manual)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMoblinkRelay {
            val relay = SettingsMoblinkRelay()
            relay.enabled.value = container.decode("enabled", false)
            relay.name.value = container.decode("name", randomName())
            relay.url.value = container.decode("url", "") { isValidWebSocketUrl(it) == null }
            relay.manual.value = container.decode("manual", false)
            return relay
        }
    }

    object Serializer : KSerializer<SettingsMoblinkRelay> by JsonObjectSerializer(
        "SettingsMoblinkRelay",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMoblink.Serializer::class)
class SettingsMoblink {
    var streamer: SettingsMoblinkStreamer = SettingsMoblinkStreamer()

    var relay: SettingsMoblinkRelay = SettingsMoblinkRelay()

    var password: String = "1234"

    fun encode(): JsonObject = encodeContainer {
        encode("server", streamer, SettingsMoblinkStreamer.serializer())
        encode("client", relay, SettingsMoblinkRelay.serializer())
        encode("password", password)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMoblink {
            val moblink = SettingsMoblink()
            moblink.streamer = container.decode("server", SettingsMoblinkStreamer.serializer(), SettingsMoblinkStreamer())
            moblink.relay = container.decode("client", SettingsMoblinkRelay.serializer(), SettingsMoblinkRelay())
            moblink.password = container.decode("password", "1234")
            return moblink
        }
    }

    object Serializer : KSerializer<SettingsMoblink> by JsonObjectSerializer(
        "SettingsMoblink",
        { it.encode() },
        { decode(it) },
    )
}
