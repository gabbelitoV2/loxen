package com.moblin.android.view.utils

import android.graphics.Typeface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.uikit.InstalledFontFace
import com.moblin.android.platform.uikit.InstalledFonts
import com.moblin.android.various.settings.SettingsFont
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private fun face(family: String, fontName: String, weight: Int = 400) = InstalledFontFace(
    familyName = family,
    fontName = fontName,
    fullName = fontName,
    styleName = "",
    weight = weight,
    italic = false,
    width = 100f,
    makeTypeface = { Typeface.DEFAULT },
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontPickerViewsSuite {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        InstalledFonts.override = InstalledFonts(
            listOf(
                face("Alpha Sans", "AlphaSans-Bold", weight = 700),
                face("Alpha Sans", "AlphaSans-Regular"),
                face("Beta Serif", "BetaSerif-Regular"),
            ),
        )
    }

    @After
    fun tearDown() {
        InstalledFonts.override = null
    }

    @Test
    fun choosingAFamilyPicksItsRegularStyle() {
        val font = mutableStateOf(SettingsFont())
        var changes = 0
        rule.setContent {
            Form(title = "Root") {
                Section {
                    FontSettingsView(font = font, onChange = { changes += 1 })
                }
            }
        }
        rule.onNodeWithText("System").assertExists()
        rule.onNodeWithText("Family").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Beta Serif").assertExists()
        rule.onNodeWithText("Alpha Sans").performClick()
        rule.waitForIdle()
        assertEquals(SettingsFont(family = "Alpha Sans", style = "AlphaSans-Regular"), font.value)
        assertEquals(1, changes)
        rule.onNodeWithText("System").performClick()
        rule.waitForIdle()
        assertEquals(SettingsFont(), font.value)
        assertEquals(2, changes)
    }

    @Test
    fun choosingAStyle() {
        val font = mutableStateOf(SettingsFont(family = "Alpha Sans", style = "AlphaSans-Regular"))
        rule.setContent {
            Form(title = "Root") {
                Section {
                    FontSettingsView(font = font, onChange = {})
                }
            }
        }
        rule.onNodeWithText("Alpha Sans").assertExists()
        rule.onNodeWithText("Style").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Bold").performClick()
        rule.waitForIdle()
        assertEquals(SettingsFont(family = "Alpha Sans", style = "AlphaSans-Bold"), font.value)
    }

    @Test
    fun aFamilyWithOneStyleCannotOpenTheStylePicker() {
        val font = mutableStateOf(SettingsFont(family = "Beta Serif", style = "BetaSerif-Regular"))
        rule.setContent {
            Form(title = "Root") {
                Section {
                    FontSettingsView(font = font, onChange = {})
                }
            }
        }
        rule.onNodeWithText("Style").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Root").assertExists()
        rule.onNodeWithText("Family").assertExists()
    }
}
