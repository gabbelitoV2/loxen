package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.moblin.android.localized

@Composable
fun BorderlessButtonView(text: String, action: () -> Unit) {
    TextButton(onClick = action) {
        Text(text = localized(text))
    }
}
