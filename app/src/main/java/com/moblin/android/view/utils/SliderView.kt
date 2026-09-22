package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.floor

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
    Row {
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onChange(it)
            },
            valueRange = minimum..maximum,
            steps = if (step > 0f) floor(((maximum - minimum) / step).toDouble()).toInt() - 1 else 0,
            onValueChangeFinished = {
                onSubmit(sliderValue)
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = format(sliderValue),
            modifier = Modifier.width(width.dp)
        )
    }
}
