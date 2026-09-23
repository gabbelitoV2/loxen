package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint

@Composable
fun SwipeLeftToDuplicateButtonView(action: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    CompositionLocalProvider(LocalTint provides Color(0xFF007AFF)) {
        Label(
            title = "Duplicate",
            systemImage = "plus",
            modifier = Modifier
                .alpha(if (pressed) 0.2f else 1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action,
                ),
        )
    }
}
