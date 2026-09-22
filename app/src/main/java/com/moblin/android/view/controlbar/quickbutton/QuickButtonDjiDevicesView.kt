package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDevices
import com.moblin.android.view.settings.djidevices.formatDjiDeviceState
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun DeviceView(
    model: Model = LocalModel.current,
    status: StatusOther,
    device: SettingsDjiDevice,
) {
    val name = device.name
    val isStarted = device.isStarted
    val state = device.state
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name)
        Spacer(Modifier.weight(1f))
        GrayTextView(text = formatDjiDeviceState(state))
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = isStarted,
            onCheckedChange = { value ->
                if (value) {
                    Unit
                } else {
                    Unit
                }
            },
            enabled = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonDjiDevicesView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val devices = djiDevices.devices
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("DJI devices") })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(devices) { device ->
                DeviceView(model = model, status = model.statusOther, device = device)
            }
            item {
                ShortcutSectionView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("DjiDevicesSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("DJI devices")
                    }
                }
            }
        }
    }
}
