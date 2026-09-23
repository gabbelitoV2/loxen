package com.moblin.android.view.settings.scenes.widgets.widget.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.various.model.resetSelectedScene

@Composable
fun WidgetMapSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    initialDelay: Double,
    initialSize: Double,
) {
    var delay by remember { mutableStateOf(initialDelay) }
    var size by remember { mutableStateOf(initialSize) }

    Column {
        Section(footer = "The map will rotate based of movement direction if disabled.") {
            Toggle(
                title = "North up",
                isOn = binding(
                    get = { widget.map.northUp },
                    set = { value ->
                        widget.map.northUp = value
                        model.resetSelectedScene(changeScene = false)
                    },
                ),
            )
        }
        ShortcutSectionView {
            NavigationLink(destination = { onNavigate("LocationSettingsView") }) {
                Label("Location", systemImage = "location")
            }
        }
        Section(header = "Size") {
            FormSlider(
                value = size.toFloat(),
                onValueChange = { value -> size = value.toDouble() },
                valueRange = 500f..10000f,
                onValueChangeFinished = {
                    widget.map.size = size
                    model.resetSelectedScene(changeScene = false)
                },
            )
        }
        Section(
            header = "Delay",
            footer = "To show the widget in sync with high latency cameras.",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormSlider(
                    value = delay.toFloat(),
                    onValueChange = { value -> delay = value.toDouble() },
                    valueRange = 0f..10f,
                    onValueChangeFinished = {
                        widget.map.delay = delay
                        model.resetSelectedScene(changeScene = false)
                    },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.width(35.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(delay.toString())
                }
            }
        }
        WidgetEffectsView(model = model, widget = widget)
    }
}
