package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.various.utils.isMac

@Composable
fun SwipeLeftToDuplicateOrDeleteHelpView(kind: String) {
    if (isMac()) {
        Text("Swipe left or right-click on $kind to duplicate or delete it.")
    } else {
        Text("Swipe left on $kind to duplicate or delete it.")
    }
}
