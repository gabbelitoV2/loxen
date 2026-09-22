package com.moblin.android.view.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import com.moblin.android.various.utils.isMac

@Composable
fun ContextMenuDeleteButtonView(action: () -> Unit) {
    TextButton(
        onClick = { action() },
        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
    ) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
        Text(text = "Delete")
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.contextMenuDeleteButton(disabled: Boolean = false, action: () -> Unit): Modifier = composed {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ContextMenuDeleteButtonView(
                action = {
                    expanded = false
                    action()
                }
            )
        }
    }
    combinedClickable(
        onClick = {},
        onLongClick = {
            if (!disabled && isMac()) {
                expanded = true
            }
        }
    )
}
