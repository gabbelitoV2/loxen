package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
fun BlackSharkCoolerDevicesSettingsView(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
) {
    Form(title = "Black Shark coolers") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "BlackSharkMagCooler4Pro")
            }
        }
        Section(footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("a cooler"))
        }) {
            ForEach(
                blackSharkCoolerDevices.devices,
                id = { it.id },
                onDelete = { offsets ->
                    blackSharkCoolerDevices.devices = blackSharkCoolerDevices.devices
                        .removing(atOffsets = offsets)
                },
            ) { device ->
                ContextMenuDeleteButton(disabled = false, action = {
                    blackSharkCoolerDevices.devices = blackSharkCoolerDevices.devices
                        .filterNot { it.id == device.id }
                }) {
                    BlackSharkCoolerDeviceSettingsView(
                        blackSharkCoolerDevices = blackSharkCoolerDevices,
                        device = device,
                        status = model.statusTopRight,
                    )
                }
            }
            CreateButtonView {
                val device = SettingsBlackSharkCoolerDevice()
                device.name = makeUniqueName(
                    name = SettingsBlackSharkCoolerDevice.baseName,
                    existingNames = blackSharkCoolerDevices.devices,
                )
                blackSharkCoolerDevices.devices = blackSharkCoolerDevices.devices + device
            }
        }
    }
}
