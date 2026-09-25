package com.moblin.android.various.model

import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.RemoteControlZoomPreset
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.platform.avfoundation.AVCaptureDevice
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
            AVCaptureDevice.Position.BACK.ordinal -> setBackZoomPreset(id)
            AVCaptureDevice.Position.FRONT.ordinal -> setFrontZoomPreset(id)
            else -> {}
        }
        if (setCameraZoomX(preset.x, database.zoom.speed) != null) {
            setZoomXWhenInRange(preset.x)
            when (getSelectedScene()?.videoSource?.cameraPosition) { SettingsSceneCameraPosition.backTripleLowEnergy -> attachBackTripleLowEnergyCamera(force = false); SettingsSceneCameraPosition.backDualLowEnergy -> attachBackDualLowEnergyCamera(force = false); SettingsSceneCameraPosition.backWideDualLowEnergy -> attachBackWideDualLowEnergyCamera(force = false); else -> {} }
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
        AVCaptureDevice.Position.BACK.ordinal -> {
            zoom.backX = x
            updateBackZoomPresetId()
        }
        AVCaptureDevice.Position.FRONT.ordinal -> {
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
        AVCaptureDevice.Position.BACK.ordinal -> setBackZoomPreset(noBackZoomPresetId)
        AVCaptureDevice.Position.FRONT.ordinal -> setFrontZoomPreset(noFrontZoomPresetId)
        else -> {}
    }
}

private fun Model.setBackZoomPreset(presetId: UUID) {
    zoom.backPresetId.value = presetId
    if (cameraPosition == AVCaptureDevice.Position.BACK.ordinal) {
        remoteControlStateChanged(RemoteControlAssistantStreamerState(zoomPreset = presetId))
        if (isWatchLocal()) {
            sendZoomPresetToWatch()
        }
    }
}

private fun Model.setFrontZoomPreset(presetId: UUID) {
    zoom.frontPresetId.value = presetId
    if (cameraPosition == AVCaptureDevice.Position.FRONT.ordinal) {
        remoteControlStateChanged(RemoteControlAssistantStreamerState(zoomPreset = presetId))
        if (isWatchLocal()) {
            sendZoomPresetToWatch()
        }
    }
}

private fun Model.findZoomPreset(id: UUID): SettingsZoomPreset? {
    return when (cameraPosition) {
        AVCaptureDevice.Position.BACK.ordinal -> database.zoom.back.firstOrNull { it.id == id }
        AVCaptureDevice.Position.FRONT.ordinal -> database.zoom.front.firstOrNull { it.id == id }
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
    if (cameraPosition == AVCaptureDevice.Position.FRONT.ordinal) {
        zoomPresetsMayHaveChanged()
    }
}

fun Model.updateBackZoomPresets() {
    zoom.backZoomPresets.value = database.zoom.back.filter { showPreset(it) }
    if (cameraPosition == AVCaptureDevice.Position.BACK.ordinal) {
        zoomPresetsMayHaveChanged()
    }
}

fun Model.zoomPresetsMayHaveChanged() {
    val presets: List<SettingsZoomPreset> = when (cameraPosition) {
        AVCaptureDevice.Position.BACK.ordinal -> zoom.backZoomPresets.value
        AVCaptureDevice.Position.FRONT.ordinal -> zoom.frontZoomPresets.value
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

private fun Model.factorToX(position: AVCaptureDevice.Position, factor: Float): Float {
    if (position == AVCaptureDevice.Position.BACK && hasUltraWideBackCamera) {
        return factor / 2
    } else if (position == AVCaptureDevice.Position.FRONT && hasUltraWideFrontCamera) {
        return factor / 2
    }
    return factor
}

fun Model.getMinMaxZoomX(position: AVCaptureDevice.Position): Pair<Float, Float> {
    var minX: Float
    var maxX: Float
    val camera = AVCaptureDevice.default(AVCaptureDevice.DeviceType.BUILT_IN_WIDE_ANGLE_CAMERA, position)
    if (camera != null) {
        minX = factorToX(position, camera.minAvailableVideoZoomFactor)
        maxX = factorToX(position, camera.maxAvailableVideoZoomFactor)
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
