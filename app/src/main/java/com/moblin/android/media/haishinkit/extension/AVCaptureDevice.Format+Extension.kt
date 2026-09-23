package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.avfoundation.AVCaptureDevice

fun AVCaptureDevice.Format.isFrameRateSupported(fps: Double): Boolean {
    for (fpsRange in videoSupportedFrameRateRanges) {
        if (fpsRange.contains(frameRate = fps)) {
            return true
        }
    }
    return false
}
