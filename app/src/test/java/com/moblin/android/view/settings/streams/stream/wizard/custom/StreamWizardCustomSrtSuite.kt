package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.AnnotatedString
import com.moblin.android.LocalModel
import com.moblin.android.various.model.Model
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StreamWizardCustomSrtSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun setContent(content: @Composable (Model) -> Unit): Model {
        val model = Model()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                content(model)
            }
        }
        rule.waitForIdle()
        return model
    }

    private fun textField(index: Int) = rule.onAllNodes(hasSetTextAction())[index]

    private fun type(index: Int, text: String) {
        for (character in text) {
            textField(index).performTextInput(character.toString())
            rule.waitForIdle()
        }
    }

    private fun hasEditableText(text: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
    }

    private fun srtContent(): Model = setContent { model ->
        StreamWizardCustomSrtSettingsView(model = model, createStreamWizard = model.createStreamWizard)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun typingAnSrtUrlKeepsEveryCharacter() {
        val model = srtContent()
        val url = "srt://192.168.1.6:8891"
        type(0, url)
        textField(0).assert(hasEditableText(url))
        assertEquals(url, model.createStreamWizard.customSrtUrl)
        rule.onAllNodesWithText("Only srt and srtla allowed", substring = true).assertCountEquals(0)
        textField(0).performKeyInput { pressKey(Key.Backspace) }
        rule.waitForIdle()
        textField(0).assert(hasEditableText(url.dropLast(1)))
        assertEquals(url.dropLast(1), model.createStreamWizard.customSrtUrl)
    }

    @Test
    fun aWrongSchemeIsShownButDoesNotBlockTyping() {
        val model = srtContent()
        type(0, "rtmp://host")
        textField(0).assert(hasEditableText("rtmp://host"))
        assertEquals("rtmp://host", model.createStreamWizard.customSrtUrl)
        rule.onAllNodesWithText("Only srt and srtla allowed", substring = true).assertCountEquals(1)
    }

    @Test
    fun pastingAUrlKeepsItAndFillsInTheStreamId() {
        val model = srtContent()
        textField(0).performTextInput("srt://192.168.1.6:8891?streamid=publish:cam")
        rule.waitForIdle()
        textField(0).assert(hasEditableText("srt://192.168.1.6:8891?streamid=publish:cam"))
        assertEquals("publish:cam", model.createStreamWizard.customSrtStreamId)
        textField(1).assert(hasEditableText("publish:cam"))
        rule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun typingAStreamIdEnablesNext() {
        val model = srtContent()
        type(0, "srt://192.168.1.6:8891")
        rule.onNodeWithText("Next").assertIsNotEnabled()
        type(1, "publish:cam")
        textField(1).assert(hasEditableText("publish:cam"))
        assertEquals("publish:cam", model.createStreamWizard.customSrtStreamId)
        rule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun typingAnRtmpUrlAndStreamKeyKeepsEveryCharacter() {
        val model = setContent { model ->
            StreamWizardCustomRtmpSettingsView(model = model, createStreamWizard = model.createStreamWizard)
        }
        type(0, "rtmp://host/app")
        type(1, "live_123")
        textField(0).assert(hasEditableText("rtmp://host/app"))
        textField(1).assert(hasEditableText("live_123"))
        assertEquals("rtmp://host/app", model.createStreamWizard.customRtmpUrl)
        assertEquals("live_123", model.createStreamWizard.customRtmpStreamKey)
    }
}
