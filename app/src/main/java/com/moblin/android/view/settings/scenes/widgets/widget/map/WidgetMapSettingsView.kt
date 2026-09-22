package com.moblin.android.view.settings.scenes.widgets.widget.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = widget.map.northUp,
                onCheckedChange = { value ->
                    widget.map.northUp = value
                    model.resetSelectedScene(changeScene = false)
                },
            )
            Text("North up", modifier = Modifier.padding(start = 8.dp))
        }
        Text("The map will rotate based of movement direction if disabled.")

        ShortcutSectionView {
            TextButton(onClick = { onNavigate("LocationSettingsView") }) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Text("Location", modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text("Size")
        Row {
            Slider(
                value = size.toFloat(),
                onValueChange = { value -> size = value.toDouble() },
                valueRange = 500f..10000f,
                steps = 94,
                onValueChangeFinished = {
                    widget.map.size = size
                    model.resetSelectedScene(changeScene = false)
                },
                modifier = Modifier.weight(1f),
            )
        }

        Text("Delay")
        Row {
            Slider(
                value = delay.toFloat(),
                onValueChange = { value -> delay = value.toDouble() },
                valueRange = 0f..10f,
                steps = 19,
                onValueChangeFinished = {
                    widget.map.delay = delay
                    model.resetSelectedScene(changeScene = false)
                },
                modifier = Modifier.weight(1f),
            )
            Text(delay.toString(), modifier = Modifier.width(35.dp))
        }
        Text("To show the widget in sync with high latency cameras.")

        WidgetEffectsView(model = model, widget = widget)
    }
}
