package com.moblin.android.view.utils

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.moblin.android.AppDelegate
import java.io.File

@Composable
fun ShareSheetView(
    activityItems: List<Any>,
) {
    LaunchedEffect(activityItems) {
        presentShareSheet(activityItems)
    }
}

private fun presentShareSheet(activityItems: List<Any>) {
    if (activityItems.isEmpty()) {
        return
    }
    val uris = activityItems.mapNotNull { item ->
        when (item) {
            is Uri -> item
            is File -> Uri.fromFile(item)
            else -> null
        }
    }
    val intent = when {
        uris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        uris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        else -> Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, activityItems.joinToString("\n") { it.toString() })
        }
    }
    AppDelegate.context.startActivity(
        Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
