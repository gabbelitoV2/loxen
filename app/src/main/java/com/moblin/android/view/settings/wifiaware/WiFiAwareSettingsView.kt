package com.moblin.android.view.settings.wifiaware

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWiFiAware
import com.moblin.android.various.settings.SettingsWiFiAwareRole
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import com.moblin.android.LocalModel

private const val serviceName = "_moblin._tcp"

fun wiFiAwarePublishableService(): WAPublishableService =
    TODO("no Android counterpart for WiFiAware WAPublishableService.allServices[serviceName]")

fun wiFiAwareSubscribableService(): WASubscribableService =
    TODO("no Android counterpart for WiFiAware WASubscribableService.allServices[serviceName]")

@Composable
private fun PairedDevicesView() {
    var pairedDevices by remember { mutableStateOf<List<WAPairedDevice>>(emptyList()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Paired devices")
        pairedDevices.forEach { device ->
            Text(device.displayName())
        }
    }
    LaunchedEffect(Unit) {
        runCatching {
            WAPairedDevice.allDevices.collect { updatedDeviceList ->
                pairedDevices = updatedDeviceList.values.toList()
            }
        }
    }
}

@Composable
private fun AdvertiseView() {
    TODO("no Android counterpart for DeviceDiscoveryUI DevicePairingView")
}

@Composable
private fun SearchView() {
    TODO("no Android counterpart for DeviceDiscoveryUI DevicePicker")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiAwareSettingsView(model: Model = LocalModel.current, wiFiAware: SettingsWiFiAware) {
    val enabled = wiFiAware.enabled
    val role = wiFiAware.role
    var roleExpanded by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        if (WACapabilities.supportedFeatures.contains(WAFeature.wifiAware)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Enabled",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newValue ->
                            wiFiAware.enabled = newValue
                            TODO("model.wiFiAwareUpdated()")
                        },
                    )
                }
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { newExpanded ->
                        if (!enabled) {
                            roleExpanded = newExpanded
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    OutlinedTextField(
                        value = role.toString(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !enabled,
                        label = { Text("Role") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !enabled)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false },
                    ) {
                        SettingsWiFiAwareRole.entries.forEach { roleCase ->
                            DropdownMenuItem(
                                text = { Text(roleCase.toString()) },
                                onClick = {
                                    wiFiAware.role = roleCase
                                    roleExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item { PairedDevicesView() }
            item { AdvertiseView() }
            item { SearchView() }
        } else {
            item {
                Text(
                    text = "This device does not support WiFi Aware",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

fun WAPairedDevice.displayName(): String {
    val deviceName = name ?: pairingInfo?.pairingName ?: "Unknown"
    val vendorName = pairingInfo?.vendorName ?: "Unknown"
    return "$deviceName ($vendorName)"
}

class WAPublishableService

class WASubscribableService

enum class WAFeature {
    wifiAware,
}

object WACapabilities {
    val supportedFeatures: Set<WAFeature> =
        TODO("no Android counterpart for WiFiAware WACapabilities.supportedFeatures")
}

class WAPairingInfo(
    val pairingName: String? = null,
    val vendorName: String? = null,
)

class WAPairedDevice(
    val name: String? = null,
    val pairingInfo: WAPairingInfo? = null,
) {
    companion object {
        val allDevices: Flow<Map<UUID, WAPairedDevice>> =
            TODO("no Android counterpart for WiFiAware WAPairedDevice.allDevices")
    }
}
