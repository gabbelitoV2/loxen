package com.moblin.android.view.settings.wifiaware

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWiFiAware
import com.moblin.android.various.settings.SettingsWiFiAwareRole
import com.moblin.android.view.utils.HCenter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import com.moblin.android.various.model.wiFiAwareUpdated

private const val serviceName = "_moblin._tcp"

fun wiFiAwarePublishableService(): WAPublishableService = WAPublishableService()

fun wiFiAwareSubscribableService(): WASubscribableService = WASubscribableService()

@Composable
private fun PairedDevicesView() {
    var pairedDevices by remember { mutableStateOf<List<WAPairedDevice>>(emptyList()) }
    Section(header = "Paired devices") {
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
    Section {
        HCenter {
            Text("Advertise")
        }
    }
}

@Composable
private fun SearchView() {
    Section {
        HCenter {
            Text("Search")
        }
    }
}

@Composable
fun WiFiAwareSettingsView(model: Model = LocalModel.current, wiFiAware: SettingsWiFiAware) {
    Form(title = "WiFi Aware") {
        if (WACapabilities.supportedFeatures.contains(WAFeature.wifiAware)) {
            Section {
                Toggle("Enabled", isOn = wiFiAware.enabled) { newValue ->
                    wiFiAware.enabled = newValue
                    model.wiFiAwareUpdated()
                }
            }
            Section {
                Picker(
                    title = "Role",
                    selection = wiFiAware.role,
                    options = SettingsWiFiAwareRole.entries,
                    enabled = !wiFiAware.enabled,
                ) { newValue ->
                    wiFiAware.role = newValue
                }
            }
            PairedDevicesView()
            AdvertiseView()
            SearchView()
        } else {
            Section {
                Text("This device does not support WiFi Aware")
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
        TODO()
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
            TODO()
    }
}
