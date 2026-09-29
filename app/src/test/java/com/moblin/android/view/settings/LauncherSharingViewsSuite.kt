package com.moblin.android.view.settings

import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.LocalModel
import com.moblin.android.platform.FileProviderRoots
import com.moblin.android.platform.loxen.Loxen
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.model.Icon
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.plainIcon
import com.moblin.android.view.settings.about.AboutSettingsView
import com.moblin.android.view.settings.importexport.ExportSettingsView
import com.moblin.android.view.settings.recordings.RecordingsSettingsView
import com.moblin.android.view.settings.store.StoreSettingsView
import com.moblin.android.view.utils.CommandCopyView
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
class LauncherSharingViewsSuite {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var application: Application
    private lateinit var model: Model

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        FileProviderRoots.forget()
        model = Model()
        model.media = Media(delegate = mediaDelegate())
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
            CompositionLocalProvider(LocalModel provides model) {
                content()
            }
        }
        rule.waitForIdle()
    }

    private fun installFilesApp() {
        val filesApp = ComponentName("com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")
        val packageManager = shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(filesApp)
        val filter = IntentFilter(Intent.ACTION_VIEW)
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addDataType(DocumentsContract.Document.MIME_TYPE_DIR)
        packageManager.addIntentFilterForActivity(filesApp, filter)
    }

    private fun clickArrowNextTo(text: String) {
        val label = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        val buttons = rule.onAllNodes(hasClickAction())
        val index = buttons.fetchSemanticsNodes().indexOfFirst {
            val bounds = it.boundsInRoot
            bounds.left >= label.right && bounds.center.y in label.top..label.bottom
        }
        assertTrue(index >= 0, "No button next to $text")
        buttons[index].performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun chooserTarget(): Intent {
        val chooser = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        return assertNotNull(chooser.getParcelableExtra(Intent.EXTRA_INTENT))
    }

    @Test
    fun theRecordingsDirectoryOpensInTheFilesApp() {
        installFilesApp()
        show { RecordingsSettingsView(model = model) }
        clickArrowNextTo("Default recordings directory")
        rule.waitForIdle()
        val started = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(DocumentsContract.Document.MIME_TYPE_DIR, started.type)
        assertEquals("Documents/Recordings", DocumentsContract.getDocumentId(started.data))
    }

    @Test
    fun withoutAFilesAppTheDirectoryIsCopied() {
        show { RecordingsSettingsView(model = model) }
        clickArrowNextTo("Replays directory")
        rule.waitForIdle()
        val clipboard = application.getSystemService(ClipboardManager::class.java)
        val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertEquals(model.replaysStorage.defaultStorageDirectory().toURI().path, copied)
        assertEquals("Directory copied to clipboard", model.toast.toast.value.title)
    }

    @Test
    fun theCommandShareButtonSharesTheCommand() {
        val command = "ffmpeg -i input.mp4 -c copy output.mp4"
        show { Form(title = "Help") { CommandCopyView(command = command) } }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        rule.waitForIdle()
        val target = chooserTarget()
        assertEquals("text/plain", target.type)
        assertEquals(command, target.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun exportSharesTheSettingsFile() {
        assumeTrue("FileProvider only matches paths with / separators", File.separatorChar == '/')
        show { ExportSettingsView(model = model) }
        rule.waitUntil(20_000) { rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)).fetchSemanticsNodes().any { node -> node.config[SemanticsProperties.Text].any { it.text == "Export" } } }
        rule.onNodeWithText("Export").performClick()
        rule.waitForIdle()
        val target = chooserTarget()
        assertEquals(Intent.ACTION_SEND, target.action)
        @Suppress("DEPRECATION")
        val uri = assertNotNull(target.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertEquals("content", uri.scheme)
        val bytes = assertNotNull(application.contentResolver.openInputStream(uri)).use { it.readBytes() }
        assertTrue(bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
    }

    @Test
    fun aboutCreditsMoblinAndListsTheLicense() {
        show { AboutSettingsView() }
        rule.onNodeWithText(Loxen.attribution).assertExists()
        rule.onNodeWithText("License").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(Loxen.license.trim(), substring = true).assertExists()
    }

    @Test
    fun theStoreShowsTheLoxenPageInsteadOfMoblinIcons() {
        model.store.myIcons.value = listOf(plainIcon, Icon(name = "San Diego", id = "AppIconSanDiego", price = "$"))
        show { StoreSettingsView(model = model, store = model.store) }
        rule.onNodeWithText(Loxen.noPurchases).assertExists()
        rule.onNodeWithText(Loxen.attribution).assertExists()
        assertTrue(rule.onAllNodesWithText("San Diego").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Restore purchases").fetchSemanticsNodes().isEmpty())
        assertEquals(plainIcon.id, model.database.iconImage)
    }
}
