package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun SwipeLeftToDeleteButtonView(action: () -> Unit) {
    CompositionLocalProvider(LocalTint provides formPalette().red) {
        Button(action = action) {
            Label(title = "Delete", systemImage = "trash")
        }
    }
}
