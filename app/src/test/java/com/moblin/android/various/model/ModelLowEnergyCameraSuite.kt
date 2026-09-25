package com.moblin.android.various.model

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Looper
import android.util.Range
import androidx.camera.core.CameraSelector
import com.moblin.android.AppDelegate
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.various.utils.getUIZoomRange
import com.moblin.android.various.utils.getZoomFactorScale
import com.moblin.android.various.utils.hasUltraWideBackCamera
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
class ModelLowEnergyCameraSuite {
    private lateinit var model: Model
    private lateinit var multiCamera: AVCaptureDevice

    private fun addCamera(facing: Int, zoomRatioRange: Range<Float>? = null): AVCaptureDevice {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadow = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadow.set(CameraCharacteristics.LENS_FACING, facing)
        zoomRatioRange?.let { shadow.set(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE, it) }
        val id = "low-power-${UUID.randomUUID()}"
        val manager = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowOf(manager).addCamera(id, characteristics)
        return AVCaptureDevice.withUniqueID(id)!!
    }

    private fun selectScene(position: SettingsSceneCameraPosition): SettingsScene {
        val scene = SettingsScene(name = "Low power")
        scene.videoSource = scene.videoSource.copy(cameraPosition = position)
        model.database.scenes.add(scene)
        model.sceneSelector.selectedSceneId = scene.id
        return scene
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Before
    fun setUp() {
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        multiCamera = addCamera(CameraCharacteristics.LENS_FACING_BACK, zoomRatioRange = Range(0.5f, 10f))
        model.database.zoom.switchToBack.enabled = false
    }

    @Test
    fun wideDualLowPowerAttachesTheBackMultiCameraWithItsZoomRange() {
        selectScene(SettingsSceneCameraPosition.backWideDualLowEnergy)
        assertNull(model.cameraDevice)
        for (backX in listOf(0.5f, 1f, 4f)) {
            model.zoom.backX = backX
            model.attachBackWideDualLowEnergyCamera()
            runMain()
            assertEquals(CameraSelector.LENS_FACING_BACK, model.cameraPosition)
            assertSame(multiCamera, model.cameraDevice?.device)
            assertEquals(backX, model.zoom.x.value)
            assertEquals(multiCamera.getZoomFactorScale(hasUltraWideBackCamera), model.cameraZoomLevelToXScale)
            val range = multiCamera.getUIZoomRange(hasUltraWideBackCamera)
            assertEquals(range.first, model.cameraZoomXMinimum)
            assertEquals(range.second, model.cameraZoomXMaximum)
        }
    }

    @Test
    fun withoutForceTheSameCameraIsNotAttachedAgain() {
        selectScene(SettingsSceneCameraPosition.backWideDualLowEnergy)
        model.attachBackWideDualLowEnergyCamera()
        runMain()
        val attached = model.cameraDevice
        model.cameraZoomLevelToXScale = 7f
        model.attachBackWideDualLowEnergyCamera(force = false)
        runMain()
        assertSame(attached, model.cameraDevice)
        assertEquals(7f, model.cameraZoomLevelToXScale)
    }

    @Test
    fun withoutForceAnotherCameraIsReplaced() {
        val front = addCamera(CameraCharacteristics.LENS_FACING_FRONT)
        selectScene(SettingsSceneCameraPosition.backWideDualLowEnergy)
        model.cameraDevice = CaptureDevice(device = front, id = UUID.randomUUID(), isVideoMirrored = false)
        model.attachBackWideDualLowEnergyCamera(force = false)
        runMain()
        assertSame(multiCamera, model.cameraDevice?.device)
    }

    @Test
    fun aZoomPresetReattachesTheLowPowerCamera() {
        val front = addCamera(CameraCharacteristics.LENS_FACING_FRONT)
        selectScene(SettingsSceneCameraPosition.backWideDualLowEnergy)
        model.cameraPosition = CameraSelector.LENS_FACING_BACK
        model.cameraDevice = CaptureDevice(device = front, id = UUID.randomUUID(), isVideoMirrored = false)
        val preset = SettingsZoomPreset(id = UUID.randomUUID(), name = "2x", x = 2f)
        model.database.zoom.back = mutableListOf(preset)
        model.setZoomPreset(id = preset.id)
        runMain()
        assertEquals(2f, model.zoom.backX)
        assertSame(multiCamera, model.cameraDevice?.device)
    }

    @Test
    fun withoutTheMultiCameraOrASceneNothingIsAttached() {
        model.attachBackWideDualLowEnergyCamera()
        assertNull(model.cameraDevice)
        selectScene(SettingsSceneCameraPosition.backTripleLowEnergy)
        model.attachBackTripleLowEnergyCamera()
        assertNull(model.cameraDevice)
        assertEquals(CameraSelector.LENS_FACING_BACK, model.cameraPosition)
        selectScene(SettingsSceneCameraPosition.backDualLowEnergy)
        model.attachBackDualLowEnergyCamera()
        assertNull(model.cameraDevice)
    }
}
