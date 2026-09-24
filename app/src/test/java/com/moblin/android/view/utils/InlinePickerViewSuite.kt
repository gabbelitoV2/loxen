package com.moblin.android.view.utils

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InlinePickerViewSuite {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun selectingAnItemDismissesThePicker() {
        var selected = ""
        rule.setContent {
            Form(title = "Root") {
                Section {
                    NavigationLink(title = "Camera") {
                        InlinePickerView(
                            title = "Camera",
                            onChange = { selected = it },
                            items = listOf(InlinePickerItem(id = "a", text = "Back"), InlinePickerItem(id = "b", text = "Front")),
                            initialSelectedId = "a",
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("Camera").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Front").performClick()
        rule.waitForIdle()
        assertEquals("b", selected)
        rule.onNodeWithText("Front").assertDoesNotExist()
        rule.onNodeWithText("Root").assertExists()
    }
}
