package com.moblin.android.various.model

import android.graphics.PointF
import android.util.Log
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.various.settings.SettingsCameraId
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorLutType
import com.moblin.android.various.settings.SettingsColorSpace
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.various.settings.SettingsWidgetVideoSource
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.various.utils.DeviceOrientation
import com.moblin.android.various.utils.clamped
import com.moblin.android.various.utils.exposureFactorStep
import com.moblin.android.various.utils.factorFromExposure
import com.moblin.android.various.utils.factorFromIso
import com.moblin.android.various.utils.factorFromWhiteBalance
import com.moblin.android.various.utils.factorToExposure
import com.moblin.android.various.utils.factorToIso
import com.moblin.android.various.utils.factorToWhiteBalance
import com.moblin.android.various.utils.getOrientation
import com.moblin.android.various.utils.hasDualBackCamera
import com.moblin.android.various.utils.hasTripleBackCamera
import com.moblin.android.various.utils.hasWideDualBackCamera
import com.moblin.android.various.utils.isMac
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.moblin.android.various.settings.SettingsQuickButtonType
import android.media.MediaFormat
import com.moblin.android.various.utils.name
import kotlinx.coroutines.launch
import com.moblin.android.platform.core.NSKeyValueObservation

private const val LOG_TAG = "Model"
private val mainScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

typealias CameraId = String

data class Camera(
    var id: CameraId,
    var name: String,
)

enum class CameraShowType {
    bias,
    whiteBalance,
    iso,
    exposure,
    focus,
}

class CameraShow {
    val type = MutableStateFlow<CameraShowType?>(null)

    fun toggle(buttonType: CameraShowType) {
        type.value = if (type.value == buttonType) {
            null
        } else {
            buttonType
        }
    }
}

class CameraState {
    val show = CameraShow()
    val isFocusesLocked: MutableMap<CaptureDevice, Boolean> = mutableMapOf()
    val lockedFocuses: MutableMap<CaptureDevice, Float> = mutableMapOf()
    var editingLockedFocus = false
    var focusObservation: NSKeyValueObservation? by NSKeyValueObservation.holder()
    val isExposuresAndIsosLocked: MutableMap<CaptureDevice, Boolean> = mutableMapOf()
    val lockedIsos: MutableMap<CaptureDevice, Float> = mutableMapOf()
    var editingLockedIso = false
    var isoObservation: NSKeyValueObservation? by NSKeyValueObservation.holder()
    val lockedExposures: MutableMap<CaptureDevice, Float> = mutableMapOf()
    var editingLockedExposure = false
    var exposureObservation: NSKeyValueObservation? by NSKeyValueObservation.holder()
    val isWhiteBalancesLocked: MutableMap<CaptureDevice, Boolean> = mutableMapOf()
    val lockedWhiteBalances: MutableMap<CaptureDevice, Float> = mutableMapOf()
    var editingLockedWhiteBalance = false
    var whiteBalanceObservation: NSKeyValueObservation? by NSKeyValueObservation.holder()
    val bias = MutableStateFlow(0.0f)
    val lockedFocus = MutableStateFlow(1.0f)
    val isFocusLocked = MutableStateFlow(false)
    val lockedIso = MutableStateFlow(1.0f)
    val lockedExposure = MutableStateFlow(1.0f)
    val exposure = MutableStateFlow(0L)
    val isExposureAndIsoLocked = MutableStateFlow(false)
    val lockedWhiteBalance = MutableStateFlow(0.0f)
    val isWhiteBalanceLocked = MutableStateFlow(false)
    val manualFocusPoint = MutableStateFlow<PointF?>(null)

    fun setBias(value: Float) {
        bias.value = value
    }

    fun setLockedFocus(value: Float) {
        lockedFocus.value = value
    }

    fun setIsFocusLocked(value: Boolean) {
        isFocusLocked.value = value
    }

    fun setLockedIso(value: Float) {
        lockedIso.value = value
    }

    fun setLockedExposure(value: Float) {
        lockedExposure.value = value
    }

    fun setExposure(value: Long) {
        exposure.value = value
    }

    fun setIsExposureAndIsoLocked(value: Boolean) {
        isExposureAndIsoLocked.value = value
    }

    fun setLockedWhiteBalance(value: Float) {
        lockedWhiteBalance.value = value
    }

    fun setIsWhiteBalanceLocked(value: Boolean) {
        isWhiteBalanceLocked.value = value
    }

    fun setManualFocusPoint(value: PointF?) {
        if (value != manualFocusPoint.value) {
            manualFocusPoint.value = value
        }
    }
}

val noneCameraName = localized("None")
val screenCaptureCameraName = localized("Screen capture")
val noneCameraId: UUID = UUID.fromString("00000000-feed-b1ac-cafe-000000000000")
val screenCaptureCameraId: UUID = UUID.fromString("00000000-cafe-babe-beef-000000000000")
private val backTripleLowEnergyCameraBaseName = localized("Triple (low power)")
private val backDualLowEnergyCameraBaseName = localized("Dual (low power)")
private val backWideDualLowEnergyCameraBaseName = localized("Wide dual (low power)")
private val backTripleLowEnergyCamera = Camera(
    id = "00000000-feed-b1ac-cafe-100000000000",
    name = localized("Back $backTripleLowEnergyCameraBaseName"),
)
private val backDualLowEnergyCamera = Camera(
    id = "00000000-feed-b1ac-cafe-200000000000",
    name = localized("Back $backDualLowEnergyCameraBaseName"),
)
private val backWideDualLowEnergyCamera = Camera(
    id = "00000000-feed-b1ac-cafe-300000000000",
    name = localized("Back $backWideDualLowEnergyCameraBaseName"),
)

fun Model.setFocusPointOfInterest(focusPoint: PointF) {
    val device = cameraDevice
    if (device == null || !device.device.isFocusPointOfInterestSupported) {
        Log.i(LOG_TAG, "Tap to focus not supported for this camera")
        makeErrorToast(title = localized("Tap to focus not supported for this camera"))
        return
    }
    val focusPointOfInterest = PointF(focusPoint.x, focusPoint.y)
    if (stream.value.portrait) {
        focusPointOfInterest.x = focusPoint.y
        focusPointOfInterest.y = 1 - focusPoint.x
    } else if (getOrientation() == DeviceOrientation.LANDSCAPE_RIGHT) {
        focusPointOfInterest.x = 1 - focusPoint.x
        focusPointOfInterest.y = 1 - focusPoint.y
    }
    try {
        device.device.lockForConfiguration()
        device.device.focusPointOfInterest = focusPointOfInterest
        device.device.focusMode = AVCaptureDevice.FocusMode.autoFocus
        device.device.exposurePointOfInterest = focusPointOfInterest
        device.device.exposureMode = AVCaptureDevice.ExposureMode.autoExpose
        device.device.unlockForConfiguration()
        camera.setManualFocusPoint(value = focusPoint)
        startMotionDetection()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for focusPointOfInterest: $error")
    }
    camera.isFocusesLocked[device] = false
    camera.setIsFocusLocked(false)
}

fun Model.setAutoFocus() {
    stopMotionDetection()
    val device = cameraDevice
    if (device == null || !device.device.isFocusPointOfInterestSupported) {
        return
    }
    try {
        device.device.lockForConfiguration()
        device.device.focusPointOfInterest = PointF(0.5f, 0.5f)
        device.device.focusMode = AVCaptureDevice.FocusMode.continuousAutoFocus
        device.device.exposurePointOfInterest = PointF(0.5f, 0.5f)
        device.device.exposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure
        device.device.unlockForConfiguration()
        camera.setManualFocusPoint(value = null)
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for focusPointOfInterest: $error")
    }
    camera.isFocusesLocked[device] = false
    camera.setIsFocusLocked(false)
}

fun Model.setManualFocus(lensPosition: Float) {
    val device = cameraDevice
    if (device == null || !device.device.isLockingFocusWithCustomLensPositionSupported) {
        makeErrorToast(title = localized("Manual focus not supported for this camera"))
        return
    }
    stopMotionDetection()
    try {
        device.device.lockForConfiguration()
        device.device.setFocusModeLocked(lensPosition = lensPosition)
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for manual focus: $error")
    }
    camera.setManualFocusPoint(value = null)
    camera.isFocusesLocked[device] = true
    camera.setIsFocusLocked(true)
    camera.lockedFocuses[device] = lensPosition
}

fun Model.setFocusAfterCameraAttach() {
    val device = cameraDevice ?: return
    camera.setLockedFocus(camera.lockedFocuses[device] ?: device.device.lensPosition)
    camera.setIsFocusLocked(camera.isFocusesLocked[device] ?: false)
    if (!camera.isFocusLocked.value) {
        setAutoFocus()
    }
    if (camera.focusObservation != null) {
        stopObservingFocus()
        startObservingFocus()
    }
}

fun Model.isCameraSupportingManualFocus(): Boolean {
    return cameraDevice?.device?.isLockingFocusWithCustomLensPositionSupported ?: false
}

fun Model.startObservingFocus() {
    val device = cameraDevice ?: return
    camera.setLockedFocus(device.device.lensPosition)
    camera.focusObservation = device.device.observe(AVCaptureDevice::lensPosition) { _, _ -> mainScope.launch { if (!camera.editingLockedFocus) { camera.lockedFocuses[device] = device.device.lensPosition; camera.setLockedFocus(device.device.lensPosition) } } }
}

fun Model.stopObservingFocus() {
    camera.focusObservation = null
}

fun Model.setAutoExposureAndIso() {
    val device = cameraDevice
    if (
        device == null ||
        !device.device.isExposureModeSupported(AVCaptureDevice.ExposureMode.continuousAutoExposure)
    ) {
        makeErrorToast(title = localized("Continuous auto exposure not supported for this camera"))
        return
    }
    try {
        device.device.lockForConfiguration()
        device.device.exposureMode = AVCaptureDevice.ExposureMode.continuousAutoExposure
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for continuous auto exposure: $error")
    }
    camera.isExposuresAndIsosLocked[device] = false
    camera.setIsExposureAndIsoLocked(false)
}

fun Model.setExposureAndIsoAfterCameraAttach(device: CaptureDevice) {
    camera.setLockedIso(camera.lockedIsos[device] ?: factorFromIso(device = device.device, iso = device.device.iso))
    camera.setLockedExposure(
        camera.lockedExposures[device] ?: factorFromExposure(
            device = device.device,
            exposure = device.device.exposureDuration,
        ),
    )
    camera.setExposure(device.device.exposureDuration)
    camera.setIsExposureAndIsoLocked(camera.isExposuresAndIsosLocked[device] ?: false)
    if (camera.isExposureAndIsoLocked.value) {
        setManualExposureAndIso(exposureFactor = camera.lockedExposure.value, isoFactor = camera.lockedIso.value)
    }
    if (camera.isoObservation != null) {
        stopObservingIso()
        startObservingIso()
    }
    if (camera.exposureObservation != null) {
        stopObservingExposure()
        startObservingExposure()
    }
}

fun Model.isCameraSupportingManualExposureAndIso(): Boolean {
    return cameraDevice?.device?.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom) ?: false
}

private fun Model.setManualExposureAndIso(exposureFactor: Float?, isoFactor: Float?) {
    val device = cameraDevice
    if (device == null || !device.device.isExposureModeSupported(AVCaptureDevice.ExposureMode.custom)) {
        makeErrorToast(title = localized("Manual exposure not supported for this camera"))
        return
    }
    val iso: Float
    val exposure: Long
    if (isoFactor != null) {
        iso = factorToIso(device = device.device, factor = isoFactor)
        camera.lockedIsos[device] = isoFactor
    } else {
        iso = AVCaptureDevice.currentISO
        camera.lockedIsos[device] = factorFromIso(device = device.device, iso = device.device.iso)
    }
    if (exposureFactor != null) {
        exposure = factorToExposure(device = device.device, factor = exposureFactor)
        camera.lockedExposures[device] = exposureFactor
        camera.setExposure(exposure)
    } else {
        exposure = AVCaptureDevice.currentExposureDuration
        camera.lockedExposures[device] = factorFromExposure(
            device = device.device,
            exposure = device.device.exposureDuration,
        )
        camera.setExposure(device.device.exposureDuration)
    }
    camera.isExposuresAndIsosLocked[device] = true
    camera.setIsExposureAndIsoLocked(true)
    try {
        device.device.lockForConfiguration()
        device.device.setExposureModeCustom(duration = exposure, iso = iso) { }
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for manual exposure: $error")
    }
}

fun Model.setManualIso(factor: Float) {
    setManualExposureAndIso(exposureFactor = null, isoFactor = factor)
}

fun Model.startObservingIso() {
    val device = cameraDevice ?: return
    camera.setLockedIso(factorFromIso(device = device.device, iso = device.device.iso))
    camera.isoObservation = device.device.observe(AVCaptureDevice::iso) { _, _ -> mainScope.launch { if (!camera.editingLockedIso) { val iso = factorFromIso(device = device.device, iso = device.device.iso); camera.lockedIsos[device] = iso; camera.setLockedIso(iso) } } }
}

fun Model.stopObservingIso() {
    camera.isoObservation = null
}

fun Model.setManualExposure(factor: Float) {
    setManualExposureAndIso(exposureFactor = factor, isoFactor = null)
}

fun Model.getExposureFactorStep(): Float {
    val device = cameraDevice ?: return 0.01f
    return exposureFactorStep(device = device.device as AVCaptureDevice)
}

fun Model.startObservingExposure() {
    val device = cameraDevice ?: return
    camera.setLockedExposure(factorFromExposure(device = device.device, exposure = device.device.exposureDuration))
    camera.setExposure(device.device.exposureDuration)
    camera.exposureObservation = device.device.observe(AVCaptureDevice::exposureDuration) { _, _ -> mainScope.launch { if (!camera.editingLockedExposure) { val exposure = factorFromExposure(device = device.device, exposure = device.device.exposureDuration); camera.lockedExposures[device] = exposure; camera.setLockedExposure(exposure); camera.setExposure(device.device.exposureDuration) } } }
}

fun Model.stopObservingExposure() {
    camera.exposureObservation = null
}

fun Model.setAutoWhiteBalance() {
    val device = cameraDevice
    if (
        device == null ||
        !device.device.isWhiteBalanceModeSupported(AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance)
    ) {
        makeErrorToast(
            title = localized("Continuous auto white balance not supported for this camera"),
        )
        return
    }
    try {
        device.device.lockForConfiguration()
        device.device.whiteBalanceMode = AVCaptureDevice.WhiteBalanceMode.continuousAutoWhiteBalance
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for continuous auto white balance: $error")
    }
    camera.isWhiteBalancesLocked[device] = false
    camera.setIsWhiteBalanceLocked(false)
    updateImageButtonState()
}

fun Model.setManualWhiteBalance(factor: Float) {
    val device = cameraDevice
    if (device == null || !device.device.isLockingWhiteBalanceWithCustomDeviceGainsSupported) {
        makeErrorToast(title = localized("Manual white balance not supported for this camera"))
        return
    }
    try {
        device.device.lockForConfiguration()
        device.device.setWhiteBalanceModeLocked(with = factorToWhiteBalance(device = device.device, factor = factor))
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
        Log.i(LOG_TAG, "while locking device for manual white balance: $error")
    }
    camera.isWhiteBalancesLocked[device] = true
    camera.setIsWhiteBalanceLocked(true)
    camera.lockedWhiteBalances[device] = factor
}

fun Model.setWhiteBalanceAfterCameraAttach(device: CaptureDevice) {
    camera.setLockedWhiteBalance(camera.lockedWhiteBalances[device] ?: 0.5f)
    camera.setIsWhiteBalanceLocked(camera.isWhiteBalancesLocked[device] ?: false)
    if (camera.isWhiteBalanceLocked.value) {
        setManualWhiteBalance(factor = camera.lockedWhiteBalance.value)
    }
    if (camera.whiteBalanceObservation != null) {
        stopObservingWhiteBalance()
        startObservingWhiteBalance()
    }
}

fun Model.isCameraSupportingManualWhiteBalance(): Boolean {
    return cameraDevice?.device?.isLockingWhiteBalanceWithCustomDeviceGainsSupported ?: false
}

fun Model.startObservingWhiteBalance() {
    val device = cameraDevice ?: return
    camera.setLockedWhiteBalance(
        factorFromWhiteBalance(
            device = device.device,
            gains = device.device.deviceWhiteBalanceGains.clamped(maxGain = device.device.maxWhiteBalanceGain),
        ),
    )
    camera.whiteBalanceObservation = device.device.observe(AVCaptureDevice::deviceWhiteBalanceGains) { _, _ -> mainScope.launch { if (!camera.editingLockedWhiteBalance) { val factor = factorFromWhiteBalance(device = device.device, gains = device.device.deviceWhiteBalanceGains.clamped(maxGain = device.device.maxWhiteBalanceGain)); camera.lockedWhiteBalances[device] = factor; camera.setLockedWhiteBalance(factor) } } }
}

fun Model.stopObservingWhiteBalance() {
    camera.whiteBalanceObservation = null
}

fun Model.listCameras(position: AVCaptureDevice.Position): List<Camera> {
    return AVCaptureDevice.DiscoverySession(deviceTypes = AVCaptureDevice.DeviceType.entries.toList(), mediaType = com.moblin.android.platform.avfoundation.AVMediaType.video, position = position).devices.map { device -> Camera(id = device.uniqueID, name = device.name()) }
}

fun Model.colorSpaceUpdated() {
    setColorSpace()
    resetSelectedScene(changeScene = false)
}

fun Model.lutEnabledUpdated() {
    if (database.color.lutEnabled && database.color.space == SettingsColorSpace.appleLog) {
        media.registerEffect(lutEffect)
    } else {
        media.unregisterEffect(lutEffect)
    }
}

fun Model.lutUpdated() {
    val lut = getLogLutById(id = database.color.lut)
    if (lut == null) {
        media.unregisterEffect(lutEffect)
        return
    }
    lutEffect.setLut(lut = lut.clone(), imageStorage = imageStorage) { title, subTitle ->
        makeErrorToast(title = title, subTitle = subTitle)
    }
}

fun Model.addLutCube(url: String) {
    val lut = SettingsColorLut(type = SettingsColorLutType.diskCube, name = "My LUT")
    imageStorage.write(id = lut.id, url = File(url))
    database.color.diskLutsCube = database.color.diskLutsCube + lut
    resetSelectedScene()
}

fun Model.removeLutCube(offsets: List<Int>) {
    for (offset in offsets) {
        val lut = database.color.diskLutsCube[offset]
        imageStorage.remove(id = lut.id)
    }
    val offsetsToRemove = offsets.toSet()
    database.color.diskLutsCube = database.color.diskLutsCube.filterIndexed { index, _ ->
        index !in offsetsToRemove
    }
    resetSelectedScene()
}

fun Model.addLutPng(data: ByteArray) {
    val lut = SettingsColorLut(type = SettingsColorLutType.disk, name = "My LUT")
    imageStorage.write(id = lut.id, data = data)
    database.color.diskLutsPng = database.color.diskLutsPng + lut
    resetSelectedScene()
}

fun Model.removeLutPng(offsets: List<Int>) {
    for (offset in offsets) {
        val lut = database.color.diskLutsPng[offset]
        imageStorage.remove(id = lut.id)
    }
    val offsetsToRemove = offsets.toSet()
    database.color.diskLutsPng = database.color.diskLutsPng.filterIndexed { index, _ ->
        index !in offsetsToRemove
    }
    resetSelectedScene()
}

fun Model.setLutName(lut: SettingsColorLut, name: String) {
    lut.name = name
}

fun Model.getLogLutById(id: UUID?): SettingsColorLut? {
    return database.color.allLuts().firstOrNull { it.id == id }
}

fun Model.updateLutsButtonState() {
    var isOn = showingPanel.value == ShowingPanel.luts
    if (database.color.allLuts().any { it.enabled }) {
        isOn = true
    }
    setQuickButton(type = SettingsQuickButtonType.luts, isOn = isOn)
}

fun Model.updateShowCameraPreview(): Boolean {
    val show = shouldShowCameraPreview()
    if (show != this.show.cameraPreview.value) {
        this.show.cameraPreview.value = show
    }
    return show
}

fun Model.toggleCameraPreview() {
    if (database.alwaysAttachCameraPreview) {
        media.setShowCameraPreview(updateShowCameraPreview())
    } else {
        attachCamera()
    }
}

private fun Model.shouldShowCameraPreview(): Boolean {
    val isOn: Boolean = getQuickButton(SettingsQuickButtonType.cameraPreview)?.isOn?.value ?: false
    if (!isOn) {
        return false
    }
    return cameraDevice != null
}

fun Model.updateCameraLists() {
    if (isMac()) {
        externalCameras = mutableListOf()
        backCameras = listCameras(position = AVCaptureDevice.Position.BACK).toMutableList()
        frontCameras = listCameras(position = AVCaptureDevice.Position.FRONT).toMutableList()
    } else {
        externalCameras = listExternalCameras().toMutableList()
        backCameras = listCameras(position = AVCaptureDevice.Position.BACK).toMutableList()
        frontCameras = listCameras(position = AVCaptureDevice.Position.FRONT).toMutableList()
    }
}

private fun Model.listExternalCameras(): List<Camera> {
    return AVCaptureDevice.DiscoverySession(deviceTypes = listOf(AVCaptureDevice.DeviceType.external), mediaType = com.moblin.android.platform.avfoundation.AVMediaType.video, position = AVCaptureDevice.Position.unspecified).devices.map { Camera(id = it.uniqueID, name = it.name()) }
}

fun Model.listCameras(excludeBuiltin: Boolean = false): List<Camera> {
    val cameras = mutableListOf<Camera>()
    if (!excludeBuiltin) {
        if (hasTripleBackCamera) {
            cameras.add(backTripleLowEnergyCamera)
        }
        if (hasDualBackCamera) {
            cameras.add(backDualLowEnergyCamera)
        }
        if (hasWideDualBackCamera) {
            cameras.add(backWideDualLowEnergyCamera)
        }
        cameras.addAll(backCameras)
        cameras.addAll(frontCameras)
        cameras.addAll(externalCameras)
    }
    cameras.addAll(rtmpCameras())
    cameras.addAll(srtlaCameras())
    cameras.addAll(srtClientCameras())
    cameras.addAll(ristCameras())
    cameras.addAll(rtspCameras())
    cameras.addAll(whipCameras())
    cameras.addAll(whepCameras())
    cameras.addAll(playerCameras())
    cameras.add(Camera(id = screenCaptureCameraId.toString(), name = screenCaptureCameraName))
    cameras.add(Camera(id = noneCameraId.toString(), name = noneCameraName))
    return cameras
}

private fun Model.isBackCamera(cameraId: CameraId): Boolean {
    return backCameras.any { it.id == cameraId }
}

private fun Model.isFrontCamera(cameraId: CameraId): Boolean {
    return frontCameras.any { it.id == cameraId }
}

private fun Model.isBackTripleLowEnergyAutoCamera(cameraId: CameraId): Boolean {
    return cameraId == backTripleLowEnergyCamera.id
}

private fun Model.isBackDualLowEnergyAutoCamera(cameraId: CameraId): Boolean {
    return cameraId == backDualLowEnergyCamera.id
}

private fun Model.isBackWideDualLowEnergyAutoCamera(cameraId: CameraId): Boolean {
    return cameraId == backWideDualLowEnergyCamera.id
}

fun Model.getCameraId(scene: SettingsScene?): CameraId {
    return getCameraId(settingsCameraId = scene?.toCameraId())
}

fun Model.getCameraId(videoSourceWidget: SettingsWidgetVideoSource?): CameraId {
    return getCameraId(settingsCameraId = videoSourceWidget?.toCameraId())
}

fun Model.getCameraId(vTuberWidget: SettingsWidgetVTuber?): CameraId {
    return getCameraId(settingsCameraId = vTuberWidget?.toCameraId())
}

fun Model.getCameraId(pngTuberWidget: SettingsWidgetPngTuber?): CameraId {
    return getCameraId(settingsCameraId = pngTuberWidget?.toCameraId())
}

fun Model.cameraIdToSettingsCameraId(cameraId: CameraId): SettingsCameraId {
    val srtlaId = getSrtlaStream(idString = cameraId)?.id
    if (srtlaId != null) {
        return SettingsCameraId.Srtla(id = srtlaId)
    }
    val srtId = getSrtClientStream(idString = cameraId)?.id
    if (srtId != null) {
        return SettingsCameraId.Srt(id = srtId)
    }
    val rtmpId = runCatching { UUID.fromString(cameraId) }.getOrNull()?.let { id ->
        getRtmpStream(id = id)?.id
    }
    if (rtmpId != null) {
        return SettingsCameraId.Rtmp(id = rtmpId)
    }
    val ristId = getRistStream(idString = cameraId)?.id
    if (ristId != null) {
        return SettingsCameraId.Rist(id = ristId)
    }
    val rtspId = getRtspStream(idString = cameraId)?.id
    if (rtspId != null) {
        return SettingsCameraId.Rtsp(id = rtspId)
    }
    val whipId = getWhipStream(idString = cameraId)?.id
    if (whipId != null) {
        return SettingsCameraId.Whip(id = whipId)
    }
    val whepId = getWhepStream(idString = cameraId)?.id
    if (whepId != null) {
        return SettingsCameraId.Whep(id = whepId)
    }
    val mediaPlayerId = getMediaPlayer(idString = cameraId)?.id
    if (mediaPlayerId != null) {
        return SettingsCameraId.MediaPlayer(id = mediaPlayerId)
    }
    if (isBackCamera(cameraId = cameraId)) {
        return SettingsCameraId.Back(id = cameraId)
    }
    if (isFrontCamera(cameraId = cameraId)) {
        return SettingsCameraId.Front(id = cameraId)
    }
    if (isScreenCaptureCamera(cameraId = cameraId)) {
        return SettingsCameraId.ScreenCapture
    }
    if (isBackTripleLowEnergyAutoCamera(cameraId = cameraId)) {
        return SettingsCameraId.BackTripleLowEnergy
    }
    if (isBackDualLowEnergyAutoCamera(cameraId = cameraId)) {
        return SettingsCameraId.BackDualLowEnergy
    }
    if (isBackWideDualLowEnergyAutoCamera(cameraId = cameraId)) {
        return SettingsCameraId.BackWideDualLowEnergy
    }
    if (isNoneCamera(cameraId = cameraId)) {
        return SettingsCameraId.None
    }
    return SettingsCameraId.External(id = cameraId, name = getExternalCameraName(cameraId = cameraId))
}

fun Model.cameraIdToSettingsCameraId(cameraId: UUID): SettingsCameraId? {
    val srtlaId = getSrtlaStream(id = cameraId)?.id
    if (srtlaId != null) {
        return SettingsCameraId.Srtla(id = srtlaId)
    }
    val srtId = getSrtClientStream(id = cameraId)?.id
    if (srtId != null) {
        return SettingsCameraId.Srt(id = srtId)
    }
    val rtmpId = getRtmpStream(id = cameraId)?.id
    if (rtmpId != null) {
        return SettingsCameraId.Rtmp(id = rtmpId)
    }
    val ristId = getRistStream(id = cameraId)?.id
    if (ristId != null) {
        return SettingsCameraId.Rist(id = ristId)
    }
    val rtspId = getRtspStream(id = cameraId)?.id
    if (rtspId != null) {
        return SettingsCameraId.Rtsp(id = rtspId)
    }
    val whipId = getWhipStream(id = cameraId)?.id
    if (whipId != null) {
        return SettingsCameraId.Whip(id = whipId)
    }
    val whepId = getWhepStream(id = cameraId)?.id
    if (whepId != null) {
        return SettingsCameraId.Whep(id = whepId)
    }
    val mediaPlayerId = getMediaPlayer(id = cameraId)?.id
    if (mediaPlayerId != null) {
        return SettingsCameraId.MediaPlayer(id = mediaPlayerId)
    }
    if (isScreenCaptureCamera(cameraId = cameraId.toString())) {
        return SettingsCameraId.ScreenCapture
    }
    if (isNoneCamera(cameraId = cameraId.toString())) {
        return SettingsCameraId.None
    }
    val deviceUniqueId = getBuiltinDeviceUniqueId(cameraId = cameraId)
    if (deviceUniqueId != null) {
        return cameraIdToSettingsCameraId(cameraId = deviceUniqueId)
    }
    return null
}

private fun Model.getCameraId(settingsCameraId: SettingsCameraId?): CameraId {
    if (settingsCameraId == null) {
        return ""
    }
    return when (settingsCameraId) {
        is SettingsCameraId.Rtmp -> settingsCameraId.id.toString()
        is SettingsCameraId.Srtla -> settingsCameraId.id.toString()
        is SettingsCameraId.Srt -> settingsCameraId.id.toString()
        is SettingsCameraId.Rist -> settingsCameraId.id.toString()
        is SettingsCameraId.Rtsp -> settingsCameraId.id.toString()
        is SettingsCameraId.Whip -> settingsCameraId.id.toString()
        is SettingsCameraId.Whep -> settingsCameraId.id.toString()
        is SettingsCameraId.MediaPlayer -> settingsCameraId.id.toString()
        is SettingsCameraId.External -> settingsCameraId.id
        is SettingsCameraId.Back -> settingsCameraId.id
        is SettingsCameraId.Front -> settingsCameraId.id
        SettingsCameraId.ScreenCapture -> screenCaptureCameraId.toString()
        SettingsCameraId.BackTripleLowEnergy -> backTripleLowEnergyCamera.id
        SettingsCameraId.BackDualLowEnergy -> backDualLowEnergyCamera.id
        SettingsCameraId.BackWideDualLowEnergy -> backWideDualLowEnergyCamera.id
        SettingsCameraId.None -> noneCameraId.toString()
    }
}

fun Model.getCameraPositionName(scene: SettingsScene?): String {
    return getCameraPositionName(settingsCameraId = scene?.toCameraId())
}

fun Model.getCameraPositionName(videoSourceWidget: SettingsWidgetVideoSource?): String {
    return getCameraPositionName(settingsCameraId = videoSourceWidget?.toCameraId())
}

fun Model.getCameraPositionName(vTuberWidget: SettingsWidgetVTuber?): String {
    return getCameraPositionName(settingsCameraId = vTuberWidget?.toCameraId())
}

fun Model.getCameraPositionName(pngTuberWidget: SettingsWidgetPngTuber?): String {
    return getCameraPositionName(settingsCameraId = pngTuberWidget?.toCameraId())
}

private fun Model.getCameraPositionName(settingsCameraId: SettingsCameraId?): String {
    if (settingsCameraId == null) {
        return unknownSad
    }
    return when (settingsCameraId) {
        is SettingsCameraId.Rtmp -> getRtmpStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Srtla -> getSrtlaStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Srt -> getSrtClientStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Rist -> getRistStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Rtsp -> getRtspStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Whip -> getWhipStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.Whep -> getWhepStream(id = settingsCameraId.id)?.camera() ?: unknownSad
        is SettingsCameraId.MediaPlayer -> getMediaPlayer(id = settingsCameraId.id)?.camera()
            ?: unknownSad

        is SettingsCameraId.External -> if (settingsCameraId.name.isNotEmpty()) {
            settingsCameraId.name
        } else {
            unknownSad
        }

        is SettingsCameraId.Back -> backCameras.firstOrNull { it.id == settingsCameraId.id }?.name
            ?: unknownSad

        is SettingsCameraId.Front -> frontCameras.firstOrNull { it.id == settingsCameraId.id }?.name
            ?: unknownSad

        SettingsCameraId.ScreenCapture -> screenCaptureCameraName
        SettingsCameraId.BackTripleLowEnergy -> backTripleLowEnergyCamera.name
        SettingsCameraId.BackDualLowEnergy -> backDualLowEnergyCamera.name
        SettingsCameraId.BackWideDualLowEnergy -> backWideDualLowEnergyCamera.name
        SettingsCameraId.None -> noneCameraName
    }
}

fun Model.getExternalCameraName(cameraId: CameraId): String {
    return externalCameras.firstOrNull { it.id == cameraId }?.name ?: unknownSad
}

fun Model.isExternalCameraConnected(cameraId: String): Boolean {
    return externalCameras.firstOrNull { it.id == cameraId } != null
}

fun Model.setColorSpace() {
    media.setColorSpace(colorSpace = MediaFormat.COLOR_STANDARD_BT709) {
        setCameraZoomX(x = zoom.x.value)?.let { x ->
            setZoomXWhenInRange(x = x)
        }
        lutEnabledUpdated()
    }
}

private fun Model.getBuiltinCameraId(deviceUniqueId: String): UUID {
    builtinCameraIds[deviceUniqueId]?.let { return it }
    val cameraId = UUID.randomUUID()
    builtinCameraIds[deviceUniqueId] = cameraId
    return cameraId
}

private fun Model.getBuiltinDeviceUniqueId(cameraId: UUID): String? {
    return builtinCameraIds.entries.firstOrNull { it.value == cameraId }?.key
}

fun Model.makeCaptureDevice(device: CaptureDevice): CaptureDevice {
    return CaptureDevice(
        device = device.device,
        id = getBuiltinCameraId(deviceUniqueId = (device.device as AVCaptureDevice).uniqueID),
        isVideoMirrored = getVideoMirroredOnStream(device = device),
    )
}

private fun Model.statusCameraText(): String {
    return getCameraPositionName(scene = findEnabledScene(id = sceneSelector.selectedSceneId))
}

fun Model.updateStatusCameraText() {
    val status = statusCameraText()
    if (status != statusTopLeft.statusCameraText.value) {
        statusTopLeft.statusCameraText.value = status
    }
}

fun Model.getVideoSourceId(cameraId: SettingsCameraId): UUID? {
    return when (cameraId) {
        is SettingsCameraId.Rtmp -> cameraId.id
        is SettingsCameraId.Srtla -> cameraId.id
        is SettingsCameraId.Srt -> cameraId.id
        is SettingsCameraId.Rist -> cameraId.id
        is SettingsCameraId.Rtsp -> cameraId.id
        is SettingsCameraId.Whip -> cameraId.id
        is SettingsCameraId.Whep -> cameraId.id
        is SettingsCameraId.MediaPlayer -> cameraId.id
        SettingsCameraId.ScreenCapture -> screenCaptureCameraId
        is SettingsCameraId.Back -> getBuiltinCameraId(deviceUniqueId = cameraId.id)
        is SettingsCameraId.Front -> getBuiltinCameraId(deviceUniqueId = cameraId.id)
        is SettingsCameraId.External -> getBuiltinCameraId(deviceUniqueId = cameraId.id)
        SettingsCameraId.BackDualLowEnergy -> null
        SettingsCameraId.BackTripleLowEnergy -> null
        SettingsCameraId.BackWideDualLowEnergy -> null
        SettingsCameraId.None -> noneCameraId
    }
}

fun Model.setExposureBias(bias: Float) {
    val device = cameraDevice ?: return
    if (bias < device.device.minExposureTargetBias) {
        return
    }
    if (bias > device.device.maxExposureTargetBias) {
        return
    }
    try {
        device.device.lockForConfiguration()
        device.device.setExposureTargetBias(bias)
        device.device.unlockForConfiguration()
    } catch (error: Exception) {
    }
}
