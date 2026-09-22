package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.RemoteControlSettingsGimbalPreset
import com.moblin.android.various.Gimbal
import com.moblin.android.various.Vector3D
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsGimbalPreset
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.utils.makeUniqueName
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val gimbalAngularVelocity: Double = 0.3

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.setGimbalTracking(on: Boolean) {
    database.gimbal.tracking = on
    Gimbal.shared?.setTracking(on)
    setQuickButton(type = SettingsQuickButtonType.gimbalTracking, isOn = on)
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(gimbalTracking = on))
}

fun Model.setGimbalMovement(x: Float, y: Float) {
    val velocity = Vector3D(
        x = x.toDouble() * gimbalAngularVelocity,
        y = y.toDouble() * gimbalAngularVelocity,
        z = 0.0,
    )
    Gimbal.shared?.setMovement(velocity)
}

fun Model.animateGimbal(motion: SettingsGimbalMotion) {
    Gimbal.shared?.animate(motion)
}

fun Model.getRemoteControlGimbalPresets(): List<RemoteControlSettingsGimbalPreset> {
    return database.gimbal.presets.map {
        RemoteControlSettingsGimbalPreset(id = it.id, name = it.name)
    }
}

fun Model.toggleGimbalTracking() {
    setGimbalTracking(on = !database.gimbal.tracking)
}

fun Model.saveGimbalPreset(id: UUID?) {
    mainScope.launch {
        try {
            val angles = Gimbal.shared?.getCurrentOrientation() ?: return@launch
            if (id != null) {
                val preset = database.gimbal.presets.firstOrNull { it.id == id }
                if (preset != null) {
                    preset.x = angles.x.toFloat()
                    preset.y = angles.y.toFloat()
                    preset.zoomX = zoom.x
                }
            } else {
                val preset = SettingsGimbalPreset()
                preset.name = makeUniqueName(
                    name = SettingsGimbalPreset.baseName,
                    existingNames = database.gimbal.presets,
                )
                preset.x = angles.x.toFloat()
                preset.y = angles.y.toFloat()
                preset.zoomX = zoom.x
                database.gimbal.presets.add(preset)
            }
            remoteControlStateChanged(
                state = RemoteControlAssistantStreamerState(
                    gimbalPresets = getRemoteControlGimbalPresets(),
                ),
            )
        } catch (error: Exception) {
            makeErrorToast(
                title = localized("Failed to get gimbal orientation"),
                subTitle = error.message ?: "",
            )
        }
    }
}

fun Model.moveToGimbalPreset(id: UUID) {
    moveToGimbalPresetQueue.add(id)
    processGimbalPresetQueue()
}

private fun Model.processGimbalPresetQueue() {
    if (moveToGimbalPresetQueueRunning) {
        return
    }
    val id = moveToGimbalPresetQueue.removeFirstOrNull() ?: return
    val preset = database.gimbal.presets.firstOrNull { it.id == id }
    if (preset == null) {
        processGimbalPresetQueue()
        return
    }
    moveToGimbalPresetQueueRunning = true
    mainScope.launch {
        delay(300)
        setZoomX(x = preset.zoomX, rate = 5f)
    }
    mainScope.launch {
        try {
            Gimbal.shared?.setOrientation(
                Vector3D(x = preset.x.toDouble(), y = preset.y.toDouble(), z = 0.0),
            )
        } finally {
            moveToGimbalPresetQueueRunning = false
            processGimbalPresetQueue()
        }
    }
}
