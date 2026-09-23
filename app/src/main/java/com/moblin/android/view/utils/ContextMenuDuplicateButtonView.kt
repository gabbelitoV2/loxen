package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Label

@Composable
fun ContextMenuDuplicateButtonView(action: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Label(
        title = localized("Duplicate"),
        systemImage = "plus.square.on.square",
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null) { action() },
    )
}
