package com.moblin.android.videoeffects.crt

private val barrelKernel: Any? by lazy<Any?> {
    TODO("OpenGL ES port: Metal library kernel crtBarrelDistortion")
}

class CrtBarrelDistortionFilter {
    var inputImage: Any? = null
    var width: Float = 1f
    var strength: Float = 0.1f

    val outputImage: Any?
        get() = TODO("OpenGL ES port: crtBarrelDistortion warp kernel")
}
