package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun FormFieldError(error: String) {
    if (error != "") {
        Column {
            Text(
                text = error,
                color = Color(0xFFFF3B30),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(text = "")
        }
    }
}
