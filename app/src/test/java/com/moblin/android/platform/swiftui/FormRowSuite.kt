package com.moblin.android.platform.swiftui

import androidx.compose.material3.Text
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FormRowSuite {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toggleInNavigationLinkIsNotIndented() {
        rule.setContent {
            Form {
                Section {
                    Toggle(isOn = true, onChange = {}) {
                        Text("Plain")
                    }
                    NavigationLink(destination = {}) {
                        Toggle(isOn = true, onChange = {}) {
                            Text("Nested")
                        }
                    }
                }
            }
        }
        val plain = rule.onNodeWithText("Plain", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val nested = rule.onNodeWithText("Nested", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(plain.left, nested.left)
    }
}
