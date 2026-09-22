package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

@Composable
fun CopyToClipboardButtonView(text: String) {
    TODO("no Compose counterpart for ShareLink")
}

@Composable
fun CommandCopyView(command: String) {
    Row {
        Text("`$command`")
        CopyToClipboardButtonView(text = command)
    }
}
