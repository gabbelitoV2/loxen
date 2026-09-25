package com.moblin.android.various.model

import android.os.Looper
import com.moblin.android.platform.coremotion.CMMotionManager
import com.moblin.android.platform.coremotion.FakeMotionSensors
import com.moblin.android.various.managers.GForceManager
import com.moblin.android.various.settings.SettingsChatBotCustomCommand
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.videoeffects.text.isGForceVariable
import com.moblin.android.videoeffects.text.loadTextFormat
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelGForceSuite {
    private lateinit var model: Model
    private lateinit var sensors: FakeMotionSensors
    private lateinit var motionManager: CMMotionManager
    private val format = "{gForce} {gForceRecentMax} {gForceMax}"
    private val scene = SettingsScene(name = "G-force")

    @Before
    fun setUp() {
        sensors = FakeMotionSensors(accelerometer = true)
        model = Model()
        runMain()
        val field = Model::class.java.getDeclaredField("motionManager")
        field.isAccessible = true
        motionManager = field.get(model) as CMMotionManager
    }

    @After
    fun tearDown() {
        model.gForceManager?.stop()
        model.stopMotionDetection()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createGForceManagerLikeSetup() {
        model.gForceManager = GForceManager(motionManager = motionManager)
    }

    private fun addTextWidget(formatString: String): SettingsWidget {
        val database = model.database
        if (database.scenes.none { it === scene }) {
            database.scenes.add(scene)
        }
        model.sceneSelector.selectedSceneId = scene.id
        val widget = SettingsWidget(name = "G-force")
        widget.text.formatString = formatString
        widget.text.needsGForce = loadTextFormat(format = formatString).isGForceVariable()
        database.widgets.add(widget)
        scene.widgets.add(SettingsSceneWidget(widgetId = widget.id))
        return widget
    }

    private fun send(z: Double, count: Int = 1) {
        repeat(count) {
            sensors.sendAcceleration(0.0, 0.0, z)
        }
        runMain()
    }

    @Test
    fun textWidgetShowsDashesWithoutAManagerAndGInsteadOfMetresPerSecondSquared() {
        assertNull(model.gForceManager)
        assertEquals("- - -", model.formatPlainText(format))
        createGForceManagerLikeSetup()
        addTextWidget(format)
        model.startGForceManager()
        send(-1.0)
        assertEquals("1.0 1.0 1.0", model.formatPlainText(format))
        send(-2.5)
        assertEquals("2.5 2.5 2.5", model.formatPlainText(format))
        send(-1.0, count = 50)
        assertEquals("1.0 2.3 2.5", model.formatPlainText(format))
        send(-1.0, count = 50)
        assertEquals("1.0 1.0 2.5", model.formatPlainText(format))
    }

    @Test
    fun accelerometerRunsOnlyWhileAnEnabledTextWidgetInTheSceneNeedsIt() {
        createGForceManagerLikeSetup()
        model.startGForceManager()
        assertTrue(sensors.accelerometerListeners().isEmpty())
        val clock = addTextWidget("{shortTime}")
        model.startGForceManager()
        assertTrue(sensors.accelerometerListeners().isEmpty())
        clock.enabled = false
        val widget = addTextWidget("Now {gForce} g")
        model.startGForceManager()
        val listener = sensors.accelerometerListeners().single()
        assertTrue(sensors.isRegistered(listener, sensors.accelerometerSensor))
        assertTrue(motionManager.isAccelerometerActive)
        model.startGForceManager()
        assertEquals(listener, sensors.accelerometerListeners().single())
        widget.enabled = false
        model.startGForceManager()
        assertTrue(sensors.accelerometerListeners().isEmpty())
        assertFalse(motionManager.isAccelerometerActive)
    }

    @Test
    fun chatBotCommandWithGForceStartsTheAccelerometer() {
        createGForceManagerLikeSetup()
        val chat = model.database.chat
        chat.botEnabled = true
        chat.customCommands.add(SettingsChatBotCustomCommand().apply { formatString = "Max {gForceMax} g" })
        model.chatBotCustomCommandsTextChanged()
        assertEquals(1, sensors.accelerometerListeners().size)
        chat.botEnabled = false
        model.startGForceManager()
        assertTrue(sensors.accelerometerListeners().isEmpty())
    }

    @Test
    fun tapToFocusMotionAndGForceShareModelsMotionManager() {
        createGForceManagerLikeSetup()
        addTextWidget("{gForce}")
        model.startGForceManager()
        model.startMotionDetection()
        assertTrue(motionManager.isDeviceMotionActive)
        assertTrue(motionManager.isAccelerometerActive)
        model.stopMotionDetection()
        assertFalse(motionManager.isDeviceMotionActive)
        assertTrue(motionManager.isAccelerometerActive)
        send(-1.5)
        assertEquals("1.5", model.formatPlainText("{gForce}"))
        model.startMotionDetection()
        model.gForceManager?.stop()
        assertTrue(motionManager.isDeviceMotionActive)
        assertFalse(motionManager.isAccelerometerActive)
        assertTrue(sensors.accelerometerListeners().isEmpty())
    }
}
