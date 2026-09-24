package com.moblin.android.platform

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.moblin.android.AppDelegate
import com.moblin.android.various.settings.SettingsStreamReplayStinger
import com.moblin.android.various.storages.ReplayTransitionsStorage
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

private const val pickedDocumentsAuthority = "com.moblin.android.test.documents"

class PickedDocumentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf<Any?>(pickedDisplayName))
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(checkNotNull(pickedSource), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "video/quicktime"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

private var pickedDisplayName: String? = null
private var pickedSource: File? = null

@RunWith(RobolectricTestRunner::class)
class DocumentPickerSuite {
    private val context: Context
        get() = AppDelegate.context

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(PickedDocumentProvider::class.java, pickedDocumentsAuthority)
    }

    private fun pick(displayName: String?, documentId: String, content: String): Uri {
        pickedDisplayName = displayName
        pickedSource = File(context.cacheDir, "provider-source").also { it.writeText(content) }
        return Uri.Builder()
            .scheme("content")
            .authority(pickedDocumentsAuthority)
            .appendPath("document")
            .appendPath(documentId)
            .build()
    }

    private fun pickStinger(uri: Uri, stinger: SettingsStreamReplayStinger): SettingsStreamReplayStinger {
        val name = DocumentPicker.displayName(context, uri)
        val file = DocumentPicker.inboxFile(context, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val url = file.invariantSeparatorsPath
        val picked = stinger.copy(name = url.substringAfterLast('/'))
        val filename = assertNotNull(picked.makeFilename())
        val storage = ReplayTransitionsStorage()
        storage.remove(filename = filename)
        storage.add(filename = filename, url = File(url))
        return picked
    }

    private fun stingerFile(stinger: SettingsStreamReplayStinger): File {
        return ReplayTransitionsStorage().makePath(assertNotNull(stinger.makeFilename()))
    }

    private fun File.isInside(directory: File): Boolean {
        return canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())
    }

    @Test
    fun safeFileNameKeepsOrdinaryNames() {
        assertEquals("My stinger.mov", DocumentPicker.safeFileName("My stinger.mov"))
        assertEquals("Övergång (1).MOV", DocumentPicker.safeFileName("Övergång (1).MOV"))
        assertEquals(".hidden", DocumentPicker.safeFileName(".hidden"))
        assertEquals("...", DocumentPicker.safeFileName("..."))
    }

    @Test
    fun safeFileNameIsOneComponent() {
        assertEquals(".._.._shared_prefs_x.xml", DocumentPicker.safeFileName("../../shared_prefs/x.xml"))
        assertEquals("a_b_c.mov", DocumentPicker.safeFileName("a\\b/c.mov"))
        assertEquals("a_b.mov", DocumentPicker.safeFileName("a\u0000b.mov"))
        assertEquals("a_b.mov", DocumentPicker.safeFileName("a\nb.mov"))
        assertEquals("Document", DocumentPicker.safeFileName(null))
        assertEquals("Document", DocumentPicker.safeFileName(""))
        assertEquals("Document", DocumentPicker.safeFileName("  "))
        assertEquals("Document", DocumentPicker.safeFileName("."))
        assertEquals("Document", DocumentPicker.safeFileName(".."))
        assertEquals("Document", DocumentPicker.safeFileName(" .. "))
    }

    @Test
    fun safeFileNameFitsTheFileSystemAndKeepsTheExtension() {
        val long = DocumentPicker.safeFileName("a".repeat(300) + ".mov")
        assertEquals(255, long.toByteArray().size)
        assertTrue(long.endsWith("a.mov"))
        val wide = DocumentPicker.safeFileName("🎬".repeat(100) + ".mov")
        assertTrue(wide.toByteArray().size <= 255)
        assertTrue(wide.endsWith("🎬.mov"))
        assertEquals(wide, String(wide.toByteArray()))
        val noExtension = DocumentPicker.safeFileName("b".repeat(400))
        assertEquals("b".repeat(255), noExtension)
    }

    @Test
    fun inboxFileIsInsideTheCacheInbox() {
        val inbox = File(context.cacheDir, "Inbox")
        for (name in listOf("x.mov", "../x.mov", "../../files/SimpleStorage/settings", "..", "")) {
            val file = DocumentPicker.inboxFile(context, name)
            assertEquals(inbox.canonicalFile, file.canonicalFile.parentFile, name)
        }
        assertTrue(inbox.isDirectory)
    }

    @Test
    fun displayNameComesFromTheProvider() {
        assertEquals("My stinger.mov", DocumentPicker.displayName(context, pick("My stinger.mov", "msf:17", "x")))
        assertEquals("x.mov", DocumentPicker.displayName(context, pick(null, "primary:Movies/../x.mov", "x")))
    }

    @Test
    fun pickedStingerIsStoredInReplayTransitionsUnderItsId() {
        val stinger = pickStinger(pick("My stinger.mov", "primary:Movies/My stinger.mov", "first"), SettingsStreamReplayStinger())
        assertEquals("My stinger.mov", stinger.name)
        val stored = stingerFile(stinger)
        assertEquals(File(Documents.directory, "ReplayTransitions/${stinger.id}.mov").canonicalFile, stored.canonicalFile)
        assertEquals("first", stored.readText())
        assertFalse(File(context.cacheDir, "Inbox/My stinger.mov").exists())
        assertFalse(File(context.filesDir, "My stinger.mov").exists())
        val replaced = pickStinger(pick("Other.mov", "msf:18", "second"), stinger)
        assertEquals(stinger.id, replaced.id)
        assertEquals("Other.mov", replaced.name)
        assertEquals(stored.canonicalFile, stingerFile(replaced).canonicalFile)
        assertEquals("second", stored.readText())
    }

    @Test
    fun pickedFileWithHostileNameStaysInsideItsFolders() {
        val stinger = pickStinger(
            pick("../../shared_prefs/evil.xml", "primary:Movies/evil.mov", "hostile"),
            SettingsStreamReplayStinger(),
        )
        assertEquals(".._.._shared_prefs_evil.xml", stinger.name)
        val stored = stingerFile(stinger)
        assertTrue(stored.isInside(File(Documents.directory, "ReplayTransitions")))
        assertEquals("hostile", stored.readText())
        val dataDirectory = checkNotNull(context.dataDir)
        assertFalse(File(dataDirectory, "shared_prefs/evil.xml").exists())
        assertFalse(dataDirectory.walkTopDown().any { it.name == "evil.xml" })
    }

    @Test
    fun stingerFilenameUsesThePathExtensionOfTheName() {
        val id = UUID.fromString("E621E1F8-C36C-495A-93FC-0C247A3E6E5F")
        fun filename(name: String) = SettingsStreamReplayStinger(id = id, name = name).makeFilename()
        assertEquals("$id.mov", filename("My stinger.mov"))
        assertEquals("$id.mov", filename("a%b [1] {x}.mov"))
        assertEquals("$id.MOV", filename("Övergång.MOV"))
        assertEquals("$id.", filename("stinger"))
        assertEquals("$id.", filename(""))
        assertEquals("$id.", filename("a#b.mov"))
        assertEquals("$id.", filename("a?b.mov"))
        assertEquals("$id.", filename("dir.v2/stinger"))
        assertEquals("$id.mp4", filename("dir/stinger.mp4"))
    }
}
