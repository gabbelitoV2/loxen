package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.formBodyStyle
import kotlin.math.round

@Composable
fun SliderView(
    value: Float,
    minimum: Float,
    maximum: Float,
    step: Float,
    onChange: (Float) -> Unit,
    width: Float,
    format: (Float) -> String,
    onSubmit: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormSlider(
            value = sliderValue,
            onValueChange = {
                val newValue = if (step > 0f) {
                    (minimum + step * round((it - minimum) / step)).coerceIn(minimum, maximum)
                } else {
                    it
                }
                sliderValue = newValue
                onChange(newValue)
            },
            modifier = Modifier.weight(1f),
            valueRange = minimum..maximum,
            onValueChangeFinished = {
                onSubmit(sliderValue)
            }
        )
        Box(
            modifier = Modifier.width(width.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = format(sliderValue),
                style = formBodyStyle
            )
        }
    }
}
