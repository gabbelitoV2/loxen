package com.moblin.android.view.settings.scenes.widgets.widget.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationStack
import com.moblin.android.platform.translation.FakeTranslationBackend
import com.moblin.android.platform.translation.TranslationSystem
import com.moblin.android.platform.translation.downloadable
import com.moblin.android.platform.translation.notAvailable
import com.moblin.android.platform.translation.onDevice
import com.moblin.android.various.model.Model
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SubtitlesLanguagePickerSuite {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        TranslationSystem.backend = FakeTranslationBackend(
            capabilities = listOf(onDevice("en", "sv"), downloadable("en", "de"), notAvailable("en", "fi")),
        )
    }

    @After
    fun tearDown() {
        TranslationSystem.reset()
        Locale.setDefault(originalLocale)
    }

    @Test
    fun installedLanguagesCanBePickedAndOthersAreMarkedNotDownloaded() {
        val model = Model()
        var text = "Hi "
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                NavigationStack {
                    Form {
                        TextFormatVariablesView(widget = true, value = text, onChange = { text = it })
                    }
                }
            }
        }
        rule.onNodeWithText("Language").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("{subtitles:<language-identifier>}").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("German").assertExists()
        rule.onNodeWithText("Not downloaded").assertExists()
        assertEquals(0, rule.onAllNodesWithText("Finnish").fetchSemanticsNodes().size)
        rule.onNodeWithText("German").performClick()
        rule.waitForIdle()
        assertEquals("Hi ", text)
        rule.onNodeWithText("Swedish").performClick()
        rule.waitForIdle()
        assertEquals("Hi {subtitles:sv}", text)
        assertEquals(0, rule.onAllNodesWithText("Not downloaded").fetchSemanticsNodes().size)
    }
}
