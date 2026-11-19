package com.moblin.android.view.settings.display.localoverlays

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsShow
import com.moblin.android.various.settings.SettingsZoomAndSceneButtonsLayout

@Composable
fun LocalOverlaysZoomAndSceneControlsSettingsView(database: Database, show: SettingsShow) {
    Form(title = "Zoom and scene controls") {
        Section(
            header = "General",
            footer = "The layout is only used in landscape.",
        ) {
            Picker(
                title = "Layout",
                selection = database.zoomAndSceneButtonsLayout,
                options = SettingsZoomAndSceneButtonsLayout.entries,
                text = { it.toString() },
            ) {
                database.zoomAndSceneButtonsLayout = it
            }
            Toggle(
                title = "Big buttons",
                isOn = binding({ database.bigButtons }, { database.bigButtons = it }),
            )
        }
        if (database.showAllSettings) {
            Section(header = "Zoom") {
                Toggle(
                    title = "Buttons",
                    isOn = binding({ show.zoomPresets }, { show.zoomPresets = it }),
                )
                Toggle(
                    title = "Slider",
                    isOn = binding({ show.zoomSlider }, { show.zoomSlider = it }),
                )
            }
        }
    }
}
