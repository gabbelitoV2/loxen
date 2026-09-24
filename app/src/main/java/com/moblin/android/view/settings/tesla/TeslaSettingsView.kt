package com.moblin.android.view.settings.tesla

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.integrations.tesla.TeslaVehicleScanner
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.teslaGeneratePrivateKey
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.rememberDismiss
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
import com.moblin.android.view.utils.TextEditNavigationView
import java.security.PrivateKey
import java.util.UUID

private fun formatTeslaVehicleState(state: TeslaVehicleState?): String {
    return when {
        state == null || state == TeslaVehicleState.idle -> localized("Disconnected")
        state == TeslaVehicleState.connecting -> localized("Connecting")
        state == TeslaVehicleState.connected -> localized("Connected")
        else -> localized("Unknown")
    }
}

private fun PrivateKey.pemRepresentation(): String {
    val base64 = android.util.Base64.encodeToString(encoded, android.util.Base64.NO_WRAP)
    val lines = base64.chunked(64).joinToString("\n")
    return "-----BEGIN PRIVATE KEY-----\n$lines\n-----END PRIVATE KEY-----\n"
}

@Composable
fun TeslaSettingsConfigurationView(
    model: Model = LocalModel.current,
    tesla: Tesla,
    settings: SettingsTesla,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val database = model.database
    val vehicleState by tesla.vehicleState.collectAsState()
    val addKeyToVehicleEnabled = database.tesla.vin.isNotEmpty() &&
        database.tesla.privateKey.isNotEmpty() &&
        vehicleState == TeslaVehicleState.connected

    fun onSubmitVin(value: String) {
        database.tesla.vin = value.trim()
        model.reloadTeslaVehicle()
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = TeslaVehicleScanner.shared.discoveredPeripherals.value
            .firstOrNull { UUID.nameUUIDFromBytes(it.address.toByteArray()) == deviceId } ?: return
        settings.bluetoothPeripheralName = peripheral.name
        settings.bluetoothPeripheralId = deviceId
        model.reloadTeslaVehicle()
    }

    Form(title = localized("Configuration")) {
        Section(
            header = localized("Vehicle"),
            footer = localized("Scroll down in your Tesla app and copy it."),
        ) {
            NavigationLink(
                destination = {
                    TeslaVehicleScannerSettingsView(
                        onChange = { onDeviceChange(it) },
                        onDismiss = rememberDismiss(),
                    )
                },
            ) {
                GrayTextView(
                    text = settings.bluetoothPeripheralName ?: localized("Select vehicle"),
                )
            }
            TextEditNavigationView(
                title = localized("VIN"),
                value = database.tesla.vin,
                onSubmit = { onSubmitVin(it) },
            )
        }
        Section(
            footer = localized(
                "Moblin identifies itself to the vehicle with this key. Tap the button below " +
                    "to add it to your vehicle.",
            ),
        ) {
            FormButton(title = "Generate new key") {
                database.tesla.privateKey = teslaGeneratePrivateKey().pemRepresentation()
                model.reloadTeslaVehicle()
            }
        }
        Section(
            footer = localized("Remove keys in Controls → Locks on your Tesla's center screen."),
        ) {
            FormButton(
                title = "Add key to vehicle",
                enabled = addKeyToVehicleEnabled,
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
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val vehicleState by tesla.vehicleState.collectAsState()
    val vehicleInfotainmentConnected by tesla.vehicleInfotainmentConnected.collectAsState()
    val vehicleVehicleSecurityConnected by tesla.vehicleVehicleSecurityConnected.collectAsState()

    Form(title = localized("Tesla")) {
        Section {
            HCenter {
                val bitmap = Bundle.image("Tesla")?.asImageBitmap()
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { model.database.tesla.enabled },
                    set = {
                        model.database.tesla.enabled = it
                        model.reloadTeslaVehicle()
                    },
                ),
            )
            NavigationLink(
                destination = {
                    TeslaSettingsConfigurationView(tesla = tesla, settings = model.database.tesla)
                },
            ) {
                Text(
                    text = localized("Configuration"),
                    style = formBodyStyle,
                    color = formPalette().label,
                )
            }
        }
        Section {
            HCenter {
                Text(
                    text = formatTeslaVehicleState(state = vehicleState),
                    style = formBodyStyle,
                    color = formPalette().label,
                )
            }
        }
        Section {
            FormButton(
                title = "Flash lights",
                enabled = vehicleInfotainmentConnected,
            ) {
                model.teslaFlashLights()
            }
        }
        Section {
            FormButton(
                title = "Honk",
                enabled = vehicleInfotainmentConnected,
            ) {
                model.teslaHonk()
            }
        }
        Section {
            FormButton(
                title = "Open trunk",
                enabled = vehicleVehicleSecurityConnected,
            ) {
                model.teslaOpenTrunk()
            }
        }
        Section {
            FormButton(
                title = "Close trunk",
                enabled = vehicleVehicleSecurityConnected,
            ) {
                model.teslaCloseTrunk()
            }
        }
        Section {
            FormButton(
                title = "Next media track",
                enabled = vehicleInfotainmentConnected,
            ) {
                model.mediaNextTrack()
            }
        }
        Section {
            FormButton(
                title = "Previous media track",
                enabled = vehicleInfotainmentConnected,
            ) {
                model.mediaPreviousTrack()
            }
        }
        Section {
            FormButton(
                title = "Toggle media playback",
                enabled = vehicleInfotainmentConnected,
            ) {
                model.mediaTogglePlayback()
            }
        }
    }
}
