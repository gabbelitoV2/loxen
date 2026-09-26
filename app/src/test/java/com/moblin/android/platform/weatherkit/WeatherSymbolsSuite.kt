package com.moblin.android.platform.weatherkit

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeatherSymbolsSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun painterColorFilters(node: LayoutInfo): List<ColorFilter?> {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val own = node.getModifierInfo()
            .map { it.modifier }
            .filter { it.javaClass.name.contains("Painter") }
            .map { modifier ->
                val field = modifier.javaClass.declaredFields.first { it.name == "colorFilter" }
                field.isAccessible = true
                field.get(modifier) as ColorFilter?
            }
        return own + children.flatMap { painterColorFilters(it as LayoutInfo) }
    }

    private fun renderedTints(name: String, tint: Color): List<ColorFilter?> {
        rule.setContent {
            Row(modifier = Modifier.testTag("symbols")) {
                SystemImage(name = name, fontSize = 20.sp, tint = tint)
            }
        }
        return painterColorFilters(rule.onNodeWithTag("symbols").fetchSemanticsNode().layoutInfo)
    }

    @Test
    fun filledWeatherSymbolsWithoutTintUseTheirMulticolorColor() {
        assertEquals(listOf(ColorFilter.tint(Color(0xFFFFCC00))), renderedTints("sun.max.fill", Color.Unspecified))
    }

    @Test
    fun rainSymbolsWithoutTintAreBlue() {
        assertEquals(listOf(ColorFilter.tint(Color(0xFF5AC8FA))), renderedTints("cloud.rain.fill", Color.Unspecified))
    }

    @Test
    fun anExplicitTintIsKept() {
        assertEquals(listOf(ColorFilter.tint(Color.Red)), renderedTints("cloud.sun", Color.Red))
    }
}
