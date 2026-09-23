package com.moblin.android.platform

import com.moblin.android.various.utils.bestBackCameraDevice

object Cameras {
    fun backCameraSwitchOverZoomFactors(): List<Float> {
        val maxZoom = bestBackCameraDevice?.maxAvailableVideoZoomFactor ?: 1f
        return generateSequence(2f) { it * 2 }.takeWhile { it * 2 <= maxZoom }.toList()
    }
}
