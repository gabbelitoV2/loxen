package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.replaceSensitive
import com.moblin.android.localized
import com.moblin.android.view.settings.formPalette

@Composable
private fun TextItemRow(name: String, value: String, sensitive: Boolean, color: Color) {
    val valueColor = if (color == Color.Gray) formPalette().gray else color
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name)
        Spacer(modifier = Modifier.width(8.dp))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = replaceSensitive(value, sensitive),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TextItemView(
    name: String,
    value: String,
    sensitive: Boolean = false,
    color: Color = Color.Gray,
) {
    TextItemRow(name = name, value = value, sensitive = sensitive, color = color)
}

@Composable
fun TextItemLocalizedView(
    name: String,
    value: String,
    sensitive: Boolean = false,
    color: Color = Color.Gray,
) {
    TextItemRow(name = localized(name), value = value, sensitive = sensitive, color = color)
}
