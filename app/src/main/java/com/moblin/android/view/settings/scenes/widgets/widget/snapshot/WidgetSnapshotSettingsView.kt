package com.moblin.android.view.settings.scenes.widgets.widget.snapshot

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetSnapshot
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.various.model.getSnapshotEffect

@Composable
fun WidgetSnapshotSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    snapshot: SettingsWidgetSnapshot,
) {
    Section {
        Picker(
            title = "Showtime",
            selection = snapshot.showtime,
            options = listOf(3, 5, 10, 15, 30, 60, 120),
            text = { "${it}s" },
        ) { showtime ->
            snapshot.showtime = showtime
            setEffectSettings(model, widget, showtime)
        }
    }
    WidgetEffectsView(model = model, widget = widget)
}

private fun setEffectSettings(model: Model, widget: SettingsWidget, showtime: Int) {
    model.getSnapshotEffect(widget.id)?.setSettings(showtime)
}
