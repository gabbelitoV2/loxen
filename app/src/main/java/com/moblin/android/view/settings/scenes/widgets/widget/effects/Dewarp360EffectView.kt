package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectDewarp360
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.LocalModel

@Composable
fun Dewarp360EffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    dewarp360: SettingsVideoEffectDewarp360,
) {
    fun updateWidget() {
        model.getWidgetDewarp360Effect(widget, effect)?.setSettings(dewarp360.toSettings())
    }

    val pan by dewarp360.pan.collectAsState()
    val tilt by dewarp360.tilt.collectAsState()
    val inverseFieldOfView by dewarp360.inverseFieldOfView.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Pan, tilt and zoom",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Slider(
                value = pan.toFloat(),
                onValueChange = { dewarp360.pan.value = it.toDouble() },
                valueRange = -180f..180f,
                steps = 359,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
        LaunchedEffect(pan) {
            updateWidget()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null)
            Slider(
                value = tilt.toFloat(),
                onValueChange = { dewarp360.tilt.value = it.toDouble() },
                valueRange = -90f..90f,
                steps = 179,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ArrowUpward, contentDescription = null)
        }
        LaunchedEffect(tilt) {
            updateWidget()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ZoomOut, contentDescription = null)
            Slider(
                value = inverseFieldOfView.toFloat(),
                onValueChange = { dewarp360.inverseFieldOfView.value = it.toDouble() },
                valueRange = 30f..170f,
                steps = 139,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ZoomIn, contentDescription = null)
        }
        LaunchedEffect(inverseFieldOfView) {
            dewarp360.updateZoomFromInverseFieldOfView()
            updateWidget()
        }
    }
}
