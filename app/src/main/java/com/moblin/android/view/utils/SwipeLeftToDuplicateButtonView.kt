package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SwipeLeftToDuplicateButtonView(action: () -> Unit) {
    Button(
        onClick = action,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Blue,
            contentColor = Color.White
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null
            )
            Text(text = "Duplicate")
        }
    }
}
