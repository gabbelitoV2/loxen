package com.moblin.android.view.settings.workoutdevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel

@Composable
fun WorkoutDevicesSettingsView(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
) {
    Form(title = "Workout devices") {
        Section {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HCenter {
                    IntegrationImageView(imageName = "HeartRateDevice", height = 80.0)
                    IntegrationImageView(imageName = "HeartRateDeviceCoros", height = 80.0)
                }
                IntegrationImageView(imageName = "CyclingPowerDevice", height = 80.0)
            }
        }
        Section(footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("a device"))
        }) {
            ForEach(
                workoutDevices.devices,
                id = { it.id },
                onDelete = { offsets -> workoutDevices.devices.remove(atOffsets = offsets) },
            ) { device ->
                ContextMenuDeleteButton(action = {
                    workoutDevices.devices.removeAll { it.id == device.id }
                }) {
                    WorkoutDeviceSettingsView(
                        model = model,
                        workoutDevices = workoutDevices,
                        device = device,
                        status = model.statusTopRight,
                    )
                }
            }
            CreateButtonView {
                val device = SettingsWorkoutDevice()
                device.name = makeUniqueName(SettingsWorkoutDevice.baseName, workoutDevices.devices)
                workoutDevices.devices.add(device)
            }
        }
    }
}
