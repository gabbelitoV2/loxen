package com.moblin.android.platform

import android.content.Context
import com.moblin.android.AppDelegate
import com.moblin.android.MessageQueue
import com.moblin.android.runMainTest
import com.moblin.android.various.model.controlBarBackgroundImagePath
import com.moblin.android.various.model.faceBackgroundImagePath
import com.moblin.android.various.model.stealthModeImagePath
import com.moblin.android.various.settings.Settings
import com.moblin.android.various.storages.ImageStorage
import com.moblin.android.various.storages.LogsStorage
import com.moblin.android.various.storages.MediaPlayerStorage
import com.moblin.android.various.storages.createAlertVideosDirectory
import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentsSuite {
    private val context: Context
        get() = AppDelegate.context

    private val dataDirectory: File
        get() = checkNotNull(context.cacheDir.parentFile)

    private val androidFolders = listOf("shared_prefs", "databases", "code_cache", "no_backup", "app_webview")

    private fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun makeArchive(entries: List<Pair<String, String>>): File {
        val archive = File(context.cacheDir, "import.moblinSettings")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            for ((name, text) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return archive
    }

    private fun import(archive: File): String? {
        var result: String? = null
        runMainTest {
            val completed = MessageQueue<String?>()
            Settings().importFromFile(archive.toURI()) { completed.put(it) }
            result = completed.get()
        }
        return result
    }

    @Test
    fun documentsIsDedicatedFolderInFilesDirectory() {
        val documents = Documents.directory
        assertEquals(File(context.filesDir, "Documents"), documents)
        assertTrue(documents.isDirectory)
        assertEquals(File(documents, "Logs"), createAndGetDirectory("Logs"))
        assertTrue(File(documents, "Logs").isDirectory)
        assertEquals(File(documents, "Alerts/Videos"), createAlertVideosDirectory())
        assertEquals(File(documents, "stealthModeImage.img"), stealthModeImagePath)
        assertEquals(File(documents, "controlBarBackgroundImage.img"), controlBarBackgroundImagePath)
        assertEquals("faceBackgroundImage.img", faceBackgroundImagePath.name)
        assertEquals("Documents", faceBackgroundImagePath.parentFile?.name)
        assertEquals(context.filesDir.name, faceBackgroundImagePath.parentFile?.parentFile?.name)
        assertFalse(File(dataDirectory, "Logs").exists())
        assertFalse(File(dataDirectory, "Alerts").exists())
    }

    @Test
    fun migrationMovesMoblinFilesOnceAndLeavesAndroidFolders() {
        assertFalse(Documents.migratedMarker(context).exists())
        write(File(dataDirectory, "Logs/log.txt"), "log")
        write(File(dataDirectory, "Recordings/recording.mp4"), "recording")
        write(File(dataDirectory, "Alerts/Videos/alert.mp4"), "alert")
        write(File(dataDirectory, "Images/image"), "image")
        write(File(dataDirectory, "Medias/media.mp4"), "media")
        write(File(dataDirectory, "PNGTuber/tuber"), "png")
        write(File(dataDirectory, "VTuber/tuber"), "vrm")
        write(File(dataDirectory, "Replays/replay.mp4"), "replay")
        write(File(dataDirectory, "ReplayTransitions/transition.mov"), "transition")
        write(File(context.filesDir, "stealthModeImage.img"), "stealth")
        write(File(context.filesDir, "faceBackgroundImage.img"), "face")
        write(File(context.filesDir, "controlBarBackgroundImage.img"), "controlBar")
        write(File(context.filesDir, "SimpleStorage/settings"), "{}")
        for (folder in androidFolders) {
            write(File(dataDirectory, "$folder/keep"), folder)
        }
        val documents = Documents.setup(context)
        assertEquals(File(context.filesDir, "Documents"), documents)
        assertEquals("log", File(documents, "Logs/log.txt").readText())
        assertEquals("recording", File(documents, "Recordings/recording.mp4").readText())
        assertEquals("alert", File(documents, "Alerts/Videos/alert.mp4").readText())
        assertEquals("image", File(documents, "Images/image").readText())
        assertEquals("media", File(documents, "Medias/media.mp4").readText())
        assertEquals("png", File(documents, "PNGTuber/tuber").readText())
        assertEquals("vrm", File(documents, "VTuber/tuber").readText())
        assertEquals("replay", File(documents, "Replays/replay.mp4").readText())
        assertEquals("transition", File(documents, "ReplayTransitions/transition.mov").readText())
        assertEquals("stealth", File(documents, "stealthModeImage.img").readText())
        assertEquals("face", File(documents, "faceBackgroundImage.img").readText())
        assertEquals("controlBar", File(documents, "controlBarBackgroundImage.img").readText())
        for (name in listOf("Logs", "Recordings", "Alerts", "Images", "Medias", "PNGTuber", "VTuber", "Replays")) {
            assertFalse(File(dataDirectory, name).exists(), name)
        }
        assertFalse(File(dataDirectory, "ReplayTransitions").exists())
        assertFalse(File(context.filesDir, "stealthModeImage.img").exists())
        assertFalse(File(context.filesDir, "faceBackgroundImage.img").exists())
        assertFalse(File(context.filesDir, "controlBarBackgroundImage.img").exists())
        assertEquals("{}", File(context.filesDir, "SimpleStorage/settings").readText())
        assertFalse(File(documents, "SimpleStorage").exists())
        for (folder in androidFolders) {
            assertEquals(folder, File(dataDirectory, "$folder/keep").readText())
            assertFalse(File(documents, folder).exists(), folder)
        }
        assertFalse(File(documents, "files").exists())
        assertFalse(File(documents, "cache").exists())
        assertTrue(Documents.migratedMarker(context).exists())
        write(File(dataDirectory, "Logs/later.txt"), "later")
        write(File(context.filesDir, "stealthModeImage.img"), "later")
        Documents.setup(context)
        assertEquals("later", File(dataDirectory, "Logs/later.txt").readText())
        assertFalse(File(documents, "Logs/later.txt").exists())
        assertEquals("stealth", File(documents, "stealthModeImage.img").readText())
    }

    @Test
    fun interruptedMigrationFinishesOnNextStart() {
        val documents = Documents.documentsDirectory(context)
        write(File(documents, "Images/image"), "image")
        write(File(documents, "stealthModeImage.img"), "stealth")
        write(File(documents, "Recordings/moved.mp4"), "moved")
        write(File(dataDirectory, "Recordings/left.mp4"), "left")
        write(File(documents, "Recordings/both.mp4"), "new")
        write(File(dataDirectory, "Recordings/both.mp4"), "old")
        write(File(dataDirectory, "Logs/log.txt"), "log")
        write(File(context.filesDir, "faceBackgroundImage.img"), "face")
        assertFalse(Documents.migratedMarker(context).exists())
        Documents.setup(context)
        assertEquals("image", File(documents, "Images/image").readText())
        assertEquals("stealth", File(documents, "stealthModeImage.img").readText())
        assertEquals("moved", File(documents, "Recordings/moved.mp4").readText())
        assertEquals("left", File(documents, "Recordings/left.mp4").readText())
        assertEquals("new", File(documents, "Recordings/both.mp4").readText())
        assertEquals("old", File(dataDirectory, "Recordings/both.mp4").readText())
        assertFalse(File(dataDirectory, "Recordings/left.mp4").exists())
        assertEquals("log", File(documents, "Logs/log.txt").readText())
        assertFalse(File(dataDirectory, "Logs").exists())
        assertEquals("face", File(documents, "faceBackgroundImage.img").readText())
        assertFalse(File(context.filesDir, "faceBackgroundImage.img").exists())
        assertTrue(Documents.migratedMarker(context).exists())
        assertTrue(Documents.migratedMarker(context).delete())
        Documents.setup(context)
        assertTrue(Documents.migratedMarker(context).exists())
        assertEquals("log", File(documents, "Logs/log.txt").readText())
        assertEquals("image", File(documents, "Images/image").readText())
    }

    @Test
    fun settingsImportWritesOnlyInsideDocuments() {
        val archive = makeArchive(
            listOf(
                "settings.json" to "{}",
                "shared_prefs/moblin.xml" to "prefs",
                "databases/moblin.db" to "database",
                "Images/00000000-0000-0000-0000-000000000001" to "image",
                "stealthModeImage.img" to "stealth",
            ),
        )
        assertNull(import(archive))
        val documents = Documents.directory
        assertEquals("prefs", File(documents, "shared_prefs/moblin.xml").readText())
        assertEquals("database", File(documents, "databases/moblin.db").readText())
        assertFalse(File(dataDirectory, "shared_prefs/moblin.xml").exists())
        assertFalse(File(dataDirectory, "databases/moblin.db").exists())
        assertEquals("image", File(documents, "Images/00000000-0000-0000-0000-000000000001").readText())
        assertEquals("stealth", stealthModeImagePath.readText())
        assertEquals(File(documents, "stealthModeImage.img"), stealthModeImagePath)
        assertFalse(File(context.filesDir, "stealthModeImage.img").exists())
        assertFalse(File(documents, "settings.json").exists())
        assertFalse(File(dataDirectory, "settings.json").exists())
    }

    @Test
    fun settingsImportRejectsEntriesOutsideDocuments() {
        val archive = makeArchive(
            listOf(
                "settings.json" to "{}",
                "../../shared_prefs/moblin.xml" to "prefs",
            ),
        )
        assertNotNull(import(archive))
        assertFalse(File(dataDirectory, "shared_prefs/moblin.xml").exists())
        assertFalse(File(context.filesDir, "shared_prefs/moblin.xml").exists())
        assertFalse(File(Documents.directory, "shared_prefs/moblin.xml").exists())
    }

    @Test
    fun settingsImportWritesNothingWhenAnEntryIsInvalid() {
        val documents = Documents.directory
        for (name in listOf(
            "/shared_prefs/moblin.xml",
            "Images/../../shared_prefs/moblin.xml",
            "Images/../..",
            "..",
        )) {
            val archive = makeArchive(
                listOf(
                    "settings.json" to "{}",
                    "Images/00000000-0000-0000-0000-000000000001" to "image",
                    name to "prefs",
                ),
            )
            assertNotNull(import(archive), name)
            assertFalse(File(documents, "Images/00000000-0000-0000-0000-000000000001").exists(), name)
            assertFalse(File(dataDirectory, "shared_prefs/moblin.xml").exists(), name)
            assertFalse(File("/shared_prefs/moblin.xml").exists(), name)
        }
    }

    @Test
    fun settingsImportKeepsOddEntryNamesInsideDocuments() {
        val documents = Documents.directory
        for ((name, inside) in listOf(
            "..\\shared_prefs\\moblin.xml" to null,
            "..\\..\\shared_prefs\\moblin.xml" to null,
            "Images\\..\\..\\..\\shared_prefs\\moblin.xml" to null,
            "C:\\shared_prefs\\moblin.xml" to null,
            "Images//00000000-0000-0000-0000-000000000002" to "Images/00000000-0000-0000-0000-000000000002",
            "./Images/./00000000-0000-0000-0000-000000000003" to "Images/00000000-0000-0000-0000-000000000003",
            "shared_prefs/moblin.xml" to "shared_prefs/moblin.xml",
        )) {
            val archive = makeArchive(listOf("settings.json" to "{}", name to "prefs"))
            val error = import(archive)
            if (inside != null) {
                assertNull(error, name)
                assertEquals("prefs", File(documents, inside).readText(), name)
            }
            assertFalse(File(dataDirectory, "shared_prefs/moblin.xml").exists(), name)
            assertFalse(File(context.filesDir, "shared_prefs/moblin.xml").exists(), name)
            assertFalse(File(dataDirectory.parentFile, "shared_prefs/moblin.xml").exists(), name)
        }
    }

    @Test
    fun storagesReadMigratedFilesOnFirstUse() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        write(File(dataDirectory, "Images/$id"), "image")
        write(File(dataDirectory, "Medias/$id.mp4"), "media")
        write(File(dataDirectory, "Logs/log.txt"), "log")
        write(File(context.filesDir, "stealthModeImage.img"), "stealth")
        assertEquals("image", ImageStorage().read(id)?.decodeToString())
        assertEquals(listOf(id), MediaPlayerStorage().ids())
        assertEquals("log", File(LogsStorage().storageDirectory(), "log.txt").readText())
        assertEquals("stealth", stealthModeImagePath.readText())
        assertFalse(File(dataDirectory, "Images").exists())
    }
}
