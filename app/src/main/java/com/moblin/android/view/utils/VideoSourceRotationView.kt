package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.Picker

@Composable
fun VideoSourceRotationView(
    selectedRotation: Double,
    onSelectedRotationChange: (Double) -> Unit,
) {
    val rotations = listOf(0.0, 90.0, 180.0, 270.0)
    Picker(
        title = "Rotation",
        selection = selectedRotation,
        options = rotations,
        text = { "${it.toInt()}°" },
        onChange = { onSelectedRotationChange(it) },
    )
}
