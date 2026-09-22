package com.moblin.android.media.haishinkit.extension

data class AVFrameRateRange(
    val minFrameRate: Double,
    val maxFrameRate: Double
) {
    fun contains(frameRate: Double): Boolean {
        return frameRate >= minFrameRate && frameRate <= maxFrameRate
    }
}

data class AVCaptureDeviceFormat(
    val videoSupportedFrameRateRanges: List<AVFrameRateRange>
)

fun AVCaptureDeviceFormat.isFrameRateSupported(fps: Double): Boolean {
    for (fpsRange in videoSupportedFrameRateRanges) {
        if (fpsRange.contains(frameRate = fps)) {
            return true
        }
    }
    return false
}
