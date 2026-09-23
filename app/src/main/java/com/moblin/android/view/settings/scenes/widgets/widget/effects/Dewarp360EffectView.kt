package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectDewarp360
import com.moblin.android.various.settings.SettingsWidget
import kotlin.math.roundToInt
import com.moblin.android.various.model.getWidgetDewarp360Effect

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

    Section(header = "Pan, tilt and zoom") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SystemImage(name = "arrow.left", fontSize = 17.sp)
            FormSlider(
                value = dewarp360.pan,
                onValueChange = { newValue ->
                    val value = newValue.roundToInt().toFloat()
                    if (value != dewarp360.pan) {
                        dewarp360.pan = value
                        updateWidget()
                    }
                },
                modifier = Modifier.weight(1f),
                valueRange = -180f..180f,
            )
            SystemImage(name = "arrow.right", fontSize = 17.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SystemImage(name = "arrow.down", fontSize = 17.sp)
            FormSlider(
                value = dewarp360.tilt,
                onValueChange = { newValue ->
                    val value = newValue.roundToInt().toFloat()
                    if (value != dewarp360.tilt) {
                        dewarp360.tilt = value
                        updateWidget()
                    }
                },
                modifier = Modifier.weight(1f),
                valueRange = -90f..90f,
            )
            SystemImage(name = "arrow.up", fontSize = 17.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SystemImage(name = "minus.magnifyingglass", fontSize = 17.sp)
            FormSlider(
                value = dewarp360.inverseFieldOfView,
                onValueChange = { newValue ->
                    val value = newValue.roundToInt().toFloat()
                    if (value != dewarp360.inverseFieldOfView) {
                        dewarp360.inverseFieldOfView = value
                        dewarp360.updateZoomFromInverseFieldOfView()
                        updateWidget()
                    }
                },
                modifier = Modifier.weight(1f),
                valueRange = 30f..170f,
            )
            SystemImage(name = "plus.magnifyingglass", fontSize = 17.sp)
        }
    }
}
