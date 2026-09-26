package com.moblin.android.platform.swiftui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResult
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.platform.Documents
import com.moblin.android.platform.FakeActivityResults
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareSheetSuite {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("txt", "text/plain")
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypeMapping("mp4", "video/mp4")
    }

    private fun assumeFileProviderPaths() {
        assumeTrue("FileProvider only matches paths with / separators", File.separatorChar == '/')
    }

    private fun stream(intent: Intent): Uri {
        @Suppress("DEPRECATION")
        return assertNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))
    }

    private fun sharedText(uri: Uri): String {
        return assertNotNull(application.contentResolver.openInputStream(uri)).use { it.readBytes().decodeToString() }
    }

    private fun startedChooserTarget(): Intent {
        val chooser = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        return assertNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT))
    }

    @Test
    fun aFileIsSharedAsAContentUriThatTheReceiverMayRead() {
        assumeFileProviderPaths()
        val file = File(application.cacheDir, "Moblin-log-1.txt")
        file.writeText("Version: 1")
        val intent = assertNotNull(ShareSheet.intent(application, listOf(file)))
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        val uri = stream(intent)
        assertEquals("content", uri.scheme)
        assertEquals("com.moblin.android.fileprovider", uri.authority)
        assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
        assertTrue((intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals("Version: 1", sharedText(uri))
    }

    @Test
    fun aPathToAnExistingFileIsSharedAsThatFile() {
        assumeFileProviderPaths()
        val recordings = File(Documents.directory, "Recordings")
        recordings.mkdirs()
        val recording = File(recordings, "2026-09-26.mp4")
        recording.writeText("video")
        val intent = assertNotNull(ShareSheet.intent(application, listOf(recording.absolutePath)))
        assertEquals("video/mp4", intent.type)
        assertNull(intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("video", sharedText(stream(intent)))
        val fileUrl = assertNotNull(ShareSheet.intent(application, listOf(recording.toURI())))
        assertEquals(stream(intent), stream(fileUrl))
    }

    @Test
    fun otherTextIsSharedAsText() {
        val command = "ffmpeg -i input.mp4 -c copy output.mp4"
        val intent = assertNotNull(ShareSheet.intent(application, listOf(command)))
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(command, intent.getStringExtra(Intent.EXTRA_TEXT))
        @Suppress("DEPRECATION")
        assertNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun aFileOutsideTheSharedFoldersIsCopiedBeforeItIsShared() {
        assumeFileProviderPaths()
        val file = File(application.filesDir, "SimpleStorage/secret.moblinSettings")
        file.parentFile?.mkdirs()
        file.writeText("settings")
        val intent = assertNotNull(ShareSheet.intent(application, listOf(file)))
        assertEquals("application/octet-stream", intent.type)
        assertEquals("settings", sharedText(stream(intent)))
        assertTrue(File(application.cacheDir, "Share/secret.moblinSettings").isFile)
    }

    @Test
    fun severalFilesAreSharedTogether() {
        assumeFileProviderPaths()
        val first = File(application.cacheDir, "a.txt").apply { writeText("a") }
        val second = File(application.cacheDir, "b.txt").apply { writeText("b") }
        val intent = assertNotNull(ShareSheet.intent(application, listOf(first, second)))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        @Suppress("DEPRECATION")
        val streams = assertNotNull(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals(listOf("a", "b"), streams.map { sharedText(it) })
        assertEquals(2, intent.clipData?.itemCount)
    }

    @Test
    fun aMissingFileSharesNothing() {
        assertNull(ShareSheet.intent(application, listOf(File(application.cacheDir, "missing.txt"))))
        assertFalse(ShareSheet.present(application, emptyList()))
    }

    @Test
    fun shareLinkOpensTheShareSheetWithItsItem() {
        val command = "ffmpeg -i input.mp4 -c copy output.mp4"
        rule.setContent {
            ShareLink(item = command) {
                Text("Share command")
            }
        }
        rule.onNodeWithText("Share command").performClick()
        rule.waitForIdle()
        val target = startedChooserTarget()
        assertEquals(Intent.ACTION_SEND, target.action)
        assertEquals(command, target.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun shareLinkSharesAnExportedFile() {
        assumeFileProviderPaths()
        val file = File(application.cacheDir, "Pixel_2026-09-26.moblinSettings").apply { writeText("zip") }
        rule.setContent {
            ShareLink(item = file.absolutePath) {
                Text("Export")
            }
        }
        rule.onNodeWithText("Export").performClick()
        rule.waitForIdle()
        val target = startedChooserTarget()
        assertEquals("application/octet-stream", target.type)
        assertEquals("zip", sharedText(stream(target)))
    }

    @Test
    fun activityViewControllerSharesAndThenClosesItsSheet() {
        val results = FakeActivityResults(ActivityResult(Activity.RESULT_CANCELED, null))
        var presented by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
                Sheet(isPresented = presented, onDismissRequest = { presented = false }) {
                    UIActivityViewController(activityItems = listOf("Log text"), applicationActivities = null)
                }
            }
        }
        rule.waitUntil(10_000) { !presented }
        val chooser = results.inputs.single() as Intent
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val target = assertNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertEquals("Log text", target.getStringExtra(Intent.EXTRA_TEXT))
    }
}
