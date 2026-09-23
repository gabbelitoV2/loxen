package com.moblin.android.view.settings.display.localoverlays

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.settings.SettingsShow

@Composable
fun LocalOverlaysSettingsView(show: SettingsShow) {
    val stream = binding({ show.stream }, { show.stream = it })
    val cameras = binding({ show.cameras }, { show.cameras = it })
    val microphone = binding({ show.microphone }, { show.microphone = it })
    val zoom = binding({ show.zoom }, { show.zoom = it })
    val obsStatus = binding({ show.obsStatus }, { show.obsStatus = it })
    val events = binding({ show.events }, { show.events = it })
    val chat = binding({ show.chat }, { show.chat = it })
    val viewers = binding({ show.viewers }, { show.viewers = it })
    val audioLevel = binding({ show.audioLevel }, { show.audioLevel = it })
    val systemMonitor = binding({ show.systemMonitor }, { show.systemMonitor = it })
    val location = binding({ show.location }, { show.location = it })
    val ingests = binding({ show.ingests }, { show.ingests = it })
    val moblink = binding({ show.moblink }, { show.moblink = it })
    val remoteControl = binding({ show.remoteControl }, { show.remoteControl = it })
    val djiDevices = binding({ show.djiDevices }, { show.djiDevices = it })
    val gameController = binding({ show.gameController }, { show.gameController = it })
    val speed = binding({ show.speed }, { show.speed = it })
    val uptime = binding({ show.uptime }, { show.uptime = it })
    val browserWidgets = binding({ show.browserWidgets }, { show.browserWidgets = it })
    val bonding = binding({ show.bonding }, { show.bonding = it })
    val bondingRtts = binding({ show.bondingRtts }, { show.bondingRtts = it })
    val catPrinter = binding({ show.catPrinter }, { show.catPrinter = it })
    val workoutDevice = binding({ show.workoutDevice }, { show.workoutDevice = it })
    val zoomPresets = binding({ show.zoomPresets }, { show.zoomPresets = it })

    Form(title = "Local overlays") {
        Section(header = "Top left") {
            Toggle(isOn = stream.value, onChange = { stream.value = it }) {
                Label(
                    "Stream",
                    systemImage = "dot.radiowaves.left.and.right",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = cameras.value, onChange = { cameras.value = it }) {
                Label(
                    "Camera",
                    systemImage = "camera",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = microphone.value, onChange = { microphone.value = it }) {
                Label(
                    "Mic",
                    systemImage = "music.mic",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = zoom.value, onChange = { zoom.value = it }) {
                Label(
                    "Zoom",
                    systemImage = "magnifyingglass",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = obsStatus.value, onChange = { obsStatus.value = it }) {
                Label(
                    "OBS remote control",
                    systemImage = "xserve",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = events.value, onChange = { events.value = it }) {
                Label(
                    "Events (alerts)",
                    systemImage = "megaphone",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = chat.value, onChange = { chat.value = it }) {
                Label(
                    "Chat",
                    systemImage = "message",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = viewers.value, onChange = { viewers.value = it }) {
                Label(
                    "Viewers",
                    systemImage = "eye",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Section(header = "Top right") {
            Toggle(isOn = audioLevel.value, onChange = { audioLevel.value = it }) {
                Label(
                    "Audio level",
                    systemImage = "waveform",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = systemMonitor.value, onChange = { systemMonitor.value = it }) {
                Label(
                    "System monitor",
                    systemImage = "cpu",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = location.value, onChange = { location.value = it }) {
                Label(
                    "Location",
                    systemImage = "location",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = ingests.value, onChange = { ingests.value = it }) {
                Label(
                    "Ingests",
                    systemImage = "server.rack",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = moblink.value, onChange = { moblink.value = it }) {
                Label(
                    "Moblink",
                    systemImage = "app.connected.to.app.below.fill",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = remoteControl.value, onChange = { remoteControl.value = it }) {
                Label(
                    "Remote control",
                    systemImage = "appletvremote.gen1",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = djiDevices.value, onChange = { djiDevices.value = it }) {
                Label(
                    "DJI devices",
                    systemImage = "appletvremote.gen1",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = gameController.value, onChange = { gameController.value = it }) {
                Label(
                    "Game controllers",
                    systemImage = "gamecontroller",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = speed.value, onChange = { speed.value = it }) {
                Label(
                    "Bitrate",
                    systemImage = "speedometer",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = uptime.value, onChange = { uptime.value = it }) {
                Label(
                    "Uptime",
                    systemImage = "deskclock",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = browserWidgets.value, onChange = { browserWidgets.value = it }) {
                Label(
                    "Browser widgets",
                    systemImage = "globe",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = bonding.value, onChange = { bonding.value = it }) {
                Label(
                    "Bonding",
                    systemImage = "phone.connection",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = bondingRtts.value, onChange = { bondingRtts.value = it }) {
                Label(
                    "Bonding RTTs",
                    systemImage = "phone.connection",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = catPrinter.value, onChange = { catPrinter.value = it }) {
                Label(
                    "Cat printers",
                    systemImage = "pawprint",
                    modifier = Modifier.weight(1f),
                )
            }
            Toggle(isOn = workoutDevice.value, onChange = { workoutDevice.value = it }) {
                Label(
                    "Workout devices",
                    systemImage = "figure.walk.motion",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Section(
            header = "Bottom right",
            footerContent = {
                Text("")
                Text("Local overlays do not appear on stream.")
            },
        ) {
            Toggle(isOn = zoomPresets.value, onChange = { zoomPresets.value = it }) {
                Label(
                    "Zoom presets",
                    systemImage = "magnifyingglass",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
