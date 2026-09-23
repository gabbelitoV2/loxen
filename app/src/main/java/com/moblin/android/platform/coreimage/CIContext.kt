package com.moblin.android.platform.coreimage

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.Renderer
import com.moblin.android.platform.coreimage.internal.TexturePool
import com.moblin.android.platform.coreimage.internal.TextureReaper
import com.moblin.android.platform.video.CVPixelBuffer

enum class CIContextOption {
    workingColorSpace,
    outputColorSpace,
    useSoftwareRenderer,
    cacheIntermediates,
    workingFormat,
    highQualityDownsample,
    priorityRequestLow,
}

class CIContext(options: Map<CIContextOption, Any>? = null) {
    init {
        if (options?.get(CIContextOption.workingColorSpace) != null) {
            EffectsLog.once("contextWorkingColorSpace", "CIContext working color space option ignored")
        }
    }

    fun render(image: CIImage, to: CVPixelBuffer) {
        render(image, to, CGRect(0.0, 0.0, to.width.toDouble(), to.height.toDouble()), null)
    }

    fun render(image: CIImage, to: CVPixelBuffer, bounds: CGRect, colorSpace: CGColorSpace?) {
        if (colorSpace != null &&
            (colorSpace.name == CGColorSpace.displayP3 || colorSpace.name == CGColorSpace.itur_2020)
        ) {
            EffectsLog.once("colorSpace:${colorSpace.name}", "CIContext.render: ${colorSpace.name} treated as sRGB")
        }
        if (bounds.isNull || bounds.isInfinite) {
            return
        }
        Renderer.onPipeline("CIContext.render", Unit) {
            Renderer.renderCoreImageToBuffer(image.node, bounds, to)
        }
    }

    fun createCGImage(image: CIImage, from: CGRect): Bitmap? {
        if (from.isNull || from.isInfinite || from.isEmpty) {
            return null
        }
        return Renderer.onPipeline("CIContext.createCGImage", null) {
            Renderer.createBitmapFromCoreImage(image.node, from)
        }
    }

    fun clearCaches() {
        Renderer.onPipeline("CIContext.clearCaches", Unit) {
            TextureReaper.poll()
            TexturePool.trim()
        }
    }
}
