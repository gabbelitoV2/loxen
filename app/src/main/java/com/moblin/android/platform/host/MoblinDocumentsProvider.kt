package com.moblin.android.platform.host

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.moblin.android.R
import com.moblin.android.platform.Documents
import com.moblin.android.platform.loxen.Loxen
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MoblinDocumentsProvider : DocumentsProvider() {
    private val rootProjection = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_TITLE,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_AVAILABLE_BYTES,
    )
    private val documentProjection = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE,
        Document.COLUMN_LAST_MODIFIED,
    )

    override fun onCreate(): Boolean = true

    private fun requireContextCompat(): Context = context ?: throw IllegalStateException("No context")

    private fun documents(): File = Documents.directory(requireContextCompat())

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: rootProjection)
        val documents = documents()
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, rootId)
            add(Root.COLUMN_DOCUMENT_ID, rootDocumentId)
            add(Root.COLUMN_TITLE, Loxen.appName)
            add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_IS_CHILD)
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_AVAILABLE_BYTES, documents.usableSpace)
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: documentProjection)
        addRow(result, documentId, file(documentId))
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val result = MatrixCursor(projection ?: documentProjection)
        val parent = file(parentDocumentId)
        for (child in parent.listFiles().orEmpty().sortedBy { it.name.lowercase() }) {
            if (child.name.startsWith(".")) {
                continue
            }
            addRow(result, childDocumentId(parentDocumentId, child.name), child)
        }
        result.setNotificationUri(
            requireContextCompat().contentResolver,
            DocumentsContract.buildChildDocumentsUri(authority(requireContextCompat()), parentDocumentId),
        )
        return result
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = file(documentId)
        if (file.isDirectory) {
            throw FileNotFoundException("$documentId is a directory")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val parent = file(parentDocumentId)
        val name = uniqueName(parent, safeName(displayName))
        val file = File(parent, name)
        val created = if (mimeType == Document.MIME_TYPE_DIR) file.mkdir() else file.createNewFile()
        if (!created) {
            throw IOException("Failed to create $name")
        }
        notifyChildrenChanged(parentDocumentId)
        return childDocumentId(parentDocumentId, name)
    }

    override fun deleteDocument(documentId: String) {
        if (documentId == rootDocumentId) {
            throw UnsupportedOperationException("The ${Loxen.appName} folder cannot be deleted")
        }
        if (!file(documentId).deleteRecursively()) {
            throw IOException("Failed to delete $documentId")
        }
        notifyChildrenChanged(parentDocumentId(documentId))
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        if (documentId == rootDocumentId) {
            throw UnsupportedOperationException("The ${Loxen.appName} folder cannot be renamed")
        }
        val file = file(documentId)
        val parentId = parentDocumentId(documentId)
        val name = safeName(displayName)
        if (name == file.name) {
            return documentId
        }
        val target = File(file.parentFile, uniqueName(file.parentFile!!, name))
        if (!file.renameTo(target)) {
            throw IOException("Failed to rename $documentId")
        }
        notifyChildrenChanged(parentId)
        return childDocumentId(parentId, target.name)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith("$parentDocumentId/")
    }

    override fun getDocumentType(documentId: String): String = mimeType(file(documentId))

    private fun addRow(result: MatrixCursor, documentId: String, file: File) {
        var flags = 0
        if (file.isDirectory) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        if (documentId != rootDocumentId) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME
        }
        result.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, documentId)
            add(Document.COLUMN_DISPLAY_NAME, if (documentId == rootDocumentId) Loxen.appName else file.name)
            add(Document.COLUMN_MIME_TYPE, mimeType(file))
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, if (file.isDirectory) null else file.length())
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        }
    }

    private fun file(documentId: String): File {
        val documents = documents()
        val file = fileIn(documents, documentId) ?: throw FileNotFoundException(documentId)
        if (!file.exists()) {
            throw FileNotFoundException(documentId)
        }
        return file
    }

    private fun notifyChildrenChanged(parentDocumentId: String) {
        val context = requireContextCompat()
        context.contentResolver.notifyChange(
            DocumentsContract.buildChildDocumentsUri(authority(context), parentDocumentId),
            null,
        )
    }

    private fun uniqueName(parent: File, name: String): String {
        if (!File(parent, name).exists()) {
            return name
        }
        val base = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "")
        var index = 1
        while (true) {
            val candidate = if (extension.isEmpty() || base == name) "$base ($index)" else "$base ($index).$extension"
            if (!File(parent, candidate).exists()) {
                return candidate
            }
            index += 1
        }
    }

    private fun safeName(name: String): String {
        val cleaned = name.replace('/', '_').replace('\u0000', '_').trim()
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") {
            throw IllegalArgumentException("Invalid name $name")
        }
        return cleaned
    }

    companion object {
        const val rootId = "Moblin"
        const val rootDocumentId = "Documents"

        fun authority(context: Context): String = context.packageName + ".documents"

        internal fun childDocumentId(parentDocumentId: String, name: String): String = "$parentDocumentId/$name"

        internal fun parentDocumentId(documentId: String): String = documentId.substringBeforeLast('/', rootDocumentId)

        internal fun fileIn(documents: File, documentId: String): File? {
            if (documentId != rootDocumentId && !documentId.startsWith("$rootDocumentId/")) {
                return null
            }
            val relative = documentId.removePrefix(rootDocumentId).trimStart('/')
            val root = documents.canonicalFile
            val file = File(root, relative).canonicalFile
            if (file != root && !file.path.startsWith(root.path + File.separator)) {
                return null
            }
            return file
        }

        internal fun documentId(documents: File, file: File): String? {
            val root = documents.canonicalFile
            val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return null
            if (canonical == root) {
                return rootDocumentId
            }
            if (!canonical.path.startsWith(root.path + File.separator)) {
                return null
            }
            return rootDocumentId + "/" + canonical.relativeTo(root).invariantSeparatorsPath
        }

        fun documentUri(context: Context, file: File): Uri? {
            val id = documentId(Documents.directory(context), file) ?: return null
            return DocumentsContract.buildDocumentUri(authority(context), id)
        }

        fun rootUri(context: Context): Uri = DocumentsContract.buildRootUri(authority(context), rootId)

        internal fun mimeType(file: File): String {
            if (file.isDirectory) {
                return Document.MIME_TYPE_DIR
            }
            val extension = file.name.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }
}
