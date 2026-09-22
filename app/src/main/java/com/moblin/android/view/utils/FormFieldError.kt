package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FormFieldError(error: String) {
    if (error != "") {
        Column {
            Text(
                text = error,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = "")
        }
    }
}
