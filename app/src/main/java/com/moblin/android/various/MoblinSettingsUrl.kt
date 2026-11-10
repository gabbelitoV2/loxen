package com.moblin.android.various

import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.common.various.trim
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsRemoteControlAssistant
import com.moblin.android.various.settings.SettingsRemoteControlServerRelay
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.SettingsStreamSrt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private class MoblinSettingsUrlError(message: String) : Exception(message)

private fun <T : Any> JsonObject.decodeEnumIfPresent(
    key: String,
    message: String,
    fromRawValue: (String) -> T?,
): T? {
    val rawValue = decodeIfPresent(key, String.serializer()) ?: return null
    return fromRawValue(rawValue) ?: throw MoblinSettingsUrlError(message)
}

@Serializable
class MoblinSettingsWebBrowser {
    var home: String? = null
}

@Serializable(with = MoblinSettingsSrt.Serializer::class)
class MoblinSettingsSrt {
    var latency: Int? = null
    var adaptiveBitrateEnabled: Boolean? = null
    var dnsLookupStrategy: SettingsDnsLookupStrategy? = null

    fun encode(): JsonObject = encodeContainer {
        encodeIfPresent("latency", latency)
        encodeIfPresent("adaptiveBitrateEnabled", adaptiveBitrateEnabled)
        encodeIfPresent("dnsLookupStrategy", dnsLookupStrategy)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsSrt {
            val srt = MoblinSettingsSrt()
            srt.latency = container.decodeIfPresent("latency")
            srt.latency?.let { latency ->
                if (!SettingsStreamSrt.isValidLatency(latency = latency)) {
                    throw MoblinSettingsUrlError(localized("SRT latency out of range"))
                }
            }
            srt.adaptiveBitrateEnabled = container.decodeIfPresent("adaptiveBitrateEnabled")
            srt.dnsLookupStrategy = container.decodeEnumIfPresent(
                "dnsLookupStrategy",
                localized("Unsupported SRT DNS lookup strategy"),
            ) { SettingsDnsLookupStrategy.fromRawValue(it) }
            return srt
        }
    }

    object Serializer : KSerializer<MoblinSettingsSrt> by JsonObjectSerializer(
        "MoblinSettingsSrt",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsUrlStreamVideo.Serializer::class)
class MoblinSettingsUrlStreamVideo {
    var resolution: SettingsStreamResolution? = null
    var fps: Int? = null
    var bitrate: Int? = null
    var codec: SettingsStreamCodec? = null
    var bFrames: Boolean? = null
    var maxKeyFrameInterval: Int? = null

    fun encode(): JsonObject = encodeContainer {
        encodeIfPresent("resolution", resolution)
        encodeIfPresent("fps", fps)
        encodeIfPresent("bitrate", bitrate)
        encodeIfPresent("codec", codec)
        encodeIfPresent("bFrames", bFrames)
        encodeIfPresent("maxKeyFrameInterval", maxKeyFrameInterval)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsUrlStreamVideo {
            val video = MoblinSettingsUrlStreamVideo()
            video.resolution = container.decodeEnumIfPresent(
                "resolution",
                localized("Unsupported video resolution"),
            ) { SettingsStreamResolution.fromRawValue(it) }
            video.fps = container.decodeIfPresent("fps")
            video.fps?.let { fps ->
                if (!SettingsStream.isValidFps(fps = fps)) {
                    throw MoblinSettingsUrlError(localized("Unsupported video FPS"))
                }
            }
            video.bitrate = container.decodeIfPresent("bitrate")
            video.bitrate?.let { bitrate ->
                if (!SettingsStream.isValidBitrate(bitrate = bitrate)) {
                    throw MoblinSettingsUrlError(localized("Video bitrate out of range"))
                }
            }
            video.codec = container.decodeEnumIfPresent(
                "codec",
                localized("Unsupported video codec"),
            ) { SettingsStreamCodec.fromRawValue(it) }
            video.bFrames = container.decodeIfPresent("bFrames")
            video.maxKeyFrameInterval = container.decodeIfPresent("maxKeyFrameInterval")
            video.maxKeyFrameInterval?.let { maxKeyFrameInterval ->
                if (!SettingsStream.isValidMaxKeyFrameInterval(interval = maxKeyFrameInterval)) {
                    throw MoblinSettingsUrlError(localized("Max key frame interval out of range"))
                }
            }
            return video
        }
    }

    object Serializer : KSerializer<MoblinSettingsUrlStreamVideo> by JsonObjectSerializer(
        "MoblinSettingsUrlStreamVideo",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsUrlStreamAudio.Serializer::class)
class MoblinSettingsUrlStreamAudio {
    var bitrate: Int? = null

    fun encode(): JsonObject = encodeContainer {
        encodeIfPresent("bitrate", bitrate)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsUrlStreamAudio {
            val audio = MoblinSettingsUrlStreamAudio()
            audio.bitrate = container.decodeIfPresent("bitrate")
            audio.bitrate?.let { bitrate ->
                if (!SettingsStream.isValidAudioBitrate(bitrate = bitrate)) {
                    throw MoblinSettingsUrlError(localized("Unsupported audio bitrate"))
                }
            }
            return audio
        }
    }

    object Serializer : KSerializer<MoblinSettingsUrlStreamAudio> by JsonObjectSerializer(
        "MoblinSettingsUrlStreamAudio",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsUrlStreamObs.Serializer::class)
class MoblinSettingsUrlStreamObs(
    var webSocketUrl: String,
    var webSocketPassword: String,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("webSocketUrl", webSocketUrl)
        encode("webSocketPassword", webSocketPassword)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsUrlStreamObs {
            val webSocketUrl = cleanUrl(container.decode("webSocketUrl", "").trim())
            isValidWebSocketUrl(webSocketUrl)?.let { message ->
                throw MoblinSettingsUrlError(message)
            }
            return MoblinSettingsUrlStreamObs(
                webSocketUrl = webSocketUrl,
                webSocketPassword = container.decode("webSocketPassword", ""),
            )
        }
    }

    object Serializer : KSerializer<MoblinSettingsUrlStreamObs> by JsonObjectSerializer(
        "MoblinSettingsUrlStreamObs",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
class MoblinSettingsUrlStreamTwitch(
    var channelName: String,
    var channelId: String,
)

@Serializable
class MoblinSettingsUrlStreamKick(
    var channelName: String,
)

@Serializable(with = MoblinSettingsUrlStream.Serializer::class)
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

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("url", url)
        encodeIfPresent("selected", selected)
        encodeIfPresent("backgroundStreaming", backgroundStreaming)
        encodeIfPresent("backgroundStreamingPiP", backgroundStreamingPiP)
        encodeIfPresent("video", video, MoblinSettingsUrlStreamVideo.Serializer)
        encodeIfPresent("audio", audio, MoblinSettingsUrlStreamAudio.Serializer)
        encodeIfPresent("srt", srt, MoblinSettingsSrt.Serializer)
        encodeIfPresent("obs", obs, MoblinSettingsUrlStreamObs.Serializer)
        encodeIfPresent("twitch", twitch, MoblinSettingsUrlStreamTwitch.serializer())
        encodeIfPresent("kick", kick, MoblinSettingsUrlStreamKick.serializer())
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsUrlStream {
            val stream = MoblinSettingsUrlStream(
                name = container.decode("name", ""),
                url = "",
            )
            stream.url = cleanUrl(container.decode("url", "").trim())
            isValidUrl(stream.url)?.let { message ->
                throw MoblinSettingsUrlError(message)
            }
            stream.selected = container.decodeIfPresent("selected")
            stream.backgroundStreaming = container.decodeIfPresent("backgroundStreaming")
            stream.backgroundStreamingPiP = container.decodeIfPresent("backgroundStreamingPiP")
            stream.video = container.decodeIfPresent("video", MoblinSettingsUrlStreamVideo.Serializer)
            stream.audio = container.decodeIfPresent("audio", MoblinSettingsUrlStreamAudio.Serializer)
            stream.srt = container.decodeIfPresent("srt", MoblinSettingsSrt.Serializer)
            stream.obs = container.decodeIfPresent("obs", MoblinSettingsUrlStreamObs.Serializer)
            stream.twitch = container.decodeIfPresent("twitch", MoblinSettingsUrlStreamTwitch.serializer())
            stream.kick = container.decodeIfPresent("kick", MoblinSettingsUrlStreamKick.serializer())
            return stream
        }
    }

    object Serializer : KSerializer<MoblinSettingsUrlStream> by JsonObjectSerializer(
        "MoblinSettingsUrlStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsButton.Serializer::class)
class MoblinSettingsButton(
    var type: SettingsQuickButtonType,
) {
    var enabled: Boolean? = null
    var page: Int? = null

    fun encode(): JsonObject = encodeContainer {
        encode("type", type, SettingsQuickButtonType.Serializer)
        encodeIfPresent("enabled", enabled)
        encodeIfPresent("page", page)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsButton {
            val button = MoblinSettingsButton(
                type = SettingsQuickButtonType.fromRawValue(container.decode("type", "")),
            )
            if (button.type == SettingsQuickButtonType.unknown) {
                throw MoblinSettingsUrlError(localized("Unknown quick button type"))
            }
            button.enabled = container.decodeIfPresent("enabled")
            button.page = container.decodeIfPresent("page")
            button.page?.let { page ->
                if (!SettingsQuickButton.isValidPage(page = page)) {
                    throw MoblinSettingsUrlError(localized("Quick button page out of range"))
                }
            }
            return button
        }
    }

    object Serializer : KSerializer<MoblinSettingsButton> by JsonObjectSerializer(
        "MoblinSettingsButton",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
class MoblinQuickButtons {
    var twoColumns: Boolean? = null
    var showName: Boolean? = null
    var enableScroll: Boolean? = null
    var disableAllButtons: Boolean? = null
    var buttons: List<MoblinSettingsButton>? = null
}

@Serializable(with = MoblinSettingsRemoteControlServerRelay.Serializer::class)
class MoblinSettingsRemoteControlServerRelay(
    var enabled: Boolean,
    var baseUrl: String,
    var bridgeId: String,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("baseUrl", baseUrl)
        encode("bridgeId", bridgeId)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsRemoteControlServerRelay {
            val enabled = container.decode("enabled", false)
            val baseUrl = container.decode("baseUrl", "").trim()
            isValidWebSocketUrl(baseUrl)?.let { message ->
                throw MoblinSettingsUrlError(message)
            }
            val bridgeId = container.decode("bridgeId", "").trim()
            if (!SettingsRemoteControlServerRelay.isValidBridgeId(bridgeId = bridgeId)) {
                throw MoblinSettingsUrlError(localized("Empty remote control relay bridge id"))
            }
            return MoblinSettingsRemoteControlServerRelay(
                enabled = enabled,
                baseUrl = baseUrl,
                bridgeId = bridgeId,
            )
        }
    }

    object Serializer : KSerializer<MoblinSettingsRemoteControlServerRelay> by JsonObjectSerializer(
        "MoblinSettingsRemoteControlServerRelay",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsRemoteControlAssistant.Serializer::class)
class MoblinSettingsRemoteControlAssistant(
    var enabled: Boolean,
    var port: UShort,
) {
    var relay: MoblinSettingsRemoteControlServerRelay? = null

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
        encodeIfPresent("relay", relay, MoblinSettingsRemoteControlServerRelay.Serializer)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsRemoteControlAssistant {
            val enabled = container.decode("enabled", false)
            val port = container.decode("port", 0.toUShort())
            if (!SettingsRemoteControlAssistant.isValidPort(port = port.toInt())) {
                throw MoblinSettingsUrlError(localized("Invalid remote control assistant port"))
            }
            val assistant = MoblinSettingsRemoteControlAssistant(enabled = enabled, port = port)
            assistant.relay = container.decodeIfPresent(
                "relay",
                MoblinSettingsRemoteControlServerRelay.Serializer,
            )
            return assistant
        }
    }

    object Serializer : KSerializer<MoblinSettingsRemoteControlAssistant> by JsonObjectSerializer(
        "MoblinSettingsRemoteControlAssistant",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsRemoteControlStreamer.Serializer::class)
class MoblinSettingsRemoteControlStreamer(
    var enabled: Boolean,
    var url: String,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("url", url)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsRemoteControlStreamer {
            val enabled = container.decode("enabled", false)
            val url = container.decode("url", "").trim()
            isValidWebSocketUrl(url)?.let { message ->
                throw MoblinSettingsUrlError(message)
            }
            return MoblinSettingsRemoteControlStreamer(enabled = enabled, url = url)
        }
    }

    object Serializer : KSerializer<MoblinSettingsRemoteControlStreamer> by JsonObjectSerializer(
        "MoblinSettingsRemoteControlStreamer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsRemoteControl.Serializer::class)
class MoblinSettingsRemoteControl(
    var assistant: MoblinSettingsRemoteControlAssistant? = null,
    var streamer: MoblinSettingsRemoteControlStreamer? = null,
    var password: String,
) {
    fun encode(): JsonObject = encodeContainer {
        encodeIfPresent("assistant", assistant, MoblinSettingsRemoteControlAssistant.Serializer)
        encodeIfPresent("streamer", streamer, MoblinSettingsRemoteControlStreamer.Serializer)
        encode("password", password)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsRemoteControl {
            val assistant = container.decodeIfPresent(
                "assistant",
                MoblinSettingsRemoteControlAssistant.Serializer,
            )
            val streamer = container.decodeIfPresent(
                "streamer",
                MoblinSettingsRemoteControlStreamer.Serializer,
            )
            val password = container.decode("password", "").trim()
            return MoblinSettingsRemoteControl(
                assistant = assistant,
                streamer = streamer,
                password = password,
            )
        }
    }

    object Serializer : KSerializer<MoblinSettingsRemoteControl> by JsonObjectSerializer(
        "MoblinSettingsRemoteControl",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = MoblinSettingsMoblinCloud.Serializer::class)
class MoblinSettingsMoblinCloud(
    var enabled: Boolean,
    var key: String,
) {
    var url: String? = null

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("key", key)
        encodeIfPresent("url", url)
    }

    companion object {
        fun decode(container: JsonObject): MoblinSettingsMoblinCloud {
            val moblinCloud = MoblinSettingsMoblinCloud(
                enabled = container.decode("enabled", false),
                key = container.decode("key", ""),
            )
            moblinCloud.url = container.decodeIfPresent<String>("url")?.trim()
            moblinCloud.url?.let { url ->
                isValidHttpUrl(url)?.let { message ->
                    throw MoblinSettingsUrlError(message)
                }
            }
            return moblinCloud
        }
    }

    object Serializer : KSerializer<MoblinSettingsMoblinCloud> by JsonObjectSerializer(
        "MoblinSettingsMoblinCloud",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
class MoblinSettingsUrl {
    var streams: List<MoblinSettingsUrlStream>? = null
    var quickButtons: MoblinQuickButtons? = null
    var webBrowser: MoblinSettingsWebBrowser? = null
    var remoteControl: MoblinSettingsRemoteControl? = null
    var moblinCloud: MoblinSettingsMoblinCloud? = null

    override fun toString(): String {
        val element = moblinSettingsUrlJson.encodeToJsonElement(MoblinSettingsUrl.serializer(), this)
        return moblinSettingsUrlJson.encodeToString(JsonElement.serializer(), sortJsonElement(element))
    }

    companion object {
        fun fromString(query: String): MoblinSettingsUrl =
            moblinSettingsUrlJson.decodeFromString(MoblinSettingsUrl.serializer(), query)
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
