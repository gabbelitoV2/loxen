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
            TODO("no Android counterpart for DockKit")
        }
    }

    fun isConnected(): Boolean {
        return accessory != null
    }

    fun setTracking(on: Boolean) {
        gimbalScope.launch {
            TODO("no Android counterpart for DockKit")
        }
    }

    suspend fun setOrientation(angles: Vector3D) {
        if (tracking) {
            return
        }
        TODO("no Android counterpart for DockKit")
    }

    fun animate(motion: SettingsGimbalMotion) {
        gimbalScope.launch {
            TODO("no Android counterpart for DockKit")
        }
    }

    fun setMovement(velocity: Vector3D) {
        gimbalScope.launch {
            TODO("no Android counterpart for DockKit")
        }
    }

    suspend fun getCurrentOrientation(): Vector3D? {
        if (tracking) {
            return null
        }
        TODO("no Android counterpart for DockKit")
    }

    private fun handleStateChange(stateChange: Any) {
        TODO("no Android counterpart for DockKit")
    }

    private fun startAccessoryEventsHandler(accessory: Any) {
        stopAccessoryEventsHandler()
        this.accessory = accessory
        shutterCount = 0
        accessoryTask = gimbalScope.launch {
            TODO("no Android counterpart for DockKit")
        }
    }

    private fun stopAccessoryEventsHandler() {
        accessoryTask?.cancel()
        accessoryTask = null
        accessory = null
    }

    private fun handleAccessoryEvent(event: Any) {
        TODO("no Android counterpart for DockKit")
    }

    private fun handleAccessoryEventCameraShutter() {
        shutterCount += 1
        if (shutterCount % 2 != 0) {
            return
        }
        val gimbal = model.database.gimbal
        model.handleControllerFunction(buttonId = "g:shutter",
                                       function = gimbal.functionShutter,
                                       functionData = gimbal.functionDataShutter,
                                       pressed = false)
    }

    private fun handleAccessoryEventCameraFlip() {
        val gimbal = model.database.gimbal
        model.handleControllerFunction(buttonId = "g:flip",
                                       function = gimbal.functionFlip,
                                       functionData = gimbal.functionDataFlip,
                                       pressed = false)
    }

    private fun handleAccessoryEventCameraZoom(factor: Double) {
        val gimbal = model.database.gimbal
        var zoomIn = factor <= 0
        if (!gimbal.naturalZoom) {
            zoomIn = !zoomIn
        }
        val zoomSpeed = 1f + gimbal.zoomSpeed.toFloat() / 1000f
        val rate = 1f + 2f * pow(gimbal.zoomSpeed.toFloat() / 50f, 1.3f)
        if (zoomIn) {
            model.setZoomX(x = model.zoom.x.toFloat() * zoomSpeed, rate = rate)
        } else {
            model.setZoomX(x = model.zoom.x.toFloat() / zoomSpeed, rate = rate)
        }
    }
}
