package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsCatPrinter.Companion::class)
class SettingsCatPrinter : Named {
    var id: UUID = UUID.randomUUID()

    private val name = MutableStateFlow("")
    override val name: StateFlow<String> = name.asStateFlow()

    val enabled = MutableStateFlow(false)

    val bluetoothPeripheralName = MutableStateFlow<String?>(null)

    val bluetoothPeripheralId = MutableStateFlow<UUID?>(null)

    val printChat = MutableStateFlow(false)

    val faxMeowSound = MutableStateFlow(true)

    val printSnapshots = MutableStateFlow(true)

    val printTwitch = MutableStateFlow(SettingsTwitchAlerts())

    val printKick = MutableStateFlow(SettingsKickAlerts())

    companion object : KSerializer<SettingsCatPrinter> {
        val baseName: String = localized("My printer")

        @Serializable
        private data class Surrogate(
            val id: String? = null,
            val name: String = "",
            val enabled: Boolean = false,
            val bluetoothPeripheralName: String? = null,
            val bluetoothPeripheralId: String? = null,
            val printChat: Boolean = false,
            val faxMeowSound: Boolean = true,
            val printSnapshots: Boolean = true,
            val printTwitch: SettingsTwitchAlerts = SettingsTwitchAlerts(),
            val printKick: SettingsKickAlerts = SettingsKickAlerts(),
        )

        override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SettingsCatPrinter) {
            val surrogate = Surrogate(
                id = value.id.toString(),
                name = value.name.value,
                enabled = value.enabled.value,
                bluetoothPeripheralName = value.bluetoothPeripheralName.value,
                bluetoothPeripheralId = value.bluetoothPeripheralId.value?.toString(),
                printChat = value.printChat.value,
                faxMeowSound = value.faxMeowSound.value,
                printSnapshots = value.printSnapshots.value,
                printTwitch = value.printTwitch.value,
                printKick = value.printKick.value,
            )
            encoder.encodeSerializableValue(Surrogate.serializer(), surrogate)
        }

        override fun deserialize(decoder: Decoder): SettingsCatPrinter {
            val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
            val settings = SettingsCatPrinter()
            settings.id = surrogate.id
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: UUID.randomUUID()
            settings.name.value = surrogate.name
            settings.enabled.value = surrogate.enabled
            settings.bluetoothPeripheralName.value = surrogate.bluetoothPeripheralName
            settings.bluetoothPeripheralId.value = surrogate.bluetoothPeripheralId
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            settings.printChat.value = surrogate.printChat
            settings.faxMeowSound.value = surrogate.faxMeowSound
            settings.printSnapshots.value = surrogate.printSnapshots
            settings.printTwitch.value = surrogate.printTwitch
            settings.printKick.value = surrogate.printKick
            return settings
        }
    }
}

@Serializable(with = SettingsCatPrinters.Companion::class)
class SettingsCatPrinters {
    val devices = MutableStateFlow<List<SettingsCatPrinter>>(emptyList())

    val backgroundPrinting = MutableStateFlow(false)

    companion object : KSerializer<SettingsCatPrinters> {
        @Serializable
        private data class Surrogate(
            val devices: List<SettingsCatPrinter> = emptyList(),
            val backgroundPrinting: Boolean = false,
        )

        override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: SettingsCatPrinters) {
            val surrogate = Surrogate(
                devices = value.devices.value,
                backgroundPrinting = value.backgroundPrinting.value,
            )
            encoder.encodeSerializableValue(Surrogate.serializer(), surrogate)
        }

        override fun deserialize(decoder: Decoder): SettingsCatPrinters {
            val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
            val settings = SettingsCatPrinters()
            settings.devices.value = surrogate.devices
            settings.backgroundPrinting.value = surrogate.backgroundPrinting
            return settings
        }
    }
}
