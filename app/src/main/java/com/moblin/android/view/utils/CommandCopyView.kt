package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.ShareLink

@Composable
fun CopyToClipboardButtonView(text: String) {
    ShareLink(item = text) {
        SystemImage(name = "square.and.arrow.up", fontSize = 20.sp, tint = LocalTint.current)
    }
}

@Composable
fun CommandCopyView(command: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("`$command`", modifier = Modifier.weight(1f, fill = false))
        CopyToClipboardButtonView(text = command)
    }
}
