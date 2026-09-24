package com.moblin.android.platform

import android.content.Context
import com.moblin.android.AppDelegate
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.storages.RecordingsStorage
import com.moblin.android.various.utils.makeRecordingPath
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkSuite {
    private val context: Context
        get() = AppDelegate.context

    private val folder: File
        get() = File(DocumentTrees.root, "Movies/Moblin")

    @Before
    fun setUp() {
        DocumentTrees.setUp()
        folder.deleteRecursively()
        folder.mkdirs()
    }

    private fun bookmark(): ByteArray {
        return assertNotNull(Bookmark.data(DocumentTrees.treeUri("Movies/Moblin").toString()))
    }

    private fun recording(recordingPath: ByteArray?): SettingsStreamRecording {
        return SettingsStreamRecording().also { it.recordingPath = recordingPath }
    }

    @Test
    fun bookmarkIsThePickedFolderWithAPersistableGrant() {
        val tree = DocumentTrees.treeUri("Movies/Moblin")
        assertEquals(tree.toString(), String(bookmark()))
        val permission = context.contentResolver.persistedUriPermissions.single()
        assertEquals(tree, permission.uri)
        assertTrue(permission.isReadPermission)
        assertTrue(permission.isWritePermission)
    }

    @Test
    fun onlyAFolderCanBeBookmarked() {
        assertNull(Bookmark.data("content://$externalStorageDocumentsAuthority/document/primary%3AMovies"))
        assertNull(Bookmark.data(folder.path))
        assertTrue(context.contentResolver.persistedUriPermissions.isEmpty())
    }

    @Test
    fun resolvedBookmarkIsTheFolderPath() {
        val data = bookmark()
        val directory = assertNotNull(Bookmark.resolve(data))
        assertIs<BookmarkedDirectory>(directory)
        assertEquals(folder.path, directory.path)
        assertTrue(directory.exists())
        assertTrue(directory.isDirectory)
        assertEquals(folder.path, makeRecordingPath(data))
    }

    @Test
    fun bookmarkWithoutGrantOrDiskDoesNotResolve() {
        val ungranted = DocumentTrees.treeUri("Movies/Moblin").toString().toByteArray()
        assertNull(Bookmark.resolve(ungranted))
        assertNull(makeRecordingPath(ungranted))
        val data = bookmark()
        assertNotNull(Bookmark.resolve(data))
        folder.deleteRecursively()
        assertNull(Bookmark.resolve(data))
        assertNull(makeRecordingPath(data))
        assertNull(makeRecordingPath("garbage".toByteArray()))
    }

    @Test
    fun recordingInTheBookmarkedFolderIsWrittenThroughTheProvider() {
        val current = assertNotNull(RecordingsStorage().createRecording(recording(bookmark())))
        val file = assertNotNull(current.url())
        assertIs<BookmarkedFile>(file)
        assertTrue(Regex("Recording_.+\\.mp4").matches(file.name))
        assertEquals(File(folder, file.name).path, file.path)
        assertFalse(file.exists())
        assertEquals(0, file.length())
        val url = assertNotNull(Bookmark.url(file))
        assertTrue(url.startsWith("content://$externalStorageDocumentsAuthority/tree/"))
        val stored = File(folder, file.name)
        assertTrue(stored.exists())
        assertNotNull(Bookmark.openOutput(url, File(url))).use { it.write("segments".toByteArray()) }
        assertEquals("segments", stored.readText())
        val again = assertNotNull(current.url())
        assertTrue(again.exists())
        assertEquals(8, again.length())
        assertEquals(url, Bookmark.url(again))
        assertEquals(1, folder.listFiles()?.size)
        assertNotNull(Bookmark.openOutput(url, File(url))).use { it.write("new".toByteArray()) }
        assertEquals("new", stored.readText())
    }

    @Test
    fun recordingInABookmarkedFolderNeedsTheDisk() {
        val data = bookmark()
        folder.deleteRecursively()
        assertNull(RecordingsStorage().createRecording(recording(data)))
        assertNull(RecordingsStorage().createRecording(recording(DocumentTrees.treeUri("Movies").toString().toByteArray())))
    }

    @Test
    fun defaultRecordingIsAFileInDocuments() {
        val current = assertNotNull(RecordingsStorage().createRecording(recording(null)))
        val file = assertNotNull(current.url())
        assertFalse(file is BookmarkedFile)
        assertEquals(File(Documents.directory, "Recordings").canonicalFile, file.canonicalFile.parentFile)
        val url = assertNotNull(Bookmark.url(file))
        assertEquals(file.toString(), url)
        assertNotNull(Bookmark.openOutput(url, File(url))).use { it.write("default".toByteArray()) }
        assertEquals("default", file.readText())
        assertEquals(7, current.url()?.length())
    }

    @Test
    fun childOfAPlainDirectoryIsAPlainFile() {
        val directory = File(context.cacheDir, "plain")
        assertEquals(File(directory, "a.mp4"), Bookmark.child(directory, "a.mp4"))
        assertFalse(Bookmark.child(directory, "a.mp4") is BookmarkedFile)
    }
}
