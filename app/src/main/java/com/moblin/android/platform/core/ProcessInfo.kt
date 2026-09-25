package com.moblin.android.platform.core

import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import com.moblin.android.AppDelegate

class ProcessInfo private constructor() {
    enum class ThermalState(val rawValue: Int) {
        nominal(0),
        fair(1),
        serious(2),
        critical(3),
    }

    private val lock = Any()
    private var observedPowerManager: PowerManager? = null
    private var observedThermalState = ThermalState.nominal

    init {
        if (sdkInt >= Build.VERSION_CODES.Q) {
            powerManager()?.let { observeThermalStatus(it) }
        }
    }

    val thermalState: ThermalState
        get() {
            if (sdkInt < Build.VERSION_CODES.Q) {
                return ThermalState.nominal
            }
            val powerManager = powerManager() ?: return ThermalState.nominal
            observeThermalStatus(powerManager)
            return processInfoThermalState(powerManager.currentThermalStatus)
        }

    private fun powerManager(): PowerManager? {
        return runCatching { AppDelegate.context.getSystemService(PowerManager::class.java) }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun observeThermalStatus(powerManager: PowerManager) {
        synchronized(lock) {
            if (observedPowerManager === powerManager) {
                return
            }
            observedPowerManager = powerManager
            observedThermalState = processInfoThermalState(powerManager.currentThermalStatus)
        }
        powerManager.addThermalStatusListener { status -> thermalStatusChanged(status) }
    }

    private fun thermalStatusChanged(status: Int) {
        val state = processInfoThermalState(status)
        synchronized(lock) {
            if (state == observedThermalState) {
                return
            }
            observedThermalState = state
        }
        NotificationCenter.default.post(thermalStateDidChangeNotification, this)
    }

    companion object {
        const val thermalStateDidChangeNotification = "NSProcessInfoThermalStateDidChangeNotification"
        internal var sdkInt = Build.VERSION.SDK_INT
        val processInfo = ProcessInfo()
    }
}

internal fun processInfoThermalState(thermalStatus: Int): ProcessInfo.ThermalState {
    return when (thermalStatus) {
        PowerManager.THERMAL_STATUS_NONE -> ProcessInfo.ThermalState.nominal
        PowerManager.THERMAL_STATUS_LIGHT -> ProcessInfo.ThermalState.fair
        PowerManager.THERMAL_STATUS_MODERATE -> ProcessInfo.ThermalState.serious
        PowerManager.THERMAL_STATUS_SEVERE,
        PowerManager.THERMAL_STATUS_CRITICAL,
        PowerManager.THERMAL_STATUS_EMERGENCY,
        PowerManager.THERMAL_STATUS_SHUTDOWN,
        -> ProcessInfo.ThermalState.critical
        else -> ProcessInfo.ThermalState.nominal
    }
}
