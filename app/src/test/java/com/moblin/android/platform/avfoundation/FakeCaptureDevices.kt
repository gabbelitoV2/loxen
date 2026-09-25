package com.moblin.android.platform.avfoundation

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.RggbChannelVector
import com.moblin.android.AppDelegate
import java.util.UUID
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCaptureResult
import org.robolectric.shadows.ShadowTotalCaptureResult

object FakeCaptureDevices {
    fun make(minimumFocusDistance: Float = 10f): AVCaptureDevice {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadow = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadow.set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_EXTERNAL)
        shadow.set(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE, minimumFocusDistance)
        val id = "fake-${UUID.randomUUID()}"
        val manager = AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowOf(manager).addCamera(id, characteristics)
        return AVCaptureDevice.withUniqueID(id)!!
    }

    fun result(
        focusDistance: Float? = null,
        sensitivity: Int? = null,
        exposureTimeNs: Long? = null,
        gains: RggbChannelVector? = null,
    ): TotalCaptureResult {
        val result = ShadowTotalCaptureResult.newTotalCaptureResult()
        val shadow = Shadow.extract<ShadowCaptureResult>(result)
        focusDistance?.let { shadow.set(CaptureResult.LENS_FOCUS_DISTANCE, it) }
        sensitivity?.let { shadow.set(CaptureResult.SENSOR_SENSITIVITY, it) }
        exposureTimeNs?.let { shadow.set(CaptureResult.SENSOR_EXPOSURE_TIME, it) }
        gains?.let { shadow.set(CaptureResult.COLOR_CORRECTION_GAINS, it) }
        return result
    }

    fun deliver(device: AVCaptureDevice, result: TotalCaptureResult) {
        device.captureCompleted(result)
    }
}
