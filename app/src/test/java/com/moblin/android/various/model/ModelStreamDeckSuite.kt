package com.moblin.android.various.model

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.moblin.android.LocalModel
import com.moblin.android.various.utils.isPad
import com.moblin.android.view.settings.SettingsView
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModelStreamDeckSuite {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = "xlarge")
    fun tabletsHideStreamDecksLikeAnIphoneBecauseAndroidCannotDriveOne() {
        assertTrue(isPad())
        val model = Model()
        model.updateIsStreamDeckDeviceDriverInstalled()
        assertFalse(model.streamDeck.isDeviceDriverInstalled.value)
        model.database.showAllSettings = true
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                SettingsView(model = model, database = model.database)
            }
        }
        rule.onNodeWithText("Keyboard").assertExists()
        rule.onNodeWithText("Game controllers").assertExists()
        rule.onAllNodesWithText("Stream decks").assertCountEquals(0)
    }
}
