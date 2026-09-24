package com.moblin.android.platform.swiftui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h800dp")
class NavigationBarSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun open(width: Dp) {
        rule.setContent {
            Box(modifier = Modifier.width(width)) {
                Form(title = "QR code widget") {
                    Section {
                        NavigationLink(title = "Message") {
                            Form(title = "Message") {}
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("Message").performClick()
        rule.waitForIdle()
    }

    @Test
    fun narrowBarShowsBack() {
        open(350.dp)
        rule.onNodeWithText("Back").assertExists()
        rule.onNodeWithText("QR code widget").assertDoesNotExist()
    }

    @Test
    fun wideBarShowsParentTitle() {
        open(800.dp)
        rule.onNodeWithText("QR code widget").assertExists()
        rule.onNodeWithText("Back").assertDoesNotExist()
    }
}
