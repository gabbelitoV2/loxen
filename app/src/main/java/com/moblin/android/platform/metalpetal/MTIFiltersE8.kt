package com.moblin.android.platform.metalpetal

import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.simd.SIMD2
import kotlin.math.pow
import kotlin.math.roundToInt

private fun unfinishedOutput(member: String): MTIImage? {
    EffectsLog.notImplemented(member)
    return null
}

class MTIPixellateFilter : MTIUnaryImageRenderingFilter() {
    var scale: SIMD2 = SIMD2(16f, 16f)

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIPixellateFilter.outputImage")
}

class MTIPinchDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var scale: Float = 0f

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIPinchDistortionFilter.outputImage")
}

class MTITwirlDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var angle: Float = 0f

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTITwirlDistortionFilter.outputImage")
}

class MTIBulgeDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var scale: Float = 0f

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIBulgeDistortionFilter.outputImage")
}

class MTICrtFilter : MTIUnaryImageRenderingFilter() {
    var barrelStrength: Float = 0.1f

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTICrtFilter.outputImage")
}

class MTIChromaKeyBlendFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var thresholdSensitivity: Float = 0.4f
    var smoothing: Float = 0.1f
    var color: MTIColor = MTIColor(0f, 1f, 0f, 1f)

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIChromaKeyBlendFilter.outputImage")
}

enum class MTIColorLookupTableType {
    typeUnknown,
    type2DSquare,
    type2DHorizontalStrip,
    type2DVerticalStrip,
    type3D,
}

class MTIColorLookupTableInfo(val type: MTIColorLookupTableType, val dimension: Int) {
    internal constructor(width: Int, height: Int) : this(tableType(width, height), tableDimension(width, height))

    private companion object {
        fun squareDimension(width: Int, height: Int): Int? {
            if (width != height) {
                return null
            }
            val pixels = width.toDouble() * height.toDouble()
            val dimension = pixels.pow(1.0 / 3.0).roundToInt()
            return if (dimension.toDouble() * dimension * dimension == pixels) dimension else null
        }

        fun tableType(width: Int, height: Int): MTIColorLookupTableType {
            if (width == height) {
                return if (squareDimension(width, height) != null) {
                    MTIColorLookupTableType.type2DSquare
                } else {
                    MTIColorLookupTableType.typeUnknown
                }
            }
            return when {
                height.toLong() * height == width.toLong() -> MTIColorLookupTableType.type2DHorizontalStrip
                width.toLong() * width == height.toLong() -> MTIColorLookupTableType.type2DVerticalStrip
                else -> MTIColorLookupTableType.typeUnknown
            }
        }

        fun tableDimension(width: Int, height: Int): Int {
            return when (tableType(width, height)) {
                MTIColorLookupTableType.type2DSquare -> squareDimension(width, height) ?: 0
                MTIColorLookupTableType.type2DHorizontalStrip -> height
                MTIColorLookupTableType.type2DVerticalStrip -> width
                else -> 0
            }
        }
    }
}

class MTIColorLookupFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputColorLookupTable: MTIImage? = null
        set(value) {
            field = value
            inputColorLookupTableInfo = value?.let { MTIColorLookupTableInfo(it.width, it.height) }
        }
    var inputColorLookupTableInfo: MTIColorLookupTableInfo? = null
        private set
    var intensity: Float = 1f

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIColorLookupFilter.outputImage")
}

class MTIBlendFilter(val blendMode: MTIBlendMode) : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var intensity: Float = 1f
    var outputAlphaType: MTIAlphaType = MTIAlphaType.nonPremultiplied

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIBlendFilter.outputImage")
}

class MTIBlendWithMaskFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var inputMask: MTIMask? = null

    override val outputImage: MTIImage?
        get() = unfinishedOutput("MTIBlendWithMaskFilter.outputImage")
}
