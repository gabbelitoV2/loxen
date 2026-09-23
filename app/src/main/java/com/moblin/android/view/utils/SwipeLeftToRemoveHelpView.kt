package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.platform.swiftui.formBodyStyle

@Composable
fun SwipeLeftToRemoveHelpView(kind: String, modifier: Modifier = Modifier) {
    Text(
        text = "Swipe left on $kind to remove it.",
        style = formBodyStyle,
        modifier = modifier,
    )
}
