package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun TextValueView(
    name: String,
    value: String,
    sensitive: Boolean = false,
) {
    TextItemLocalizedView(
        name = name,
        value = value,
        sensitive = sensitive,
        color = formPalette().label,
    )
}
