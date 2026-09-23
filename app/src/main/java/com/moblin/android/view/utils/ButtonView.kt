package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.utils.openUrl
import com.moblin.android.view.settings.FormRow
import com.moblin.android.view.settings.LocalTint
import com.moblin.android.view.settings.Section
import com.moblin.android.view.settings.formPalette

@Composable
fun TextButtonView(title: String, action: () -> Unit) {
    val palette = formPalette()
    val tint = LocalTint.current.takeOrElse { palette.accent }
    FormRow(onClick = action, highlight = false) {
        Text(
            text = localized(title),
            color = tint,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ExternalButtonView(action: () -> Unit, content: @Composable () -> Unit) {
    val palette = formPalette()
    val tint = LocalTint.current.takeOrElse { palette.accent }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Section {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = action,
                    )
                    .alpha(if (pressed) 0.3f else 1f),
            ) {
                SystemImage(name = "arrow.turn.up.right", fontSize = 22.sp, tint = tint)
            }
        }
    }
}

@Composable
fun ExternalUrlButtonView(url: String, content: @Composable () -> Unit) {
    ExternalButtonView(
        action = { openUrl(url) },
        content = { content() },
    )
}
