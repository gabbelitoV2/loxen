package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.utils.Named
import com.moblin.android.various.utils.randomHumanString
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class SettingsRemoteControlAssistant(
    @Serializable(with = SettingsRemoteControlUuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    override var name: String = SettingsRemoteControlAssistant.baseName,
    var enabled: Boolean = false,
    var port: Int = 0,
    var relay: SettingsRemoteControlServerRelay = SettingsRemoteControlServerRelay(),
) : Named {
    companion object {
        val baseName: String = localized("Streamer name")
    }
}

@Serializable
class SettingsRemoteControlStreamerUrl(
    @Serializable(with = SettingsRemoteControlUuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var url: String = "",
)

@Serializable
class SettingsRemoteControlStreamer(
    var enabled: Boolean = false,
    var name: String = "",
    var url: String = "",
    var previewFps: Float = 1.0f,
    var reliableChatAndEvents: Boolean = false,
    var savedUrls: List<SettingsRemoteControlStreamerUrl> = emptyList(),
)

@Serializable
class SettingsRemoteControlServerRelay(
    var enabled: Boolean = true,
    var baseUrl: String = "wss://moblin.mys-lang.org/moblin-remote-control-relay",
    var bridgeId: String = UUID.randomUUID().toString().lowercase(),
)

@Serializable
class SettingsRemoteControlWeb(
    var enabled: Boolean = false,
    var port: Int = DefaultTcpPorts.remoteControlWeb,
    var deviceName: String = "",
)

@Serializable
class SettingsRemoteControl(
    @SerialName("client")
    var assistant: SettingsRemoteControlAssistant = SettingsRemoteControlAssistant(),
    @SerialName("server")
    var streamer: SettingsRemoteControlStreamer = SettingsRemoteControlStreamer(),
    var web: SettingsRemoteControlWeb = SettingsRemoteControlWeb(),
    var password: String = randomHumanString(),
    var streamers: List<SettingsRemoteControlAssistant> = emptyList(),
    @Serializable(with = SettingsRemoteControlNullableUuidSerializer::class)
    var selectedStreamer: UUID? = null,
    var hasMigratedAssistant: Boolean = true,
) {
    init {
        if (!hasMigratedAssistant) {
            val streamer = SettingsRemoteControlAssistant()
            streamer.name = "Streamer"
            streamer.enabled = assistant.enabled
            streamer.port = assistant.port
            streamer.relay.enabled = assistant.relay.enabled
            streamer.relay.baseUrl = assistant.relay.baseUrl
            streamer.relay.bridgeId = assistant.relay.bridgeId
            streamers = streamers + streamer
            selectedStreamer = streamer.id
            hasMigratedAssistant = true
        }
    }

    fun getSelectedStreamerName(): String? =
        streamers.firstOrNull { it.id == selectedStreamer }?.name
}

private object SettingsRemoteControlUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

private object SettingsRemoteControlNullableUuidSerializer : KSerializer<UUID?> {
    private val delegate: KSerializer<UUID?> = SettingsRemoteControlUuidSerializer.nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: UUID?) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): UUID? = delegate.deserialize(decoder)
}
