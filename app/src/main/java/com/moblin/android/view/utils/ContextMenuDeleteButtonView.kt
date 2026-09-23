package com.moblin.android.view.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.ContextMenu
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.isMac

@Composable
fun ContextMenuDeleteButtonView(action: () -> Unit) {
    CompositionLocalProvider(LocalTint provides formPalette().red) {
        Button(action = action) {
            Label(title = "Delete", systemImage = "trash")
        }
    }
}

@Composable
fun ContextMenuDeleteButton(
    disabled: Boolean = false,
    action: () -> Unit,
    content: @Composable () -> Unit,
) {
    ContextMenu(
        menu = {
            if (!disabled && isMac()) {
                ContextMenuDeleteButtonView(action = action)
            }
        },
        content = content,
    )
}
