package com.moblin.android.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object DocumentPicker {
    private const val FALLBACK_NAME = "Document"
    private const val MAX_NAME_BYTES = 255
    private const val MAX_EXTENSION_LENGTH = 16

    fun inboxDirectory(context: Context): File = File(context.cacheDir, "Inbox")

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
