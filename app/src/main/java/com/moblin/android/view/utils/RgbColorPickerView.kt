package com.moblin.android.view.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import kotlin.math.roundToInt

@Composable
fun RgbColorPickerView(
    title: String,
    color: Color,
    onColorChanged: (Color) -> Unit,
    opacity: Boolean = false,
    onChange: (RgbColor) -> Unit,
) {
    var previous by remember { mutableStateOf(color) }
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(color) {
        if (previous != color) {
            previous = color
            onChange(
                RgbColor(
                    red = (color.red * 255f).roundToInt().coerceIn(0, 255),
                    green = (color.green * 255f).roundToInt().coerceIn(0, 255),
                    blue = (color.blue * 255f).roundToInt().coerceIn(0, 255),
                )
            )
        }
    }
    FormRow(onClick = { showPicker = true }) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = formPalette().label,
            style = formBodyStyle,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, formPalette().separator, CircleShape),
        )
    }
    if (showPicker) {
        Sheet(onDismissRequest = { showPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(1.dp, formPalette().separator, RoundedCornerShape(12.dp)),
                )
                Text("Red", color = formPalette().label, style = formBodyStyle)
                FormSlider(
                    value = color.red * 255f,
                    onValueChange = { value ->
                        onColorChanged(Color(value / 255f, color.green, color.blue, color.alpha))
                    },
                    valueRange = 0f..255f,
                )
                Text("Green", color = formPalette().label, style = formBodyStyle)
                FormSlider(
                    value = color.green * 255f,
                    onValueChange = { value ->
                        onColorChanged(Color(color.red, value / 255f, color.blue, color.alpha))
                    },
                    valueRange = 0f..255f,
                )
                Text("Blue", color = formPalette().label, style = formBodyStyle)
                FormSlider(
                    value = color.blue * 255f,
                    onValueChange = { value ->
                        onColorChanged(Color(color.red, color.green, value / 255f, color.alpha))
                    },
                    valueRange = 0f..255f,
                )
                if (opacity) {
                    Text("Opacity", color = formPalette().label, style = formBodyStyle)
                    FormSlider(
                        value = color.alpha * 255f,
                        onValueChange = { value ->
                            onColorChanged(Color(color.red, color.green, color.blue, value / 255f))
                        },
                        valueRange = 0f..255f,
                    )
                }
            }
        }
    }
}
