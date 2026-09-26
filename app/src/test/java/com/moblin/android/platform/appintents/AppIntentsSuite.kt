package com.moblin.android.platform.appintents

import android.app.Application
import android.content.Intent
import android.content.res.XmlResourceParser
import android.os.Looper
import com.moblin.android.R
import com.moblin.android.intents.MoblinShortcuts
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

private const val androidNamespace = "http://schemas.android.com/apk/res/android"

private class AppIntentsCounter {
    var count = 0
}

private class AppIntentsCountIntent : AppIntent {
    override suspend fun perform(): IntentResult {
        counter.count += 1
        return IntentResult.result()
    }

    private val counter: AppIntentsCounter by Dependency<AppIntentsCounter>()
}

private object AppIntentsTestShortcuts : AppShortcutsProvider {
    override val appShortcuts: List<AppShortcut> = listOf(
        AppShortcut(
            intent = AppIntentsCountIntent(),
            phrases = listOf("${AppShortcutPhraseToken.applicationName}, count"),
            shortTitle = "Count",
            systemImageName = "plus",
        ),
    )
}

@RunWith(RobolectricTestRunner::class)
class AppIntentsSuite {
    private lateinit var application: Application

    private class StaticShortcut(val id: String, val label: String, val action: String, val targetClass: String)

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        AppDependencyManager.shared.reset()
    }

    @After
    fun tearDown() {
        AppDependencyManager.shared.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun staticShortcuts(): List<StaticShortcut> {
        val shortcuts = mutableListOf<StaticShortcut>()
        val parser = application.resources.getXml(R.xml.shortcuts)
        var id = ""
        var label = ""
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            if (parser.eventType != XmlResourceParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "shortcut" -> {
                    id = parser.getAttributeValue(androidNamespace, "shortcutId")
                    val labelId = parser.getAttributeResourceValue(androidNamespace, "shortcutShortLabel", 0)
                    label = application.getString(labelId)
                }
                "intent" -> shortcuts.add(
                    StaticShortcut(
                        id = id,
                        label = label,
                        action = parser.getAttributeValue(androidNamespace, "action"),
                        targetClass = parser.getAttributeValue(androidNamespace, "targetClass"),
                    ),
                )
            }
        }
        return shortcuts
    }

    @Test
    fun aShortcutRunsItsIntentOnceItsDependencyIsAdded() {
        val intent = Intent(AppShortcuts.action(AppIntentsTestShortcuts.appShortcuts.single()))
        assertTrue(AppShortcuts.perform(AppIntentsTestShortcuts, intent))
        runMain()
        val counter = AppIntentsCounter()
        AppDependencyManager.shared.add(dependency = counter)
        runMain()
        assertEquals(1, counter.count)
        assertTrue(AppShortcuts.perform(AppIntentsTestShortcuts, intent))
        runMain()
        assertEquals(2, counter.count)
    }

    @Test
    fun otherIntentsAreNotShortcuts() {
        assertFalse(AppShortcuts.perform(AppIntentsTestShortcuts, Intent(Intent.ACTION_MAIN)))
        assertFalse(AppShortcuts.perform(AppIntentsTestShortcuts, null))
        assertTrue(AppShortcuts.perform(AppIntentsTestShortcuts, Intent(AppShortcuts.actionPrefix + "Removed")))
    }

    @Test
    fun theLauncherShortcutsAreMoblinsAppShortcuts() {
        val shortcuts = staticShortcuts()
        assertEquals(MoblinShortcuts.appShortcuts.map { AppShortcuts.action(it) }, shortcuts.map { it.action })
        assertEquals(MoblinShortcuts.appShortcuts.map { it.shortTitle }, shortcuts.map { it.label })
        assertEquals(MoblinShortcuts.appShortcuts.map { it.intent.javaClass.simpleName }, shortcuts.map { it.id })
        assertTrue(shortcuts.all { it.targetClass == "com.moblin.android.MainActivity" })
        assertEquals(ShortcutTileColor.navy, MoblinShortcuts.shortcutTileColor)
    }
}
