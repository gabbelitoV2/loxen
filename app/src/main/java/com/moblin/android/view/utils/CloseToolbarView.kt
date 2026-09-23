package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun CloseToolbarButtonView(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        name = "xmark",
        fontSize = 17.sp,
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null) {
                onPresentingChange(false)
            },
        tint = formPalette().accent,
    )
}

@Composable
fun CloseToolbar(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    Row {
        CloseToolbarButtonView(presenting = presenting, onPresentingChange = onPresentingChange)
    }
}
