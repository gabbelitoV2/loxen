package com.moblin.android.platform.swiftui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized

@Composable
fun ColorPicker(
    title: String,
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    supportsOpacity: Boolean = true,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val palette = formPalette()
    FormRow(onClick = { showPicker = true }, enabled = enabled) {
        Text(
            localized(title),
            modifier = Modifier.weight(1f),
            color = palette.label,
            style = formBodyStyle,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(selection)
                .border(1.dp, palette.separator, CircleShape),
        )
    }
    Sheet(isPresented = showPicker, onDismissRequest = { showPicker = false }) {
        ColorPickerSheetContent(
            selection = selection,
            onSelectionChange = onSelectionChange,
            supportsOpacity = supportsOpacity,
        )
    }
}

@Composable
private fun ColorPickerSheetContent(
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    supportsOpacity: Boolean,
) {
    val palette = formPalette()
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
                .background(selection)
                .border(1.dp, palette.separator, RoundedCornerShape(12.dp)),
        )
        ColorPickerComponent("Red", selection.red) { onSelectionChange(selection.copy(red = it)) }
        ColorPickerComponent("Green", selection.green) { onSelectionChange(selection.copy(green = it)) }
        ColorPickerComponent("Blue", selection.blue) { onSelectionChange(selection.copy(blue = it)) }
        if (supportsOpacity) {
            ColorPickerComponent("Opacity", selection.alpha) { onSelectionChange(selection.copy(alpha = it)) }
        }
    }
}

@Composable
private fun ColorPickerComponent(title: String, value: Float, onChange: (Float) -> Unit) {
    Text(localized(title), color = formPalette().label, style = formBodyStyle)
    FormSlider(
        value = value * 255f,
        onValueChange = { onChange((it / 255f).coerceIn(0f, 1f)) },
        valueRange = 0f..255f,
    )
}
