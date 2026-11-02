package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun makeWidget(name: String, enabled: Boolean = true): SettingsWidget {
    val widget = SettingsWidget(name = name)
    widget.enabled = enabled
    return widget
}

private fun makeSceneWidget(
    name: String,
    scene: SettingsScene,
    enabled: Boolean = true,
): SettingsWidget {
    val widget = makeWidget(name, enabled)
    widget.type = SettingsWidgetType.scene
    widget.scene.sceneId = scene.id
    return widget
}

private fun makeScene(name: String, widgets: List<SettingsWidget>): SettingsScene {
    val scene = SettingsScene(name = name)
    scene.widgets = widgets.map { SettingsSceneWidget(widgetId = it.id) }.toMutableList()
    return scene
}

@RunWith(RobolectricTestRunner::class)
class SceneResolverSuite {
    @Test
    fun nestedScene() {
        val text = makeWidget("Text")
        val image = makeWidget("Image")
        val nested = makeScene("Nested", listOf(image))
        val sceneWidget = makeSceneWidget("Scene", nested)
        val main = makeScene("Main", listOf(text, sceneWidget))
        val resolver = SceneResolver(listOf(main, nested), listOf(text, image, sceneWidget))
        val resolved = resolver.resolve(main)
        assertEquals(listOf("Text", "Scene", "Image"), resolved.widgets.map { it.widget.name })
        assertEquals(listOf("Main", "Main", "Nested"), resolved.widgets.map { it.scene.name })
        assertEquals(setOf(main.id, nested.id), resolved.sceneIds)
    }

    @Test
    fun disabledSceneWidget() {
        val image = makeWidget("Image")
        val nested = makeScene("Nested", listOf(image))
        val sceneWidget = makeSceneWidget("Scene", nested, enabled = false)
        val main = makeScene("Main", listOf(sceneWidget))
        val resolver = SceneResolver(listOf(main, nested), listOf(image, sceneWidget))
        assertEquals(listOf("Scene"), resolver.resolve(main).widgets.map { it.widget.name })
        assertTrue(resolver.resolve(main).drawnWidgets().isEmpty())
        val resolved = resolver.resolve(main, includeDisabledSceneWidgets = true)
        assertEquals(listOf("Scene", "Image"), resolved.widgets.map { it.widget.name })
        assertEquals(listOf(false, false), resolved.widgets.map { it.enabled })
        assertTrue(resolved.drawnWidgets().isEmpty())
    }

    @Test
    fun disabledNestedSceneWidgetDisablesItsContent() {
        val image = makeWidget("Image")
        val innerScene = makeScene("Inner", listOf(image))
        val innerWidget = makeSceneWidget("Inner scene", innerScene, enabled = false)
        val outerScene = makeScene("Outer", listOf(innerWidget))
        val outerWidget = makeSceneWidget("Outer scene", outerScene)
        val main = makeScene("Main", listOf(outerWidget))
        val resolver = SceneResolver(
            scenes = listOf(main, outerScene, innerScene),
            widgets = listOf(image, innerWidget, outerWidget),
        )
        val resolved = resolver.resolve(main, includeDisabledSceneWidgets = true)
        assertEquals(
            listOf("Outer scene", "Inner scene", "Image"),
            resolved.widgets.map { it.widget.name },
        )
        assertEquals(listOf(true, false, false), resolved.widgets.map { it.enabled })
        assertEquals(listOf("Outer scene"), resolved.drawnWidgets().map { it.widget.name })
    }

    @Test
    fun duplicatedWidgetIsDrawnOnce() {
        val text = makeWidget("Text")
        val nested = makeScene("Nested", listOf(text))
        val sceneWidget = makeSceneWidget("Scene", nested)
        val main = makeScene("Main", listOf(text, sceneWidget))
        val resolver = SceneResolver(listOf(main, nested), listOf(text, sceneWidget))
        val resolved = resolver.resolve(main)
        assertEquals(listOf(true, true, false), resolved.widgets.map { it.first })
        assertEquals(listOf("Main", "Main"), resolved.firstWidgets().map { it.scene.name })
        assertEquals(mapOf(text.id to 1), resolved.duplicatedWidgetCounts())
    }

    @Test
    fun cyclicScenes() {
        val first = makeScene("First", emptyList())
        val second = makeScene("Second", emptyList())
        val firstWidget = makeSceneWidget("First", first)
        val secondWidget = makeSceneWidget("Second", second)
        first.widgets = mutableListOf(SettingsSceneWidget(widgetId = secondWidget.id))
        second.widgets = mutableListOf(SettingsSceneWidget(widgetId = firstWidget.id))
        val resolver = SceneResolver(listOf(first, second), listOf(firstWidget, secondWidget))
        assertEquals(listOf("Second", "First"), resolver.resolve(first).widgets.map { it.widget.name })
    }

    @Test
    fun remoteWidget() {
        val text = makeWidget("Text")
        val remoteImage = makeWidget("Remote image")
        val remoteScene = makeScene("Remote", listOf(remoteImage))
        val remoteWidget = makeSceneWidget("Remote scene", remoteScene)
        val main = makeScene("Main", listOf(text))
        val resolver = SceneResolver(
            scenes = listOf(main, remoteScene),
            widgets = listOf(text, remoteImage, remoteWidget),
            remoteWidgetId = remoteWidget.id,
        )
        assertEquals(listOf("Text"), resolver.resolve(main).widgets.map { it.widget.name })
        val resolved = resolver.resolve(main, includeRemote = true)
        assertEquals(
            listOf("Text", "Remote scene", "Remote image"),
            resolved.widgets.map { it.widget.name },
        )
        assertEquals(listOf(false, true, true), resolved.widgets.map { it.remote })
        assertEquals(listOf("Text"), resolved.localDrawnWidgets().map { it.widget.name })
    }

    @Test
    fun addableWidgets() {
        val text = makeWidget("Text")
        val image = makeWidget("Image")
        val nested = makeScene("Nested", listOf(image))
        val sceneWidget = makeSceneWidget("Scene", nested)
        val main = makeScene("Main", listOf(text, sceneWidget))
        val other = makeScene("Other", emptyList())
        val resolver = SceneResolver(
            scenes = listOf(main, nested, other),
            widgets = listOf(text, image, sceneWidget),
        )
        assertTrue(resolver.addableWidgets(main).isEmpty())
        assertTrue(resolver.addableWidgets(nested).isEmpty())
        assertEquals(listOf("Text", "Image", "Scene"), resolver.addableWidgets(other).map { it.name })
    }

    @Test
    fun addableWidgetsWithOldDuplicates() {
        val text = makeWidget("Text")
        val image = makeWidget("Image")
        val main = makeScene("Main", listOf(text, text))
        val resolver = SceneResolver(listOf(main), listOf(text, image))
        assertEquals(listOf("Image"), resolver.addableWidgets(main).map { it.name })
    }

    @Test
    fun selectableScenes() {
        val text = makeWidget("Text")
        val image = makeWidget("Image")
        val withText = makeScene("With text", listOf(text))
        val withImage = makeScene("With image", listOf(image))
        val sceneWidget = makeSceneWidget("Scene", withImage)
        val main = makeScene("Main", listOf(text, sceneWidget))
        val resolver = SceneResolver(
            scenes = listOf(main, withText, withImage),
            widgets = listOf(text, image, sceneWidget),
        )
        assertEquals(
            listOf("Main", "With image"),
            resolver.selectableScenes(sceneWidget).map { it.name },
        )
    }
}
