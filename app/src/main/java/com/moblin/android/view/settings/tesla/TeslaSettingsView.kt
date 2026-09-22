package com.moblin.android.view.settings.tesla

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.moblin.android.R
import com.moblin.android.integrations.tesla.TeslaVehicleScanner
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.teslaGeneratePrivateKey
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Tesla
import com.moblin.android.various.settings.SettingsTesla
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID

private fun formatTeslaVehicleState(state: TeslaVehicleState?): String {
    return when {
        state == null || state == TeslaVehicleState.idle -> localized("Disconnected")
        state == TeslaVehicleState.connecting -> localized("Connecting")
        state == TeslaVehicleState.connected -> localized("Connected")
        else -> localized("Unknown")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeslaSettingsConfigurationView(
    model: Model,
    tesla: Tesla,
    settings: SettingsTesla,
    onNavigate: (String) -> Unit,
) {
    val database = model.database
    val bluetoothPeripheralName by settings.bluetoothPeripheralName.collectAsState()
    val vin by database.tesla.vin.collectAsState()
    val privateKey by database.tesla.privateKey.collectAsState()
    val vehicleState by tesla.vehicleState.collectAsState()
    val addKeyToVehicleEnabled = vin.isNotEmpty() && privateKey.isNotEmpty() &&
        vehicleState == TeslaVehicleState.connected

    fun onSubmitVin(value: String) {
        database.tesla.vin.value = value.trim()
        model.reloadTeslaVehicle()
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = TeslaVehicleScanner.shared.discoveredPeripherals
            .firstOrNull { it.identifier == deviceId } ?: return
        settings.bluetoothPeripheralName.value = peripheral.name
        settings.bluetoothPeripheralId.value = deviceId
        model.reloadTeslaVehicle()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configuration") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text("Vehicle", style = MaterialTheme.typography.titleSmall)
            }
            item {
                Box(
                    modifier = Modifier.clickable {
                        onNavigate("TeslaVehicleScannerSettingsView")
                    },
                ) {
                    GrayTextView(text = bluetoothPeripheralName ?: localized("Select vehicle"))
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("VIN"),
                    value = vin,
                    onSubmit = { onSubmitVin(it) },
                )
            }
            item {
                Text("Scroll down in your Tesla app and copy it.")
            }
            item {
                TextButtonView(text = "Generate new key") {
                    database.tesla.privateKey.value = teslaGeneratePrivateKey().pemRepresentation
                    model.reloadTeslaVehicle()
                }
            }
            item {
                Text(
                    "Moblin identifies itself to the vehicle with this key. Tap the button below " +
                        "to add it to your vehicle.",
                )
            }
            item {
                TextButtonView(text = "Add key to vehicle", enabled = addKeyToVehicleEnabled) {
                    model.teslaAddKeyToVehicle()
                }
            }
            item {
                Text("Remove keys in Controls → Locks on your Tesla's center screen.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeslaSettingsView(model: Model, tesla: Tesla, onNavigate: (String) -> Unit) {
    val database = model.database
    val enabled by database.tesla.enabled.collectAsState()
    val vehicleState by tesla.vehicleState.collectAsState()
    val vehicleInfotainmentConnected by tesla.vehicleInfotainmentConnected.collectAsState()
    val vehicleVehicleSecurityConnected by tesla.vehicleVehicleSecurityConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tesla") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                HCenter {
                    Image(
                        painter = painterResource(id = R.drawable.tesla),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            database.tesla.enabled.value = it
                            model.reloadTeslaVehicle()
                        },
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier.clickable {
                        onNavigate("TeslaSettingsConfigurationView")
                    },
                ) {
                    Text("Configuration")
                }
            }
            item {
                HCenter {
                    Text(formatTeslaVehicleState(state = vehicleState))
                }
            }
            item {
                TextButtonView(
                    text = "Flash lights",
                    enabled = vehicleInfotainmentConnected,
                ) {
                    model.teslaFlashLights()
                }
            }
            item {
                TextButtonView(text = "Honk", enabled = vehicleInfotainmentConnected) {
                    model.teslaHonk()
                }
            }
            item {
                TextButtonView(text = "Open trunk", enabled = vehicleVehicleSecurityConnected) {
                    model.teslaOpenTrunk()
                }
            }
            item {
                TextButtonView(text = "Close trunk", enabled = vehicleVehicleSecurityConnected) {
                    model.teslaCloseTrunk()
                }
            }
            item {
                TextButtonView(text = "Next media track", enabled = vehicleInfotainmentConnected) {
                    model.mediaNextTrack()
                }
            }
            item {
                TextButtonView(
                    text = "Previous media track",
                    enabled = vehicleInfotainmentConnected,
                ) {
                    model.mediaPreviousTrack()
                }
            }
            item {
                TextButtonView(
                    text = "Toggle media playback",
                    enabled = vehicleInfotainmentConnected,
                ) {
                    model.mediaTogglePlayback()
                }
            }
        }
    }
}
