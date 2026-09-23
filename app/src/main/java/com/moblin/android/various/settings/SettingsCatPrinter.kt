package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsCatPrinter.Serializer::class)
class SettingsCatPrinter : Named {
    var id: UUID = UUID.randomUUID()

    override var name: String by Published("")

    val enabled = MutableStateFlow(false)

    val bluetoothPeripheralName = MutableStateFlow<String?>(null)

    val bluetoothPeripheralId = MutableStateFlow<UUID?>(null)

    val printChat = MutableStateFlow(false)

    val faxMeowSound = MutableStateFlow(true)

    val printSnapshots = MutableStateFlow(true)

    val printTwitch = MutableStateFlow(SettingsTwitchAlerts())

    val printKick = MutableStateFlow(SettingsKickAlerts())

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("enabled", enabled)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
        encode("printChat", printChat)
        encode("faxMeowSound", faxMeowSound)
        encode("printSnapshots", printSnapshots)
        encode("printTwitch", printTwitch, SettingsTwitchAlerts.serializer())
        encode("printKick", printKick, SettingsKickAlerts.serializer())
    }

    companion object {
        val baseName: String = localized("My printer")

        fun decode(container: JsonObject): SettingsCatPrinter {
            val printer = SettingsCatPrinter()
            printer.id = container.decode("id", UUID.randomUUID())
            printer.name = container.decode("name", "")
            printer.enabled.value = container.decode("enabled", false)
            printer.bluetoothPeripheralName.value = container.decodeIfPresent<String>("bluetoothPeripheralName")
            printer.bluetoothPeripheralId.value = container.decodeIfPresent<UUID>("bluetoothPeripheralId")
            printer.printChat.value = container.decode("printChat", false)
            printer.faxMeowSound.value = container.decode("faxMeowSound", true)
            printer.printSnapshots.value = container.decode("printSnapshots", true)
            printer.printTwitch.value = container.decode(
                "printTwitch",
                SettingsTwitchAlerts.serializer(),
                SettingsTwitchAlerts(),
            )
            printer.printKick.value = container.decode("printKick", SettingsKickAlerts.serializer(), SettingsKickAlerts())
            return printer
        }
    }

    object Serializer : KSerializer<SettingsCatPrinter> by JsonObjectSerializer(
        "SettingsCatPrinter",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsCatPrinters.Serializer::class)
class SettingsCatPrinters {
    val devices = MutableStateFlow<List<SettingsCatPrinter>>(emptyList())

    val backgroundPrinting = MutableStateFlow(false)

    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsCatPrinter.serializer()))
        encode("backgroundPrinting", backgroundPrinting)
    }

    companion object {
        fun decode(container: JsonObject): SettingsCatPrinters {
            val printers = SettingsCatPrinters()
            printers.devices.value = container.decode(
                "devices",
                ListSerializer(SettingsCatPrinter.serializer()),
                emptyList(),
            )
            printers.backgroundPrinting.value = container.decode("backgroundPrinting", false)
            return printers
        }
    }

    object Serializer : KSerializer<SettingsCatPrinters> by JsonObjectSerializer(
        "SettingsCatPrinters",
        { it.encode() },
        { decode(it) },
    )
}
