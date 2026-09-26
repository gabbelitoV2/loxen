package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.UIActivityViewController

@Composable
fun ShareSheetView(activityItems: List<Any?>) {
    UIActivityViewController(activityItems = activityItems, applicationActivities = null)
}
