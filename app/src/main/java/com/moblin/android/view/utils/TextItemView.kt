package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.moblin.android.common.various.localized
import com.moblin.android.common.various.replaceSensitive

@Composable
fun TextItemView(
    name: String,
    value: String,
    sensitive: Boolean = false,
    color: Color = Color.Gray,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(name)
        Spacer(Modifier.weight(1f))
        Text(
            text = replaceSensitive(value, sensitive),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TextItemLocalizedView(
    name: String,
    value: String,
    sensitive: Boolean = false,
    color: Color = Color.Gray,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(localized(name))
        Spacer(Modifier.weight(1f))
        Text(
            text = replaceSensitive(value, sensitive),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
