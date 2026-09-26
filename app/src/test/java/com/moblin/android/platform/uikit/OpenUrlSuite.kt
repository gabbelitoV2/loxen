package com.moblin.android.platform.uikit

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.DocumentsContract
import com.moblin.android.platform.Bookmark
import com.moblin.android.platform.DocumentTrees
import com.moblin.android.platform.Documents
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class OpenUrlSuite {
    private lateinit var application: Application
    private val filesApp = ComponentName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
    }

    private fun installFilesApp(vararg mimeTypes: String) {
        val packageManager = shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(filesApp)
        for (mimeType in mimeTypes) {
            val filter = IntentFilter(Intent.ACTION_VIEW)
            filter.addCategory(Intent.CATEGORY_DEFAULT)
            filter.addDataType(mimeType)
            packageManager.addIntentFilterForActivity(filesApp, filter)
        }
    }

    private fun recordings(): File = File(Documents.directory, "Recordings").also { it.mkdirs() }

    @Test
    fun aDocumentsFolderOpensInTheFilesApp() {
        installFilesApp(DocumentsContract.Document.MIME_TYPE_DIR)
        val url = "shareddocuments://${recordings().path}"
        assertTrue(UIApplication.shared.canOpenURL(url))
        var success: Boolean? = null
        UIApplication.shared.open(url) { success = it }
        assertEquals(true, success)
        val started = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, started.type)
        assertEquals("com.moblin.android.documents", started.data?.authority)
        assertEquals("Documents/Recordings", DocumentsContract.getDocumentId(started.data))
    }

    @Test
    fun aFolderWithSpacesInItsNameIsFound() {
        installFilesApp(DocumentsContract.Document.MIME_TYPE_DIR)
        val folder = File(Documents.directory, "My Replays").also { it.mkdirs() }
        UIApplication.shared.open("shareddocuments://${folder.path}")
        val started = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals("Documents/My Replays", DocumentsContract.getDocumentId(started.data))
    }

    @Test
    fun theRootOpensWhenTheFilesAppCannotOpenAFolder() {
        installFilesApp("vnd.android.document/root")
        val url = "shareddocuments://${recordings().path}"
        assertTrue(UIApplication.shared.canOpenURL(url))
    }

    @Test
    fun withoutAFilesAppOrOutsideMoblinsFoldersNothingOpens() {
        assertFalse(UIApplication.shared.canOpenURL("shareddocuments://${recordings().path}"))
        var success: Boolean? = null
        UIApplication.shared.open(application.cacheDir.toURI()) { success = it }
        assertEquals(false, success)
        installFilesApp(DocumentsContract.Document.MIME_TYPE_DIR)
        assertFalse(UIApplication.shared.canOpenURL("shareddocuments://${application.cacheDir.path}"))
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun aBookmarkedFolderOpensAsItsDocument() {
        DocumentTrees.setUp()
        installFilesApp(DocumentsContract.Document.MIME_TYPE_DIR)
        File(DocumentTrees.root, "Movies/Moblin").mkdirs()
        val tree = DocumentTrees.treeUri("Movies/Moblin")
        assertNotNull(Bookmark.data(tree.toString()))
        val path = assertNotNull(Bookmark.path(tree.toString().toByteArray()))
        UIApplication.shared.open("shareddocuments://$path")
        val started = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals("com.android.externalstorage.documents", started.data?.authority)
        assertEquals("primary:Movies/Moblin", DocumentsContract.getDocumentId(started.data))
        assertTrue(DocumentsContract.isTreeUri(assertNotNull(started.data)))
    }
}
