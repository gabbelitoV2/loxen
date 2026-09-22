package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized

@Composable
fun ContextMenuDuplicateButtonView(action: () -> Unit) {
    TextButton(onClick = { action() }) {
        Row {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = localized("Duplicate"))
        }
    }
}
