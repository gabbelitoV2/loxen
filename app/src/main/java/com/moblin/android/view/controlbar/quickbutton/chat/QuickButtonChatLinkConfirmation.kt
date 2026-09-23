package com.moblin.android.view.controlbar.quickbutton.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Visibility

@Composable
fun QuickButtonChatLinkConfirmationModifier(url: String?, onUrlChange: (String?) -> Unit) {
    val context = LocalContext.current
    val presentedUrl = remember { arrayOfNulls<String>(1) }
    if (url != null) {
        presentedUrl[0] = url
    }
    val shownUrl = url ?: presentedUrl[0] ?: ""
    ConfirmationDialog(
        title = shownUrl,
        isPresented = url != null,
        onDismissRequest = { onUrlChange(null) },
        titleVisibility = Visibility.visible,
    ) {
        Button("Open link") {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shownUrl)))
        }
    }
}

@Composable
fun quickButtonChatLinkConfirmation(url: String?, onUrlChange: (String?) -> Unit) {
    QuickButtonChatLinkConfirmationModifier(url = url, onUrlChange = onUrlChange)
}
