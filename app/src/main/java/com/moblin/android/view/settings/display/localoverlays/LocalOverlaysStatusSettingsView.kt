package com.moblin.android.view.settings.display.localoverlays

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsShow
import com.moblin.android.various.utils.isMac

@Composable
fun LocalOverlaysStatusSettingsView(database: Database, show: SettingsShow) {
    Form(title = "Status") {
        if (database.showAllSettings) {
            Section(header = "Top left") {
                Toggle(isOn = show.stream, onChange = { show.stream = it }) {
                    Label("Stream", systemImage = "dot.radiowaves.left.and.right")
                }
                Toggle(isOn = show.cameras, onChange = { show.cameras = it }) {
                    Label("Camera", systemImage = "camera")
                }
                Toggle(isOn = show.microphone, onChange = { show.microphone = it }) {
                    Label("Mic", systemImage = "music.mic")
                }
                Toggle(isOn = show.zoom, onChange = { show.zoom = it }) {
                    Label("Zoom", systemImage = "magnifyingglass")
                }
                Toggle(isOn = show.obsStatus, onChange = { show.obsStatus = it }) {
                    Label("OBS remote control", systemImage = "xserve")
                }
                Toggle(isOn = show.events, onChange = { show.events = it }) {
                    Label("Events (alerts)", systemImage = "megaphone")
                }
                Toggle(isOn = show.chat, onChange = { show.chat = it }) {
                    Label("Chat", systemImage = "message")
                }
                Toggle(isOn = show.viewers, onChange = { show.viewers = it }) {
                    Label("Viewers", systemImage = "eye")
                }
            }
        }
        Section(header = "Top right") {
            Toggle(isOn = show.audioLevel, onChange = { show.audioLevel = it }) {
                Label("Audio level", systemImage = "waveform")
            }
            Toggle(isOn = database.bigAudioLevelMeter, onChange = { database.bigAudioLevelMeter = it }) {
                Label("Big audio level meter", systemImage = "waveform")
            }
            if (database.showAllSettings) {
                Toggle(isOn = show.systemMonitor, onChange = { show.systemMonitor = it }) {
                    Label("System monitor", systemImage = "cpu")
                }
                Toggle(isOn = show.location, onChange = { show.location = it }) {
                    Label("Location", systemImage = "location")
                }
                Toggle(isOn = show.ingests, onChange = { show.ingests = it }) {
                    Label("Ingests", systemImage = "server.rack")
                }
                Toggle(isOn = show.moblink, onChange = { show.moblink = it }) {
                    Label("Moblink", systemImage = "app.connected.to.app.below.fill")
                }
                Toggle(isOn = show.remoteControl, onChange = { show.remoteControl = it }) {
                    Label("Remote control", systemImage = "appletvremote.gen1")
                }
                if (!isMac()) {
                    Toggle(isOn = show.moblinCloud, onChange = { show.moblinCloud = it }) {
                        Label("Moblin Cloud", systemImage = "cloud")
                    }
                }
                Toggle(isOn = show.djiDevices, onChange = { show.djiDevices = it }) {
                    Label("DJI devices", systemImage = "appletvremote.gen1")
                }
                Toggle(isOn = show.gameController, onChange = { show.gameController = it }) {
                    Label("Game controllers", systemImage = "gamecontroller")
                }
                Toggle(isOn = show.speed, onChange = { show.speed = it }) {
                    Label("Bitrate", systemImage = "speedometer")
                }
                Toggle(isOn = show.uptime, onChange = { show.uptime = it }) {
                    Label("Uptime", systemImage = "deskclock")
                }
                Toggle(isOn = show.browserWidgets, onChange = { show.browserWidgets = it }) {
                    Label("Browser widgets", systemImage = "globe")
                }
                Toggle(isOn = show.bonding, onChange = { show.bonding = it }) {
                    Label("Bonding", systemImage = "phone.connection")
                }
                Toggle(isOn = show.bondingRtts, onChange = { show.bondingRtts = it }) {
                    Label("Bonding RTTs", systemImage = "phone.connection")
                }
                Toggle(isOn = show.catPrinter, onChange = { show.catPrinter = it }) {
                    Label("Cat printers", systemImage = "pawprint")
                }
                Toggle(isOn = show.workoutDevice, onChange = { show.workoutDevice = it }) {
                    Label("Workout devices", systemImage = "figure.walk.motion")
                }
            }
        }
    }
}
