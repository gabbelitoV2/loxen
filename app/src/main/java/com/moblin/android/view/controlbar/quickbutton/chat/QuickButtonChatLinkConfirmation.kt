package com.moblin.android.view.controlbar.quickbutton.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun QuickButtonChatLinkConfirmationModifier(url: String?, onUrlChange: (String?) -> Unit) {
    if (url == null) {
        return
    }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { onUrlChange(null) },
        title = { Text(url) },
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    onUrlChange(null)
                }
            ) {
                Text("Open link")
            }
        }
    )
}

@Composable
fun quickButtonChatLinkConfirmation(url: String?, onUrlChange: (String?) -> Unit) {
    QuickButtonChatLinkConfirmationModifier(url = url, onUrlChange = onUrlChange)
}
