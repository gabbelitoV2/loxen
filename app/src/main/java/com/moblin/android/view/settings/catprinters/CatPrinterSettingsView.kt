package com.moblin.android.view.settings.catprinters

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.integrations.catprinter.CatPrinterState
import com.moblin.android.integrations.catprinter.catPrinterScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.model.catPrinterPrintTestImage
import com.moblin.android.various.model.catPrinterSetFaxMeowSound
import com.moblin.android.various.model.disableCatPrinter
import com.moblin.android.various.model.enableCatPrinter
import com.moblin.android.various.model.isCatPrinterEnabled
import com.moblin.android.various.model.setCurrentCatPrinter
import com.moblin.android.various.settings.SettingsCatPrinter
import com.moblin.android.various.settings.SettingsCatPrinters
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.KickLogoAndNameView
import com.moblin.android.view.settings.streams.stream.TwitchLogoAndNameView
import com.moblin.android.view.settings.streams.stream.kick.KickAlertsSettingsView
import com.moblin.android.view.settings.streams.stream.twitch.TwitchAlertsSettingsView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID

private fun formatCatPrinterState(state: CatPrinterState?): String {
    return if (state == null || state == CatPrinterState.disconnected) {
        localized("Disconnected")
    } else if (state == CatPrinterState.discovering) {
        localized("Discovering")
    } else if (state == CatPrinterState.connecting) {
        localized("Connecting")
    } else if (state == CatPrinterState.connected) {
        localized("Connected")
    } else {
        localized("Unknown")
    }
}

@Composable
fun CatPrinterSettingsView(
    model: Model = LocalModel.current,
    catPrinters: SettingsCatPrinters,
    device: SettingsCatPrinter,
    status: StatusTopRight,
) {
    val scanner = catPrinterScanner
    val bluetoothPeripheralId by device.bluetoothPeripheralId.collectAsState()
    val bluetoothPeripheralName by device.bluetoothPeripheralName.collectAsState()
    val enabled by device.enabled.collectAsState()
    val printChat by device.printChat.collectAsState()
    val printSnapshots by device.printSnapshots.collectAsState()
    val faxMeowSound by device.faxMeowSound.collectAsState()
    val existingDevices by catPrinters.devices.collectAsState()
    val catPrinterState by status.catPrinterState.collectAsState()

    fun state(): String = formatCatPrinterState(catPrinterState)

    fun canEnable(): Boolean = bluetoothPeripheralId != null

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = scanner.discoveredPeripherals.value.firstOrNull { it.identifier == deviceId }
            ?: return
        device.bluetoothPeripheralName.value = peripheral.name
        device.bluetoothPeripheralId.value = deviceId
    }

    DisposableEffect(Unit) {
        model.setCurrentCatPrinter(device = device)
        onDispose {}
    }

    Form(title = "Cat printer") {
        Section {
            NameEditView(
                name = device.name,
                onNameChange = { device.name = it },
                existingNames = existingDevices,
            )
        }
        Section(header = "Device") {
            NavigationLink(
                destination = {
                    CatPrinterScannerSettingsView(
                        onChange = { onDeviceChange(it) },
                        selectedId = bluetoothPeripheralId?.toString() ?: localized("Select device"),
                    )
                },
                enabled = !model.isCatPrinterEnabled(device = device),
            ) {
                GrayTextView(text = bluetoothPeripheralName ?: localized("Select device"))
            }
        }
        Section {
            Toggle(
                "Enabled",
                isOn = enabled,
                enabled = canEnable(),
                onChange = { value ->
                    device.enabled.value = value
                    if (value) {
                        model.enableCatPrinter(device = device)
                    } else {
                        model.disableCatPrinter(device = device)
                    }
                },
            )
        }
        Section {
            Toggle("Print chat", isOn = printChat, onChange = { device.printChat.value = it })
            Toggle("Print snapshots", isOn = printSnapshots, onChange = { device.printSnapshots.value = it })
            NavigationLink(
                destination = {
                    val printTwitch by device.printTwitch.collectAsState()
                    val printKick by device.printKick.collectAsState()
                    Form(title = "Print alerts") {
                        NavigationLink(
                            destination = {
                                TwitchAlertsSettingsView(
                                    title = localized("Twitch"),
                                    alerts = printTwitch,
                                )
                            },
                        ) {
                            TwitchLogoAndNameView()
                        }
                        NavigationLink(
                            destination = {
                                KickAlertsSettingsView(
                                    title = localized("Kick"),
                                    alerts = printKick,
                                    showBans = false,
                                )
                            },
                        ) {
                            KickLogoAndNameView()
                        }
                    }
                },
            ) {
                Text(localized("Print alerts"))
            }
        }
        Section {
            Toggle(
                "Fax meow sound",
                isOn = faxMeowSound,
                onChange = { value ->
                    device.faxMeowSound.value = value
                    model.catPrinterSetFaxMeowSound(device = device)
                },
            )
        }
        if (enabled) {
            Section {
                HCenter {
                    Text(state())
                }
            }
            Section {
                TextButtonView(title = "Test") {
                    model.catPrinterPrintTestImage(device = device)
                }
            }
        }
    }
}
