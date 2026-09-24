package com.moblin.android.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import com.moblin.android.AppDelegate
import com.moblin.android.platform.swiftui.LocalSheetDismiss
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DocumentPicker {
    private const val TAG = "DocumentPicker"
    private const val FALLBACK_NAME = "Document"
    private const val MAX_NAME_BYTES = 255
    private const val MAX_EXTENSION_LENGTH = 16
    private const val ANY = "*/*"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mimeTypes = mapOf(
        "item" to ANY,
        "content" to ANY,
        "data" to ANY,
        "public.item" to ANY,
        "public.content" to ANY,
        "public.data" to ANY,
        "image" to "image/*",
        "public.image" to "image/*",
        "gif" to "image/gif",
        "com.compuserve.gif" to "image/gif",
        "png" to "image/png",
        "public.png" to "image/png",
        "jpeg" to "image/jpeg",
        "public.jpeg" to "image/jpeg",
        "audio" to "audio/*",
        "public.audio" to "audio/*",
        "mp3" to "audio/mpeg",
        "public.mp3" to "audio/mpeg",
        "movie" to "video/*",
        "video" to "video/*",
        "public.movie" to "video/*",
        "public.video" to "video/*",
        "mpeg4movie" to "video/mp4",
        "public.mpeg-4" to "video/mp4",
        "quicktimemovie" to "video/quicktime",
        "com.apple.quicktime-movie" to "video/quicktime",
        "zip" to "application/zip",
        "public.zip-archive" to "application/zip",
        "json" to "application/json",
        "public.json" to "application/json",
        "text" to "text/*",
        "public.text" to "text/*",
        "plaintext" to "text/plain",
        "public.plain-text" to "text/plain",
    )

    private val cleanedInboxes = mutableSetOf<File>()

    @Synchronized
    fun inboxDirectory(context: Context): File {
        val directory = File(context.cacheDir, "Inbox")
        if (cleanedInboxes.add(directory) && !directory.deleteRecursively()) {
            Log.i(TAG, "document-picker: Failed to remove old picked documents")
        }
        return directory
    }

    private fun temporaryDirectory(context: Context): File = context.cacheDir

    fun contentTypes(vararg types: String): Array<String> {
        return types.map { mimeType(it) }.distinct().toTypedArray()
    }

    private fun mimeType(type: String): String {
        if (type.contains('/')) {
            return type
        }
        return mimeTypes[type.lowercase()] ?: ANY
    }

    fun displayName(context: Context, uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null
            }
        }.getOrNull()
        return safeFileName(name ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':'))
    }

    fun inboxFile(context: Context, name: String): File {
        val directory = inboxDirectory(context)
        directory.mkdirs()
        return File(directory, safeFileName(name))
    }

    fun copy(context: Context, uri: Uri, directory: File = inboxDirectory(context)): File {
        directory.mkdirs()
        val file = File(directory, displayName(context, uri))
        val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
        try {
            input.use {
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (error: Exception) {
            if (file.isFile) {
                file.delete()
            }
            throw error
        }
        return file
    }

    fun copy(uri: Uri?, onUrl: (String) -> Unit) {
        if (uri == null) {
            return
        }
        val context = AppDelegate.context
        scope.launch {
            val file = try {
                copy(context, uri)
            } catch (error: Exception) {
                Log.i(TAG, "document-picker: Failed to copy picked document: $error")
                return@launch
            }
            withContext(Dispatchers.Main) {
                onUrl(file.invariantSeparatorsPath)
            }
        }
    }

    suspend fun loadData(uri: Uri): ByteArray? {
        val context = AppDelegate.context
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (error: Exception) {
                Log.i(TAG, "photos-picker: Failed to load data: $error")
                null
            }
        }
    }

    fun readInput(context: Context, uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { ByteArrayInputStream(it.readBytes()) }
        } catch (error: Exception) {
            Log.i(TAG, "photos-picker: Failed to load data: $error")
            null
        }
    }

    fun loadMovie(uri: Uri, completion: (String?) -> Unit) {
        val context = AppDelegate.context
        scope.launch {
            val url = try {
                copy(context, uri, temporaryDirectory(context)).invariantSeparatorsPath
            } catch (error: Exception) {
                Log.i(TAG, "photos-picker: Failed to load movie: $error")
                null
            }
            withContext(Dispatchers.Main) {
                completion(url)
            }
        }
    }

    @Composable
    fun <I, O> rememberLauncher(
        contract: ActivityResultContract<I, O>,
        onResult: (O) -> Unit,
    ): ManagedActivityResultLauncher<I, O> {
        val dismissSheet = LocalSheetDismiss.current
        return rememberLauncherForActivityResult(contract) { result ->
            onResult(result)
            dismissSheet?.invoke()
        }
    }

    fun safeFileName(name: String?): String {
        val cleaned = name.orEmpty()
            .map { if (it == '/' || it == '\\' || it.code < 0x20 || it.code == 0x7f) '_' else it }
            .joinToString("")
            .trim()
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") {
            return FALLBACK_NAME
        }
        return truncated(cleaned)
    }

    private fun truncated(name: String): String {
        if (name.toByteArray().size <= MAX_NAME_BYTES) {
            return name
        }
        val extension = name.substringAfterLast('.', "")
        val suffix = if (extension.isNotEmpty() && extension.length <= MAX_EXTENSION_LENGTH) ".$extension" else ""
        var base = name.dropLast(suffix.length)
        while (base.isNotEmpty() && (base + suffix).toByteArray().size > MAX_NAME_BYTES) {
            base = base.dropLast(1)
            if (base.lastOrNull()?.isHighSurrogate() == true) {
                base = base.dropLast(1)
            }
        }
        return safeFileName(base + suffix)
    }
}
