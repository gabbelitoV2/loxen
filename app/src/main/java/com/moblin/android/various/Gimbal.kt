package com.moblin.android.various

import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsGimbalMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow

data class Vector3D(val x: Double, val y: Double, val z: Double)

private val gimbalScope = CoroutineScope(Dispatchers.Main)

class Gimbal(private val model: Model) {
    private var task: Job? = null
    private var accessoryTask: Job? = null
    private var accessory: Any? = null
    private var shutterCount: Int = 0
    private var tracking: Boolean = true

    companion object {
        var shared: Gimbal? = null
    }

    init {
        task = gimbalScope.launch {
            Unit
        }
    }

    fun isConnected(): Boolean {
        return accessory != null
    }

    fun setTracking(on: Boolean) {
        gimbalScope.launch {
            Unit
        }
    }

    suspend fun setOrientation(angles: Vector3D) {
        if (tracking) {
            return
        }
        Unit
    }

    fun animate(motion: SettingsGimbalMotion) {
        gimbalScope.launch {
            Unit
        }
    }

    fun setMovement(velocity: Vector3D) {
        gimbalScope.launch {
            Unit
        }
    }

    suspend fun getCurrentOrientation(): Vector3D? {
        if (tracking) {
            return null
        }
        return null
    }

    private fun handleStateChange(stateChange: Any) {
        Unit
    }

    private fun startAccessoryEventsHandler(accessory: Any) {
        stopAccessoryEventsHandler()
        this.accessory = accessory
        shutterCount = 0
        accessoryTask = gimbalScope.launch {
            Unit
        }
    }

    private fun stopAccessoryEventsHandler() {
        accessoryTask?.cancel()
        accessoryTask = null
        accessory = null
    }

    private fun handleAccessoryEvent(event: Any) {
        Unit
    }

    private fun handleAccessoryEventCameraShutter() {
        shutterCount += 1
        if (shutterCount % 2 != 0) {
            return
        }
        Unit
    }

    private fun handleAccessoryEventCameraFlip() {
        Unit
    }

    private fun handleAccessoryEventCameraZoom(factor: Double) {
        Unit
    }
}
