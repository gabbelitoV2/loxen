package com.moblin.android.platform.metalpetal

import android.graphics.Bitmap
import com.moblin.android.platform.coreimage.internal.Renderer
import com.moblin.android.platform.video.CVPixelBuffer

class MTIError(message: String) : Exception(message)

class MTIContext(val device: MTLDevice) {
    fun render(image: MTIImage, to: CVPixelBuffer) {
        if (image.width <= 0 || image.height <= 0) {
            throw MTIError("Cannot render an empty image")
        }
        Renderer.onPipeline("MTIContext.render", Unit) {
            Renderer.renderMetalPetalToBuffer(image.node, image.width, image.height, to)
        }
    }

    fun makeCGImage(from: MTIImage): Bitmap? {
        if (from.width <= 0 || from.height <= 0) {
            return null
        }
        return Renderer.onPipeline("MTIContext.makeCGImage", null) {
            Renderer.createBitmapFromMetalPetal(from.node, from.width, from.height)
        }
    }

    fun reclaimResources() {
        Renderer.onPipeline("MTIContext.reclaimResources", Unit) {
            Renderer.clearCaches()
        }
    }
}
