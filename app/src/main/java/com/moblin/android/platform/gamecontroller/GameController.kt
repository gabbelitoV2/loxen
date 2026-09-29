package com.moblin.android.platform.gamecontroller

import android.app.Activity
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.Window
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.NotificationCenter
import kotlin.math.abs
import kotlin.math.max

private const val TAG = "GCController"
private const val pressedThreshold = 0.5f
private const val sony = 0x054C
private const val nintendo = 0x057E
private const val microsoft = 0x045E

class GCControllerAxisInput internal constructor() {
    var value: Float = 0f
        internal set
}

class GCControllerButtonInput internal constructor(val sfSymbolsName: String?) {
    var value: Float = 0f
        private set
    var isPressed: Boolean = false
        private set
    var pressedChangedHandler: ((GCControllerButtonInput, Float, Boolean) -> Unit)? = null
    var valueChangedHandler: ((GCControllerButtonInput, Float, Boolean) -> Unit)? = null
    private var keyDown = false
    private var axis = 0f

    internal fun setKey(down: Boolean) {
        keyDown = down
        update()
    }

    internal fun setAxis(value: Float) {
        axis = value
        update()
    }

    private fun update() {
        val newValue = if (keyDown) 1f else axis
        val newPressed = keyDown || axis > pressedThreshold
        if (newValue == value && newPressed == isPressed) {
            return
        }
        val pressedChanged = newPressed != isPressed
        value = newValue
        isPressed = newPressed
        valueChangedHandler?.invoke(this, value, isPressed)
        if (pressedChanged) {
            pressedChangedHandler?.invoke(this, value, isPressed)
        }
    }
}

class GCControllerDirectionPad internal constructor(up: String?, down: String?, left: String?, right: String?) {
    val up = GCControllerButtonInput(up)
    val down = GCControllerButtonInput(down)
    val left = GCControllerButtonInput(left)
    val right = GCControllerButtonInput(right)
    val xAxis = GCControllerAxisInput()
    val yAxis = GCControllerAxisInput()
    var valueChangedHandler: ((GCControllerDirectionPad, Float, Float) -> Unit)? = null

    internal fun set(x: Float, y: Float) {
        if (x == xAxis.value && y == yAxis.value) {
            return
        }
        xAxis.value = x
        yAxis.value = y
        valueChangedHandler?.invoke(this, x, y)
        left.setAxis(max(0f, -x))
        right.setAxis(max(0f, x))
        up.setAxis(max(0f, y))
        down.setAxis(max(0f, -y))
    }
}

private class ButtonSymbols(
    val a: String,
    val b: String,
    val x: String,
    val y: String,
    val leftShoulder: String,
    val rightShoulder: String,
    val leftTrigger: String,
    val rightTrigger: String,
)

private fun buttonSymbols(vendorId: Int): ButtonSymbols = when (vendorId) {
    sony -> ButtonSymbols(
        "xmark.circle", "circle.circle", "square.circle", "triangle.circle",
        "l1.rectangle.roundedbottom", "r1.rectangle.roundedbottom",
        "l2.rectangle.roundedtop", "r2.rectangle.roundedtop",
    )
    nintendo -> ButtonSymbols(
        "b.circle", "a.circle", "y.circle", "x.circle",
        "l.rectangle.roundedbottom", "r.rectangle.roundedbottom",
        "zl.rectangle.roundedtop", "zr.rectangle.roundedtop",
    )
    microsoft -> ButtonSymbols(
        "a.circle", "b.circle", "x.circle", "y.circle",
        "lb.rectangle.roundedbottom", "rb.rectangle.roundedbottom",
        "lt.rectangle.roundedtop", "rt.rectangle.roundedtop",
    )
    else -> ButtonSymbols(
        "a.circle", "b.circle", "x.circle", "y.circle",
        "l1.rectangle.roundedbottom", "r1.rectangle.roundedbottom",
        "l2.rectangle.roundedtop", "r2.rectangle.roundedtop",
    )
}

class GCExtendedGamepad internal constructor(vendorId: Int) {
    private val symbols = buttonSymbols(vendorId)
    val dpad = GCControllerDirectionPad("dpad.up.fill", "dpad.down.fill", "dpad.left.fill", "dpad.right.fill")
    val leftThumbstick = GCControllerDirectionPad(null, null, null, null)
    val rightThumbstick = GCControllerDirectionPad(null, null, null, null)
    val buttonA = GCControllerButtonInput(symbols.a)
    val buttonB = GCControllerButtonInput(symbols.b)
    val buttonX = GCControllerButtonInput(symbols.x)
    val buttonY = GCControllerButtonInput(symbols.y)
    val buttonMenu = GCControllerButtonInput("line.horizontal.3.circle")
    val buttonOptions: GCControllerButtonInput? = GCControllerButtonInput(null)
    val buttonHome: GCControllerButtonInput? = GCControllerButtonInput(null)
    val leftShoulder = GCControllerButtonInput(symbols.leftShoulder)
    val rightShoulder = GCControllerButtonInput(symbols.rightShoulder)
    val leftTrigger = GCControllerButtonInput(symbols.leftTrigger)
    val rightTrigger = GCControllerButtonInput(symbols.rightTrigger)
    val leftThumbstickButton: GCControllerButtonInput? = GCControllerButtonInput(null)
    val rightThumbstickButton: GCControllerButtonInput? = GCControllerButtonInput(null)

    internal fun button(keyCode: Int): GCControllerButtonInput? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> buttonA
        KeyEvent.KEYCODE_BUTTON_B -> buttonB
        KeyEvent.KEYCODE_BUTTON_X -> buttonX
        KeyEvent.KEYCODE_BUTTON_Y -> buttonY
        KeyEvent.KEYCODE_BUTTON_L1 -> leftShoulder
        KeyEvent.KEYCODE_BUTTON_R1 -> rightShoulder
        KeyEvent.KEYCODE_BUTTON_L2 -> leftTrigger
        KeyEvent.KEYCODE_BUTTON_R2 -> rightTrigger
        KeyEvent.KEYCODE_BUTTON_START -> buttonMenu
        KeyEvent.KEYCODE_BUTTON_SELECT -> buttonOptions
        KeyEvent.KEYCODE_BUTTON_MODE -> buttonHome
        KeyEvent.KEYCODE_BUTTON_THUMBL -> leftThumbstickButton
        KeyEvent.KEYCODE_BUTTON_THUMBR -> rightThumbstickButton
        KeyEvent.KEYCODE_DPAD_UP -> dpad.up
        KeyEvent.KEYCODE_DPAD_DOWN -> dpad.down
        KeyEvent.KEYCODE_DPAD_LEFT -> dpad.left
        KeyEvent.KEYCODE_DPAD_RIGHT -> dpad.right
        else -> null
    }
}

internal class GCControllerAxes(
    val rightX: Int = MotionEvent.AXIS_Z,
    val rightY: Int = MotionEvent.AXIS_RZ,
    val leftTriggers: IntArray = intArrayOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE),
    val rightTriggers: IntArray = intArrayOf(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS),
    val flats: Map<Int, Float> = emptyMap(),
) {
    fun stick(event: MotionEvent, axis: Int): Float {
        val value = event.getAxisValue(axis)
        return if (abs(value) <= (flats[axis] ?: 0f)) 0f else value
    }

    fun trigger(event: MotionEvent, axes: IntArray): Float = axes.maxOf { event.getAxisValue(it) }
}

class GCController internal constructor(
    internal val deviceId: Int,
    val vendorName: String?,
    val extendedGamepad: GCExtendedGamepad?,
    private val axes: GCControllerAxes,
) {
    private fun handleKeyEvent(event: KeyEvent): Boolean {
        val button = extendedGamepad?.button(event.keyCode) ?: return KeyEvent.isGamepadButton(event.keyCode)
        if (event.repeatCount == 0) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> button.setKey(true)
                KeyEvent.ACTION_UP -> button.setKey(false)
            }
        }
        return true
    }

    private fun handleMotionEvent(event: MotionEvent) {
        val gamepad = extendedGamepad ?: return
        gamepad.dpad.set(event.getAxisValue(MotionEvent.AXIS_HAT_X), 0f - event.getAxisValue(MotionEvent.AXIS_HAT_Y))
        gamepad.leftThumbstick.set(axes.stick(event, MotionEvent.AXIS_X), 0f - axes.stick(event, MotionEvent.AXIS_Y))
        gamepad.rightThumbstick.set(axes.stick(event, axes.rightX), 0f - axes.stick(event, axes.rightY))
        gamepad.leftTrigger.setAxis(axes.trigger(event, axes.leftTriggers))
        gamepad.rightTrigger.setAxis(axes.trigger(event, axes.rightTriggers))
    }

    companion object {
        const val didConnectNotification = "GCControllerDidConnect"
        const val didDisconnectNotification = "GCControllerDidDisconnect"
        private val handler = Handler(Looper.getMainLooper())
        private val connected = LinkedHashMap<Int, GCController>()
        private var listener: InputManager.InputDeviceListener? = null
        private var discoveryCompletionHandler: (() -> Unit)? = null

        fun controllers(): List<GCController> = connected.values.toList()

        fun startWirelessControllerDiscovery(completionHandler: () -> Unit) {
            discoveryCompletionHandler = completionHandler
            if (listener != null) {
                return
            }
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    update(deviceId)
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    disconnect(deviceId)
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    update(deviceId)
                }
            }
            this.listener = listener
            try {
                AppDelegate.context.getSystemService(InputManager::class.java)
                    ?.registerInputDeviceListener(listener, handler)
            } catch (error: RuntimeException) {
                Log.i(TAG, "Failed to observe input devices: ${error.message}")
            }
            handler.post {
                for (deviceId in InputDevice.getDeviceIds()) {
                    update(deviceId)
                }
            }
        }

        fun stopWirelessControllerDiscovery() {
            val completionHandler = discoveryCompletionHandler ?: return
            discoveryCompletionHandler = null
            completionHandler()
        }

        fun install(activity: Activity) {
            install(activity.window ?: return)
        }

        fun install(window: Window) {
            val callback = window.callback ?: return
            if (callback is GameControllerWindowCallback) {
                return
            }
            window.callback = GameControllerWindowCallback(callback)
        }

        private fun isController(device: InputDevice): Boolean {
            return !device.isVirtual &&
                (device.supportsSource(InputDevice.SOURCE_GAMEPAD) || device.supportsSource(InputDevice.SOURCE_JOYSTICK))
        }

        private fun update(deviceId: Int) {
            val device = InputDevice.getDevice(deviceId)
            if (device == null || !isController(device)) {
                disconnect(deviceId)
                return
            }
            if (connected.containsKey(deviceId)) {
                return
            }
            val hasGamepad = device.supportsSource(InputDevice.SOURCE_GAMEPAD) &&
                device.hasKeys(KeyEvent.KEYCODE_BUTTON_A).firstOrNull() == true
            connect(deviceId, device.name, device.vendorId, hasGamepad, axes(device))
        }

        private fun axes(device: InputDevice): GCControllerAxes {
            fun range(axis: Int) = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK)
            val z = range(MotionEvent.AXIS_Z)
            val rightStickOnZ = (z != null && z.min < 0f) || range(MotionEvent.AXIS_RX) == null
            val rightX = if (rightStickOnZ) MotionEvent.AXIS_Z else MotionEvent.AXIS_RX
            val rightY = if (rightStickOnZ) MotionEvent.AXIS_RZ else MotionEvent.AXIS_RY
            val leftTriggers = mutableListOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
            val rightTriggers = mutableListOf(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
            if (!rightStickOnZ && z != null) {
                leftTriggers.add(MotionEvent.AXIS_Z)
                rightTriggers.add(MotionEvent.AXIS_RZ)
            }
            val flats = listOf(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, rightX, rightY).associateWith { axis ->
                range(axis)?.flat ?: 0f
            }
            return GCControllerAxes(rightX, rightY, leftTriggers.toIntArray(), rightTriggers.toIntArray(), flats)
        }

        internal fun connect(
            deviceId: Int,
            vendorName: String?,
            vendorId: Int,
            hasGamepad: Boolean,
            axes: GCControllerAxes = GCControllerAxes(),
        ): GCController {
            val controller = GCController(
                deviceId = deviceId,
                vendorName = vendorName,
                extendedGamepad = if (hasGamepad) GCExtendedGamepad(vendorId) else null,
                axes = axes,
            )
            connected[deviceId] = controller
            NotificationCenter.default.post(didConnectNotification, controller)
            return controller
        }

        internal fun disconnect(deviceId: Int) {
            val controller = connected.remove(deviceId) ?: return
            NotificationCenter.default.post(didDisconnectNotification, controller)
        }

        internal fun dispatchKeyEvent(event: KeyEvent): Boolean {
            val controller = connected[event.deviceId] ?: return false
            return controller.handleKeyEvent(event)
        }

        internal fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
                return false
            }
            val controller = connected[event.deviceId] ?: return false
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                controller.handleMotionEvent(event)
            }
            return true
        }

        internal fun reset() {
            listener?.let { listener ->
                runCatching {
                    AppDelegate.context.getSystemService(InputManager::class.java)?.unregisterInputDeviceListener(listener)
                }
            }
            listener = null
            discoveryCompletionHandler = null
            connected.clear()
        }
    }
}

private class GameControllerWindowCallback(private val wrapped: Window.Callback) : Window.Callback by wrapped {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return GCController.dispatchKeyEvent(event) || wrapped.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return GCController.dispatchGenericMotionEvent(event) || wrapped.dispatchGenericMotionEvent(event)
    }

    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>?, menu: Menu?, deviceId: Int) {
        wrapped.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        wrapped.onPointerCaptureChanged(hasCapture)
    }
}
