package com.moblin.android.videoeffects.crt

import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.CIWarpKernel
import java.io.File

private val barrelKernel: CIWarpKernel? by lazy {
    val url = Bundle.url("default", "metallib") ?: return@lazy null
    val data = runCatching { File(url).readBytes() }.getOrNull() ?: return@lazy null
    runCatching {
        CIWarpKernel(functionName = "crtBarrelDistortion", fromMetalLibraryData = data)
    }.getOrNull()
}

open class CrtBarrelDistortionFilter : CIFilter() {
    open var inputImage: CIImage? = null
    open var width: Double = 1.0
    open var strength: Float = 0.1f

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val kernel = barrelKernel ?: return null
            val extent = image.extent
            return kernel.apply(
                extent = extent,
                roiCallback = { _, rect ->
                    rect.insetBy(dx = -rect.width * 0.25, dy = -rect.height * 0.25)
                },
                image = image,
                arguments = listOf(
                    width.toFloat(),
                    extent.height.toFloat(),
                    strength
                )
            )
        }
}
