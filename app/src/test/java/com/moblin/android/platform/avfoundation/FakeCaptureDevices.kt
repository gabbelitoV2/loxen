package com.moblin.android.platform.avfoundation

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.util.Range
import android.util.Rational
import com.moblin.android.AppDelegate
import java.util.UUID
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCaptureResult
import org.robolectric.shadows.ShadowTotalCaptureResult

object FakeCaptureDevices {
    fun make(
        minimumFocusDistance: Float = 10f,
        facing: Int = CameraCharacteristics.LENS_FACING_EXTERNAL,
        sensorOrientation: Int? = null,
        capabilities: IntArray? = null,
        hardwareLevel: Int? = null,
        afModes: IntArray? = null,
        aeModes: IntArray? = null,
        awbModes: IntArray? = null,
        maxRegionsAf: Int? = null,
        maxRegionsAe: Int? = null,
        aeLockAvailable: Boolean? = null,
        awbLockAvailable: Boolean? = null,
        sensitivityRange: Range<Int>? = null,
        exposureTimeRange: Range<Long>? = null,
        compensationRange: Range<Int>? = null,
        compensationStep: Rational? = null,
        activeArraySize: Rect? = null,
    ): AVCaptureDevice {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadow = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadow.set(CameraCharacteristics.LENS_FACING, facing)
        shadow.set(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE, minimumFocusDistance)
        sensorOrientation?.let { shadow.set(CameraCharacteristics.SENSOR_ORIENTATION, it) }
        capabilities?.let { shadow.set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, it) }
        hardwareLevel?.let { shadow.set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, it) }
        afModes?.let { shadow.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, it) }
        aeModes?.let { shadow.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, it) }
        awbModes?.let { shadow.set(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, it) }
        maxRegionsAf?.let { shadow.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, it) }
        maxRegionsAe?.let { shadow.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, it) }
        aeLockAvailable?.let { shadow.set(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE, it) }
        awbLockAvailable?.let { shadow.set(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE, it) }
        sensitivityRange?.let { shadow.set(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE, it) }
        exposureTimeRange?.let { shadow.set(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE, it) }
        compensationRange?.let { shadow.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, it) }
        compensationStep?.let { shadow.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, it) }
        activeArraySize?.let { shadow.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, it) }
        val id = "fake-${UUID.randomUUID()}"
        val manager = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowOf(manager).addCamera(id, characteristics)
        return AVCaptureDevice.withUniqueID(id)!!
    }

    fun makeManual(
        minimumFocusDistance: Float = 10f,
        facing: Int = CameraCharacteristics.LENS_FACING_BACK,
        sensorOrientation: Int = 90,
        capabilities: IntArray = intArrayOf(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING,
        ),
        hardwareLevel: Int = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        afModes: IntArray = intArrayOf(
            CameraMetadata.CONTROL_AF_MODE_OFF,
            CameraMetadata.CONTROL_AF_MODE_AUTO,
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
        ),
        aeModes: IntArray = intArrayOf(CameraMetadata.CONTROL_AE_MODE_OFF, CameraMetadata.CONTROL_AE_MODE_ON),
        awbModes: IntArray = intArrayOf(CameraMetadata.CONTROL_AWB_MODE_OFF, CameraMetadata.CONTROL_AWB_MODE_AUTO),
        maxRegionsAf: Int = 1,
        maxRegionsAe: Int = 1,
        aeLockAvailable: Boolean = true,
        awbLockAvailable: Boolean = true,
        sensitivityRange: Range<Int> = Range(50, 3200),
        exposureTimeRange: Range<Long> = Range(10_000L, 500_000_000L),
        compensationRange: Range<Int> = Range(-12, 12),
        compensationStep: Rational = Rational(1, 6),
        activeArraySize: Rect = Rect(0, 0, 4000, 3000),
    ): AVCaptureDevice {
        return make(
            minimumFocusDistance = minimumFocusDistance,
            facing = facing,
            sensorOrientation = sensorOrientation,
            capabilities = capabilities,
            hardwareLevel = hardwareLevel,
            afModes = afModes,
            aeModes = aeModes,
            awbModes = awbModes,
            maxRegionsAf = maxRegionsAf,
            maxRegionsAe = maxRegionsAe,
            aeLockAvailable = aeLockAvailable,
            awbLockAvailable = awbLockAvailable,
            sensitivityRange = sensitivityRange,
            exposureTimeRange = exposureTimeRange,
            compensationRange = compensationRange,
            compensationStep = compensationStep,
            activeArraySize = activeArraySize,
        )
    }

    fun result(
        focusDistance: Float? = null,
        sensitivity: Int? = null,
        exposureTimeNs: Long? = null,
        gains: RggbChannelVector? = null,
        afState: Int? = null,
        aeState: Int? = null,
        colorTransform: ColorSpaceTransform? = null,
        awbMode: Int? = null,
    ): TotalCaptureResult {
        val result = ShadowTotalCaptureResult.newTotalCaptureResult()
        val shadow = Shadow.extract<ShadowCaptureResult>(result)
        focusDistance?.let { shadow.set(CaptureResult.LENS_FOCUS_DISTANCE, it) }
        sensitivity?.let { shadow.set(CaptureResult.SENSOR_SENSITIVITY, it) }
        exposureTimeNs?.let { shadow.set(CaptureResult.SENSOR_EXPOSURE_TIME, it) }
        gains?.let { shadow.set(CaptureResult.COLOR_CORRECTION_GAINS, it) }
        afState?.let { shadow.set(CaptureResult.CONTROL_AF_STATE, it) }
        aeState?.let { shadow.set(CaptureResult.CONTROL_AE_STATE, it) }
        colorTransform?.let { shadow.set(CaptureResult.COLOR_CORRECTION_TRANSFORM, it) }
        awbMode?.let { shadow.set(CaptureResult.CONTROL_AWB_MODE, it) }
        return result
    }

    fun deliver(device: AVCaptureDevice, result: TotalCaptureResult) {
        device.captureCompleted(result)
    }

    fun deliver(device: AVCaptureDevice, result: TotalCaptureResult, requestTag: Any?) {
        device.captureCompleted(result, requestTag)
    }
}
