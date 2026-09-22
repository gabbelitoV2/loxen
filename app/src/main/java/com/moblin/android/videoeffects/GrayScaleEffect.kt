package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

private class ColorMatrix(val matrix: FloatArray, val bias: FloatArray)

private fun makeGrayScaleColorMatrix(): ColorMatrix {
    val luminance = floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f)
    for (index in luminance.indices) {
        luminance[index] = luminance[index] * 0.75f
    }
    val matrix = FloatArray(16)
    for (column in 0 until 3) {
        for (row in 0 until 4) {
            matrix[column * 4 + row] = luminance[row]
        }
    }
    matrix[15] = 1f
    return ColorMatrix(matrix = matrix, bias = floatArrayOf(0f, 0f, 0f, 0f))
}

class GrayScaleEffect : VideoEffect() {
    private val colorMatrix: ColorMatrix = makeGrayScaleColorMatrix()

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO("no Android counterpart for MetalPetal")
    }
}
