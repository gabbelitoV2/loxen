package com.moblin.android.platform.core

import android.os.Build
import android.os.PowerManager
import com.moblin.android.AppDelegate

class ProcessInfo private constructor() {
    enum class ThermalState(val rawValue: Int) {
        nominal(0),
        fair(1),
        serious(2),
        critical(3),
    }

    val thermalState: ThermalState
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return ThermalState.nominal
            }
            val powerManager = runCatching { AppDelegate.context.getSystemService(PowerManager::class.java) }
                .getOrNull() ?: return ThermalState.nominal
            return processInfoThermalState(powerManager.currentThermalStatus)
        }

    companion object {
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
