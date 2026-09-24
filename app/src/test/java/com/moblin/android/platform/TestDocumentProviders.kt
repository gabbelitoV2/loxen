package com.moblin.android.platform

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsProvider
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import com.moblin.android.AppDelegate
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import org.robolectric.Robolectric

const val pickerDocumentsAuthority = "com.moblin.android.test.pickerdocuments"
const val externalStorageDocumentsAuthority = "com.android.externalstorage.documents"

object PickerDocuments {
    private val documents = mutableMapOf<String, Pair<String?, ByteArray>>()
    val sources: File
        get() = File(AppDelegate.context.cacheDir, "picker-sources")

    fun setUp() {
        documents.clear()
        sources.mkdirs()
        Robolectric.setupContentProvider(PickerDocumentsProvider::class.java, pickerDocumentsAuthority)
    }

    fun add(displayName: String?, content: ByteArray, id: String = "msf:${UUID.randomUUID()}"): Uri {
        documents[id] = displayName to content
        return Uri.Builder()
            .scheme("content")
            .authority(pickerDocumentsAuthority)
            .appendPath("document")
            .appendPath(id)
            .build()
    }

    fun add(displayName: String?, content: String): Uri = add(displayName, content.toByteArray())

    fun missing(): Uri = Uri.Builder()
        .scheme("content")
        .authority(pickerDocumentsAuthority)
        .appendPath("document")
        .appendPath("missing")
        .build()

    internal fun document(uri: Uri): Pair<String?, ByteArray>? = documents[uri.lastPathSegment]
}

class PickerDocumentsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        PickerDocuments.document(uri)?.let { (name, content) ->
            cursor.addRow(arrayOf<Any?>(name, content.size.toLong()))
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val (_, content) = PickerDocuments.document(uri) ?: throw FileNotFoundException(uri.toString())
        val source = File(PickerDocuments.sources, UUID.randomUUID().toString())
        source.writeBytes(content)
        return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "application/octet-stream"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

object DocumentTrees {
    val root: File
        get() = Environment.getExternalStorageDirectory()

    fun setUp() {
        root.mkdirs()
        val info = ProviderInfo().apply {
            authority = externalStorageDocumentsAuthority
            exported = true
            grantUriPermissions = true
            readPermission = Manifest.permission.MANAGE_DOCUMENTS
            writePermission = Manifest.permission.MANAGE_DOCUMENTS
        }
        Robolectric.buildContentProvider(ExternalStorageDocumentsProvider::class.java).create(info)
    }

    fun treeUri(relativePath: String): Uri {
        return DocumentsContract.buildTreeDocumentUri(externalStorageDocumentsAuthority, "primary:$relativePath")
    }

    fun file(documentId: String): File {
        return File(root, documentId.substringAfter(':'))
    }
}

class ExternalStorageDocumentsProvider : DocumentsProvider() {
    private val columns = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_SIZE,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_FLAGS,
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        return MatrixCursor(arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = DocumentTrees.file(documentId)
        if (!file.exists()) {
            throw FileNotFoundException(documentId)
        }
        return MatrixCursor(columns).also { addRow(it, documentId, file) }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val directory = DocumentTrees.file(parentDocumentId)
        if (!directory.isDirectory) {
            throw FileNotFoundException(parentDocumentId)
        }
        val cursor = MatrixCursor(columns)
        for (file in directory.listFiles().orEmpty().sortedBy { it.name }) {
            addRow(cursor, childId(parentDocumentId, file.name), file)
        }
        return cursor
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(DocumentTrees.file(documentId), ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val file = File(DocumentTrees.file(parentDocumentId), displayName)
        if (mimeType == Document.MIME_TYPE_DIR) {
            file.mkdirs()
        } else {
            file.createNewFile()
        }
        return childId(parentDocumentId, displayName)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(childId(parentDocumentId, ""))
    }

    private fun childId(parentDocumentId: String, name: String): String {
        return if (parentDocumentId.endsWith(":")) "$parentDocumentId$name" else "$parentDocumentId/$name"
    }

    private fun addRow(cursor: MatrixCursor, documentId: String, file: File) {
        val mimeType = if (file.isDirectory) Document.MIME_TYPE_DIR else "application/octet-stream"
        val flags = if (file.isDirectory) Document.FLAG_DIR_SUPPORTS_CREATE else Document.FLAG_SUPPORTS_WRITE
        cursor.addRow(arrayOf<Any?>(documentId, file.name, file.length(), mimeType, flags))
    }
}

class FakeActivityResults(var result: Any?) : ActivityResultRegistryOwner {
    val inputs = mutableListOf<Any?>()

    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            inputs.add(input)
            Handler(Looper.getMainLooper()).post {
                dispatchResult(requestCode, result)
            }
        }
    }
}
