package com.moblin.android.view.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.moblin.android.AppDelegate
import com.moblin.android.LocalModel
import com.moblin.android.platform.Bookmark
import com.moblin.android.platform.DocumentTrees
import com.moblin.android.platform.Documents
import com.moblin.android.platform.FakeActivityResults
import com.moblin.android.platform.PickerDocuments
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.settings.SettingsStreamReplayTransitionType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsAlertMediaType
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.various.settings.SettingsWidgetVTuberType
import com.moblin.android.various.utils.makeRecordingPath
import com.moblin.android.view.settings.camera.CameraSettingsLutsView
import com.moblin.android.view.settings.importexport.ImportSettingsView
import com.moblin.android.view.settings.mediaplayer.MediaPlayerSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.alerts.AlertMediaView
import com.moblin.android.view.settings.scenes.widgets.widget.alerts.CustomImageView
import com.moblin.android.view.settings.scenes.widgets.widget.alerts.CustomSoundView
import com.moblin.android.view.settings.scenes.widgets.widget.image.WidgetImagePickerView
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetPngTuberPickerView
import com.moblin.android.view.settings.scenes.widgets.widget.vtuber.WidgetVTuberPickerView
import com.moblin.android.view.settings.streams.stream.recording.RecordingPathFormView
import com.moblin.android.view.settings.streams.stream.replay.StreamReplaySettingsView
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Proxy
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentPickersSuite {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context
        get() = AppDelegate.context

    private val inbox: File
        get() = File(context.cacheDir, "Inbox")

    private lateinit var model: Model
    private lateinit var results: FakeActivityResults

    @Before
    fun setUp() {
        PickerDocuments.setUp()
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        results = FakeActivityResults(null)
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model, LocalActivityResultRegistryOwner provides results) {
                content()
            }
        }
        rule.waitForIdle()
    }

    private fun pick(result: Uri?, button: String) {
        results.result = result
        rule.onNodeWithText(button).performClick()
        rule.waitForIdle()
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000
        while (!condition()) {
            assertTrue(System.nanoTime() < deadline, "Timed out")
            Thread.sleep(10)
            rule.waitForIdle()
        }
        rule.waitForIdle()
    }

    private fun contentTypes(): List<Any?> = (results.inputs.last() as Array<*>).toList()

    private fun assertNoSheet() {
        assertTrue(rule.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, text) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    @Test
    fun alertImageIsStoredInAlertsUnderItsId() {
        val media = SettingsAlertsMediaGalleryItem(name = "My image")
        show { CustomImageView(model = model, media = media, image = null) }
        pick(PickerDocuments.add("Party.gif", "GIF89a"), "Select image")
        val stored = File(Documents.directory, "Alerts/${media.id}")
        waitFor { stored.exists() }
        assertEquals(listOf<Any?>("image/gif"), contentTypes())
        assertEquals(1, results.inputs.size)
        assertEquals("GIF89a", stored.readText())
        assertFalse(File(inbox, "Party.gif").exists())
        assertContentEquals("GIF89a".toByteArray(), model.alertMediaStorage.tryRead(media.id))
        assertNoSheet()
    }

    @Test
    fun alertSoundIsStoredInAlertsUnderItsId() {
        val media = SettingsAlertsMediaGalleryItem(name = "My sound")
        show { CustomSoundView(model = model, media = media) }
        pick(PickerDocuments.add("Horn.mp3", "ID3"), "Select sound")
        val stored = File(Documents.directory, "Alerts/${media.id}")
        waitFor { stored.exists() }
        assertEquals(listOf<Any?>("audio/*"), contentTypes())
        assertEquals("ID3", stored.readText())
        assertFalse(File(inbox, "Horn.mp3").exists())
        assertNoSheet()
    }

    @Test
    fun cancelledAlertPickerStoresNothingAndClosesTheSheet() {
        val media = SettingsAlertsMediaGalleryItem(name = "My sound")
        show { CustomSoundView(model = model, media = media) }
        pick(null, "Select sound")
        assertEquals(1, results.inputs.size)
        assertFalse(File(Documents.directory, "Alerts/${media.id}").exists())
        assertNoSheet()
    }

    @Test
    fun alertVideoIsStoredInAlertVideosUnderTheAlertId() {
        val alert = SettingsWidgetAlertsAlert()
        alert.mediaType = SettingsWidgetAlertsAlertMediaType.video
        show { Form(title = "Root") { AlertMediaView(model = model, alert = alert) } }
        rule.onAllNodesWithText("Video")[1].performClick()
        rule.waitForIdle()
        pick(PickerDocuments.add("Alert video.mp4", "video"), "Select video")
        val stored = File(Documents.directory, "Alerts/Videos/${alert.id}.mp4")
        waitFor { stored.exists() }
        assertEquals(listOf<Any?>("video/*"), contentTypes())
        assertEquals("Alert video.mp4", alert.videoName)
        assertEquals("video", stored.readText())
        assertFalse(File(inbox, "Alert video.mp4").exists())
    }

    @Test
    fun cubeLutIsWrittenToImagesUnderItsId() {
        val color = model.database.color
        val before = color.diskLutsCube.size
        show { CameraSettingsLutsView(model = model, color = color) }
        results.result = PickerDocuments.add("Film.cube", "LUT_3D_SIZE 2")
        rule.onAllNodesWithText("Add")[0].performClick()
        rule.waitForIdle()
        waitFor { color.diskLutsCube.size == before + 1 }
        assertEquals(listOf<Any?>("*/*"), contentTypes())
        val lut = color.diskLutsCube.last()
        assertEquals("LUT_3D_SIZE 2", File(Documents.directory, "Images/${lut.id}").readText())
        assertNoSheet()
    }

    @Test
    fun pngTuberModelIsStoredUnderTheWidgetId() {
        val pngTuber = SettingsWidgetPngTuber()
        var selected = 0
        show { Form(title = "Root") { WidgetPngTuberPickerView(model = model, pngTuber = pngTuber, onSelected = { selected += 1 }) } }
        pick(PickerDocuments.add("Avatar.save", "{\"layers\":[]}"), "Select model")
        waitFor { selected == 1 }
        assertEquals(listOf<Any?>("*/*"), contentTypes())
        assertEquals("Avatar.save", pngTuber.modelName)
        assertEquals("{\"layers\":[]}", File(Documents.directory, "PNGTuber/${pngTuber.id}").readText())
        assertFalse(File(inbox, "Avatar.save").exists())
        assertNoSheet()
    }

    @Test
    fun vTuberLive2DModelIsUnzippedAndAVrmModelReplacesIt() {
        val vTuber = SettingsWidgetVTuber()
        var selected = 0
        show { Form(title = "Root") { WidgetVTuberPickerView(model = model, vTuber = vTuber, onSelected = { selected += 1 }) } }
        val archive = zip(
            "Hiyori/Hiyori.model3.json" to "{}",
            "Hiyori/textures/texture_00.png" to "png",
            "__MACOSX/Hiyori/._Hiyori.model3.json" to "junk",
        )
        pick(PickerDocuments.add("Hiyori.zip", archive), "Select model")
        waitFor { selected == 1 }
        val stored = File(Documents.directory, "VTuber/${vTuber.id}")
        assertEquals(SettingsWidgetVTuberType.live2D, vTuber.type)
        assertEquals("Hiyori.zip", vTuber.modelName)
        assertEquals("{}", File(stored, "Hiyori/Hiyori.model3.json").readText())
        assertEquals("png", File(stored, "Hiyori/textures/texture_00.png").readText())
        assertFalse(File(stored, "__MACOSX").exists())
        assertNoSheet()
        pick(PickerDocuments.add("Avatar.vrm", "glTF"), "Hiyori.zip")
        waitFor { selected == 2 }
        assertEquals(SettingsWidgetVTuberType.vrm, vTuber.type)
        assertEquals("Avatar.vrm", vTuber.modelName)
        assertTrue(stored.isFile)
        assertEquals("glTF", stored.readText())
        assertFalse(File(inbox, "Avatar.vrm").exists())
        pick(PickerDocuments.add("Hiyori.zip", archive), "Avatar.vrm")
        waitFor { selected == 3 }
        assertTrue(stored.isDirectory)
        assertEquals("{}", File(stored, "Hiyori/Hiyori.model3.json").readText())
        model.vTuberStorage.remove(id = vTuber.id)
        assertFalse(stored.exists())
        model.vTuberStorage.remove(id = vTuber.id)
        assertFalse(stored.exists())
    }

    @Test
    fun mediaPlayerVideoIsMovedToMediasUnderTheFileId() {
        val player = SettingsMediaPlayer(name = "Player")
        val players = SettingsMediaPlayers(listOf(player))
        show { Form(title = "Root") { MediaPlayerSettingsView(model = model, mediaPlayers = players, player = player) } }
        rule.onNodeWithText("Player").performClick()
        rule.waitForIdle()
        pick(PickerDocuments.add("Holiday.mp4", "mp4"), "Add")
        waitFor { player.playlist.size == 1 }
        assertIs<PickVisualMediaRequest>(results.inputs.single())
        val stored = File(Documents.directory, "Medias/${player.playlist.single().id}.mp4")
        assertEquals("mp4", stored.readText())
        assertFalse(File(context.cacheDir, "Holiday.mp4").exists())
    }

    @Test
    fun widgetImageIsWrittenToImagesUnderTheWidgetId() {
        val widget = SettingsWidget(name = "Image")
        show {
            Form(title = "Root") {
                WidgetImagePickerView(model = model, widget = widget, image = null, onImageChange = {}, sizeScale = 6.0)
            }
        }
        pick(PickerDocuments.add("Logo.png", byteArrayOf(1, 2, 3)), "Select image")
        val stored = File(Documents.directory, "Images/${widget.id}")
        waitFor { stored.exists() }
        assertIs<PickVisualMediaRequest>(results.inputs.single())
        assertContentEquals(byteArrayOf(1, 2, 3), stored.readBytes())
    }

    @Test
    fun recordingFolderIsStoredAsABookmark() {
        DocumentTrees.setUp()
        File(DocumentTrees.root, "Movies/Moblin").mkdirs()
        val recording = SettingsStreamRecording()
        val tree = DocumentTrees.treeUri("Movies/Moblin")
        show { RecordingPathFormView(recording = recording, model = model) }
        pick(tree, "Select")
        assertEquals(listOf<Any?>(null), results.inputs)
        val bookmark = assertNotNull(recording.recordingPath)
        assertEquals(tree.toString(), String(bookmark))
        assertEquals(tree, context.contentResolver.persistedUriPermissions.single().uri)
        assertEquals(File(DocumentTrees.root, "Movies/Moblin").path, makeRecordingPath(bookmark))
        assertNotNull(Bookmark.resolve(bookmark))
        assertNoSheet()
        pick(
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3AMovies"),
            File(DocumentTrees.root, "Movies/Moblin").path,
        )
        assertContentEquals(bookmark, recording.recordingPath)
    }

    @Test
    fun replayStingerIsCopiedOffTheMainThreadAndStoredUnderItsId() {
        model.database.showAllSettings = true
        val stream = SettingsStream(name = "Stream")
        val replay = stream.replay
        replay.transitionType = SettingsStreamReplayTransitionType.stingers
        show { StreamReplaySettingsView(model = model, database = model.database, stream = stream, replay = replay) }
        rule.onNodeWithText("In video").performScrollTo().performClick()
        rule.waitForIdle()
        pick(PickerDocuments.add("My stinger.mov", "stinger"), "Select video")
        waitFor { replay.inStinger.name == "My stinger.mov" }
        assertEquals(listOf<Any?>("video/*"), contentTypes())
        val stored = File(Documents.directory, "ReplayTransitions/${replay.inStinger.id}.mov")
        assertEquals("stinger", stored.readText())
        assertFalse(File(inbox, "My stinger.mov").exists())
        assertNoSheet()
    }

    @Test
    fun settingsArchiveIsCopiedToTheInboxBeforeTheImportIsConfirmed() {
        show { Form(title = "Root") { ImportSettingsView(model = model) } }
        val archive = zip("settings.json" to "{}", "Images/00000000-0000-0000-0000-000000000009" to "image")
        pick(PickerDocuments.add("Backup.moblinSettings", archive), "Import from file")
        waitFor { model.presentingSettingsImportConfirmation.value }
        assertEquals(1, results.inputs.size)
        assertEquals(listOf<Any?>("*/*"), contentTypes())
        assertNotNull(model.pendingSettingsImportAction)
        assertContentEquals(archive, File(inbox, "Backup.moblinSettings").readBytes())
        assertNoSheet()
    }
}
