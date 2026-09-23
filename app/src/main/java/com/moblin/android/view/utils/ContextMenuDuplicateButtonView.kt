package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Label

@Composable
fun ContextMenuDuplicateButtonView(action: () -> Unit) {
    Button(action = action) {
        Label(title = "Duplicate", systemImage = "plus.square.on.square")
    }
}
