package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsGameController
import com.moblin.android.view.controlbar.quickbutton.chat.NavigationLinkView
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameControllerGlyphsSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun vectorNames(node: LayoutInfo): List<String> {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val own = node.getModifierInfo()
            .map { it.modifier }
            .filter { it.javaClass.name.contains("Painter") }
            .mapNotNull { modifier ->
                val field = modifier.javaClass.declaredFields.first { it.name == "painter" }
                field.isAccessible = true
                val painter = field.get(modifier) as? VectorPainter ?: return@mapNotNull null
                painter.javaClass.getMethod("getName\$ui_release").invoke(painter) as String
            }
        return own + children.flatMap { vectorNames(it as LayoutInfo) }
    }

    private fun rowVectorNames(text: String): List<String> {
        return vectorNames(rule.onAllNodesWithText(text).onFirst().fetchSemanticsNode().layoutInfo)
            .filter { it != "Filled.ChevronRight" }
    }

    @Test
    fun controllerSettingsShowTheGlyphOfEachButton() {
        val model = Model()
        val gameController = SettingsGameController()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                GameControllersControllerSettingsView(model = model, gameController = gameController)
            }
        }
        val buttons = gameController.buttons.value
        assertEquals(20, buttons.size)
        for (button in buttons) {
            val names = rowVectorNames(button.text)
            assertEquals(listOf(button.name), names, button.text)
            assertFalse("Filled.RadioButtonUnchecked" in names, button.text)
        }
    }

    @Test
    fun raidChannelRowShowsPlayTv() {
        rule.setContent {
            Form {
                NavigationLinkView(text = "Raid channel", image = "play.tv") {}
            }
        }
        val names = rowVectorNames("Raid channel")
        assertTrue("Filled.OndemandVideo" in names, names.toString())
        assertFalse("Filled.RadioButtonUnchecked" in names)
    }
}
