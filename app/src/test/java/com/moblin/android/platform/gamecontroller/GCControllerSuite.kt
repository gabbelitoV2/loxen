package com.moblin.android.platform.gamecontroller

import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.moblin.android.platform.core.Notification
import com.moblin.android.platform.core.NotificationCenter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class GCControllerSuite {
    private val notifications = mutableListOf<Notification>()
    private val presses = mutableListOf<Triple<String?, Float, Boolean>>()
    private val controllerId = 7

    @Before
    fun setUp() {
        GCController.reset()
        for (name in listOf(GCController.didConnectNotification, GCController.didDisconnectNotification)) {
            NotificationCenter.default.addObserver(this, name, null) { notifications.add(it) }
        }
    }

    @After
    fun tearDown() {
        NotificationCenter.default.removeObserver(this)
        GCController.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun connect(vendorId: Int = 0x045E, hasGamepad: Boolean = true): GCController {
        return GCController.connect(controllerId, "Pad", vendorId, hasGamepad)
    }

    private fun recordPresses(vararg buttons: GCControllerButtonInput) {
        for (button in buttons) {
            button.pressedChangedHandler = { input, value, pressed -> presses.add(Triple(input.sfSymbolsName, value, pressed)) }
        }
    }

    private fun key(action: Int, keyCode: Int, repeat: Int = 0, deviceId: Int = controllerId): KeyEvent {
        return KeyEvent(0, 0, action, keyCode, repeat, 0, deviceId, 0, 0, InputDevice.SOURCE_GAMEPAD)
    }

    private fun press(keyCode: Int, deviceId: Int = controllerId): Boolean {
        val down = GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, keyCode, deviceId = deviceId))
        val up = GCController.dispatchKeyEvent(key(KeyEvent.ACTION_UP, keyCode, deviceId = deviceId))
        return down && up
    }

    private fun motion(vararg axes: Pair<Int, Float>, deviceId: Int = controllerId): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_UNKNOWN
        }
        val coords = MotionEvent.PointerCoords().apply {
            for ((axis, value) in axes) {
                setAxisValue(axis, value)
            }
        }
        return MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_MOVE, 1, arrayOf(properties), arrayOf(coords),
            0, 0, 1f, 1f, deviceId, 0, InputDevice.SOURCE_JOYSTICK, 0,
        )
    }

    @Test
    fun connectAndDisconnectArePostedWithTheSameController() {
        val controller = connect()
        assertNotNull(controller.extendedGamepad)
        assertEquals(listOf(controller), GCController.controllers())
        GCController.disconnect(controllerId)
        assertEquals(
            listOf(GCController.didConnectNotification, GCController.didDisconnectNotification),
            notifications.map { it.name },
        )
        assertSame(controller, notifications[0].obj)
        assertSame(controller, notifications[1].obj)
        assertTrue(GCController.controllers().isEmpty())
        GCController.disconnect(controllerId)
        assertEquals(2, notifications.size)
    }

    @Test
    fun aJoystickWithoutGamepadButtonsHasNoExtendedGamepad() {
        val controller = connect(hasGamepad = false)
        assertNull(controller.extendedGamepad)
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun discoveryCanBeStartedTwiceAndStopCallsTheCompletionHandler() {
        var completed = 0
        GCController.startWirelessControllerDiscovery { completed += 1 }
        GCController.startWirelessControllerDiscovery { completed += 1 }
        runMain()
        assertEquals(0, completed)
        GCController.stopWirelessControllerDiscovery()
        GCController.stopWirelessControllerDiscovery()
        assertEquals(1, completed)
    }

    @Test
    fun buttonAIsPressedAndReleasedOnceWithTheXboxSymbol() {
        val gamepad = assertNotNull(connect().extendedGamepad)
        recordPresses(gamepad.buttonA)
        assertTrue(GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A)))
        assertTrue(GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, repeat = 1)))
        assertTrue(gamepad.buttonA.isPressed)
        assertTrue(GCController.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A)))
        assertEquals(listOf(Triple("a.circle", 1f, true), Triple("a.circle", 0f, false)), presses.toList())
        assertEquals("lb.rectangle.roundedbottom", gamepad.leftShoulder.sfSymbolsName)
        assertEquals("rt.rectangle.roundedtop", gamepad.rightTrigger.sfSymbolsName)
    }

    @Test
    fun sonyNintendoAndOtherControllersUseTheirButtonSymbols() {
        val sony = assertNotNull(GCController.connect(1, "DualSense", 0x054C, true).extendedGamepad)
        assertEquals(
            listOf("xmark.circle", "circle.circle", "square.circle", "triangle.circle"),
            listOf(sony.buttonA, sony.buttonB, sony.buttonX, sony.buttonY).map { it.sfSymbolsName },
        )
        assertEquals("l1.rectangle.roundedbottom", sony.leftShoulder.sfSymbolsName)
        assertEquals("r2.rectangle.roundedtop", sony.rightTrigger.sfSymbolsName)
        val nintendo = assertNotNull(GCController.connect(2, "Pro Controller", 0x057E, true).extendedGamepad)
        assertEquals(
            listOf("b.circle", "a.circle", "y.circle", "x.circle"),
            listOf(nintendo.buttonA, nintendo.buttonB, nintendo.buttonX, nintendo.buttonY).map { it.sfSymbolsName },
        )
        assertEquals("zl.rectangle.roundedtop", nintendo.leftTrigger.sfSymbolsName)
        val other = assertNotNull(GCController.connect(3, "Gamepad", 0x2DC8, true).extendedGamepad)
        assertEquals("a.circle", other.buttonA.sfSymbolsName)
        assertEquals("l1.rectangle.roundedbottom", other.leftShoulder.sfSymbolsName)
        assertEquals("r2.rectangle.roundedtop", other.rightTrigger.sfSymbolsName)
        assertEquals("dpad.left.fill", other.dpad.left.sfSymbolsName)
    }

    @Test
    fun theHatPressesAndReleasesTheDpadButtons() {
        val gamepad = assertNotNull(connect().extendedGamepad)
        recordPresses(gamepad.dpad.up, gamepad.dpad.down, gamepad.dpad.left, gamepad.dpad.right)
        assertTrue(GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_HAT_Y to -1f)))
        assertTrue(GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_HAT_Y to 0f)))
        assertTrue(GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_HAT_X to 1f)))
        assertEquals(
            listOf(
                Triple("dpad.up.fill", 1f, true),
                Triple("dpad.up.fill", 0f, false),
                Triple("dpad.right.fill", 1f, true),
            ),
            presses.toList(),
        )
        assertTrue(press(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(Triple("dpad.left.fill", 0f, false), presses.last())
    }

    @Test
    fun anAnalogTriggerIsPressedOnceEvenWhenItAlsoSendsAKey() {
        val gamepad = assertNotNull(connect().extendedGamepad)
        recordPresses(gamepad.rightTrigger)
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_RTRIGGER to 0.3f))
        assertTrue(presses.isEmpty())
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_RTRIGGER to 0.8f))
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R2))
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R2))
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_GAS to 0f))
        assertEquals(listOf(true, false), presses.map { it.third })
    }

    @Test
    fun thumbsticksReportOnlyChangesWithYUpAndIgnoreTheFlatZone() {
        GCController.connect(
            controllerId,
            "Pad",
            0x045E,
            true,
            GCControllerAxes(flats = mapOf(MotionEvent.AXIS_X to 0.1f, MotionEvent.AXIS_Y to 0.1f)),
        )
        val gamepad = assertNotNull(GCController.controllers().single().extendedGamepad)
        val values = mutableListOf<Pair<Float, Float>>()
        gamepad.leftThumbstick.valueChangedHandler = { _, x, y -> values.add(x to y) }
        val right = mutableListOf<Pair<Float, Float>>()
        gamepad.rightThumbstick.valueChangedHandler = { _, x, y -> right.add(x to y) }
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_X to 0.5f, MotionEvent.AXIS_Y to -0.25f))
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_X to 0.5f, MotionEvent.AXIS_Y to -0.25f))
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_X to 0.05f, MotionEvent.AXIS_Y to 0.05f))
        GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_Z to -1f))
        assertEquals(listOf(0.5f to 0.25f, 0f to 0f), values)
        assertEquals(listOf(-1f to 0f), right)
    }

    @Test
    fun onlyInputFromAConnectedControllerIsConsumed() {
        connect()
        assertFalse(press(KeyEvent.KEYCODE_BUTTON_A, deviceId = 99))
        assertFalse(press(KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(press(KeyEvent.KEYCODE_BACK))
        assertTrue(press(KeyEvent.KEYCODE_BUTTON_1))
        assertFalse(GCController.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_X to 1f, deviceId = 99)))
    }

    @Test
    fun theWindowCallbackIsWrappedOnceAndGamepadInputNeverReachesTheUi() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        var backPressed = false
        activity.onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed = true
                }
            },
        )
        GCController.install(activity)
        val wrapper = activity.window.callback
        GCController.install(activity)
        assertSame(wrapper, activity.window.callback)
        connect()
        assertTrue(wrapper.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B)))
        assertTrue(wrapper.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B)))
        assertTrue(wrapper.dispatchGenericMotionEvent(motion(MotionEvent.AXIS_HAT_X to 1f)))
        runMain()
        assertFalse(backPressed)
        assertFalse(wrapper.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, deviceId = 99)))
        controller.pause().stop().destroy()
    }
}
