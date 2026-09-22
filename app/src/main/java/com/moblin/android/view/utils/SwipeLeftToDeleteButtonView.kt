package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SwipeLeftToDeleteButtonView(action: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = action,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
        modifier = modifier
    ) {
        Row {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Delete")
        }
    }
}
