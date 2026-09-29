package com.moblin.android.platform.gamecontroller

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.window.DialogWindowProvider
import com.moblin.android.platform.swiftui.Sheet
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GCControllerPresentationSuite {
    @get:Rule
    val rule = createComposeRule()

    private val controllerId = 7

    @Before
    fun setUp() {
        GCController.reset()
    }

    @After
    fun tearDown() {
        GCController.reset()
    }

    private fun key(action: Int, keyCode: Int): KeyEvent {
        return KeyEvent(0, 0, action, keyCode, 0, 0, controllerId, 0, 0, InputDevice.SOURCE_GAMEPAD)
    }

    private fun thumbstick(x: Float): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_UNKNOWN
        }
        val coords = MotionEvent.PointerCoords().apply {
            setAxisValue(MotionEvent.AXIS_X, x)
        }
        return MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_MOVE, 1, arrayOf(properties), arrayOf(coords),
            0, 0, 1f, 1f, controllerId, 0, InputDevice.SOURCE_JOYSTICK, 0,
        )
    }

    @Test
    fun aControllerStillWorksAndCannotDismissOrNavigateWhileASheetIsShown() {
        val presented = mutableStateOf(true)
        var window: Window? = null
        rule.setContent {
            Sheet(isPresented = presented.value, onDismissRequest = { presented.value = false }) {
                val view = LocalView.current
                SideEffect {
                    window = (view.parent as? DialogWindowProvider)?.window
                }
                Text("Sheet")
            }
        }
        rule.waitForIdle()
        val sheetWindow = assertNotNull(window)
        val gamepad = assertNotNull(GCController.connect(controllerId, "Pad", 0x045E, true).extendedGamepad)
        val presses = mutableListOf<Boolean>()
        gamepad.buttonA.pressedChangedHandler = { _, _, pressed -> presses.add(pressed) }
        val sticks = mutableListOf<Float>()
        gamepad.leftThumbstick.valueChangedHandler = { _, x, _ -> sticks.add(x) }
        val decorView = sheetWindow.decorView
        for (keyCode in listOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_DPAD_DOWN)) {
            assertTrue(decorView.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, keyCode)))
            assertTrue(decorView.dispatchKeyEvent(key(KeyEvent.ACTION_UP, keyCode)))
        }
        assertTrue(decorView.dispatchGenericMotionEvent(thumbstick(0.5f)))
        rule.waitForIdle()
        assertEquals(listOf(true, false), presses)
        assertEquals(listOf(0.5f), sticks)
        assertTrue(presented.value)
        rule.onNodeWithText("Sheet").assertExists()
        decorView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        decorView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        rule.waitForIdle()
        assertFalse(presented.value)
    }
}
