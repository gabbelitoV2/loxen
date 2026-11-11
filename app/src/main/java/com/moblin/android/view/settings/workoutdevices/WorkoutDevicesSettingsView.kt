package com.moblin.android.view.settings.workoutdevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.integrationImageRow
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
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
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .aspectRatio(1f / (500f / 1455f + 500f / 931f))
                    .integrationImageRow(height = 180.0),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.aspectRatio(955f / 500f + 1f),
                ) {
                    AssetImage(
                        name = "HeartRateDevice",
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(955f / 500f),
                        contentScale = ContentScale.Fit,
                    )
                    AssetImage(
                        name = "HeartRateDeviceCoros",
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit,
                    )
                }
                AssetImage(
                    name = "CyclingPowerDevice",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(931f / 500f),
                    contentScale = ContentScale.Fit,
                )
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
