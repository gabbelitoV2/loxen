package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun SwipeLeftToDuplicateButtonView(action: () -> Unit) {
    CompositionLocalProvider(LocalTint provides formPalette().accent) {
        Button(action = action) {
            Label(title = "Duplicate", systemImage = "plus.square.on.square")
        }
    }
}
