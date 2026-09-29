package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import com.moblin.android.LocalModel
import com.moblin.android.various.model.Model
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
class StreamWizardGeneralSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun setContent(): Model {
        val model = Model()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                StreamWizardGeneralSettingsView(model = model, createStreamWizard = model.createStreamWizard)
            }
        }
        rule.waitForIdle()
        return model
    }

    @Test
    fun theStreamNameKeepsEveryCharacterAndEnablesCreate() {
        val model = setContent()
        rule.onNodeWithText("Create").assertIsNotEnabled()
        for (character in "My stream") {
            rule.onNode(hasSetTextAction()).performTextInput(character.toString())
            rule.waitForIdle()
        }
        rule.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("My stream")))
        assertEquals("My stream", model.createStreamWizard.name)
        rule.onNodeWithText("Create").assertIsEnabled()
    }

    @Test
    fun backgroundStreamingTogglesOnAndOff() {
        val model = setContent()
        val toggle = rule.onAllNodes(isToggleable())[0]
        toggle.assertIsOff()
        toggle.performClick()
        rule.waitForIdle()
        toggle.assertIsOn()
        assertTrue(model.createStreamWizard.backgroundStreaming)
        toggle.performClick()
        rule.waitForIdle()
        toggle.assertIsOff()
        assertFalse(model.createStreamWizard.backgroundStreaming)
    }
}
