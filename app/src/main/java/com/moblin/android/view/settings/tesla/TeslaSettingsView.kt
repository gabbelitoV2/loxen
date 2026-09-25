package com.moblin.android.view.settings.tesla

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.moblin.android.LocalModel
import com.moblin.android.integrations.tesla.TeslaVehicleScanner
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.teslaGeneratePrivateKey
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Tesla
import com.moblin.android.various.model.mediaNextTrack
import com.moblin.android.various.model.mediaPreviousTrack
import com.moblin.android.various.model.mediaTogglePlayback
import com.moblin.android.various.model.reloadTeslaVehicle
import com.moblin.android.various.model.teslaAddKeyToVehicle
import com.moblin.android.various.model.teslaCloseTrunk
import com.moblin.android.various.model.teslaFlashLights
import com.moblin.android.various.model.teslaHonk
import com.moblin.android.various.model.teslaOpenTrunk
import com.moblin.android.various.settings.SettingsTesla
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID

private fun formatTeslaVehicleState(state: TeslaVehicleState?): String {
    return if (state == null || state == TeslaVehicleState.idle) {
        localized("Disconnected")
    } else if (state == TeslaVehicleState.connecting) {
        localized("Connecting")
    } else if (state == TeslaVehicleState.connected) {
        localized("Connected")
    } else {
        localized("Unknown")
    }
}

@Composable
private fun TeslaSettingsConfigurationView(
    model: Model = LocalModel.current,
    tesla: Tesla,
    settings: SettingsTesla,
) {
    val database = model.database
    val vehicleState by tesla.vehicleState.collectAsState()

    fun onSubmitVin(value: String) {
        database.tesla.vin = value.trim()
        model.reloadTeslaVehicle()
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = TeslaVehicleScanner.shared.discoveredPeripherals.value
            .firstOrNull { it.identifier == deviceId } ?: return
        settings.bluetoothPeripheralName = peripheral.name
        settings.bluetoothPeripheralId = deviceId
        model.reloadTeslaVehicle()
    }

    Form(title = "Configuration") {
        Section(
            header = "Vehicle",
            footer = "Scroll down in your Tesla app and copy it.",
        ) {
            NavigationLink(destination = {
                TeslaVehicleScannerSettingsView(onChange = { onDeviceChange(it) })
            }) {
                GrayTextView(text = settings.bluetoothPeripheralName ?: localized("Select vehicle"))
            }
            TextEditNavigationView(
                title = localized("VIN"),
                value = database.tesla.vin,
                onSubmit = { onSubmitVin(it) },
            )
        }
        Section(
            footer = "Moblin identifies itself to the vehicle with this key. Tap the button below to add it to your vehicle.",
        ) {
            TextButtonView("Generate new key") {
                database.tesla.privateKey = teslaGeneratePrivateKey().pemRepresentation
                model.reloadTeslaVehicle()
            }
        }
        Section(footer = "Remove keys in Controls → Locks on your Tesla's center screen.") {
            FormButton(
                title = "Add key to vehicle",
                centered = true,
                enabled = !(vehicleState != TeslaVehicleState.connected || database.tesla.vin.isEmpty() || database.tesla.privateKey.isEmpty()),
            ) {
                model.teslaAddKeyToVehicle()
            }
        }
    }
}

@Composable
fun TeslaSettingsView(
    model: Model = LocalModel.current,
    tesla: Tesla,
) {
    val vehicleState by tesla.vehicleState.collectAsState()
    val vehicleVehicleSecurityConnected by tesla.vehicleVehicleSecurityConnected.collectAsState()
    val vehicleInfotainmentConnected by tesla.vehicleInfotainmentConnected.collectAsState()

    Form(title = "Tesla") {
        Section {
            HCenter {
                AssetImage(
                    name = "Tesla",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Section {
            Toggle(
                title = "Enabled",
                isOn = binding(
                    get = { model.database.tesla.enabled },
                    set = {
                        model.database.tesla.enabled = it
                        model.reloadTeslaVehicle()
                    },
                ),
            )
            NavigationLink(destination = {
                TeslaSettingsConfigurationView(tesla = tesla, settings = model.database.tesla)
            }) {
                Text(localized("Configuration"))
            }
        }
        Section {
            HCenter {
                Text(formatTeslaVehicleState(state = vehicleState))
            }
        }
        Section {
            FormButton(title = "Flash lights", centered = true, enabled = vehicleInfotainmentConnected) {
                model.teslaFlashLights()
            }
        }
        Section {
            FormButton(title = "Honk", centered = true, enabled = vehicleInfotainmentConnected) {
                model.teslaHonk()
            }
        }
        Section {
            FormButton(title = "Open trunk", centered = true, enabled = vehicleVehicleSecurityConnected) {
                model.teslaOpenTrunk()
            }
        }
        Section {
            FormButton(title = "Close trunk", centered = true, enabled = vehicleVehicleSecurityConnected) {
                model.teslaCloseTrunk()
            }
        }
        Section {
            FormButton(title = "Next media track", centered = true, enabled = vehicleInfotainmentConnected) {
                model.mediaNextTrack()
            }
        }
        Section {
            FormButton(title = "Previous media track", centered = true, enabled = vehicleInfotainmentConnected) {
                model.mediaPreviousTrack()
            }
        }
        Section {
            FormButton(title = "Toggle media playback", centered = true, enabled = vehicleInfotainmentConnected) {
                model.mediaTogglePlayback()
            }
        }
    }
}
