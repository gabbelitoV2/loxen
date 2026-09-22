package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun DjiDeviceSettingsWrapperView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    device: SettingsDjiDevice,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = device.name
    val state = device.state
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("DjiDeviceSettingsView") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        Text(name)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = formatDjiDeviceState(state = state))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DjiDevicesSettingsView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val devices = djiDevices.devices

    fun deleteDevice(offsets: List<Int>) {
        djiDevices.devices = djiDevices.devices.filterIndexed { index, _ -> index !in offsets }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("DJI devices") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                HCenter {
                    IntegrationImageView(imageName = "DjiOa4")
                }
            }
            items(items = devices, key = { it.id.toString() }) { device ->
                DjiDeviceSettingsWrapperView(
                    model = model,
                    djiDevices = djiDevices,
                    device = device,
                    onNavigate = onNavigate,
                )
                Unit
            }
            item {
                CreateButtonView {
                    val device = SettingsDjiDevice()
                    device.name = makeUniqueName(
                        name = SettingsDjiDevice.baseName,
                        existingNames = djiDevices.devices,
                    )
                    djiDevices.devices = djiDevices.devices + device
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a device"))
            }
        }
    }
}
