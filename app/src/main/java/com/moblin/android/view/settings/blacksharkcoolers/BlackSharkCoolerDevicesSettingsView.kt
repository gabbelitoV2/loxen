package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel

@Composable
fun BlackSharkCoolerDevicesSettingsView(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
) {
    val statusTopRight = model.statusTopRight
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            HCenter {
                IntegrationImageView(imageName = "BlackSharkMagCooler4Pro")
            }
        }
        items(blackSharkCoolerDevices.devices, key = { it.id }) { device ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    BlackSharkCoolerDeviceSettingsView(
                        blackSharkCoolerDevices = blackSharkCoolerDevices,
                        device = device,
                        status = statusTopRight,
                    )
                }
                IconButton(onClick = {
                    blackSharkCoolerDevices.devices =
                        blackSharkCoolerDevices.devices.filterNot { it.id == device.id }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        item {
            CreateButtonView {
                val device = SettingsBlackSharkCoolerDevice()
                device.name = makeUniqueName(
                    name = SettingsBlackSharkCoolerDevice.baseName,
                    existingNames = blackSharkCoolerDevices.devices,
                )
                blackSharkCoolerDevices.devices = blackSharkCoolerDevices.devices + device
            }
        }
        item {
            SwipeLeftToDeleteHelpView(kind = localized("a cooler"))
        }
    }
}
