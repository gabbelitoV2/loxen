package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
fun CloseToolbarButtonView(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    IconButton(onClick = { onPresentingChange(false) }) {
        Icon(Icons.Default.Close, contentDescription = null)
    }
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
