package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.utils.Named
import com.moblin.android.various.utils.randomHumanString
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsRemoteControlAssistant.Serializer::class)
class SettingsRemoteControlAssistant(
    var id: UUID = UUID.randomUUID(),
    override var name: String = SettingsRemoteControlAssistant.baseName,
    var enabled: Boolean = false,
    var port: Int = 0,
    var relay: SettingsRemoteControlServerRelay = SettingsRemoteControlServerRelay(),
) : Named {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("enabled", enabled)
        encode("port", port)
        encode("relay", relay)
    }

    companion object {
        val baseName: String = localized("Streamer name")

        fun decode(container: JsonObject): SettingsRemoteControlAssistant {
            val assistant = SettingsRemoteControlAssistant()
            assistant.id = container.decode("id", UUID.randomUUID())
            assistant.name = container.decode("name", baseName)
            assistant.enabled = container.decode("enabled", false)
            assistant.port = container.decode("port", 0) { it in 0..65535 }
            assistant.relay = container.decode(
                "relay",
                SettingsRemoteControlServerRelay.serializer(),
                SettingsRemoteControlServerRelay(),
            )
            return assistant
        }
    }

    object Serializer : KSerializer<SettingsRemoteControlAssistant> by JsonObjectSerializer(
        "SettingsRemoteControlAssistant",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRemoteControlStreamerUrl.Serializer::class)
class SettingsRemoteControlStreamerUrl(
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var url: String = "",
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("url", url)
    }

    companion object {
        fun decode(container: JsonObject): SettingsRemoteControlStreamerUrl {
            val streamerUrl = SettingsRemoteControlStreamerUrl()
            streamerUrl.id = container.decode("id", UUID.randomUUID())
            streamerUrl.name = container.decode("name", "")
            streamerUrl.url = container.decode("url", "")
            return streamerUrl
        }
    }

    object Serializer : KSerializer<SettingsRemoteControlStreamerUrl> by JsonObjectSerializer(
        "SettingsRemoteControlStreamerUrl",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRemoteControlStreamer.Serializer::class)
class SettingsRemoteControlStreamer(
    var enabled: Boolean = false,
    var name: String = "",
    var url: String = "",
    var previewFps: Float = 1.0f,
    var reliableChatAndEvents: Boolean = false,
    var savedUrls: List<SettingsRemoteControlStreamerUrl> = emptyList(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("name", name)
        encode("url", url)
        encode("previewFps", previewFps)
        encode("reliableChatAndEvents", reliableChatAndEvents)
        encode("savedUrls", savedUrls)
    }

    companion object {
        fun decode(container: JsonObject): SettingsRemoteControlStreamer {
            val streamer = SettingsRemoteControlStreamer()
            streamer.enabled = container.decode("enabled", false)
            streamer.name = container.decode("name", "")
            streamer.url = container.decode("url", "")
            streamer.previewFps = container.decode("previewFps", 1.0f)
            streamer.reliableChatAndEvents = container.decode("reliableChatAndEvents", false)
            streamer.savedUrls = container.decode(
                "savedUrls",
                ListSerializer(SettingsRemoteControlStreamerUrl.serializer()),
                emptyList(),
            )
            return streamer
        }
    }

    object Serializer : KSerializer<SettingsRemoteControlStreamer> by JsonObjectSerializer(
        "SettingsRemoteControlStreamer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRemoteControlServerRelay.Serializer::class)
class SettingsRemoteControlServerRelay(
    var enabled: Boolean = true,
    var baseUrl: String = "wss://moblin.mys-lang.org/moblin-remote-control-relay",
    var bridgeId: String = UUID.randomUUID().toString().lowercase(),
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("baseUrl", baseUrl)
        encode("bridgeId", bridgeId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsRemoteControlServerRelay {
            val relay = SettingsRemoteControlServerRelay()
            relay.enabled = container.decode("enabled", false)
            relay.baseUrl = container.decode(
                "baseUrl",
                "wss://moblin.mys-lang.org/moblin-remote-control-relay",
            )
            relay.bridgeId = container.decode("bridgeId", UUID.randomUUID().toString().lowercase())
            return relay
        }
    }

    object Serializer : KSerializer<SettingsRemoteControlServerRelay> by JsonObjectSerializer(
        "SettingsRemoteControlServerRelay",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRemoteControlWeb.Serializer::class)
class SettingsRemoteControlWeb(
    var enabled: Boolean = false,
    var port: Int = DefaultTcpPorts.remoteControlWeb,
    var deviceName: String = "",
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
        encode("deviceName", deviceName)
    }

    companion object {
        fun decode(container: JsonObject): SettingsRemoteControlWeb {
            val web = SettingsRemoteControlWeb()
            web.enabled = container.decode("enabled", false)
            web.port = container.decode("port", DefaultTcpPorts.remoteControlWeb) { it in 0..65535 }
            web.deviceName = container.decode("deviceName", "")
            return web
        }
    }

    object Serializer : KSerializer<SettingsRemoteControlWeb> by JsonObjectSerializer(
        "SettingsRemoteControlWeb",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRemoteControl.Serializer::class)
class SettingsRemoteControl(
    var assistant: SettingsRemoteControlAssistant = SettingsRemoteControlAssistant(),
    var streamer: SettingsRemoteControlStreamer = SettingsRemoteControlStreamer(),
    var web: SettingsRemoteControlWeb = SettingsRemoteControlWeb(),
    var password: String = randomHumanString(),
    var streamers: List<SettingsRemoteControlAssistant> = emptyList(),
    var selectedStreamer: UUID? = null,
    var hasMigratedAssistant: Boolean = true,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("client", assistant)
        encode("server", streamer)
        encode("web", web)
        encode("password", password)
        encode("streamers", streamers)
        encode("selectedStreamer", selectedStreamer)
        encode("hasMigratedAssistant", hasMigratedAssistant)
    }

    fun getSelectedStreamerName(): String? =
        streamers.firstOrNull { it.id == selectedStreamer }?.name

    companion object {
        fun decode(container: JsonObject): SettingsRemoteControl {
            val remoteControl = SettingsRemoteControl()
            remoteControl.assistant = container.decode(
                "client",
                SettingsRemoteControlAssistant.serializer(),
                SettingsRemoteControlAssistant(),
            )
            remoteControl.streamer = container.decode(
                "server",
                SettingsRemoteControlStreamer.serializer(),
                SettingsRemoteControlStreamer(),
            )
            remoteControl.web = container.decode("web", SettingsRemoteControlWeb.serializer(), SettingsRemoteControlWeb())
            remoteControl.password = container.decode("password", randomHumanString())
            remoteControl.streamers = container.decode(
                "streamers",
                ListSerializer(SettingsRemoteControlAssistant.serializer()),
                emptyList(),
            )
            remoteControl.selectedStreamer = container.decode<UUID?>("selectedStreamer", null)
            remoteControl.hasMigratedAssistant = container.decode("hasMigratedAssistant", false)
            if (!remoteControl.hasMigratedAssistant) {
                val streamer = SettingsRemoteControlAssistant()
                streamer.name = "Streamer"
                streamer.enabled = remoteControl.assistant.enabled
                streamer.port = remoteControl.assistant.port
                streamer.relay.enabled = remoteControl.assistant.relay.enabled
                streamer.relay.baseUrl = remoteControl.assistant.relay.baseUrl
                streamer.relay.bridgeId = remoteControl.assistant.relay.bridgeId
                remoteControl.streamers = remoteControl.streamers + streamer
                remoteControl.selectedStreamer = streamer.id
                remoteControl.hasMigratedAssistant = true
            }
            return remoteControl
        }
    }

    object Serializer : KSerializer<SettingsRemoteControl> by JsonObjectSerializer(
        "SettingsRemoteControl",
        { it.encode() },
        { decode(it) },
    )
}
