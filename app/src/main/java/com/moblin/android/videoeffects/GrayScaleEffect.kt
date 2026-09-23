package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIColorMatrix
import com.moblin.android.platform.metalpetal.MTIColorMatrixFilter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.simd.SIMD4
import com.moblin.android.platform.simd.simd_float4x4

private fun makeGrayScaleColorMatrix(): MTIColorMatrix {
    val luminance = SIMD4(0.2126, 0.7152, 0.0722, 0) * 0.75f
    return MTIColorMatrix(
        matrix = simd_float4x4(
            columns = listOf(
                luminance,
                luminance,
                luminance,
                SIMD4(0, 0, 0, 1)
            )
        ),
        bias = SIMD4(0, 0, 0, 0)
    )
}

class GrayScaleEffect : VideoEffect() {
    private val filter = CIFilter.colorMonochrome()
    private val filterMetalPetal = MTIColorMatrixFilter()

    init {
        filterMetalPetal.colorMatrix = makeGrayScaleColorMatrix()
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        filter.inputImage = image
        filter.color = CIColor(0.75, 0.75, 0.75)
        filter.intensity = 1.0f
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        filterMetalPetal.inputImage = image
        return filterMetalPetal.outputImage ?: image
    }
}
