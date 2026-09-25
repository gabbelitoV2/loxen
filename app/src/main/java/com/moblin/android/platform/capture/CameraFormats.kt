package com.moblin.android.platform.capture

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import com.moblin.android.platform.avfoundation.AVCaptureColorSpace
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVFrameRateRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import kotlin.math.abs

internal object CameraFormats {
    fun make(entry: CameraCatalog.Entry): List<AVCaptureDevice.Format> {
        val sizes = CameraCatalog.surfaceTextureSizes(entry)
            .distinctBy { (size, _) -> size.width to size.height }
            .sortedWith(compareBy({ it.first.width.toLong() * it.first.height }, { it.first.width }))
        val colorSpaces = if (entry.hlgSupported) {
            listOf(AVCaptureColorSpace.sRGB, AVCaptureColorSpace.HLG_BT2020)
        } else {
            listOf(AVCaptureColorSpace.sRGB)
        }
        val sensor = SensorRanges(entry)
        return sizes.map { (size, minFrameDurationNs) ->
            val maxFrameRate = if (minFrameDurationNs > 0) 1_000_000_000.0 / minFrameDurationNs else 30.0
            var ranges = entry.aeFpsRanges
                .filter { it.upper <= maxFrameRate + 0.5 }
                .map { AVFrameRateRange(minFrameRate = it.lower.toDouble(), maxFrameRate = it.upper.toDouble()) }
            if (ranges.isEmpty()) {
                val rate = maxOf(1, maxFrameRate.toInt()).toDouble()
                ranges = listOf(AVFrameRateRange(minFrameRate = rate, maxFrameRate = rate))
            }
            AVCaptureDevice.Format(
                formatDescription = AVCaptureDevice.Format.FormatDescription(
                    dimensions = AVCaptureDevice.Format.Dimensions(size.width, size.height),
                    mediaSubType = AVCaptureDevice.Format.MediaSubType(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange),
                ),
                videoSupportedFrameRateRanges = ranges,
                supportedColorSpaces = colorSpaces,
                videoMaxZoomFactor = entry.maxZoomFactor,
                supportedMaxPhotoDimensions = photoDimensions(entry, size),
                minISO = sensor.minISO,
                maxISO = sensor.maxISO,
                minExposureDuration = sensor.minExposureDuration,
                maxExposureDuration = sensor.maxExposureDuration,
            )
        }
    }

    fun defaultFormat(formats: List<AVCaptureDevice.Format>, entry: CameraCatalog.Entry? = null): AVCaptureDevice.Format {
        formats.firstOrNull { width(it) == 1920 && height(it) == 1080 }?.let { return it }
        val wide = formats.filter { abs(width(it) * 9 - height(it) * 16) <= 16 }
        val candidates = wide.ifEmpty { formats }
        return candidates.minByOrNull { abs(width(it).toLong() * height(it) - 1920L * 1080) } ?: placeholder(entry)
    }

    private fun width(format: AVCaptureDevice.Format): Int = format.formatDescription.dimensions.width

    private fun height(format: AVCaptureDevice.Format): Int = format.formatDescription.dimensions.height

    private fun photoDimensions(entry: CameraCatalog.Entry, size: Size): List<Size> {
        val sameAspect = entry.jpegSizes.filter {
            abs(it.width.toLong() * size.height - it.height.toLong() * size.width) <= size.width.toLong() * size.height / 50
        }
        return sameAspect.ifEmpty { entry.jpegSizes }.ifEmpty { listOf(size) }
    }

    private fun placeholder(entry: CameraCatalog.Entry?): AVCaptureDevice.Format {
        val sensor = SensorRanges(entry)
        return AVCaptureDevice.Format(
            formatDescription = AVCaptureDevice.Format.FormatDescription(
                dimensions = AVCaptureDevice.Format.Dimensions(1920, 1080),
                mediaSubType = AVCaptureDevice.Format.MediaSubType(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange),
            ),
            videoSupportedFrameRateRanges = listOf(AVFrameRateRange(minFrameRate = 30.0, maxFrameRate = 30.0)),
            supportedColorSpaces = listOf(AVCaptureColorSpace.sRGB),
            videoMaxZoomFactor = 1f,
            supportedMaxPhotoDimensions = listOf(Size(1920, 1080)),
            minISO = sensor.minISO,
            maxISO = sensor.maxISO,
            minExposureDuration = sensor.minExposureDuration,
            maxExposureDuration = sensor.maxExposureDuration,
        )
    }

    private class SensorRanges(entry: CameraCatalog.Entry?) {
        private val isoRange = entry?.characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        private val exposureRange = entry?.characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minISO = isoRange?.lower?.toFloat() ?: 34f
        val maxISO = isoRange?.upper?.toFloat() ?: 3264f
        val minExposureDuration = exposureRange?.lower?.let { it / 1000 } ?: 14L
        val maxExposureDuration = exposureRange?.upper?.let { it / 1000 } ?: 1_000_000L
    }
}
