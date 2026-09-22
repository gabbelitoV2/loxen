package com.moblin.android.view.settings.display.localoverlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moblin.android.various.settings.SettingsShow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOverlaysSettingsView(show: SettingsShow) {
    val stream by show.stream.collectAsState()
    val cameras by show.cameras.collectAsState()
    val microphone by show.microphone.collectAsState()
    val zoom by show.zoom.collectAsState()
    val obsStatus by show.obsStatus.collectAsState()
    val events by show.events.collectAsState()
    val chat by show.chat.collectAsState()
    val viewers by show.viewers.collectAsState()
    val audioLevel by show.audioLevel.collectAsState()
    val systemMonitor by show.systemMonitor.collectAsState()
    val location by show.location.collectAsState()
    val ingests by show.ingests.collectAsState()
    val moblink by show.moblink.collectAsState()
    val remoteControl by show.remoteControl.collectAsState()
    val djiDevices by show.djiDevices.collectAsState()
    val gameController by show.gameController.collectAsState()
    val speed by show.speed.collectAsState()
    val uptime by show.uptime.collectAsState()
    val browserWidgets by show.browserWidgets.collectAsState()
    val bonding by show.bonding.collectAsState()
    val bondingRtts by show.bondingRtts.collectAsState()
    val catPrinter by show.catPrinter.collectAsState()
    val workoutDevice by show.workoutDevice.collectAsState()
    val zoomPresets by show.zoomPresets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Local overlays") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Text(
                    "Top left",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                LocalOverlayToggle("Stream", Icons.Default.PlayArrow, stream) {
                    show.stream.value = it
                }
            }
            item {
                LocalOverlayToggle("Camera", Icons.Default.CameraAlt, cameras) {
                    show.cameras.value = it
                }
            }
            item {
                LocalOverlayToggle("Mic", Icons.Default.Mic, microphone) {
                    show.microphone.value = it
                }
            }
            item {
                LocalOverlayToggle("Zoom", Icons.Default.Search, zoom) {
                    show.zoom.value = it
                }
            }
            item {
                LocalOverlayToggle("OBS remote control", Icons.Default.Dns, obsStatus) {
                    show.obsStatus.value = it
                }
            }
            item {
                LocalOverlayToggle("Events (alerts)", Icons.Default.Notifications, events) {
                    show.events.value = it
                }
            }
            item {
                LocalOverlayToggle("Chat", Icons.Default.Chat, chat) {
                    show.chat.value = it
                }
            }
            item {
                LocalOverlayToggle("Viewers", Icons.Default.Visibility, viewers) {
                    show.viewers.value = it
                }
            }
            item {
                Text(
                    "Top right",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                LocalOverlayToggle("Audio level", Icons.Default.GraphicEq, audioLevel) {
                    show.audioLevel.value = it
                }
            }
            item {
                LocalOverlayToggle("System monitor", Icons.Default.Memory, systemMonitor) {
                    show.systemMonitor.value = it
                }
            }
            item {
                LocalOverlayToggle("Location", Icons.Default.LocationOn, location) {
                    show.location.value = it
                }
            }
            item {
                LocalOverlayToggle("Ingests", Icons.Default.Storage, ingests) {
                    show.ingests.value = it
                }
            }
            item {
                LocalOverlayToggle("Moblink", Icons.Default.Link, moblink) {
                    show.moblink.value = it
                }
            }
            item {
                LocalOverlayToggle("Remote control", Icons.Default.SettingsRemote, remoteControl) {
                    show.remoteControl.value = it
                }
            }
            item {
                LocalOverlayToggle("DJI devices", Icons.Default.SettingsRemote, djiDevices) {
                    show.djiDevices.value = it
                }
            }
            item {
                LocalOverlayToggle("Game controllers", Icons.Default.VideogameAsset, gameController) {
                    show.gameController.value = it
                }
            }
            item {
                LocalOverlayToggle("Bitrate", Icons.Default.Speed, speed) {
                    show.speed.value = it
                }
            }
            item {
                LocalOverlayToggle("Uptime", Icons.Default.AccessTime, uptime) {
                    show.uptime.value = it
                }
            }
            item {
                LocalOverlayToggle("Browser widgets", Icons.Default.Public, browserWidgets) {
                    show.browserWidgets.value = it
                }
            }
            item {
                LocalOverlayToggle("Bonding", Icons.Default.Phone, bonding) {
                    show.bonding.value = it
                }
            }
            item {
                LocalOverlayToggle("Bonding RTTs", Icons.Default.Phone, bondingRtts) {
                    show.bondingRtts.value = it
                }
            }
            item {
                LocalOverlayToggle("Cat printers", Icons.Default.Pets, catPrinter) {
                    show.catPrinter.value = it
                }
            }
            item {
                LocalOverlayToggle("Workout devices", Icons.Default.DirectionsWalk, workoutDevice) {
                    show.workoutDevice.value = it
                }
            }
            item {
                Text(
                    "Bottom right",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                LocalOverlayToggle("Zoom presets", Icons.Default.Search, zoomPresets) {
                    show.zoomPresets.value = it
                }
            }
            item {
                Text(
                    "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Text(
                    "Local overlays do not appear on stream.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LocalOverlayToggle(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
