package com.moblin.android.view.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextEditNavigationViewSuite {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun submittingReturnsToTheParentPage() {
        val submitted = mutableListOf<String>()
        rule.setContent {
            var name by remember { mutableStateOf("Foo") }
            Form(title = "Root") {
                Section {
                    TextEditNavigationView(
                        title = "Name",
                        value = name,
                        onSubmit = {
                            submitted.add(it)
                            name = it
                        },
                    )
                }
            }
        }
        rule.onNodeWithText("Name").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement(" Bar ")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        assertEquals(listOf("Bar"), submitted)
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        rule.onNodeWithText("Bar").assertExists()
    }
}
