package com.moblin.android.view.settings.catprinters

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.integrations.catprinter.CatPrinterState
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
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
import com.moblin.android.various.model.catPrinterPrintTestImage
import com.moblin.android.various.model.catPrinterSetFaxMeowSound
import com.moblin.android.various.model.disableCatPrinter
import com.moblin.android.various.model.enableCatPrinter
import com.moblin.android.various.model.isCatPrinterEnabled
import com.moblin.android.various.model.setCurrentCatPrinter

private val catPrinterScanner = CatPrinterScanner()

private fun formatCatPrinterState(state: CatPrinterState?): String {
    return when (state) {
        null, CatPrinterState.disconnected -> localized("Disconnected")
        CatPrinterState.discovering -> localized("Discovering")
        CatPrinterState.connecting -> localized("Connecting")
        CatPrinterState.connected -> localized("Connected")
        else -> localized("Unknown")
    }
}

fun onDeviceChange(device: SettingsCatPrinter, value: String) {
    val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
    val peripheral = catPrinterScanner.discoveredPeripherals.value
        .firstOrNull { it.identifier == value } ?: return
    device.bluetoothPeripheralName.value = peripheral.name
    device.bluetoothPeripheralId.value = deviceId
}

@Composable
fun CatPrinterSettingsView(
    model: Model = LocalModel.current,
    catPrinters: SettingsCatPrinters,
    device: SettingsCatPrinter,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = device.name
    val bluetoothPeripheralId by device.bluetoothPeripheralId.collectAsState()
    val bluetoothPeripheralName by device.bluetoothPeripheralName.collectAsState()
    val enabled by device.enabled.collectAsState()
    val printChat by device.printChat.collectAsState()
    val printSnapshots by device.printSnapshots.collectAsState()
    val faxMeowSound by device.faxMeowSound.collectAsState()
    val catPrinterState by status.catPrinterState.collectAsState()
    val devices by catPrinters.devices.collectAsState()

    fun state(): String {
        return formatCatPrinterState(catPrinterState)
    }

    fun canEnable(): Boolean {
        return bluetoothPeripheralId != null
    }

    LaunchedEffect(Unit) {
        model.setCurrentCatPrinter(device)
    }

    Form(title = localized("Cat printer")) {
        Section {
            NameEditView(
                name = name,
                onNameChange = { newName ->
                    device.name = newName
                },
                existingNames = devices,
            )
        }
        Section(header = localized("Device")) {
            NavigationLink(
                destination = {
                    CatPrinterScannerSettingsView(
                        scanner = catPrinterScanner,
                        selectedId = bluetoothPeripheralId?.toString()
                            ?: localized("Select device"),
                        onChange = { value ->
                            onDeviceChange(device, value)
                        },
                        onDismiss = rememberDismiss(),
                    )
                },
                enabled = !model.isCatPrinterEnabled(device),
                label = {
                    GrayTextView(
                        text = bluetoothPeripheralName ?: localized("Select device")
                    )
                },
            )
        }
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = enabled,
                enabled = canEnable(),
                onChange = { value ->
                    device.enabled.value = value
                    if (value) {
                        model.enableCatPrinter(device)
                    } else {
                        model.disableCatPrinter(device)
                    }
                },
            )
        }
        Section {
            Toggle(
                title = localized("Print chat"),
                isOn = printChat,
                onChange = { value ->
                    device.printChat.value = value
                },
            )
            Toggle(
                title = localized("Print snapshots"),
                isOn = printSnapshots,
                onChange = { value ->
                    device.printSnapshots.value = value
                },
            )
            NavigationLink(
                destination = {
                    Form(title = localized("Print alerts")) {
                        NavigationLink(
                            destination = {
                                val printTwitch by device.printTwitch.collectAsState()
                                TwitchAlertsSettingsView(
                                    title = localized("Twitch"),
                                    alerts = printTwitch,
                                )
                            },
                            label = {
                                TwitchLogoAndNameView()
                            },
                        )
                        NavigationLink(
                            destination = {
                                val printKick by device.printKick.collectAsState()
                                KickAlertsSettingsView(
                                    title = localized("Kick"),
                                    alerts = printKick,
                                    showBans = false,
                                )
                            },
                            label = {
                                KickLogoAndNameView()
                            },
                        )
                    }
                },
                label = {
                    Text(localized("Print alerts"))
                },
            )
        }
        Section {
            Toggle(
                title = localized("Fax meow sound"),
                isOn = faxMeowSound,
                onChange = { value ->
                    device.faxMeowSound.value = value
                    model.catPrinterSetFaxMeowSound(device)
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
                TextButtonView(localized("Test")) {
                    model.catPrinterPrintTestImage(device)
                }
            }
        }
    }
}
