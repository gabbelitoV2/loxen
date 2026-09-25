package com.moblin.android.various.model

import android.os.Looper
import android.os.PowerManager
import androidx.compose.ui.graphics.Color
import com.moblin.android.AppDelegate
import com.moblin.android.common.various.color
import com.moblin.android.common.various.string
import com.moblin.android.moblink.MoblinkThermalState
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.core.ProcessInfo
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.remotecontrol.RemoteControlStatusGeneralFlame
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.various.storages.ThermalState
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
class ModelThermalStateSuite {
    private lateinit var powerManager: ShadowPowerManager
    private val models = mutableListOf<Model>()

    @Before
    fun setUp() {
        SystemEventsState.reset()
        powerManager = shadowOf(AppDelegate.context.getSystemService(PowerManager::class.java))
    }

    @After
    fun tearDown() {
        for (model in models) {
            NotificationCenter.default.removeObserver(model)
        }
        SystemEventsState.reset()
    }

    private fun makeModel(): Model {
        val model = Model()
        model.media = Media(delegate = mediaDelegate())
        models.add(model)
        val setupThermalState = Model::class.java.getDeclaredMethod("setupThermalState")
        setupThermalState.isAccessible = true
        setupThermalState.invoke(model)
        runMain()
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

    private fun setThermalStatus(status: Int) {
        powerManager.setCurrentThermalStatus(status)
        runMain()
    }

    private fun toastTitle(model: Model) = model.toast.toast.value.title

    @Test
    fun theStatusStartsWithTheCurrentThermalState() {
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        val model = makeModel()
        assertEquals(ProcessInfo.ThermalState.serious, model.statusOther.thermalState.value)
    }

    @Test
    fun aThermalStateChangeUpdatesTheStatusAndTheStreamingHistory() {
        val model = makeModel()
        assertEquals(ProcessInfo.ThermalState.nominal, model.statusOther.thermalState.value)
        val stream = StreamingHistoryStream(settings = SettingsStream())
        model.streamingHistoryStream = stream
        setThermalStatus(PowerManager.THERMAL_STATUS_LIGHT)
        assertEquals(ProcessInfo.ThermalState.fair, model.statusOther.thermalState.value)
        assertEquals(ThermalState.FAIR, stream.highestThermalState)
        setThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        assertEquals(ProcessInfo.ThermalState.serious, model.statusOther.thermalState.value)
        assertEquals(ThermalState.SERIOUS, stream.highestThermalState)
        setThermalStatus(PowerManager.THERMAL_STATUS_NONE)
        assertEquals(ProcessInfo.ThermalState.nominal, model.statusOther.thermalState.value)
        assertEquals(ThermalState.SERIOUS, stream.highestThermalState)
    }

    @Test
    fun criticalMakesTheFlameRedToastAndSeriousDoesNot() {
        val model = makeModel()
        setThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        assertNotEquals(flameRedMessage, toastTitle(model))
        setThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)
        assertEquals(ProcessInfo.ThermalState.critical, model.statusOther.thermalState.value)
        assertEquals(flameRedMessage, toastTitle(model))
        assertEquals(flameRedSubMessage, model.toast.toast.value.subTitle)
    }

    @Test
    fun theMoblinkRelayReportsTheThermalStateAsAFlameColor() {
        val model = makeModel()
        model.battery.level.value = 0.5
        val expected = mapOf(
            ProcessInfo.ThermalState.nominal to MoblinkThermalState.white,
            ProcessInfo.ThermalState.fair to MoblinkThermalState.white,
            ProcessInfo.ThermalState.serious to MoblinkThermalState.yellow,
            ProcessInfo.ThermalState.critical to MoblinkThermalState.red,
        )
        for ((state, flame) in expected) {
            model.statusOther.thermalState.value = state
            assertEquals(50 to flame, model.moblinkRelayGetStatus(), "$state")
        }
    }

    @Test
    fun theFlameUsesTheSwiftColorsAndNames() {
        val colors = ProcessInfo.ThermalState.entries.map { it.color() }
        assertEquals(listOf(Color.White, Color.White, Color.Yellow, Color.Red), colors)
        val names = ProcessInfo.ThermalState.entries.map { it.string() }
        assertEquals(listOf("nominal", "fair", "serious", "critical"), names)
    }

    @Test
    fun theStreamingHistoryThermalStateMapsOneToOne() {
        for (state in ProcessInfo.ThermalState.entries) {
            val history = ThermalState.from(from = state)
            assertEquals(state.rawValue, history.rawValue)
            assertEquals(state, history.toProcessInfo())
        }
    }

    @Test
    fun aRemoteStreamersFlameIsTheSwiftThermalState() {
        assertEquals(ProcessInfo.ThermalState.fair, RemoteControlStatusGeneralFlame.White.toThermalState())
        assertEquals(ProcessInfo.ThermalState.serious, RemoteControlStatusGeneralFlame.Yellow.toThermalState())
        assertEquals(ProcessInfo.ThermalState.critical, RemoteControlStatusGeneralFlame.Red.toThermalState())
    }
}
