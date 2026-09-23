package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.isMac

@Composable
fun SwipeLeftToDeleteHelpView(kind: String) {
    val text = if (isMac()) {
        "Swipe left or right-click on $kind to delete it."
    } else {
        "Swipe left on $kind to delete it."
    }
    Text(text = text, style = formBodyStyle, color = formPalette().label)
}
