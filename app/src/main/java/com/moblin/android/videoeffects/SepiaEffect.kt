package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIColorMatrix
import com.moblin.android.platform.metalpetal.MTIColorMatrixFilter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.simd.SIMD4
import com.moblin.android.platform.simd.simd_float4x4

private val colorMatrix = MTIColorMatrix(
    matrix = simd_float4x4(
        SIMD4(0.393f, 0.769f, 0.189f, 0f),
        SIMD4(0.349f, 0.686f, 0.168f, 0f),
        SIMD4(0.272f, 0.534f, 0.131f, 0f),
        SIMD4(0.0000f, 0.0000f, 0.0000f, 1.0f),
    ),
    bias = SIMD4(0f, 0f, 0f, 0f),
)

class SepiaEffect : VideoEffect() {
    private val filter = CIFilter.sepiaTone()
    private val filterMetalPetal = MTIColorMatrixFilter()

    init {
        filterMetalPetal.colorMatrix = colorMatrix
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        filter.inputImage = image
        filter.intensity = 0.9f
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        filterMetalPetal.inputImage = image
        return filterMetalPetal.outputImage ?: image
    }
}
