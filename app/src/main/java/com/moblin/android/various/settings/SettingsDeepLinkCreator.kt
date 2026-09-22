package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class DeepLinkCreatorStreamVideo {
    @SerialName("resolution")
    var resolution: SettingsStreamResolution = SettingsStream.defaultResolution

    @SerialName("fps")
    var fps: Int = SettingsStream.defaultFps

    @SerialName("bitrate")
    var bitrate: Int = 5_000_000

    @SerialName("codec")
    var codec: SettingsStreamCodec = SettingsStreamCodec.h265hevc

    @SerialName("bFrames")
    var bFrames: Boolean = false

    @SerialName("maxKeyFrameInterval")
    var maxKeyFrameInterval: Int = 2
}

@Serializable
class DeepLinkCreatorStreamAudio {
    @SerialName("bitrate")
    var bitrate: Int = 128_000

    @Transient
    val bitrateFloat: Float
        get() = (bitrate / 1000).toFloat()
}

@Serializable
class DeepLinkCreatorStreamSrt {
    @SerialName("latency")
    var latency: Int = defaultSrtLatency

    @SerialName("adaptiveBitrateEnabled")
    var adaptiveBitrateEnabled: Boolean = true

    @SerialName("dnsLookupStrategy")
    var dnsLookupStrategy: SettingsDnsLookupStrategy = SettingsDnsLookupStrategy.system
}

@Serializable
class DeepLinkCreatorStreamObs {
    @SerialName("webSocketUrl")
    var webSocketUrl: String = ""

    @SerialName("webSocketPassword")
    var webSocketPassword: String = ""
}

@Serializable
class DeepLinkCreatorStreamTwitch {
    @SerialName("channelName")
    var channelName: String = ""

    @SerialName("channelId")
    var channelId: String = ""
}

@Serializable
class DeepLinkCreatorStreamKick {
    @SerialName("channelName")
    var channelName: String = ""
}

@Serializable
class DeepLinkCreatorStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    var name: String = baseName

    @SerialName("url")
    var url: String = defaultStreamUrl

    @SerialName("selected")
    var selected: Boolean = false

    @SerialName("video")
    var video: DeepLinkCreatorStreamVideo = DeepLinkCreatorStreamVideo()

    @SerialName("audio")
    var audio: DeepLinkCreatorStreamAudio = DeepLinkCreatorStreamAudio()

    @SerialName("srt")
    var srt: DeepLinkCreatorStreamSrt = DeepLinkCreatorStreamSrt()

    @SerialName("obs")
    var obs: DeepLinkCreatorStreamObs = DeepLinkCreatorStreamObs()

    @SerialName("twitch")
    var twitch: DeepLinkCreatorStreamTwitch = DeepLinkCreatorStreamTwitch()

    @SerialName("kick")
    var kick: DeepLinkCreatorStreamKick = DeepLinkCreatorStreamKick()
}

@Serializable
class DeepLinkCreatorQuickButton {
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID()

    @SerialName("type")
    var type: SettingsQuickButtonType = SettingsQuickButtonType.unknown

    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("page")
    var page: Int = 1
}

@Serializable
class DeepLinkCreatorQuickButtons {
    @SerialName("twoColumns")
    var twoColumns: Boolean = true

    @SerialName("showName")
    var showName: Boolean = true

    @SerialName("enableScroll")
    var enableScroll: Boolean = true

    @SerialName("buttons")
    var buttons: MutableList<DeepLinkCreatorQuickButton> = mutableListOf()
}

@Serializable
class DeepLinkCreatorWebBrowser {
    @SerialName("home")
    var home: String = ""
}

@Serializable
class DeepLinkCreator {
    @SerialName("streams")
    var streams: MutableList<DeepLinkCreatorStream> = mutableListOf()

    @SerialName("quickButtonsEnabled")
    var quickButtonsEnabled: Boolean = false

    @SerialName("quickButtons")
    var quickButtons: DeepLinkCreatorQuickButtons = DeepLinkCreatorQuickButtons()

    @SerialName("webBrowserEnabled")
    var webBrowserEnabled: Boolean = false

    @SerialName("webBrowser")
    var webBrowser: DeepLinkCreatorWebBrowser = DeepLinkCreatorWebBrowser()
}

object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}
