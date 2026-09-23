package com.moblin.android.videoeffects.blur

import com.moblin.android.platform.video.CVPixelBuffer as Image

class BlurFilter {
    var inputImage: Image? = null
    var radius: Float = 8f

    val outputImage: Image?
        get() {
            val image = inputImage ?: return null
            return TODO("OpenGL ES port")
        }
}
