package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.isMac

@Composable
fun SwipeLeftToDuplicateOrDeleteHelpView(kind: String) {
    val palette = formPalette()
    val text = if (isMac()) {
        "Swipe left or right-click on $kind to duplicate or delete it."
    } else {
        "Swipe left on $kind to duplicate or delete it."
    }
    Text(text, color = palette.label)
}
