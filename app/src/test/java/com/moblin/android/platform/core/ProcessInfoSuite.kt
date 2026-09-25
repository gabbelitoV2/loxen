package com.moblin.android.platform.core

import android.os.Build
import android.os.PowerManager
import com.moblin.android.AppDelegate
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
class ProcessInfoSuite {
    private lateinit var powerManager: ShadowPowerManager
    private val observer = Any()
    private val posted = mutableListOf<Notification>()

    @Before
    fun setUp() {
        ProcessInfo.sdkInt = Build.VERSION.SDK_INT
        powerManager = shadowOf(AppDelegate.context.getSystemService(PowerManager::class.java))
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_NONE)
        NotificationCenter.default.addObserver(observer, ProcessInfo.thermalStateDidChangeNotification, null) {
            posted.add(it)
        }
    }

    @After
    fun tearDown() {
        NotificationCenter.default.removeObserver(observer)
        ProcessInfo.sdkInt = Build.VERSION.SDK_INT
    }

    @Test
    fun androidThermalStatusesMapToTheFourAppleStates() {
        val expected = mapOf(
            PowerManager.THERMAL_STATUS_NONE to ProcessInfo.ThermalState.nominal,
            PowerManager.THERMAL_STATUS_LIGHT to ProcessInfo.ThermalState.fair,
            PowerManager.THERMAL_STATUS_MODERATE to ProcessInfo.ThermalState.serious,
            PowerManager.THERMAL_STATUS_SEVERE to ProcessInfo.ThermalState.critical,
            PowerManager.THERMAL_STATUS_CRITICAL to ProcessInfo.ThermalState.critical,
            PowerManager.THERMAL_STATUS_EMERGENCY to ProcessInfo.ThermalState.critical,
            PowerManager.THERMAL_STATUS_SHUTDOWN to ProcessInfo.ThermalState.critical,
            -1 to ProcessInfo.ThermalState.nominal,
            7 to ProcessInfo.ThermalState.nominal,
        )
        for ((status, state) in expected) {
            assertEquals(state, processInfoThermalState(status), "status $status")
        }
        assertEquals(listOf(0, 1, 2, 3), ProcessInfo.ThermalState.entries.map { it.rawValue })
    }

    @Test
    fun theThermalStateIsThePowerManagersCurrentStatus() {
        assertEquals(ProcessInfo.ThermalState.nominal, ProcessInfo.processInfo.thermalState)
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        assertEquals(ProcessInfo.ThermalState.serious, ProcessInfo.processInfo.thermalState)
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_LIGHT)
        assertEquals(ProcessInfo.ThermalState.fair, ProcessInfo.processInfo.thermalState)
    }

    @Test
    fun theNotificationIsPostedOncePerAppleStateChange() {
        ProcessInfo.processInfo.thermalState
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)
        assertEquals(1, posted.size)
        assertEquals(ProcessInfo.thermalStateDidChangeNotification, posted[0].name)
        assertSame(ProcessInfo.processInfo, posted[0].obj)
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_CRITICAL)
        assertEquals(1, posted.size)
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_LIGHT)
        assertEquals(2, posted.size)
        assertEquals(ProcessInfo.ThermalState.fair, ProcessInfo.processInfo.thermalState)
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_NONE)
        assertEquals(3, posted.size)
    }

    @Test
    fun theListenerIsRegisteredWithThePowerManager() {
        ProcessInfo.processInfo.thermalState
        assertEquals(1, powerManager.thermalStatusListeners.size)
        ProcessInfo.processInfo.thermalState
        assertEquals(1, powerManager.thermalStatusListeners.size)
    }

    @Test
    fun beforeAndroid10TheStateIsAlwaysNominal() {
        ProcessInfo.sdkInt = Build.VERSION_CODES.P
        powerManager.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_CRITICAL)
        assertEquals(ProcessInfo.ThermalState.nominal, ProcessInfo.processInfo.thermalState)
    }
}
