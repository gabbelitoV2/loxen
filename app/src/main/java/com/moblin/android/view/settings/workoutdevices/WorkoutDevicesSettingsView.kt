package com.moblin.android.view.settings.workoutdevices

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDevicesSettingsView(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
) {
    val devices = workoutDevices.devices
    val statusTopRight = model.statusTopRight
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(localized("Workout devices")) })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column {
                    HCenter {
                        IntegrationImageView(imageName = "HeartRateDevice", height = 80.0)
                        IntegrationImageView(imageName = "HeartRateDeviceCoros", height = 80.0)
                    }
                    IntegrationImageView(imageName = "CyclingPowerDevice", height = 80.0)
                }
            }
            items(items = devices, key = { it.id }) { device ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            workoutDevices.devices = workoutDevices.devices
                                .filterNot { it.id == device.id }
                                .toMutableList()
                            true
                        } else {
                            false
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                        )
                    },
                    enableDismissFromStartToEnd = false,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contextMenuDeleteButton {
                                workoutDevices.devices = workoutDevices.devices
                                    .filterNot { it.id == device.id }
                                    .toMutableList()
                            },
                    ) {
                        WorkoutDeviceSettingsView(
                            model = model,
                            workoutDevices = workoutDevices,
                            device = device,
                            status = statusTopRight,
                        )
                    }
                }
            }
            item {
                CreateButtonView {
                    val device = SettingsWorkoutDevice()
                    device.name = makeUniqueName(SettingsWorkoutDevice.baseName, devices)
                    workoutDevices.devices = (workoutDevices.devices + device).toMutableList()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a device"))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.contextMenuDeleteButton(onDelete: () -> Unit): Modifier =
    this.combinedClickable(onClick = {}, onLongClick = onDelete)
