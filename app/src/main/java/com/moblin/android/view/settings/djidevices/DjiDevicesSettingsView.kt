package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
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

@Composable
private fun DjiDeviceSettingsWrapperView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    device: SettingsDjiDevice,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            val status = model.statusTopRight
            DjiDeviceSettingsView(
                djiDevices = djiDevices,
                device = device,
                status = status,
            )
        },
    ) {
        DraggableItemPrefixView()
        Text(device.name)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = formatDjiDeviceState(state = device.state))
    }
}

@Composable
fun DjiDevicesSettingsView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteDevice(offsets: List<Int>) {
        djiDevices.devices = djiDevices.devices.filterIndexed { index, _ -> index !in offsets }
    }

    Form(title = "DJI devices") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "DjiOa4")
            }
        }
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a device"))
            },
        ) {
            for (device in djiDevices.devices) {
                key(device.id) {
                    DjiDeviceSettingsWrapperView(
                        model = model,
                        djiDevices = djiDevices,
                        device = device,
                        onNavigate = onNavigate,
                    )
                }
            }
            CreateButtonView {
                val device = SettingsDjiDevice()
                device.name = makeUniqueName(
                    name = SettingsDjiDevice.baseName,
                    existingNames = djiDevices.devices,
                )
                djiDevices.devices = djiDevices.devices + device
            }
        }
    }
}
