package com.moblin.android.platform

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import com.moblin.android.AppDelegate
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BookmarkedDirectory internal constructor(
    internal val treeUri: Uri,
    internal val documentId: String,
    path: String,
) : File(path) {
    override fun exists(): Boolean {
        return Bookmark.query(AppDelegate.context, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)) != null
    }

    override fun isDirectory(): Boolean = exists()

    override fun isFile(): Boolean = false
}

class BookmarkedFile internal constructor(
    private val directory: BookmarkedDirectory,
    private val fileName: String,
) : File(directory, fileName) {
    override fun exists(): Boolean = document() != null

    override fun isFile(): Boolean = exists()

    override fun isDirectory(): Boolean = false

    override fun length(): Long = document()?.size ?: 0

    internal fun documentUri(create: Boolean): Uri? {
        val document = document()
        if (document != null) {
            return DocumentsContract.buildDocumentUriUsingTree(directory.treeUri, document.documentId)
        }
        if (!create) {
            return null
        }
        return try {
            DocumentsContract.createDocument(
                AppDelegate.context.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(directory.treeUri, directory.documentId),
                Bookmark.mimeType(fileName),
                fileName,
            )
        } catch (error: Exception) {
            Log.i("Bookmark", "bookmark: Failed to create $fileName: $error")
            null
        }
    }

    private fun document(): Bookmark.Document? {
        return Bookmark.findChild(AppDelegate.context, directory.treeUri, directory.documentId, fileName)
    }
}

object Bookmark {
    private const val TAG = "Bookmark"
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PERMISSIONS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
    )

    internal class Document(val documentId: String, val name: String?, val size: Long)

    fun data(url: String): ByteArray? = data(AppDelegate.context, url)

    internal fun data(context: Context, url: String): ByteArray? {
        return try {
            val uri = Uri.parse(url)
            DocumentsContract.getTreeDocumentId(uri)
            context.contentResolver.takePersistableUriPermission(uri, PERMISSIONS)
            uri.toString().toByteArray()
        } catch (error: Exception) {
            Log.i(TAG, "Failed to create bookmark with error: $error")
            null
        }
    }

    fun resolve(data: ByteArray): File? = resolve(AppDelegate.context, data)

    internal fun resolve(context: Context, data: ByteArray): BookmarkedDirectory? {
        val treeUri = Uri.parse(String(data))
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        if (!isGranted(context, treeUri)) {
            return null
        }
        val document = query(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)) ?: return null
        return BookmarkedDirectory(treeUri, documentId, path(treeUri, documentId, document.name))
    }

    fun path(data: ByteArray): String? = resolve(data)?.path

    fun child(directory: File, name: String): File {
        return if (directory is BookmarkedDirectory) {
            BookmarkedFile(directory, name)
        } else {
            File(directory, name)
        }
    }

    fun url(file: File): String? {
        return if (file is BookmarkedFile) {
            file.documentUri(create = true)?.toString()
        } else {
            file.toString()
        }
    }

    fun openOutput(url: String, file: File): FileOutputStream? {
        val key = writtenKey(file)
        return try {
            val released = AtomicBoolean(false)
            val release = {
                if (released.compareAndSet(false, true)) {
                    writers.computeIfPresent(key) { _, count -> if (count > 1) count - 1 else null }
                }
            }
            if (url.startsWith("content:")) {
                AppDelegate.context.contentResolver.openFileDescriptor(Uri.parse(url), "wt")?.let {
                    object : ParcelFileDescriptor.AutoCloseOutputStream(it) {
                        override fun close() {
                            try {
                                super.close()
                            } finally {
                                release()
                            }
                        }
                    }
                }
            } else {
                object : FileOutputStream(file) {
                    override fun close() {
                        try {
                            super.close()
                        } finally {
                            release()
                        }
                    }
                }
            }?.also { writers.merge(key, 1, Int::plus) }
        } catch (error: Exception) {
            Log.i(TAG, "bookmark: Failed to open $url: $error")
            null
        }
    }

    fun awaitWritten(file: File, timeoutMs: Long = 3_000) {
        val key = writtenKey(file)
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (writers.containsKey(key) && System.nanoTime() < deadline) {
            Thread.sleep(20)
        }
    }

    private val writers = ConcurrentHashMap<String, Int>()

    private fun writtenKey(file: File): String = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

    internal fun mimeType(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (extension == "mp4") "video/mp4" else "application/octet-stream"
    }

    internal fun query(context: Context, uri: Uri): Document? {
        return try {
            context.contentResolver.query(uri, projection, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) document(cursor) else null
            }
        } catch (error: Exception) {
            null
        }
    }

    internal fun findChild(context: Context, treeUri: Uri, parentDocumentId: String, name: String): Document? {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        return try {
            context.contentResolver.query(uri, projection, null, null)?.use { cursor ->
                var found: Document? = null
                while (found == null && cursor.moveToNext()) {
                    found = document(cursor).takeIf { it.name == name }
                }
                found
            }
        } catch (error: Exception) {
            null
        }
    }

    private fun document(cursor: Cursor): Document {
        val id = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val name = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val size = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        return Document(
            documentId = cursor.getString(id),
            name = if (name >= 0 && !cursor.isNull(name)) cursor.getString(name) else null,
            size = if (size >= 0 && !cursor.isNull(size)) cursor.getLong(size) else 0,
        )
    }

    private fun isGranted(context: Context, treeUri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isWritePermission }
    }

    private fun path(treeUri: Uri, documentId: String, name: String?): String {
        if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return "/" + (name ?: documentId)
        }
        val volume = documentId.substringBefore(':')
        val relative = documentId.substringAfter(':', "")
        val root = when (volume) {
            "primary" -> Environment.getExternalStorageDirectory().path
            "home" -> File(Environment.getExternalStorageDirectory(), "Documents").path
            else -> "/storage/$volume"
        }
        return if (relative.isEmpty()) root else "$root/$relative"
    }
}
