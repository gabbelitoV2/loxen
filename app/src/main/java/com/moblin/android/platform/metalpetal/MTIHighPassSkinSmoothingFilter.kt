package com.moblin.android.platform.metalpetal

import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.simd.SIMD2

class MTIHighPassSkinSmoothingFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var amount: Float = 0.65f
    var radius: Float = 8f

    var toneCurveControlPoints: List<SIMD2> = defaultToneCurveControlPoints
        set(value) {
            field = if (value.size >= 2) value else defaultToneCurveControlPoints
        }

    override val outputImage: MTIImage?
        get() {
            inputImage ?: return null
            EffectsLog.notImplemented("MTIHighPassSkinSmoothingFilter.outputImage")
            return null
        }

    companion object {
        private val defaultToneCurveControlPoints: List<SIMD2> = listOf(
            SIMD2(0f, 0f),
            SIMD2(120f / 255f, 146f / 255f),
            SIMD2(1f, 1f)
        )

        fun isSupported(on: MTLDevice): Boolean {
            return true
        }
    }
}
