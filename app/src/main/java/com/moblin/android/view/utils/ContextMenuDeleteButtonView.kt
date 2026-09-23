package com.moblin.android.view.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.isMac

@Composable
fun ContextMenuDeleteButtonView(action: () -> Unit) {
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    CompositionLocalProvider(LocalTint provides palette.red) {
        Label(
            title = "Delete",
            systemImage = "trash",
            modifier = Modifier
                .alpha(if (pressed) 0.2f else 1f)
                .clickable(interactionSource = interactionSource, indication = null) {
                    action()
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.contextMenuDeleteButton(disabled: Boolean = false, action: () -> Unit): Modifier = composed {
    if (disabled || !isMac()) {
        this
    } else {
        var expanded by remember { mutableStateOf(false) }
        ConfirmationDialog(title = "", isPresented = expanded, onDismissRequest = { expanded = false }) {
            Button("Delete", role = ButtonRole.destructive) {
                action()
            }
        }
        combinedClickable(
            onClick = {},
            onLongClick = { expanded = true },
        )
    }
}
