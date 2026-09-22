package com.moblin.android.moblinwatch.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WatchSettingsChat.Serializer::class)
class WatchSettingsChat {
    private val _fontSize = MutableStateFlow(17.0f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _timestampEnabled = MutableStateFlow(true)
    val timestampEnabled: StateFlow<Boolean> = _timestampEnabled

    private val _notificationOnMessage = MutableStateFlow(false)
    val notificationOnMessage: StateFlow<Boolean> = _notificationOnMessage

    private val _notificationRate = MutableStateFlow(30)
    val notificationRate: StateFlow<Int> = _notificationRate

    private val _badges = MutableStateFlow(true)
    val badges: StateFlow<Boolean> = _badges

    enum class CodingKeys {
        fontSize,
        timestampEnabled,
        notificationOnMessage,
        notificationRate,
        badges,
    }

    object Serializer : KSerializer<WatchSettingsChat> {
        @Serializable
        private data class Snapshot(
            @SerialName("fontSize") val fontSize: Float = 17.0f,
            @SerialName("timestampEnabled") val timestampEnabled: Boolean = true,
            @SerialName("notificationOnMessage") val notificationOnMessage: Boolean = false,
            @SerialName("notificationRate") val notificationRate: Int = 30,
            @SerialName("badges") val badges: Boolean = true,
        )

        override val descriptor: SerialDescriptor = Snapshot.serializer().descriptor

        override fun serialize(encoder: Encoder, value: WatchSettingsChat) {
            encoder.encodeSerializableValue(
                Snapshot.serializer(),
                Snapshot(
                    fontSize = value.fontSize.value,
                    timestampEnabled = value.timestampEnabled.value,
                    notificationOnMessage = value.notificationOnMessage.value,
                    notificationRate = value.notificationRate.value,
                    badges = value.badges.value,
                ),
            )
        }

        override fun deserialize(decoder: Decoder): WatchSettingsChat {
            val snapshot = decoder.decodeSerializableValue(Snapshot.serializer())
            val value = WatchSettingsChat()
            value._fontSize.value = snapshot.fontSize
            value._timestampEnabled.value = snapshot.timestampEnabled
            value._notificationOnMessage.value = snapshot.notificationOnMessage
            value._notificationRate.value = snapshot.notificationRate
            value._badges.value = snapshot.badges
            return value
        }
    }
}

@Serializable(with = WatchSettingsShow.Serializer::class)
class WatchSettingsShow {
    private val _thermalState = MutableStateFlow(true)
    val thermalState: StateFlow<Boolean> = _thermalState

    private val _audioLevel = MutableStateFlow(true)
    val audioLevel: StateFlow<Boolean> = _audioLevel

    private val _speed = MutableStateFlow(true)
    val speed: StateFlow<Boolean> = _speed

    enum class CodingKeys {
        thermalState,
        audioLevel,
        speed,
    }

    object Serializer : KSerializer<WatchSettingsShow> {
        @Serializable
        private data class Snapshot(
            @SerialName("thermalState") val thermalState: Boolean = true,
            @SerialName("audioLevel") val audioLevel: Boolean = true,
            @SerialName("speed") val speed: Boolean = true,
        )

        override val descriptor: SerialDescriptor = Snapshot.serializer().descriptor

        override fun serialize(encoder: Encoder, value: WatchSettingsShow) {
            encoder.encodeSerializableValue(
                Snapshot.serializer(),
                Snapshot(
                    thermalState = value.thermalState.value,
                    audioLevel = value.audioLevel.value,
                    speed = value.speed.value,
                ),
            )
        }

        override fun deserialize(decoder: Decoder): WatchSettingsShow {
            val snapshot = decoder.decodeSerializableValue(Snapshot.serializer())
            val value = WatchSettingsShow()
            value._thermalState.value = snapshot.thermalState
            value._audioLevel.value = snapshot.audioLevel
            value._speed.value = snapshot.speed
            return value
        }
    }
}

@Serializable(with = WatchSettings.Serializer::class)
class WatchSettings {
    var chat: WatchSettingsChat = WatchSettingsChat()
    var show: WatchSettingsShow = WatchSettingsShow()

    private val _viaRemoteControl = MutableStateFlow(false)
    val viaRemoteControl: StateFlow<Boolean> = _viaRemoteControl

    enum class CodingKeys {
        chat,
        show,
        viaRemoteControl,
    }

    object Serializer : KSerializer<WatchSettings> {
        @Serializable
        private data class Snapshot(
            @SerialName("chat")
            @Serializable(with = WatchSettingsChat.Serializer::class)
            val chat: WatchSettingsChat = WatchSettingsChat(),
            @SerialName("show")
            @Serializable(with = WatchSettingsShow.Serializer::class)
            val show: WatchSettingsShow = WatchSettingsShow(),
            @SerialName("viaRemoteControl") val viaRemoteControl: Boolean = false,
        )

        override val descriptor: SerialDescriptor = Snapshot.serializer().descriptor

        override fun serialize(encoder: Encoder, value: WatchSettings) {
            encoder.encodeSerializableValue(
                Snapshot.serializer(),
                Snapshot(
                    chat = value.chat,
                    show = value.show,
                    viaRemoteControl = value.viaRemoteControl.value,
                ),
            )
        }

        override fun deserialize(decoder: Decoder): WatchSettings {
            val snapshot = decoder.decodeSerializableValue(Snapshot.serializer())
            val value = WatchSettings()
            value.chat = snapshot.chat
            value.show = snapshot.show
            value._viaRemoteControl.value = snapshot.viaRemoteControl
            return value
        }
    }
}
