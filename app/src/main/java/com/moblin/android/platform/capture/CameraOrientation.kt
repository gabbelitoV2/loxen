package com.moblin.android.platform.capture

import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation
import com.moblin.android.platform.uikit.UIDevice

internal object CameraOrientation {
    private const val SIGN = 1

    fun rotationFor(videoOrientation: Int, entry: CameraCatalog.Entry): Int {
        if (entry.isExternal) {
            return 0
        }
        val fromPortraitCw = when (videoOrientation) {
            AVCaptureVideoOrientation.landscapeLeft -> 90
            AVCaptureVideoOrientation.portraitUpsideDown -> 180
            AVCaptureVideoOrientation.landscapeRight -> 270
            else -> 0
        }
        val fromNaturalCw = (fromPortraitCw - UIDevice.current.naturalOrientationOffsetCw + 360) % 360
        val rotation = if (entry.isFront) (360 - fromNaturalCw) % 360 else fromNaturalCw
        return ((SIGN * rotation) % 360 + 360) % 360
    }

    fun isSwapped(rotation: Int, entry: CameraCatalog.Entry): Boolean {
        return ((entry.sensorOrientation + rotation) / 90) % 2 == 1
    }

    fun jpegOrientation(videoOrientation: Int, entry: CameraCatalog.Entry): Int {
        return (entry.sensorOrientation + rotationFor(videoOrientation, entry)) % 360
    }
}
