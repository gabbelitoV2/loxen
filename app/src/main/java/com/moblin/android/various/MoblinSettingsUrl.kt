package com.moblin.android.various

import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamResolution
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
class MoblinSettingsWebBrowser {
    var home: String? = null
}

@Serializable
class MoblinSettingsSrt {
    var latency: Int? = null
    var adaptiveBitrateEnabled: Boolean? = null
    var dnsLookupStrategy: SettingsDnsLookupStrategy? = null
}

@Serializable
class MoblinSettingsUrlStreamVideo {
    var resolution: SettingsStreamResolution? = null
    var fps: Int? = null
    var bitrate: UInt? = null
    var codec: SettingsStreamCodec? = null
    var bFrames: Boolean? = null
    var maxKeyFrameInterval: Int? = null
}

@Serializable
class MoblinSettingsUrlStreamAudio {
    var bitrate: Int? = null
}

@Serializable
class MoblinSettingsUrlStreamObs(
    var webSocketUrl: String,
    var webSocketPassword: String,
)

@Serializable
class MoblinSettingsUrlStreamTwitch(
    var channelName: String,
    var channelId: String,
)

@Serializable
class MoblinSettingsUrlStreamKick(
    var channelName: String,
)

@Serializable
class MoblinSettingsUrlStream(
    var name: String,
    var url: String,
) {
    var selected: Boolean? = null
    var backgroundStreaming: Boolean? = null
    var backgroundStreamingPiP: Boolean? = null
    var video: MoblinSettingsUrlStreamVideo? = null
    var audio: MoblinSettingsUrlStreamAudio? = null
    var srt: MoblinSettingsSrt? = null
    var obs: MoblinSettingsUrlStreamObs? = null
    var twitch: MoblinSettingsUrlStreamTwitch? = null
    var kick: MoblinSettingsUrlStreamKick? = null
}

@Serializable
class MoblinSettingsButton(
    var type: SettingsQuickButtonType,
) {
    var enabled: Boolean? = null
    var page: Int? = null
}

@Serializable
class MoblinQuickButtons {
    var twoColumns: Boolean? = null
    var showName: Boolean? = null
    var enableScroll: Boolean? = null
    var disableAllButtons: Boolean? = null
    var buttons: List<MoblinSettingsButton>? = null
}

@Serializable
class MoblinSettingsRemoteControlServerRelay(
    var enabled: Boolean,
    var baseUrl: String,
    var bridgeId: String,
)

@Serializable
class MoblinSettingsRemoteControlAssistant(
    var enabled: Boolean,
    var port: UShort,
) {
    var relay: MoblinSettingsRemoteControlServerRelay? = null
}

@Serializable
class MoblinSettingsRemoteControlStreamer(
    var enabled: Boolean,
    var url: String,
)

@Serializable
class MoblinSettingsRemoteControl(
    var assistant: MoblinSettingsRemoteControlAssistant? = null,
    var streamer: MoblinSettingsRemoteControlStreamer? = null,
    var password: String,
)

@Serializable
class MoblinSettingsUrl {
    var streams: List<MoblinSettingsUrlStream>? = null
    var quickButtons: MoblinQuickButtons? = null
    var webBrowser: MoblinSettingsWebBrowser? = null
    var remoteControl: MoblinSettingsRemoteControl? = null

    override fun toString(): String {
        val element = moblinSettingsUrlJson.encodeToJsonElement(MoblinSettingsUrl.serializer(), this)
        return moblinSettingsUrlJson.encodeToString(JsonElement.serializer(), sortJsonElement(element))
    }

    companion object {
        fun fromString(query: String): MoblinSettingsUrl {
            val settings = moblinSettingsUrlJson.decodeFromString(MoblinSettingsUrl.serializer(), query)
            for (stream in settings.streams ?: emptyList()) {
                isValidUrl(cleanUrl(stream.url))?.let { message ->
                    throw Exception(message)
                }
                stream.srt?.let { srt ->
                    srt.latency?.let { latency ->
                        if (latency < 0) {
                            throw Exception("Negative SRT latency")
                        }
                        if (latency > 65535) {
                            throw Exception("Too big SRT latency")
                        }
                    }
                }
                stream.obs?.let { obs ->
                    isValidWebSocketUrl(cleanUrl(obs.webSocketUrl))?.let { message ->
                        throw Exception(message)
                    }
                }
            }
            return settings
        }
    }
}

private val moblinSettingsUrlJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private fun sortJsonElement(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.entries.sortedBy { it.key }.associate { it.key to sortJsonElement(it.value) },
    )
    is JsonArray -> JsonArray(element.map { sortJsonElement(it) })
    else -> element
}
