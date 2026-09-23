package com.moblin.android.platform.capture

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import com.moblin.android.AppDelegate
import com.moblin.android.platform.avfoundation.AVCaptureDevice

internal object CameraCatalog {
    class Entry(
        val id: String,
        val characteristics: CameraCharacteristics,
        val facing: Int,
        val position: AVCaptureDevice.Position,
        val deviceType: AVCaptureDevice.DeviceType,
        val localizedName: String,
        val sensorOrientation: Int,
        val timestampSource: Int,
        val zoomRatioLower: Float,
        val zoomRatioUpper: Float,
        val zoomScale: Float,
        val usesZoomRatio: Boolean,
        val minZoomFactor: Float,
        val maxZoomFactor: Float,
        val switchOverZoomFactors: List<Float>,
        val hasFlash: Boolean,
        val torchMaxLevel: Int,
        val stabilizationSupported: Boolean,
        val lowLightBoostSupported: Boolean,
        val aeCompensationRange: Range<Int>?,
        val aeCompensationStep: Float,
        val activeArraySize: Rect?,
        val aeFpsRanges: List<Range<Int>>,
        val hlgSupported: Boolean,
        val rotateAndCropNoneSupported: Boolean,
        val jpegSizes: List<Size>,
    ) {
        val isFront: Boolean
            get() = facing == CameraCharacteristics.LENS_FACING_FRONT

        val isExternal: Boolean
            get() = facing == CameraCharacteristics.LENS_FACING_EXTERNAL

        val reachesUltraWide: Boolean
            get() = zoomRatioLower < 0.99f
    }

    private const val TAG = "MoblinCamera"
    private val cache = HashMap<String, Entry>()

    val manager: CameraManager?
        get() = try {
            AppDelegate.context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (error: Throwable) {
            null
        }

    fun ids(): List<String> {
        return try {
            manager?.cameraIdList?.toList() ?: emptyList()
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to list cameras: $error")
            emptyList()
        }
    }

    fun isPresent(id: String): Boolean = ids().contains(id)

    @Synchronized
    fun entries(): List<Entry> {
        val ids = ids()
        val characteristics = ids.mapNotNull { id ->
            (cache[id]?.characteristics ?: characteristics(id))?.let { id to it }
        }
        val referenceFieldOfView = HashMap<Int, Float>()
        for ((_, value) in characteristics) {
            val facing = value.get(CameraCharacteristics.LENS_FACING) ?: continue
            if (!referenceFieldOfView.containsKey(facing)) {
                referenceFieldOfView[facing] = fieldOfViewMetric(value)
            }
        }
        return characteristics.map { (id, value) ->
            cache.getOrPut(id) {
                val facing = value.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_EXTERNAL
                makeEntry(id, value, referenceFieldOfView[facing] ?: 0f)
            }
        }
    }

    fun entry(id: String): Entry? {
        synchronized(this) {
            cache[id]?.let { return it }
        }
        return entries().firstOrNull { it.id == id }
    }

    fun concurrentCameraIds(): Set<Set<String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return emptySet()
        }
        return try {
            manager?.concurrentCameraIds ?: emptySet()
        } catch (error: Throwable) {
            emptySet()
        }
    }

    private fun characteristics(id: String): CameraCharacteristics? {
        return try {
            manager?.getCameraCharacteristics(id)
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to get characteristics of camera $id: $error")
            null
        }
    }

    private fun fieldOfViewMetric(characteristics: CameraCharacteristics): Float {
        val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.minOrNull() ?: return 0f
        val sensorWidth = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width ?: return 0f
        if (focalLength <= 0f) {
            return 0f
        }
        return sensorWidth / focalLength
    }

    private fun physicalCameraCount(characteristics: CameraCharacteristics): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return 1
        }
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return 1
        if (!capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)) {
            return 1
        }
        return maxOf(1, characteristics.physicalCameraIds.size)
    }

    private fun makeEntry(id: String, characteristics: CameraCharacteristics, referenceFieldOfView: Float): Entry {
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_EXTERNAL
        val position = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> AVCaptureDevice.Position.back
            CameraCharacteristics.LENS_FACING_FRONT -> AVCaptureDevice.Position.front
            else -> AVCaptureDevice.Position.unspecified
        }
        val maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        var zoomRange: Range<Float>? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        }
        val zoomRatioLower = zoomRange?.lower ?: 1f
        val zoomRatioUpper = maxOf(zoomRatioLower, zoomRange?.upper ?: maxDigitalZoom)
        val zoomScale = if (zoomRatioLower < 0.99f) 0.5f else 1f
        val minZoomFactor = maxOf(1f, zoomRatioLower / zoomScale)
        val maxZoomFactor = maxOf(minZoomFactor, zoomRatioUpper / zoomScale)
        val switchOverZoomFactors = if (zoomRatioLower < 0.99f) listOf(1f / zoomScale) else emptyList()
        val physicalCameras = physicalCameraCount(characteristics)
        val deviceType = if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
            AVCaptureDevice.DeviceType.external
        } else if (physicalCameras >= 3) {
            AVCaptureDevice.DeviceType.builtInTripleCamera
        } else if (zoomRatioLower < 0.99f) {
            AVCaptureDevice.DeviceType.builtInDualWideCamera
        } else if (physicalCameras == 2) {
            AVCaptureDevice.DeviceType.builtInDualCamera
        } else {
            val metric = fieldOfViewMetric(characteristics)
            if (referenceFieldOfView > 0f && metric > referenceFieldOfView * 1.35f) {
                AVCaptureDevice.DeviceType.builtInUltraWideCamera
            } else if (referenceFieldOfView > 0f && metric > 0f && metric < referenceFieldOfView * 0.65f) {
                AVCaptureDevice.DeviceType.builtInTelephotoCamera
            } else {
                AVCaptureDevice.DeviceType.builtInWideAngleCamera
            }
        }
        val positionName = when (position) {
            AVCaptureDevice.Position.back -> "Back"
            AVCaptureDevice.Position.front -> "Front"
            AVCaptureDevice.Position.unspecified -> "External"
        }
        val localizedName = when (deviceType) {
            AVCaptureDevice.DeviceType.builtInTripleCamera -> "$positionName Triple Camera"
            AVCaptureDevice.DeviceType.builtInDualCamera -> "$positionName Dual Camera"
            AVCaptureDevice.DeviceType.builtInDualWideCamera -> "$positionName Dual Wide Camera"
            AVCaptureDevice.DeviceType.builtInUltraWideCamera -> "$positionName Ultra Wide Camera"
            AVCaptureDevice.DeviceType.builtInTelephotoCamera -> "$positionName Telephoto Camera"
            AVCaptureDevice.DeviceType.external -> "External Camera $id"
            else -> "$positionName Camera"
        }
        var torchMaxLevel = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            torchMaxLevel = characteristics.get(CameraCharacteristics.FLASH_TORCH_STRENGTH_MAX_LEVEL) ?: 1
        }
        val stabilizationModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        var lowLightBoostSupported = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            lowLightBoostSupported = aeModes?.contains(
                CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
            ) == true
        }
        var hlgSupported = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hlgSupported = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
                ?.supportedProfiles?.contains(DynamicRangeProfiles.HLG10) == true
        }
        var rotateAndCropNoneSupported = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rotateAndCropNoneSupported = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_ROTATE_AND_CROP_MODES)
                ?.contains(CameraMetadata.SCALER_ROTATE_AND_CROP_NONE) == true
        }
        val jpegSizes = try {
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.JPEG)
                ?.sortedBy { it.width.toLong() * it.height }
                ?: emptyList()
        } catch (error: Throwable) {
            emptyList()
        }
        return Entry(
            id = id,
            characteristics = characteristics,
            facing = facing,
            position = position,
            deviceType = deviceType,
            localizedName = localizedName,
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0,
            timestampSource = characteristics.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)
                ?: CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN,
            zoomRatioLower = zoomRatioLower,
            zoomRatioUpper = zoomRatioUpper,
            zoomScale = zoomScale,
            usesZoomRatio = zoomRange != null,
            minZoomFactor = minZoomFactor,
            maxZoomFactor = maxZoomFactor,
            switchOverZoomFactors = switchOverZoomFactors,
            hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            torchMaxLevel = torchMaxLevel,
            stabilizationSupported = stabilizationModes?.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            ) == true,
            lowLightBoostSupported = lowLightBoostSupported,
            aeCompensationRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE),
            aeCompensationStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                ?.toFloat() ?: 0f,
            activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE),
            aeFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.toList() ?: emptyList(),
            hlgSupported = hlgSupported,
            rotateAndCropNoneSupported = rotateAndCropNoneSupported,
            jpegSizes = jpegSizes,
        )
    }

    fun surfaceTextureSizes(entry: Entry): List<Pair<Size, Long>> {
        val map = entry.characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return emptyList()
        return try {
            val sizes = map.getOutputSizes(SurfaceTexture::class.java) ?: return emptyList()
            sizes.map { size ->
                size to map.getOutputMinFrameDuration(SurfaceTexture::class.java, size)
            }
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to read output sizes of camera ${entry.id}: $error")
            emptyList()
        }
    }
}
