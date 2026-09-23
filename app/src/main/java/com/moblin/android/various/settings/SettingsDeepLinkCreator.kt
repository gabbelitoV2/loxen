package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = DeepLinkCreatorStreamVideo.Serializer::class)
class DeepLinkCreatorStreamVideo {
    var resolution: SettingsStreamResolution by Published(SettingsStream.defaultResolution)
    var fps: Int by Published(SettingsStream.defaultFps)
    var bitrate: Int by Published(5_000_000)
    var codec: SettingsStreamCodec by Published(SettingsStreamCodec.h265hevc)
    var bFrames: Boolean by Published(false)
    var maxKeyFrameInterval: Int by Published(2)

    fun encode(): JsonObject = encodeContainer {
        encode("resolution", resolution)
        encode("fps", fps)
        encode("bitrate", bitrate)
        encode("codec", codec)
        encode("bFrames", bFrames)
        encode("maxKeyFrameInterval", maxKeyFrameInterval)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamVideo {
            val video = DeepLinkCreatorStreamVideo()
            video.resolution = container.decode("resolution", SettingsStream.defaultResolution)
            video.fps = container.decode("fps", SettingsStream.defaultFps)
            video.bitrate = container.decode("bitrate", 5_000_000) { it >= 0 }
            video.codec = container.decode("codec", SettingsStreamCodec.h265hevc)
            video.bFrames = container.decode("bFrames", false)
            video.maxKeyFrameInterval = container.decode("maxKeyFrameInterval", 2)
            return video
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamVideo> by JsonObjectSerializer(
        "DeepLinkCreatorStreamVideo",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStreamAudio.Serializer::class)
class DeepLinkCreatorStreamAudio {
    var bitrate: Int by Published(128_000)

    val bitrateFloat: Float
        get() = (bitrate / 1000).toFloat()

    fun encode(): JsonObject = encodeContainer {
        encode("bitrate", bitrate)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamAudio {
            val audio = DeepLinkCreatorStreamAudio()
            audio.bitrate = container.decode("bitrate", 128_000)
            return audio
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamAudio> by JsonObjectSerializer(
        "DeepLinkCreatorStreamAudio",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStreamSrt.Serializer::class)
class DeepLinkCreatorStreamSrt {
    var latency: Int by Published(defaultSrtLatency)
    var adaptiveBitrateEnabled: Boolean by Published(true)
    var dnsLookupStrategy: SettingsDnsLookupStrategy by Published(SettingsDnsLookupStrategy.system)

    fun encode(): JsonObject = encodeContainer {
        encode("latency", latency)
        encode("adaptiveBitrateEnabled", adaptiveBitrateEnabled)
        encode("dnsLookupStrategy", dnsLookupStrategy)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamSrt {
            val srt = DeepLinkCreatorStreamSrt()
            srt.latency = container.decode("latency", defaultSrtLatency)
            srt.adaptiveBitrateEnabled = container.decode("adaptiveBitrateEnabled", true)
            srt.dnsLookupStrategy = container.decode("dnsLookupStrategy", SettingsDnsLookupStrategy.system)
            return srt
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamSrt> by JsonObjectSerializer(
        "DeepLinkCreatorStreamSrt",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStreamObs.Serializer::class)
class DeepLinkCreatorStreamObs {
    var webSocketUrl: String by Published("")
    var webSocketPassword: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("webSocketUrl", webSocketUrl)
        encode("webSocketPassword", webSocketPassword)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamObs {
            val obs = DeepLinkCreatorStreamObs()
            obs.webSocketUrl = container.decode("webSocketUrl", "")
            obs.webSocketPassword = container.decode("webSocketPassword", "")
            return obs
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamObs> by JsonObjectSerializer(
        "DeepLinkCreatorStreamObs",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStreamTwitch.Serializer::class)
class DeepLinkCreatorStreamTwitch {
    var channelName: String by Published("")
    var channelId: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("channelName", channelName)
        encode("channelId", channelId)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamTwitch {
            val twitch = DeepLinkCreatorStreamTwitch()
            twitch.channelName = container.decode("channelName", "")
            twitch.channelId = container.decode("channelId", "")
            return twitch
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamTwitch> by JsonObjectSerializer(
        "DeepLinkCreatorStreamTwitch",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStreamKick.Serializer::class)
class DeepLinkCreatorStreamKick {
    var channelName: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("channelName", channelName)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorStreamKick {
            val kick = DeepLinkCreatorStreamKick()
            kick.channelName = container.decode("channelName", "")
            return kick
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStreamKick> by JsonObjectSerializer(
        "DeepLinkCreatorStreamKick",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorStream.Serializer::class)
class DeepLinkCreatorStream : Named {
    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var url: String by Published(defaultStreamUrl)
    var selected: Boolean by Published(false)
    var video: DeepLinkCreatorStreamVideo by Published(DeepLinkCreatorStreamVideo())
    var audio: DeepLinkCreatorStreamAudio by Published(DeepLinkCreatorStreamAudio())
    var srt: DeepLinkCreatorStreamSrt by Published(DeepLinkCreatorStreamSrt())
    var obs: DeepLinkCreatorStreamObs by Published(DeepLinkCreatorStreamObs())
    var twitch: DeepLinkCreatorStreamTwitch by Published(DeepLinkCreatorStreamTwitch())
    var kick: DeepLinkCreatorStreamKick by Published(DeepLinkCreatorStreamKick())

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("url", url)
        encode("selected", selected)
        encode("video", video)
        encode("audio", audio)
        encode("srt", srt)
        encode("obs", obs)
        encode("twitch", twitch)
        encode("kick", kick)
    }

    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): DeepLinkCreatorStream {
            val stream = DeepLinkCreatorStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.url = container.decode("url", defaultStreamUrl)
            stream.selected = container.decode("selected", false)
            stream.video = container.decode("video", DeepLinkCreatorStreamVideo.serializer(), DeepLinkCreatorStreamVideo())
            stream.audio = container.decode("audio", DeepLinkCreatorStreamAudio.serializer(), DeepLinkCreatorStreamAudio())
            stream.srt = container.decode("srt", DeepLinkCreatorStreamSrt.serializer(), DeepLinkCreatorStreamSrt())
            stream.obs = container.decode("obs", DeepLinkCreatorStreamObs.serializer(), DeepLinkCreatorStreamObs())
            stream.twitch = container.decode(
                "twitch",
                DeepLinkCreatorStreamTwitch.serializer(),
                DeepLinkCreatorStreamTwitch(),
            )
            stream.kick = container.decode("kick", DeepLinkCreatorStreamKick.serializer(), DeepLinkCreatorStreamKick())
            return stream
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorStream> by JsonObjectSerializer(
        "DeepLinkCreatorStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorQuickButton.Serializer::class)
class DeepLinkCreatorQuickButton {
    var id: UUID = UUID.randomUUID()
    var type: SettingsQuickButtonType by Published(SettingsQuickButtonType.unknown)
    var enabled: Boolean by Published(false)
    var page: Int by Published(1)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("type", type)
        encode("enabled", enabled)
        encode("page", page)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorQuickButton {
            val button = DeepLinkCreatorQuickButton()
            button.id = container.decode("id", UUID.randomUUID())
            button.type = container.decode("type", SettingsQuickButtonType.unknown)
            button.enabled = container.decode("enabled", false)
            button.page = container.decode("page", 1)
            return button
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorQuickButton> by JsonObjectSerializer(
        "DeepLinkCreatorQuickButton",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorQuickButtons.Serializer::class)
class DeepLinkCreatorQuickButtons {
    var twoColumns: Boolean by Published(true)
    var showName: Boolean by Published(true)
    var enableScroll: Boolean by Published(true)
    var buttons: MutableList<DeepLinkCreatorQuickButton> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("twoColumns", twoColumns)
        encode("showName", showName)
        encode("enableScroll", enableScroll)
        encode("buttons", buttons)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorQuickButtons {
            val quickButtons = DeepLinkCreatorQuickButtons()
            quickButtons.twoColumns = container.decode("twoColumns", true)
            quickButtons.showName = container.decode("showName", true)
            quickButtons.enableScroll = container.decode("enableScroll", true)
            quickButtons.buttons = container.decode(
                "buttons",
                ListSerializer(DeepLinkCreatorQuickButton.serializer()),
                emptyList(),
            ).toMutableList()
            return quickButtons
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorQuickButtons> by JsonObjectSerializer(
        "DeepLinkCreatorQuickButtons",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreatorWebBrowser.Serializer::class)
class DeepLinkCreatorWebBrowser {
    var home: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("home", home)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreatorWebBrowser {
            val webBrowser = DeepLinkCreatorWebBrowser()
            webBrowser.home = container.decode("home", "")
            return webBrowser
        }
    }

    object Serializer : KSerializer<DeepLinkCreatorWebBrowser> by JsonObjectSerializer(
        "DeepLinkCreatorWebBrowser",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = DeepLinkCreator.Serializer::class)
class DeepLinkCreator {
    var streams: MutableList<DeepLinkCreatorStream> by PublishedList()
    var quickButtonsEnabled: Boolean by Published(false)
    var quickButtons: DeepLinkCreatorQuickButtons by Published(DeepLinkCreatorQuickButtons())
    var webBrowserEnabled: Boolean by Published(false)
    var webBrowser: DeepLinkCreatorWebBrowser by Published(DeepLinkCreatorWebBrowser())

    fun encode(): JsonObject = encodeContainer {
        encode("streams", streams)
        encode("quickButtonsEnabled", quickButtonsEnabled)
        encode("quickButtons", quickButtons)
        encode("webBrowserEnabled", webBrowserEnabled)
        encode("webBrowser", webBrowser)
    }

    companion object {
        fun decode(container: JsonObject): DeepLinkCreator {
            val deepLinkCreator = DeepLinkCreator()
            deepLinkCreator.streams = container.decode(
                "streams",
                ListSerializer(DeepLinkCreatorStream.serializer()),
                emptyList(),
            ).toMutableList()
            deepLinkCreator.quickButtonsEnabled = container.decode("quickButtonsEnabled", false)
            deepLinkCreator.quickButtons = container.decode(
                "quickButtons",
                DeepLinkCreatorQuickButtons.serializer(),
                DeepLinkCreatorQuickButtons(),
            )
            deepLinkCreator.webBrowserEnabled = container.decode("webBrowserEnabled", false)
            deepLinkCreator.webBrowser = container.decode(
                "webBrowser",
                DeepLinkCreatorWebBrowser.serializer(),
                DeepLinkCreatorWebBrowser(),
            )
            return deepLinkCreator
        }
    }

    object Serializer : KSerializer<DeepLinkCreator> by JsonObjectSerializer(
        "DeepLinkCreator",
        { it.encode() },
        { decode(it) },
    )
}
