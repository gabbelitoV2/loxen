package com.moblin.android.platform

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import com.moblin.android.intents.MoblinShortcuts
import com.moblin.android.intents.MuteIntent
import com.moblin.android.intents.UnmuteIntent
import com.moblin.android.platform.appintents.AppDependencyManager
import com.moblin.android.platform.appintents.AppShortcuts
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.setupAppIntents
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Proxy
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidHostIntentsSuite {
    private lateinit var application: Application
    private lateinit var model: Model

    private val inbox: File
        get() = File(application.cacheDir, "Inbox")

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        AppDependencyManager.shared.reset()
        PickerDocuments.setUp()
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        setBoundModel(model)
    }

    @After
    fun tearDown() {
        setBoundModel(null)
        AppDependencyManager.shared.reset()
    }

    private fun setBoundModel(model: Model?) {
        val field = AndroidHost::class.java.getDeclaredField("boundModel")
        field.isAccessible = true
        field.set(AndroidHost, model)
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

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000
        while (!condition()) {
            assertTrue(System.nanoTime() < deadline, "Timed out")
            Thread.sleep(10)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun shortcut(name: String): Intent {
        val shortcut = MoblinShortcuts.appShortcuts.first { it.intent.javaClass.simpleName == name }
        return Intent(AppShortcuts.action(shortcut))
    }

    @Test
    fun openingASettingsFileAsksToImportACopyOfIt() {
        val archive = zip("settings.json" to "{}")
        val uri = PickerDocuments.add("Pixel_2026-09-26.moblinSettings", archive)
        AndroidHost.handleIntent(application, Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/octet-stream"))
        waitFor { model.presentingSettingsImportConfirmation.value }
        assertNotNull(model.pendingSettingsImportAction)
        assertContentEquals(archive, File(inbox, "Pixel_2026-09-26.moblinSettings").readBytes())
    }

    @Test
    fun sharingASettingsFileToMoblinAsksToImportIt() {
        val uri = PickerDocuments.add("Backup.moblinSettings", zip("settings.json" to "{}"))
        val intent = Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM, uri)
        AndroidHost.handleIntent(application, intent)
        waitFor { model.presentingSettingsImportConfirmation.value }
        assertTrue(File(inbox, "Backup.moblinSettings").isFile)
    }

    @Test
    fun anIntentFromRecentsIsNotHandledAgain() {
        val uri = PickerDocuments.add("Old.moblinSettings", zip("settings.json" to "{}"))
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        AndroidHost.handleIntent(application, intent)
        Thread.sleep(200)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(model.presentingSettingsImportConfirmation.value)
        assertFalse(File(inbox, "Old.moblinSettings").exists())
    }

    @Test
    fun theManifestOffersMoblinForSettingsFiles() {
        val files = listOf(
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FPixel_2026.09.26.moblinSettings"),
            Uri.parse("file:///storage/emulated/0/Download/Backup.moblinSettings"),
        )
        for (uri in files) {
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/zip")
            val activities = application.packageManager.queryIntentActivities(intent, 0)
            assertTrue(activities.any { it.activityInfo.name == "com.moblin.android.MainActivity" }, uri.toString())
        }
        val send = Intent(Intent.ACTION_SEND).setType("application/octet-stream")
        assertTrue(application.packageManager.queryIntentActivities(send, 0).any { it.activityInfo.name == "com.moblin.android.MainActivity" })
    }

    @Test
    fun theMuteAndUnmuteShortcutsActOnTheRunningModel() {
        AndroidHost.handleIntent(application, shortcut(MuteIntent::class.java.simpleName))
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(model.audio.muted.value)
        model.setupAppIntents()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(model.audio.muted.value)
        AndroidHost.handleIntent(application, shortcut(UnmuteIntent::class.java.simpleName))
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(model.audio.muted.value)
        assertEquals(false, model.presentingSettingsImportConfirmation.value)
    }
}
