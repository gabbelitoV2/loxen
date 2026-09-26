package com.moblin.android.platform.swiftui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.core.content.FileProvider
import com.moblin.android.localized
import com.moblin.android.platform.Bookmark
import com.moblin.android.platform.BookmarkedFile
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareSheet {
    private const val TAG = "ShareSheet"
    private const val anyType = "*/*"
    private const val binaryType = "application/octet-stream"
    private const val textType = "text/plain"

    private class Stream(val uri: Uri, val type: String)

    fun authority(context: Context): String = context.packageName + ".fileprovider"

    fun intent(context: Context, activityItems: List<Any?>): Intent? {
        val streams = mutableListOf<Stream>()
        val texts = mutableListOf<String>()
        for (item in activityItems) {
            when (val resolved = resolve(context, item)) {
                is Stream -> streams.add(resolved)
                is String -> texts.add(resolved)
            }
        }
        if (streams.isEmpty() && texts.isEmpty()) {
            return null
        }
        val intent = if (streams.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(streams.map { it.uri }))
        } else {
            Intent(Intent.ACTION_SEND)
        }
        streams.singleOrNull()?.let { intent.putExtra(Intent.EXTRA_STREAM, it.uri) }
        intent.type = when {
            streams.isEmpty() -> textType
            streams.map { it.type }.distinct().size == 1 -> streams.first().type
            else -> anyType
        }
        if (texts.isNotEmpty()) {
            intent.putExtra(Intent.EXTRA_TEXT, texts.joinToString("\n"))
        }
        if (streams.isNotEmpty()) {
            val clip = ClipData.newRawUri(null, streams.first().uri)
            for (stream in streams.drop(1)) {
                clip.addItem(ClipData.Item(stream.uri))
            }
            intent.clipData = clip
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent
    }

    fun chooser(context: Context, activityItems: List<Any?>): Intent? {
        return intent(context, activityItems)?.let { Intent.createChooser(it, null) }
    }

    fun present(context: Context, activityItems: List<Any?>): Boolean {
        val chooser = chooser(context, activityItems) ?: return false
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (error: ActivityNotFoundException) {
            Log.i(TAG, "No activity to share with: $error")
            false
        }
    }

    private fun resolve(context: Context, item: Any?): Any? {
        return when (item) {
            null -> null
            is File -> file(context, item)
            is Uri -> uri(context, item)
            is URI -> if (item.scheme == "file") file(context, File(item)) else item.toString()
            is String -> string(context, item)
            else -> item.toString()
        }
    }

    private fun string(context: Context, item: String): Any? {
        if (item.startsWith("content://") || item.startsWith("file:")) {
            return uri(context, Uri.parse(item))
        }
        if (!item.contains('\n')) {
            val file = File(item)
            if (file.isAbsolute && file.isFile) {
                return file(context, file)
            }
        }
        return item
    }

    private fun uri(context: Context, uri: Uri): Any? {
        return when (uri.scheme) {
            "content" -> Stream(uri, context.contentResolver.getType(uri) ?: type(uri.lastPathSegment.orEmpty()))
            "file" -> uri.path?.let { file(context, File(it)) }
            else -> uri.toString()
        }
    }

    private fun file(context: Context, file: File): Any? {
        if (file is BookmarkedFile) {
            return Bookmark.url(file)?.let { Stream(Uri.parse(it), type(file.name)) }
        }
        if (!file.isFile) {
            Log.i(TAG, "Not sharing missing file $file")
            return null
        }
        val uri = try {
            FileProvider.getUriForFile(context, authority(context), file)
        } catch (error: IllegalArgumentException) {
            val copy = File(File(context.cacheDir, "Share"), file.name)
            try {
                copy.parentFile?.mkdirs()
                file.copyTo(copy, overwrite = true)
                FileProvider.getUriForFile(context, authority(context), copy)
            } catch (error: Exception) {
                Log.i(TAG, "Failed to share $file: $error")
                return null
            }
        }
        return Stream(uri, type(file.name))
    }

    private fun type(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) {
            return binaryType
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: binaryType
    }
}

@Composable
fun ShareLink(item: Any, label: @Composable () -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val color = LocalTint.current.takeOrElse { formPalette().accent }
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button) {
                ShareSheet.present(context, listOf(item))
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides color, LocalTint provides color) {
            label()
        }
    }
}

@Composable
fun ShareLink(item: Any) {
    ShareLink(item = item) {
        Label(title = "Share", systemImage = "square.and.arrow.up")
    }
}

@Composable
fun ShareLink(title: String, item: Any) {
    ShareLink(item = item) {
        Text(text = localized(title))
    }
}

@Composable
fun UIActivityViewController(activityItems: List<Any?>, applicationActivities: List<Any>? = null) {
    val context = LocalContext.current
    val dismiss by rememberUpdatedState(LocalSheetDismiss.current)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        dismiss?.invoke()
    }
    LaunchedEffect(Unit) {
        val chooser = withContext(Dispatchers.IO) { ShareSheet.chooser(context, activityItems) }
        if (chooser == null) {
            dismiss?.invoke()
            return@LaunchedEffect
        }
        try {
            launcher.launch(chooser)
        } catch (error: ActivityNotFoundException) {
            dismiss?.invoke()
        }
    }
}
