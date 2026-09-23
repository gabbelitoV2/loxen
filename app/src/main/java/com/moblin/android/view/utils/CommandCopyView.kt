package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun CopyToClipboardButtonView(text: String) {
    val clipboard = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        name = "square.and.arrow.up",
        fontSize = 20.sp,
        tint = LocalTint.current.takeOrElse { formPalette().accent },
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null) {
                clipboard.setText(AnnotatedString(text))
            },
    )
}

@Composable
fun CommandCopyView(command: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("`$command`")
        CopyToClipboardButtonView(text = command)
    }
}
