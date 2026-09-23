package com.moblin.android.view.settings.watch.display.localoverlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.moblinwatch.shared.WatchSettingsShow
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.sendSettingsToWatch
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun WatchLocalOverlaysSettingsView(model: Model = LocalModel.current, show: WatchSettingsShow) {
    val thermalState by show.thermalState.collectAsState()
    val audioLevel by show.audioLevel.collectAsState()
    val speed by show.speed.collectAsState()

    Form(title = "Local overlays") {
        Section {
            Toggle(
                title = "Thermal state",
                isOn = thermalState,
                onChange = { newValue ->
                    (show.thermalState as MutableStateFlow<Boolean>).value = newValue
                    model.sendSettingsToWatch()
                },
            )
            Toggle(
                title = "Audio level",
                isOn = audioLevel,
                onChange = { newValue ->
                    (show.audioLevel as MutableStateFlow<Boolean>).value = newValue
                    model.sendSettingsToWatch()
                },
            )
            Toggle(
                title = "Bitrate",
                isOn = speed,
                onChange = { newValue ->
                    (show.speed as MutableStateFlow<Boolean>).value = newValue
                    model.sendSettingsToWatch()
                },
            )
        }
    }
}
