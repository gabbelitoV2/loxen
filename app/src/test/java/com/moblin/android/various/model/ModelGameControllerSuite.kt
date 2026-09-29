package com.moblin.android.various.model

import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import com.moblin.android.common.various.noValue
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.gamecontroller.GCController
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.settings.SettingsControllerFunction
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelGameControllerSuite {
    private val models = mutableListOf<Model>()

    @Before
    fun setUp() {
        SystemEventsState.reset()
        GCController.reset()
    }

    @After
    fun tearDown() {
        for (model in models) {
            NotificationCenter.default.removeObserver(model)
        }
        GCController.reset()
        SystemEventsState.reset()
    }

    private fun makeModel(): Model {
        val model = Model()
        model.media = Media(delegate = mediaDelegate())
        models.add(model)
        NotificationCenter.default.addObserver(model, GCController.didConnectNotification, null) {
            model.handleGameControllerDidConnect(it)
        }
        NotificationCenter.default.addObserver(model, GCController.didDisconnectNotification, null) {
            model.handleGameControllerDidDisconnect(it)
        }
        return model
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun key(action: Int, keyCode: Int, deviceId: Int): KeyEvent {
        return KeyEvent(0, 0, action, keyCode, 0, 0, deviceId, 0, 0, InputDevice.SOURCE_GAMEPAD)
    }

    private fun toastTitle(model: Model) = model.toast.toast.value.title

    private fun useButtonAForStreamDeckLayout(model: Model) {
        val button = model.database.gameControllers[0].buttons.value.first { it.name == "a.circle" }
        button.setFunction(SettingsControllerFunction.STREAM_DECK_LAYOUT)
    }

    @Test
    fun aConnectedControllerIsCountedAndToasted() {
        val model = makeModel()
        GCController.connect(1, "Pad", 0x045E, true)
        runMain()
        assertEquals("1", model.statusTopRight.gameControllersTotal.value)
        assertEquals("Game controller 1 connected", toastTitle(model))
        GCController.disconnect(1)
        runMain()
        assertEquals("0", model.statusTopRight.gameControllersTotal.value)
        assertEquals("Game controller 1 disconnected", toastTitle(model))
        GCController.connect(2, "Pad", 0x045E, true)
        runMain()
        assertEquals("1", model.statusTopRight.gameControllersTotal.value)
        assertEquals("Game controller 1 connected", toastTitle(model))
    }

    @Test
    fun aControllerWithoutExtendedGamepadIsIgnoredLikeIos() {
        val model = makeModel()
        GCController.connect(1, "Joystick", 0x1234, false)
        runMain()
        assertEquals(noValue, model.statusTopRight.gameControllersTotal.value)
    }

    @Test
    fun aButtonRunsItsFunctionWhenReleased() {
        val model = makeModel()
        useButtonAForStreamDeckLayout(model)
        val selected = UUID.randomUUID()
        model.database.streamDecks.setSelectedId(selected)
        GCController.connect(1, "Pad", 0x045E, true)
        runMain()
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 1))
        assertEquals(selected, model.database.streamDecks.selectedId.value)
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A, 1))
        assertNull(model.database.streamDecks.selectedId.value)
    }

    @Test
    fun aControllerWithoutSettingsIsCountedButItsButtonsDoNothing() {
        val model = makeModel()
        useButtonAForStreamDeckLayout(model)
        val selected = UUID.randomUUID()
        model.database.streamDecks.setSelectedId(selected)
        GCController.connect(1, "Pad", 0x045E, true)
        GCController.connect(2, "Pad", 0x045E, true)
        runMain()
        assertEquals("2", model.statusTopRight.gameControllersTotal.value)
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A, 2))
        GCController.dispatchKeyEvent(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A, 2))
        assertEquals(selected, model.database.streamDecks.selectedId.value)
    }
}
