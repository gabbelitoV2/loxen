package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HCenter(content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        content()
        Spacer(Modifier.weight(1f))
    }
}

fun Modifier.hCenter(center: Boolean): Modifier = if (center) {
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
} else {
    this
}

@Composable
private fun HCenterModifier(center: Boolean, content: @Composable () -> Unit) {
    if (center) {
        HCenter {
            content()
        }
    } else {
        content()
    }
}
