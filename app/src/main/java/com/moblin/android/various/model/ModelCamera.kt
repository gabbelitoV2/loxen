package com.moblin.android.various.model

import android.graphics.PointF
import android.util.Log
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.media.haishinkit.media.video.CaptureDevicePosition
import com.moblin.android.media.haishinkit.media.video.CaptureSessionDevice
import com.moblin.android.various.settings.SettingsCameraId
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorLutType
import com.moblin.android.various.settings.SettingsColorSpace
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.various.settings.SettingsWidgetVideoSource
import com.moblin.android.various.settings.toCameraId
import com.moblin.android.various.utils.exposureFactorStep
import com.moblin.android.various.utils.hasDualBackCamera
import com.moblin.android.various.utils.hasTripleBackCamera
import com.moblin.android.various.utils.hasWideDualBackCamera
import com.moblin.android.various.utils.isMac
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LOG_TAG = "Model"

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
    val isFocusesLocked: MutableMap<CaptureSessionDevice, Boolean> = mutableMapOf()
    val lockedFocuses: MutableMap<CaptureSessionDevice, Float> = mutableMapOf()
    var editingLockedFocus = false
    var focusObservation: Any? = null
    val isExposuresAndIsosLocked: MutableMap<CaptureSessionDevice, Boolean> = mutableMapOf()
    val lockedIsos: MutableMap<CaptureSessionDevice, Float> = mutableMapOf()
    var editingLockedIso = false
    var isoObservation: Any? = null
    val lockedExposures: MutableMap<CaptureSessionDevice, Float> = mutableMapOf()
    var editingLockedExposure = false
    var exposureObservation: Any? = null
    val isWhiteBalancesLocked: MutableMap<CaptureSessionDevice, Boolean> = mutableMapOf()
    val lockedWhiteBalances: MutableMap<CaptureSessionDevice, Float> = mutableMapOf()
    var editingLockedWhiteBalance = false
    var whiteBalanceObservation: Any? = null
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
    if (cameraDevice == null) {
        Log.i(LOG_TAG, "Tap to focus not supported for this camera")
        makeErrorToast(title = localized("Tap to focus not supported for this camera"))
        return
    }
    TODO("no Android counterpart for AVCaptureDevice focusPointOfInterest configuration")
}

fun Model.setAutoFocus() {
    TODO("no Android counterpart for AVCaptureDevice continuousAutoFocus configuration")
}

fun Model.setManualFocus(lensPosition: Float) {
    TODO("no Android counterpart for AVCaptureDevice setFocusModeLocked(lensPosition:)")
}

fun Model.setFocusAfterCameraAttach() {
    TODO("no Android counterpart for AVCaptureDevice lensPosition")
}

fun Model.isCameraSupportingManualFocus(): Boolean {
    TODO("no Android counterpart for AVCaptureDevice isLockingFocusWithCustomLensPositionSupported")
}

fun Model.startObservingFocus() {
    TODO("no Android counterpart for KVO observation of AVCaptureDevice lensPosition")
}

fun Model.stopObservingFocus() {
    camera.focusObservation = null
}

fun Model.setAutoExposureAndIso() {
    TODO("no Android counterpart for AVCaptureDevice continuousAutoExposure configuration")
}

fun Model.setExposureAndIsoAfterCameraAttach(device: CaptureSessionDevice) {
    TODO("no Android counterpart for AVCaptureDevice iso and exposureDuration")
}

fun Model.isCameraSupportingManualExposureAndIso(): Boolean {
    TODO("no Android counterpart for AVCaptureDevice isExposureModeSupported(.custom)")
}

private fun Model.setManualExposureAndIso(exposureFactor: Float?, isoFactor: Float?) {
    TODO("no Android counterpart for AVCaptureDevice setExposureModeCustom(duration:iso:)")
}

fun Model.setManualIso(factor: Float) {
    setManualExposureAndIso(exposureFactor = null, isoFactor = factor)
}

fun Model.startObservingIso() {
    TODO("no Android counterpart for KVO observation of AVCaptureDevice iso")
}

fun Model.stopObservingIso() {
    camera.isoObservation = null
}

fun Model.setManualExposure(factor: Float) {
    setManualExposureAndIso(exposureFactor = factor, isoFactor = null)
}

fun Model.getExposureFactorStep(): Float {
    val device = cameraDevice ?: return 0.01f
    return exposureFactorStep(device = device)
}

fun Model.startObservingExposure() {
    TODO("no Android counterpart for KVO observation of AVCaptureDevice exposureDuration")
}

fun Model.stopObservingExposure() {
    camera.exposureObservation = null
}

fun Model.setAutoWhiteBalance() {
    TODO("no Android counterpart for AVCaptureDevice continuousAutoWhiteBalance configuration")
}

fun Model.setManualWhiteBalance(factor: Float) {
    TODO("no Android counterpart for AVCaptureDevice setWhiteBalanceModeLocked(with:)")
}

fun Model.setWhiteBalanceAfterCameraAttach(device: CaptureSessionDevice) {
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
    TODO("no Android counterpart for AVCaptureDevice isLockingWhiteBalanceWithCustomDeviceGainsSupported")
}

fun Model.startObservingWhiteBalance() {
    TODO("no Android counterpart for KVO observation of AVCaptureDevice deviceWhiteBalanceGains")
}

fun Model.stopObservingWhiteBalance() {
    camera.whiteBalanceObservation = null
}

fun Model.listCameras(position: CaptureDevicePosition): List<Camera> {
    TODO("no Android counterpart for AVCaptureDevice.DiscoverySession")
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
    imageStorage.write(id = lut.id, url = url)
    database.color.diskLutsCube.add(lut)
    resetSelectedScene()
}

fun Model.removeLutCube(offsets: List<Int>) {
    for (offset in offsets) {
        val lut = database.color.diskLutsCube[offset]
        imageStorage.remove(id = lut.id)
    }
    offsets.sortedDescending().forEach { database.color.diskLutsCube.removeAt(it) }
    resetSelectedScene()
}

fun Model.addLutPng(data: ByteArray) {
    val lut = SettingsColorLut(type = SettingsColorLutType.disk, name = "My LUT")
    imageStorage.write(id = lut.id, data = data)
    database.color.diskLutsPng.add(lut)
    resetSelectedScene()
}

fun Model.removeLutPng(offsets: List<Int>) {
    for (offset in offsets) {
        val lut = database.color.diskLutsPng[offset]
        imageStorage.remove(id = lut.id)
    }
    offsets.sortedDescending().forEach { database.color.diskLutsPng.removeAt(it) }
    resetSelectedScene()
}

fun Model.setLutName(lut: SettingsColorLut, name: String) {
    lut.name = name
}

fun Model.getLogLutById(id: UUID?): SettingsColorLut? {
    return database.color.allLuts().firstOrNull { it.id == id }
}

fun Model.updateLutsButtonState() {
    var isOn = showingPanel == ShowingPanel.luts
    if (database.color.allLuts().any { it.enabled }) {
        isOn = true
    }
    setQuickButton(type = QuickButtonType.luts, isOn = isOn)
}

fun Model.updateShowCameraPreview(): Boolean {
    val show = shouldShowCameraPreview()
    if (show != this.show.cameraPreview) {
        this.show.cameraPreview = show
    }
    return show
}

fun Model.toggleCameraPreview() {
    if (database.alwaysAttachCameraPreview) {
        media.setShowCameraPreview(updateShowCameraPreview())
    } else {
        reattachCamera()
    }
}

private fun Model.shouldShowCameraPreview(): Boolean {
    if (!(getQuickButton(type = QuickButtonType.cameraPreview)?.isOn ?: false)) {
        return false
    }
    return cameraDevice != null
}

fun Model.updateCameraLists() {
    if (isMac()) {
        externalCameras = emptyList()
        backCameras = listCameras(position = CaptureDevicePosition.back)
        frontCameras = listCameras(position = CaptureDevicePosition.front)
    } else {
        externalCameras = listExternalCameras()
        backCameras = listCameras(position = CaptureDevicePosition.back)
        frontCameras = listCameras(position = CaptureDevicePosition.front)
    }
}

private fun Model.listExternalCameras(): List<Camera> {
    TODO("no Android counterpart for AVCaptureDevice.DiscoverySession with external devices")
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
    val rtmpId = getRtmpStream(idString = cameraId)?.id
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
    media.setColorSpace(colorSpace = database.color.space) {
        setCameraZoomX(x = zoom.x)?.let { x ->
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

fun Model.makeCaptureDevice(device: CaptureSessionDevice): CaptureDevice {
    return CaptureDevice(
        device = device,
        id = getBuiltinCameraId(deviceUniqueId = device.uniqueID),
        isVideoMirrored = getVideoMirroredOnStream(device = device),
    )
}

private fun Model.statusCameraText(): String {
    return getCameraPositionName(scene = findEnabledScene(id = sceneSelector.selectedSceneId))
}

fun Model.updateStatusCameraText() {
    val status = statusCameraText()
    if (status != statusTopLeft.statusCameraText) {
        statusTopLeft.statusCameraText = status
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
    TODO("no Android counterpart for AVCaptureDevice setExposureTargetBias(_:)")
}
