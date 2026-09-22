package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.various.utils.isMac

@Composable
fun SwipeLeftToDeleteHelpView(kind: String) {
    Column {
        if (isMac()) {
            Text("Swipe left or right-click on $kind to delete it.")
        } else {
            Text("Swipe left on $kind to delete it.")
        }
    }
}
