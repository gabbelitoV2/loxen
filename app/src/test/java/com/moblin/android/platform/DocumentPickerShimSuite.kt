package com.moblin.android.platform

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.moblin.android.AppDelegate
import com.moblin.android.MessageQueue
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.runMainTest
import com.moblin.android.various.utils.moblinSettingsFileType
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentPickerShimSuite {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context
        get() = AppDelegate.context

    private val inbox: File
        get() = File(context.cacheDir, "Inbox")

    @Before
    fun setUp() {
        PickerDocuments.setUp()
    }

    @Test
    fun contentTypesAreMimeTypes() {
        assertContentEquals(arrayOf("image/gif"), DocumentPicker.contentTypes("gif"))
        assertContentEquals(arrayOf("audio/*"), DocumentPicker.contentTypes("audio"))
        assertContentEquals(arrayOf("*/*"), DocumentPicker.contentTypes("item"))
        assertContentEquals(arrayOf("video/*"), DocumentPicker.contentTypes("movie"))
        assertContentEquals(arrayOf("image/*"), DocumentPicker.contentTypes("public.image"))
        assertContentEquals(arrayOf("application/zip"), DocumentPicker.contentTypes("zip"))
        assertContentEquals(arrayOf("video/*"), DocumentPicker.contentTypes("video/*"))
        assertContentEquals(arrayOf("*/*"), DocumentPicker.contentTypes(moblinSettingsFileType))
        assertContentEquals(
            arrayOf("image/gif", "*/*"),
            DocumentPicker.contentTypes("com.compuserve.gif", "public.item", "data"),
        )
    }

    @Test
    fun copyStoresThePickedDocumentInTheInboxUnderItsName() {
        val file = DocumentPicker.copy(context, PickerDocuments.add("My alert.gif", "gif"))
        assertEquals(File(inbox, "My alert.gif").canonicalFile, file.canonicalFile)
        assertEquals("gif", file.readText())
        val again = DocumentPicker.copy(context, PickerDocuments.add("My alert.gif", "other"))
        assertEquals(file.canonicalFile, again.canonicalFile)
        assertEquals("other", again.readText())
    }

    @Test
    fun copyWithAHostileOrMissingNameStaysInTheInbox() {
        val hostile = DocumentPicker.copy(context, PickerDocuments.add("../../shared_prefs/x.xml", "hostile"))
        assertEquals(inbox.canonicalFile, hostile.canonicalFile.parentFile)
        assertEquals(".._.._shared_prefs_x.xml", hostile.name)
        assertFalse(File(context.cacheDir.parentFile, "shared_prefs/x.xml").exists())
        val unnamed = DocumentPicker.copy(context, PickerDocuments.add(null, "unnamed".toByteArray(), id = "primary:Sounds/alert.mp3"))
        assertEquals(File(inbox, "alert.mp3").canonicalFile, unnamed.canonicalFile)
        assertEquals("unnamed", unnamed.readText())
    }

    @Test
    fun copyOfAMissingDocumentThrows() {
        assertFailsWith<FileNotFoundException> {
            DocumentPicker.copy(context, PickerDocuments.missing())
        }
    }

    @Test
    fun pickedCopyIsDeliveredAsAPathOnTheMainThread() {
        runMainTest {
            val urls = MessageQueue<Pair<String, Looper?>>()
            DocumentPicker.copy(PickerDocuments.add("Stinger 1.mov", "movie")) { url ->
                urls.put(url to Looper.myLooper())
            }
            val (url, looper) = urls.get()
            assertSame(Looper.getMainLooper(), looper)
            assertEquals(File(inbox, "Stinger 1.mov").invariantSeparatorsPath, url)
            assertEquals("Stinger 1.mov", url.substringAfterLast('/'))
            assertEquals("movie", File(url).readText())
        }
    }

    @Test
    fun nothingIsDeliveredWhenNothingWasPickedOrTheCopyFails() {
        runMainTest {
            var calls = 0
            DocumentPicker.copy(null) { calls += 1 }
            DocumentPicker.copy(PickerDocuments.missing()) { calls += 1 }
            val urls = MessageQueue<String>()
            DocumentPicker.copy(PickerDocuments.add("ok.gif", "ok")) { urls.put(it) }
            urls.get()
            delay(200)
            assertEquals(0, calls)
        }
    }

    @Test
    fun loadDataReadsThePickedPhoto() {
        runMainTest {
            assertContentEquals(byteArrayOf(1, 2, 3), DocumentPicker.loadData(PickerDocuments.add("photo.png", byteArrayOf(1, 2, 3))))
            assertNull(DocumentPicker.loadData(PickerDocuments.missing()))
        }
    }

    @Test
    fun readInputReadsThePickedPhotoAndNeverThrows() {
        val input = assertNotNull(DocumentPicker.readInput(context, PickerDocuments.add("photo.png", byteArrayOf(4, 5))))
        assertContentEquals(byteArrayOf(4, 5), input.use { it.readBytes() })
        assertNull(DocumentPicker.readInput(context, PickerDocuments.missing()))
    }

    @Test
    fun loadMovieCopiesIntoTheTemporaryDirectory() {
        runMainTest {
            val urls = MessageQueue<Pair<String?, Looper?>>()
            DocumentPicker.loadMovie(PickerDocuments.add("clip.mp4", "clip")) { url ->
                urls.put(url to Looper.myLooper())
            }
            val (url, looper) = urls.get()
            assertSame(Looper.getMainLooper(), looper)
            assertEquals(File(context.cacheDir, "clip.mp4").invariantSeparatorsPath, url)
            assertEquals("clip", File(assertNotNull(url)).readText())
            DocumentPicker.loadMovie(PickerDocuments.missing()) { urls.put(it to Looper.myLooper()) }
            val (missing, missingLooper) = urls.get()
            assertNull(missing)
            assertSame(Looper.getMainLooper(), missingLooper)
        }
    }

    @Test
    fun inboxOfAnEarlierRunIsEmptiedOnFirstUse() {
        val cache = File(context.cacheDir, "earlier-run").also { it.mkdirs() }
        val earlierRun = object : ContextWrapper(context) {
            override fun getCacheDir(): File = cache
        }
        val stale = File(cache, "Inbox/Model.zip")
        stale.parentFile?.mkdirs()
        stale.writeText("stale")
        val first = DocumentPicker.copy(earlierRun, PickerDocuments.add("first.gif", "first"))
        assertFalse(stale.exists())
        assertEquals(File(cache, "Inbox/first.gif").canonicalFile, first.canonicalFile)
        val second = DocumentPicker.copy(earlierRun, PickerDocuments.add("second.gif", "second"))
        assertEquals("first", first.readText())
        assertEquals("second", second.readText())
    }

    @Test
    fun failedCopyKeepsWhatWasThere() {
        val directory = File(DocumentPicker.inboxDirectory(context), "taken.gif")
        File(directory, "inner").also { it.parentFile?.mkdirs() }.writeText("inner")
        assertFailsWith<FileNotFoundException> {
            DocumentPicker.copy(context, PickerDocuments.add("taken.gif", "new"))
        }
        assertTrue(directory.isDirectory)
        assertEquals("inner", File(directory, "inner").readText())
    }

    private fun presentPicker(results: FakeActivityResults, inSheet: Boolean, onResult: (Uri?) -> Unit): () -> Boolean {
        var presented by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
                val picker = @androidx.compose.runtime.Composable {
                    val launcher = DocumentPicker.rememberLauncher(ActivityResultContracts.OpenDocument()) { uri ->
                        onResult(uri)
                    }
                    LaunchedEffect(Unit) {
                        launcher.launch(DocumentPicker.contentTypes("gif"))
                    }
                }
                if (inSheet) {
                    Sheet(isPresented = presented, onDismissRequest = { presented = false }) {
                        picker()
                    }
                } else if (presented) {
                    picker()
                }
            }
        }
        rule.waitForIdle()
        return { presented }
    }

    @Test
    fun pickedDocumentDismissesThePickerSheet() {
        val uri = PickerDocuments.add("a.gif", "a")
        val results = FakeActivityResults(uri)
        var picked: Uri? = null
        val presented = presentPicker(results, inSheet = true) { picked = it }
        assertEquals(uri, picked)
        assertFalse(presented())
        assertEquals(1, results.inputs.size)
        assertEquals(listOf<Any?>("image/gif"), (results.inputs.single() as Array<*>).toList())
        rule.onAllNodes(isDialog()).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
    }

    @Test
    fun sheetDismissedBeforeItHasAppearedDoesNotComeBack() {
        var presented by mutableStateOf(true)
        var appearances = 0
        rule.setContent {
            Sheet(isPresented = presented, onDismissRequest = { presented = false }) {
                DisposableEffect(Unit) {
                    appearances += 1
                    onDispose {}
                }
                LaunchedEffect(Unit) {
                    presented = false
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, appearances)
        assertFalse(presented)
        assertTrue(rule.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun cancelledPickerDismissesThePickerSheet() {
        var called = false
        val presented = presentPicker(FakeActivityResults(null), inSheet = true) {
            called = true
            assertNull(it)
        }
        assertTrue(called)
        assertFalse(presented())
    }

    @Test
    fun pickerOutsideASheetOnlyDeliversTheResult() {
        val uri = PickerDocuments.add("b.gif", "b")
        var picked: Uri? = null
        val presented = presentPicker(FakeActivityResults(uri), inSheet = false) { picked = it }
        assertEquals(uri, picked)
        assertTrue(presented())
    }
}
