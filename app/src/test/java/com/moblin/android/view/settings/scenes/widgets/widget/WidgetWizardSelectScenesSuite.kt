package com.moblin.android.view.settings.scenes.widgets.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.scenes.widgets.WidgetsSettingsView
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetWizardSelectScenesSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun images(node: LayoutInfo): Int {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val painters = node.getModifierInfo().count { it.modifier.javaClass.name.contains("Painter") }
        return painters + children.sumOf { images(it as LayoutInfo) }
    }

    private fun checkmarks(sceneName: String): Int {
        return images(rule.onNodeWithText(sceneName).fetchSemanticsNode().layoutInfo)
    }

    private fun setContent(content: @Composable (Model) -> Unit) {
        val model = Model()
        val database = model.database
        database.scenes.first { it.name == "Back" }.name = "Rear"
        model.sceneSelector.selectedSceneId = database.scenes.first { it.name == "Front" }.id
        model.createWidgetWizard.reset()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                Form {
                    content(model)
                }
            }
        }
    }

    private fun assertSelectedSceneIsCheckedAndTapToggles() {
        assertEquals(1, checkmarks("Front"))
        assertEquals(0, checkmarks("Rear"))
        rule.onNodeWithText("Rear").performClick()
        rule.waitForIdle()
        assertEquals(1, checkmarks("Rear"))
        rule.onNodeWithText("Front").performClick()
        rule.waitForIdle()
        assertEquals(0, checkmarks("Front"))
    }

    @Test
    fun selectScenes() {
        setContent { model ->
            WidgetWizardSelectScenesNavigationView(
                database = model.database,
                createWidgetWizard = model.createWidgetWizard,
                presentingCreateWizard = true,
                onPresentingCreateWizardChange = {},
            )
        }
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        assertSelectedSceneIsCheckedAndTapToggles()
    }

    @Test
    fun selectScenesInCreateWizardSheet() {
        setContent { model ->
            WidgetsSettingsView(database = model.database)
        }
        rule.onNodeWithText("Create").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        assertSelectedSceneIsCheckedAndTapToggles()
    }
}
