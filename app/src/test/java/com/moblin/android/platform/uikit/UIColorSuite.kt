package com.moblin.android.platform.uikit

import androidx.compose.ui.graphics.Color
import com.moblin.android.various.model.ShowingPanel
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class UIColorSuite {
    private val light = Color(0xFFF2F2F7)
    private val dark = Color(0xFF1C1C1E)

    @Test
    fun secondarySystemBackgroundIsLightInLightMode() {
        assertEquals(light, UIColor.secondarySystemBackground)
        assertEquals(light, ShowingPanel.settings.buttonsBackgroundColor())
        assertEquals(Color.Black, ShowingPanel.chat.buttonsBackgroundColor())
    }

    @Test
    @Config(qualifiers = "night")
    fun secondarySystemBackgroundIsDarkInDarkMode() {
        assertEquals(dark, UIColor.secondarySystemBackground)
        assertEquals(dark, ShowingPanel.settings.buttonsBackgroundColor())
        assertEquals(Color.Black, ShowingPanel.chat.buttonsBackgroundColor())
    }

    @Test
    fun secondarySystemBackgroundFollowsTheSystemThemeWhileRunning() {
        assertEquals(light, UIColor.secondarySystemBackground)
        RuntimeEnvironment.setQualifiers("+night")
        assertEquals(dark, UIColor.secondarySystemBackground)
        RuntimeEnvironment.setQualifiers("+notnight")
        assertEquals(light, UIColor.secondarySystemBackground)
    }
}
