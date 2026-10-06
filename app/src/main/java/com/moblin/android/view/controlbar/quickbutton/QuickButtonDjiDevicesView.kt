package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDevices
import com.moblin.android.view.settings.djidevices.DjiDevicesSettingsView
import com.moblin.android.view.settings.djidevices.formatDjiDeviceState
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.integrations.dji.djidevice.canStartLive
import com.moblin.android.various.model.startDjiDeviceLiveStream
import com.moblin.android.various.model.stopDjiDeviceLiveStream

@Composable
private fun DeviceView(
    model: Model = LocalModel.current,
    status: StatusOther,
    device: SettingsDjiDevice,
) {
    val isStarted = device.isStarted
    val state = device.state
    Toggle(
        isOn = isStarted,
        onChange = { value ->
            if (value) {
                model.startDjiDeviceLiveStream(device = device)
            } else {
                model.stopDjiDeviceLiveStream(device = device)
            }
        },
        enabled = device.canStartLive(status.isConnectedToIpv4WiFi()),
        label = {
            Text(device.name)
            Spacer(Modifier.weight(1f))
            GrayTextView(text = formatDjiDeviceState(state))
        },
    )
}

@Composable
fun QuickButtonDjiDevicesView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "DJI devices") {
        Section {
            djiDevices.devices.forEach { device ->
                DeviceView(model = model, status = model.statusOther, device = device)
            }
        }
        ShortcutSectionView {
            NavigationLink(
                destination = {
                    DjiDevicesSettingsView(model = model, djiDevices = djiDevices)
                },
            ) {
                Label("DJI devices", systemImage = "appletvremote.gen1")
            }
        }
    }
}
