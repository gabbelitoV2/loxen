package com.moblin.android.platform.capture

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.SizeF
import com.moblin.android.AppDelegate
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVMediaType
import java.util.UUID
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
class CameraCatalogLensesSuite {
    private fun addCamera(facing: Int, zoomRatioRange: Range<Float>? = null, focalLength: Float? = null): String {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadow = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadow.set(CameraCharacteristics.LENS_FACING, facing)
        zoomRatioRange?.let { shadow.set(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE, it) }
        focalLength?.let {
            shadow.set(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS, floatArrayOf(it))
            shadow.set(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, SizeF(6f, 4.5f))
        }
        val id = "lenses-${UUID.randomUUID()}"
        val manager = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowOf(manager).addCamera(id, characteristics)
        return id
    }

    private fun default(deviceType: AVCaptureDevice.DeviceType): AVCaptureDevice? {
        return AVCaptureDevice.default(deviceType, AVMediaType.video, AVCaptureDevice.Position.back)
    }

    private fun assertCloseList(expected: List<Float>, actual: List<Float>) {
        assertEquals(expected.size, actual.size, "expected $expected but was $actual")
        for ((e, a) in expected.zip(actual)) {
            assertTrue(abs(e - a) < 1e-3f, "expected $expected but was $actual")
        }
    }

    @Test
    fun telephotoLensesAreThoseClearlyNarrowerThanTheMainCamera() {
        assertCloseList(listOf(3f), CameraCatalog.telephotoZoomRatios(listOf(2f, 1f, 1f / 3f), zoomRatioLower = 0.5f))
        assertCloseList(listOf(2f), CameraCatalog.telephotoZoomRatios(listOf(1f, 0.5f), zoomRatioLower = 1f))
        assertCloseList(
            listOf(2f, 5f),
            CameraCatalog.telephotoZoomRatios(listOf(0.2f, 1.5f, 1f, 0.5f), zoomRatioLower = 2f / 3f),
        )
        assertEquals(emptyList(), CameraCatalog.telephotoZoomRatios(listOf(2f, 1f), zoomRatioLower = 0.5f))
        assertEquals(emptyList(), CameraCatalog.telephotoZoomRatios(listOf(1f, 0.9f), zoomRatioLower = 1f))
        assertEquals(emptyList(), CameraCatalog.telephotoZoomRatios(emptyList(), zoomRatioLower = 0.5f))
    }

    @Test
    fun aMultiCameraIsNamedAfterTheLensesItReaches() {
        val back = CameraCharacteristics.LENS_FACING_BACK
        fun type(zoomRatioLower: Float, telephotos: List<Float>) = CameraCatalog.deviceType(
            facing = back,
            zoomRatioLower = zoomRatioLower,
            telephotoZoomRatios = telephotos,
            fieldOfViewMetric = 1f,
            referenceFieldOfView = 1f,
        )
        assertEquals(AVCaptureDevice.DeviceType.builtInTripleCamera, type(0.5f, listOf(3f)))
        assertEquals(AVCaptureDevice.DeviceType.builtInDualWideCamera, type(0.5f, emptyList()))
        assertEquals(AVCaptureDevice.DeviceType.builtInDualCamera, type(1f, listOf(2f)))
        assertEquals(AVCaptureDevice.DeviceType.builtInWideAngleCamera, type(1f, emptyList()))
        assertEquals(
            AVCaptureDevice.DeviceType.external,
            CameraCatalog.deviceType(CameraCharacteristics.LENS_FACING_EXTERNAL, 0.5f, listOf(3f), 1f, 1f),
        )
        assertEquals(
            AVCaptureDevice.DeviceType.builtInUltraWideCamera,
            CameraCatalog.deviceType(back, 1f, emptyList(), fieldOfViewMetric = 2f, referenceFieldOfView = 1f),
        )
        assertEquals(
            AVCaptureDevice.DeviceType.builtInTelephotoCamera,
            CameraCatalog.deviceType(back, 1f, emptyList(), fieldOfViewMetric = 0.4f, referenceFieldOfView = 1f),
        )
    }

    @Test
    fun switchOverFactorsAreTheLensesZoomRatiosInDeviceZoomFactors() {
        assertEquals(listOf(2f, 6f), CameraCatalog.switchOverZoomFactors(0.5f, 0.5f, listOf(3f)))
        assertEquals(listOf(2f), CameraCatalog.switchOverZoomFactors(0.5f, 0.5f, emptyList()))
        assertEquals(listOf(2f), CameraCatalog.switchOverZoomFactors(1f, 1f, listOf(2f)))
        assertEquals(emptyList(), CameraCatalog.switchOverZoomFactors(1f, 1f, emptyList()))
    }

    @Test
    fun aBackCameraThatZoomsBelowOneStandsInForTheLensesBehindIt() {
        val id = addCamera(CameraCharacteristics.LENS_FACING_BACK, zoomRatioRange = Range(0.5f, 10f))
        val camera = AVCaptureDevice.withUniqueID(id)!!
        assertEquals(AVCaptureDevice.DeviceType.builtInDualWideCamera, camera.deviceType)
        assertEquals(listOf(2f), camera.virtualDeviceSwitchOverVideoZoomFactors)
        assertSame(camera, default(AVCaptureDevice.DeviceType.builtInDualWideCamera))
        assertSame(camera, default(AVCaptureDevice.DeviceType.builtInUltraWideCamera))
        assertSame(camera, default(AVCaptureDevice.DeviceType.builtInWideAngleCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInTelephotoCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInTripleCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInDualCamera))
    }

    @Test
    fun defaultBackZoomPresetsOfAMultiCameraStayWithinItsZoomRange() {
        addCamera(CameraCharacteristics.LENS_FACING_BACK, zoomRatioRange = Range(0.5f, 10f))
        assertEquals(listOf(2f, 4f), com.moblin.android.platform.Cameras.backCameraSwitchOverZoomFactors())
    }

    @Test
    fun aSeparateLensIsPreferredOverTheMultiCamera() {
        val wide = addCamera(CameraCharacteristics.LENS_FACING_BACK, focalLength = 4f)
        val telephoto = addCamera(CameraCharacteristics.LENS_FACING_BACK, focalLength = 16f)
        val logical = addCamera(CameraCharacteristics.LENS_FACING_BACK, zoomRatioRange = Range(0.5f, 10f), focalLength = 2f)
        assertEquals(AVCaptureDevice.DeviceType.builtInDualWideCamera, AVCaptureDevice.withUniqueID(logical)!!.deviceType)
        assertSame(AVCaptureDevice.withUniqueID(wide), default(AVCaptureDevice.DeviceType.builtInWideAngleCamera))
        assertSame(AVCaptureDevice.withUniqueID(telephoto), default(AVCaptureDevice.DeviceType.builtInTelephotoCamera))
        assertSame(AVCaptureDevice.withUniqueID(logical), default(AVCaptureDevice.DeviceType.builtInUltraWideCamera))
    }

    @Test
    fun aSingleBackCameraIsOnlyTheWideCamera() {
        val id = addCamera(CameraCharacteristics.LENS_FACING_BACK)
        val camera = AVCaptureDevice.withUniqueID(id)!!
        assertEquals(AVCaptureDevice.DeviceType.builtInWideAngleCamera, camera.deviceType)
        assertEquals(emptyList(), camera.virtualDeviceSwitchOverVideoZoomFactors)
        assertSame(camera, default(AVCaptureDevice.DeviceType.builtInWideAngleCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInUltraWideCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInTelephotoCamera))
        assertNull(default(AVCaptureDevice.DeviceType.builtInDualWideCamera))
    }
}
