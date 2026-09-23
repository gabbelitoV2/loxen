package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun BorderlessButtonView(text: String, action: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint = LocalTint.current
    val color = if (tint == Color.Unspecified) formPalette().accent else tint
    Text(
        text = localized(text),
        style = formBodyStyle,
        color = color,
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null) { action() },
    )
}
