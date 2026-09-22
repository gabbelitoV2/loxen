package com.moblin.android.view.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

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
        color = MaterialTheme.colorScheme.onSurface,
    )
}
