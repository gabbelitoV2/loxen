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

    private val _name = MutableStateFlow("")
    override val name: StateFlow<String> = _name.asStateFlow()

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bluetoothPeripheralName = MutableStateFlow<String?>(null)
    val bluetoothPeripheralName: StateFlow<String?> = _bluetoothPeripheralName.asStateFlow()

    private val _bluetoothPeripheralId = MutableStateFlow<UUID?>(null)
    val bluetoothPeripheralId: StateFlow<UUID?> = _bluetoothPeripheralId.asStateFlow()

    private val _printChat = MutableStateFlow(false)
    val printChat: StateFlow<Boolean> = _printChat.asStateFlow()

    private val _faxMeowSound = MutableStateFlow(true)
    val faxMeowSound: StateFlow<Boolean> = _faxMeowSound.asStateFlow()

    private val _printSnapshots = MutableStateFlow(true)
    val printSnapshots: StateFlow<Boolean> = _printSnapshots.asStateFlow()

    private val _printTwitch = MutableStateFlow(SettingsTwitchAlerts())
    val printTwitch: StateFlow<SettingsTwitchAlerts> = _printTwitch.asStateFlow()

    private val _printKick = MutableStateFlow(SettingsKickAlerts())
    val printKick: StateFlow<SettingsKickAlerts> = _printKick.asStateFlow()

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
            settings._name.value = surrogate.name
            settings._enabled.value = surrogate.enabled
            settings._bluetoothPeripheralName.value = surrogate.bluetoothPeripheralName
            settings._bluetoothPeripheralId.value = surrogate.bluetoothPeripheralId
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            settings._printChat.value = surrogate.printChat
            settings._faxMeowSound.value = surrogate.faxMeowSound
            settings._printSnapshots.value = surrogate.printSnapshots
            settings._printTwitch.value = surrogate.printTwitch
            settings._printKick.value = surrogate.printKick
            return settings
        }
    }
}

@Serializable(with = SettingsCatPrinters.Companion::class)
class SettingsCatPrinters {
    private val _devices = MutableStateFlow<List<SettingsCatPrinter>>(emptyList())
    val devices: StateFlow<List<SettingsCatPrinter>> = _devices.asStateFlow()

    private val _backgroundPrinting = MutableStateFlow(false)
    val backgroundPrinting: StateFlow<Boolean> = _backgroundPrinting.asStateFlow()

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
            settings._devices.value = surrogate.devices
            settings._backgroundPrinting.value = surrogate.backgroundPrinting
            return settings
        }
    }
}
