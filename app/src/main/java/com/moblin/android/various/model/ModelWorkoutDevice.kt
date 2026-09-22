package com.moblin.android.various.model

import com.moblin.android.integrations.workoutdevice.WorkoutDevice
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceDelegate
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceRunningMetrics
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceState
import com.moblin.android.various.settings.SettingsWorkoutDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val mainScope = CoroutineScope(Dispatchers.Main)

private class WorkoutDeviceDelegateAdapter(private val model: Model) : WorkoutDeviceDelegate {
    override fun workoutDeviceState(device: WorkoutDevice, state: WorkoutDeviceState) {
        model.workoutDeviceState(device = device, state = state)
    }

    override fun workoutDeviceHeartRate(device: WorkoutDevice, heartRate: Int) {
        model.workoutDeviceHeartRate(device = device, heartRate = heartRate)
    }

    override fun workoutDeviceCyclingPower(device: WorkoutDevice, power: Int, cadence: Int?) {
        model.workoutDeviceCyclingPower(device = device, power = power, cadence = cadence)
    }

    override fun workoutDeviceCyclingSpeedCadence(device: WorkoutDevice, speed: Double?, cadence: Int?) {
        model.workoutDeviceCyclingSpeedCadence(device = device, speed = speed, cadence = cadence)
    }

    override fun workoutDeviceRunningMetrics(device: WorkoutDevice, metrics: WorkoutDeviceRunningMetrics) {
        model.workoutDeviceRunningMetrics(device = device, metrics = metrics)
    }
}

data class CyclingSampleInfo(
    val source: CyclingSource,
    val time: TimeSource.Monotonic.ValueTimeMark,
)

enum class CyclingSource(val rawValue: Int) {
    watch(0),
    cyclingPower(1),
    cyclingSpeedCadence(2),
    ;

    fun canReplace(latest: CyclingSampleInfo?): Boolean {
        if (latest == null) {
            return true
        }
        return rawValue >= latest.source.rawValue || latest.time.elapsedNow() > 5.seconds
    }

    companion object {
        fun fromRawValue(rawValue: Int): CyclingSource? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

fun Model.isWorkoutDeviceEnabled(device: SettingsWorkoutDevice): Boolean {
    return device.enabled
}

fun Model.enableWorkoutDevice(device: SettingsWorkoutDevice) {
    if (!workoutDevices.containsKey(device.id)) {
        val workoutDevice = WorkoutDevice(wheelCircumference = device.wheelCircumference)
        workoutDevice.delegate = WorkoutDeviceDelegateAdapter(model = this)
        workoutDevices[device.id] = workoutDevice
    }
    workoutDevices[device.id]?.start(deviceId = device.bluetoothPeripheralId)
}

fun Model.disableWorkoutDevice(device: SettingsWorkoutDevice) {
    workoutDevices[device.id]?.stop()
}

private fun Model.getWorkoutDeviceSettings(device: WorkoutDevice): SettingsWorkoutDevice? {
    return database.workoutDevices.devices.firstOrNull { workoutDevices[it.id] === device }
}

fun Model.setWorkoutDeviceWheelCircumference(device: SettingsWorkoutDevice) {
    workoutDevices[device.id]?.setWheelCircumference(millimeters = device.wheelCircumference)
}

fun Model.setCurrentWorkoutDevice(device: SettingsWorkoutDevice) {
    currentWorkoutDeviceSettings = device
    statusTopRight.workoutDeviceState.value = getWorkoutDeviceState(device = device)
}

fun Model.getWorkoutDeviceState(device: SettingsWorkoutDevice): WorkoutDeviceState {
    return workoutDevices[device.id]?.getState() ?: WorkoutDeviceState.disconnected
}

fun Model.autoStartWorkoutDevices() {
    for (device in database.workoutDevices.devices) {
        if (device.enabled) {
            enableWorkoutDevice(device = device)
        }
    }
}

fun Model.stopWorkoutDevices() {
    for (device in workoutDevices.values.toList()) {
        device.stop()
    }
}

fun Model.isAnyWorkoutDeviceConfigured(): Boolean {
    return database.workoutDevices.devices.any { it.enabled }
}

fun Model.areAllWorkoutDevicesConnected(): Boolean {
    return workoutDevices.values.none { device ->
        getWorkoutDeviceSettings(device)?.enabled == true &&
            device.getState() != WorkoutDeviceState.connected
    }
}

fun Model.setCyclingPower(power: Int, source: CyclingSource): Boolean {
    if (!source.canReplace(latestCyclingPower)) {
        return false
    }
    cyclingPower = power
    latestCyclingPower = CyclingSampleInfo(source = source, time = TimeSource.Monotonic.markNow())
    return true
}

fun Model.setCyclingCadence(cadence: Int, source: CyclingSource): Boolean {
    if (!source.canReplace(latestCyclingCadence)) {
        return false
    }
    cyclingCadence = cadence
    latestCyclingCadence = CyclingSampleInfo(source = source, time = TimeSource.Monotonic.markNow())
    return true
}

fun Model.workoutDeviceState(device: WorkoutDevice, state: WorkoutDeviceState) {
    mainScope.launch {
        val deviceSettings = getWorkoutDeviceSettings(device) ?: return@launch
        val deviceName = deviceSettings.name.lowercase()
        heartRates.remove(deviceName)
        runningMetrics.remove(deviceName)
        if (deviceSettings === currentWorkoutDeviceSettings) {
            statusTopRight.workoutDeviceState.value = state
        }
    }
}

fun Model.workoutDeviceHeartRate(device: WorkoutDevice, heartRate: Int) {
    mainScope.launch {
        val deviceSettings = getWorkoutDeviceSettings(device) ?: return@launch
        heartRates[deviceSettings.name.lowercase()] = heartRate
        addWorkoutHeartRate(heartRate)
    }
}

fun Model.workoutDeviceCyclingPower(device: WorkoutDevice, power: Int, cadence: Int?) {
    mainScope.launch {
        if (setCyclingPower(power, CyclingSource.cyclingPower)) {
            addWorkoutCyclingPower(power)
        }
        if (cadence != null && setCyclingCadence(cadence, CyclingSource.cyclingPower)) {
            addWorkoutCyclingCadence(cadence)
        }
    }
}

fun Model.workoutDeviceCyclingSpeedCadence(device: WorkoutDevice, speed: Double?, cadence: Int?) {
    mainScope.launch {
        if (cadence != null && setCyclingCadence(cadence, CyclingSource.cyclingSpeedCadence)) {
            addWorkoutCyclingCadence(cadence)
        }
        if (speed != null) {
            cyclingSpeed = speed
        }
    }
}

fun Model.workoutDeviceRunningMetrics(device: WorkoutDevice, metrics: WorkoutDeviceRunningMetrics) {
    mainScope.launch {
        val deviceSettings = getWorkoutDeviceSettings(device) ?: return@launch
        runningMetrics[deviceSettings.name.lowercase()] = metrics
    }
}
