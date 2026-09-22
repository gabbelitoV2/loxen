package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor

@Composable
fun RgbColorPickerView(
    title: String,
    color: Color,
    onColorChanged: (Color) -> Unit,
    opacity: Boolean = false,
    onChange: (RgbColor) -> Unit,
) {
    LaunchedEffect(color) {
        val rgbColor: RgbColor? = TODO("Color.toRgb() has no Kotlin counterpart")
        if (rgbColor != null) {
            onChange(rgbColor)
        }
    }
    Unit
}
