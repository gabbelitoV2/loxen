package com.moblin.android.platform.video

const val kCVPixelFormatName = "Name"
const val kCVPixelFormatComponentRange = "ComponentRange"
const val kCVPixelFormatComponentRange_VideoRange = "VideoRange"
const val kCVPixelFormatComponentRange_FullRange = "FullRange"
const val kCVPixelFormatComponentRange_WideRange = "WideRange"
const val kCVPixelFormatBitsPerComponent = "BitsPerComponent"
const val kCVPixelFormatContainsYCbCr = "ContainsYCbCr"
const val kCVPixelFormatContainsRGB = "ContainsRGB"
const val kCVPixelFormatContainsAlpha = "ContainsAlpha"

internal class PixelFormatInfo(
    val pixelFormatType: Int,
    val name: String,
    val isYCbCr: Boolean,
    val isVideoRange: Boolean,
    val bitsPerComponent: Int,
    val isBgra: Boolean = false,
) {
    val bytesPerComponent: Int
        get() = if (bitsPerComponent > 8) 2 else 1

    val planeCount: Int
        get() = if (isYCbCr) 2 else 1

    val maximumCode: Int
        get() = (1 shl bitsPerComponent) - 1
}

internal object PixelFormats {
    val bgra = PixelFormatInfo(kCVPixelFormatType_32BGRA, "BGRA", false, false, 8, isBgra = true)
    val rgba = PixelFormatInfo(kCVPixelFormatType_32RGBA, "RGBA", false, false, 8)
    val fullRange8Bit = PixelFormatInfo(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, "420f", true, false, 8)
    val videoRange8Bit = PixelFormatInfo(kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange, "420v", true, true, 8)
    val fullRange10Bit = PixelFormatInfo(kCVPixelFormatType_420YpCbCr10BiPlanarFullRange, "xf20", true, false, 10)
    val videoRange10Bit = PixelFormatInfo(kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange, "x420", true, true, 10)

    private val all = listOf(bgra, rgba, fullRange8Bit, videoRange8Bit, fullRange10Bit, videoRange10Bit)

    fun info(pixelFormatType: Int): PixelFormatInfo? {
        return all.firstOrNull { it.pixelFormatType == pixelFormatType }
    }

    fun isYCbCr(pixelFormatType: Int): Boolean {
        return info(pixelFormatType)?.isYCbCr == true
    }

    fun isFullRange(pixelFormatType: Int): Boolean {
        val info = info(pixelFormatType) ?: return false
        return info.isYCbCr && !info.isVideoRange
    }

    fun eightBitFormat(pixelFormatType: Int): Int {
        return when (pixelFormatType) {
            kCVPixelFormatType_420YpCbCr10BiPlanarFullRange -> kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
            kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange -> kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
            else -> pixelFormatType
        }
    }
}

fun CVPixelFormatDescriptionCreateWithPixelFormatType(allocator: Any?, pixelFormat: Int): Map<String, Any>? {
    val info = PixelFormats.info(pixelFormat) ?: return null
    val description = mutableMapOf<String, Any>(
        kCVPixelFormatName to info.name,
        kCVPixelFormatBitsPerComponent to info.bitsPerComponent,
        kCVPixelFormatContainsYCbCr to info.isYCbCr,
        kCVPixelFormatContainsRGB to !info.isYCbCr,
        kCVPixelFormatContainsAlpha to !info.isYCbCr,
    )
    if (info.isYCbCr) {
        description[kCVPixelFormatComponentRange] = if (info.isVideoRange) {
            kCVPixelFormatComponentRange_VideoRange
        } else {
            kCVPixelFormatComponentRange_FullRange
        }
    }
    return description
}
