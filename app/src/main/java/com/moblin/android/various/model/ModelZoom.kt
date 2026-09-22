package com.moblin.android.various.model

import com.moblin.android.media.CameraPosition
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.RemoteControlZoomPreset
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.various.utils.hasUltraWideBackCamera
import com.moblin.android.various.utils.hasUltraWideFrontCamera
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

private val noBackZoomPresetId = UUID.randomUUID()
private val noFrontZoomPresetId = UUID.randomUUID()

class Zoom {
    var xPinch: Float = 1.0f
    var backX: Float = 0.5f
    var frontX: Float = 0.5f
    val backPresetId = MutableStateFlow(UUID.randomUUID())
    val frontPresetId = MutableStateFlow(UUID.randomUUID())
    val x = MutableStateFlow(1.0f)
    val hasZoom = MutableStateFlow(true)
    val backZoomPresets = MutableStateFlow<List<SettingsZoomPreset>>(emptyList())
    val frontZoomPresets = MutableStateFlow<List<SettingsZoomPreset>>(emptyList())

    fun statusText(): String {
        return String.format(Locale.US, "%.1f", x.value)
    }
}

fun Model.setZoomPreset(id: UUID) {
    val preset = findZoomPreset(id)
    if (preset != null) {
        when (cameraPosition) {
            CameraPosition.BACK -> setBackZoomPreset(id)
            CameraPosition.FRONT -> setFrontZoomPreset(id)
            else -> {}
        }
        if (setCameraZoomX(preset.x, database.zoom.speed) != null) {
            setZoomXWhenInRange(preset.x)
            when (getSelectedScene()?.videoSource?.cameraPosition) {
                CameraPosition.BACK_TRIPLE_LOW_ENERGY -> attachBackTripleLowEnergyCamera(false)
                CameraPosition.BACK_DUAL_LOW_ENERGY -> attachBackDualLowEnergyCamera(false)
                CameraPosition.BACK_WIDE_DUAL_LOW_ENERGY -> attachBackWideDualLowEnergyCamera(false)
                else -> {}
            }
        }
    } else {
        clearZoomPresetId()
    }
}

fun Model.setZoomX(x: Float, rate: Float? = null, setPinch: Boolean = true) {
    clearZoomPresetId()
    val newX = setCameraZoomX(x, rate)
    if (newX != null) {
        setZoomXWhenInRange(newX, setPinch)
    }
}

fun Model.setZoomXWhenInRange(x: Float, setPinch: Boolean = true) {
    when (cameraPosition) {
        CameraPosition.BACK -> {
            zoom.backX = x
            updateBackZoomPresetId()
        }
        CameraPosition.FRONT -> {
            zoom.frontX = x
            updateFrontZoomPresetId()
        }
        else -> {}
    }
    zoom.x.value = x
    remoteControlStateChanged(RemoteControlAssistantStreamerState(zoom = x))
    if (isWatchLocal()) {
        sendZoomToWatch(x)
    }
    if (setPinch) {
        zoom.xPinch = zoom.x.value
    }
}

fun Model.changeZoomX(amount: Float, rate: Float? = null) {
    if (!zoom.hasZoom.value) {
        return
    }
    setZoomX(zoom.xPinch * amount, rate, setPinch = false)
}

fun Model.commitZoomX(amount: Float, rate: Float? = null) {
    if (!zoom.hasZoom.value) {
        return
    }
    setZoomX(zoom.xPinch * amount, rate)
}

private fun Model.clearZoomPresetId() {
    when (cameraPosition) {
        CameraPosition.BACK -> setBackZoomPreset(noBackZoomPresetId)
        CameraPosition.FRONT -> setFrontZoomPreset(noFrontZoomPresetId)
        else -> {}
    }
}

private fun Model.setBackZoomPreset(presetId: UUID) {
    zoom.backPresetId.value = presetId
    if (cameraPosition == CameraPosition.BACK) {
        remoteControlStateChanged(RemoteControlAssistantStreamerState(zoomPreset = presetId))
        if (isWatchLocal()) {
            sendZoomPresetToWatch()
        }
    }
}

private fun Model.setFrontZoomPreset(presetId: UUID) {
    zoom.frontPresetId.value = presetId
    if (cameraPosition == CameraPosition.FRONT) {
        remoteControlStateChanged(RemoteControlAssistantStreamerState(zoomPreset = presetId))
        if (isWatchLocal()) {
            sendZoomPresetToWatch()
        }
    }
}

private fun Model.findZoomPreset(id: UUID): SettingsZoomPreset? {
    return when (cameraPosition) {
        CameraPosition.BACK -> database.zoom.back.firstOrNull { it.id == id }
        CameraPosition.FRONT -> database.zoom.front.firstOrNull { it.id == id }
        else -> null
    }
}

fun Model.backZoomPresetSettingsUpdated() {
    if (!database.zoom.back.any { it.id == zoom.backPresetId.value }) {
        setBackZoomPreset(noBackZoomPresetId)
    }
    updateBackZoomPresets()
}

fun Model.frontZoomPresetSettingUpdated() {
    if (!database.zoom.front.any { it.id == zoom.frontPresetId.value }) {
        setFrontZoomPreset(noFrontZoomPresetId)
    }
    updateFrontZoomPresets()
}

fun Model.updateFrontZoomPresets() {
    zoom.frontZoomPresets.value = database.zoom.front.filter { showPreset(it) }
    if (cameraPosition == CameraPosition.FRONT) {
        zoomPresetsMayHaveChanged()
    }
}

fun Model.updateBackZoomPresets() {
    zoom.backZoomPresets.value = database.zoom.back.filter { showPreset(it) }
    if (cameraPosition == CameraPosition.BACK) {
        zoomPresetsMayHaveChanged()
    }
}

fun Model.zoomPresetsMayHaveChanged() {
    val presets: List<SettingsZoomPreset> = when (cameraPosition) {
        CameraPosition.BACK -> zoom.backZoomPresets.value
        CameraPosition.FRONT -> zoom.frontZoomPresets.value
        else -> emptyList()
    }
    val zoomPresets = presets.map { RemoteControlZoomPreset(it.id, it.name) }
    remoteControlStateChanged(RemoteControlAssistantStreamerState(zoomPresets = zoomPresets))
    if (isWatchLocal()) {
        sendZoomPresetsToWatch(presets)
    }
}

fun Model.lowEnergyCameraUpdateBackZoom(force: Boolean) {
    if (force) {
        updateBackZoomSwitchTo()
    }
}

private fun Model.updateBackZoomPresetId() {
    val preset = database.zoom.back.firstOrNull { it.x == zoom.backX }
    if (preset != null) {
        setBackZoomPreset(preset.id)
    }
}

private fun Model.updateFrontZoomPresetId() {
    val preset = database.zoom.front.firstOrNull { it.x == zoom.frontX }
    if (preset != null) {
        setFrontZoomPreset(preset.id)
    }
}

fun Model.updateBackZoomSwitchTo() {
    if (database.zoom.switchToBack.enabled) {
        clearZoomPresetId()
        zoom.backX = database.zoom.switchToBack.x
        updateBackZoomPresetId()
    }
}

fun Model.updateFrontZoomSwitchTo() {
    if (database.zoom.switchToFront.enabled) {
        clearZoomPresetId()
        zoom.frontX = database.zoom.switchToFront.x
        updateFrontZoomPresetId()
    }
}

private fun Model.factorToX(position: CameraPosition, factor: Float): Float {
    if (position == CameraPosition.BACK && hasUltraWideBackCamera) {
        return factor / 2
    } else if (position == CameraPosition.FRONT && hasUltraWideFrontCamera) {
        return factor / 2
    }
    return factor
}

fun Model.getMinMaxZoomX(position: CameraPosition): Pair<Float, Float> {
    var minX: Float
    var maxX: Float
    val camera = preferredCamera(position)
    if (camera != null) {
        val zoomState = camera.cameraInfo.zoomState.value
        if (zoomState != null) {
            minX = factorToX(position, zoomState.minZoomRatio)
            maxX = factorToX(position, zoomState.maxZoomRatio)
        } else {
            minX = 1.0f
            maxX = 1.0f
        }
    } else {
        minX = 1.0f
        maxX = 1.0f
    }
    return Pair(minX, maxX)
}

fun Model.isShowingStatusZoom(): Boolean {
    return database.show.zoom && zoom.hasZoom.value && !isChatPhone()
}

private fun Model.showPreset(preset: SettingsZoomPreset): Boolean {
    val x = preset.x
    return x >= cameraZoomXMinimum && x <= cameraZoomXMaximum
}

fun Model.setCameraZoomX(x: Float, rate: Float? = null): Float? {
    return cameraZoomLevelToX(media.setCameraZoomLevel(cameraDevice, x / cameraZoomLevelToXScale, rate))
}

fun Model.stopCameraZoom(): Float? {
    return cameraZoomLevelToX(media.stopCameraZoomLevel(cameraDevice))
}

private fun Model.cameraZoomLevelToX(level: Float?): Float? {
    if (level != null) {
        return level * cameraZoomLevelToXScale
    }
    return null
}
