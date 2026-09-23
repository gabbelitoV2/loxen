package com.moblin.android.platform

import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVMediaType

object Cameras {
    fun backCameraSwitchOverZoomFactors(): List<Float> {
        val device = AVCaptureDevice.default(
            AVCaptureDevice.DeviceType.builtInWideAngleCamera,
            AVMediaType.video,
            AVCaptureDevice.Position.back,
        )
        val maxZoom = device?.maxAvailableVideoZoomFactor ?: 1f
        return generateSequence(2f) { it * 2 }.takeWhile { it * 2 <= maxZoom }.toList()
    }
}
