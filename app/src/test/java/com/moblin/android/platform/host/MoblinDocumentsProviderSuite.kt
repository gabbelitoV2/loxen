package com.moblin.android.platform.host

import android.app.Application
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import com.moblin.android.platform.Documents
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MoblinDocumentsProviderSuite {
    private lateinit var application: Application
    private lateinit var provider: MoblinDocumentsProvider
    private lateinit var documents: File
    private val authority = "com.moblin.android.documents"

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        val info = ProviderInfo().apply {
            this.authority = this@MoblinDocumentsProviderSuite.authority
            exported = true
            grantUriPermissions = true
            readPermission = android.Manifest.permission.MANAGE_DOCUMENTS
            writePermission = android.Manifest.permission.MANAGE_DOCUMENTS
        }
        provider = Robolectric.buildContentProvider(MoblinDocumentsProvider::class.java).create(info).get()
        documents = Documents.directory
        File(documents, "Recordings").mkdirs()
        File(documents, "Recordings/2026-09-26 12-00-00.mp4").writeText("recording")
        File(documents, "Replays").mkdirs()
        File(documents, ".hidden").writeText("hidden")
    }

    private fun Cursor.rows(): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        use {
            while (moveToNext()) {
                rows.add(columnNames.associateWith { name ->
                    val index = getColumnIndex(name)
                    when (getType(index)) {
                        Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                        Cursor.FIELD_TYPE_NULL -> null
                        else -> getString(index)
                    }
                })
            }
        }
        return rows
    }

    @Test
    fun theManifestDeclaresTheProviderForTheFilesApp() {
        val info = assertNotNull(application.packageManager.resolveContentProvider(authority, 0))
        assertEquals(MoblinDocumentsProvider::class.java.name, info.name)
        assertEquals(android.Manifest.permission.MANAGE_DOCUMENTS, info.readPermission)
        assertTrue(info.exported)
        val providers = application.packageManager.queryIntentContentProviders(
            Intent(DocumentsContract.PROVIDER_INTERFACE),
            0,
        )
        assertTrue(providers.any { it.providerInfo.authority == authority })
    }

    @Test
    fun theRootIsMoblinsDocumentsFolder() {
        val root = provider.queryRoots(null).rows().single()
        assertEquals(MoblinDocumentsProvider.rootDocumentId, root[Root.COLUMN_DOCUMENT_ID])
        assertEquals("Moblin", root[Root.COLUMN_TITLE])
        val children = provider.queryChildDocuments(MoblinDocumentsProvider.rootDocumentId, null, null as String?).rows()
        assertEquals(listOf("Recordings", "Replays"), children.map { it[Document.COLUMN_DISPLAY_NAME] })
        assertTrue(children.all { it[Document.COLUMN_MIME_TYPE] == Document.MIME_TYPE_DIR })
        val recordings = provider.queryChildDocuments("Documents/Recordings", null, null as String?).rows().single()
        assertEquals("Documents/Recordings/2026-09-26 12-00-00.mp4", recordings[Document.COLUMN_DOCUMENT_ID])
        assertEquals(9L, recordings[Document.COLUMN_SIZE])
        val flags = recordings[Document.COLUMN_FLAGS] as Long
        assertTrue((flags and Document.FLAG_SUPPORTS_DELETE.toLong()) != 0L)
    }

    @Test
    fun documentsCanBeReadCreatedRenamedAndDeleted() {
        val read = provider.openDocument("Documents/Recordings/2026-09-26 12-00-00.mp4", "r", null)
        assertEquals("recording", android.os.ParcelFileDescriptor.AutoCloseInputStream(read).use { it.readBytes().decodeToString() })
        val created = provider.createDocument("Documents/Replays", "video/mp4", "Replay.mp4")
        assertEquals("Documents/Replays/Replay.mp4", created)
        assertTrue(File(documents, "Replays/Replay.mp4").isFile)
        assertEquals("Documents/Replays/Replay (1).mp4", provider.createDocument("Documents/Replays", "video/mp4", "Replay.mp4"))
        val renamed = provider.renameDocument(created, "Best.mp4")
        assertEquals("Documents/Replays/Best.mp4", renamed)
        assertTrue(File(documents, "Replays/Best.mp4").isFile)
        provider.deleteDocument(renamed)
        assertFalse(File(documents, "Replays/Best.mp4").exists())
        assertTrue(provider.isChildDocument("Documents", "Documents/Replays/Replay (1).mp4"))
        assertFalse(provider.isChildDocument("Documents/Replays", "Documents/Recordings"))
    }

    @Test
    fun documentsOutsideTheFolderAreNotReachable() {
        File(application.filesDir, "SimpleStorage").mkdirs()
        assertFailsWith<FileNotFoundException> {
            provider.queryDocument("Documents/../SimpleStorage", null)
        }
        assertFailsWith<FileNotFoundException> {
            provider.queryDocument("SimpleStorage", null)
        }
        assertFailsWith<UnsupportedOperationException> {
            provider.deleteDocument(MoblinDocumentsProvider.rootDocumentId)
        }
    }

    @Test
    fun aFileInTheFolderHasADocumentUri() {
        val uri = assertNotNull(MoblinDocumentsProvider.documentUri(application, File(documents, "Recordings")))
        assertEquals(authority, uri.authority)
        assertEquals("Documents/Recordings", DocumentsContract.getDocumentId(uri))
        assertEquals(
            Uri.parse("content://$authority/document/Documents"),
            MoblinDocumentsProvider.documentUri(application, documents),
        )
        assertNull(MoblinDocumentsProvider.documentUri(application, application.cacheDir))
    }
}
