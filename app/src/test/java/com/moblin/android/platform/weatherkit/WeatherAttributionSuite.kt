package com.moblin.android.platform.weatherkit

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeatherAttributionSuite {
    @get:Rule
    val rule = createComposeRule()

    private val footer =
        "[Weather data by Open-Meteo.com](https://open-meteo.com/) " +
            "([CC BY 4.0](https://creativecommons.org/licenses/by/4.0/))."

    @Test
    fun markdownLinksBecomeLinkAnnotations() {
        val text = weatherAttribution(text = footer, linkColor = Color.Blue)
        assertEquals("Weather data by Open-Meteo.com (CC BY 4.0).", text.text)
        val links = text.getLinkAnnotations(0, text.length).map {
            text.text.substring(it.start, it.end) to (it.item as LinkAnnotation.Url).url
        }
        assertEquals(
            listOf(
                "Weather data by Open-Meteo.com" to "https://open-meteo.com/",
                "CC BY 4.0" to "https://creativecommons.org/licenses/by/4.0/",
            ),
            links,
        )
        assertEquals(Color.Blue, (text.getLinkAnnotations(0, 1).first().item as LinkAnnotation.Url).styles?.style?.color)
    }

    @Test
    fun textWithoutLinksIsUnchanged() {
        val text = weatherAttribution(text = "No links here.", linkColor = Color.Blue)
        assertEquals("No links here.", text.text)
        assertEquals(0, text.getLinkAnnotations(0, text.length).size)
    }

    @Test
    fun footerShowsTheCreditAndOpensOpenMeteo() {
        val opened = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                opened.add(uri)
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                WeatherAttributionText(text = footer)
            }
        }
        val shownText = "Weather data by Open-Meteo.com (CC BY 4.0)."
        val node = rule.onNodeWithText(shownText)
        val config = node.fetchSemanticsNode().config
        val shown = config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
        assertEquals(2, shown?.getLinkAnnotations(0, shown.length)?.size)
        val layouts = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.first()
        node.performTouchInput { click(layout.getBoundingBox(3).center) }
        rule.waitForIdle()
        node.performTouchInput { click(layout.getBoundingBox(shownText.indexOf("CC BY") + 1).center) }
        rule.waitForIdle()
        assertEquals(listOf("https://open-meteo.com/", "https://creativecommons.org/licenses/by/4.0/"), opened)
    }
}
