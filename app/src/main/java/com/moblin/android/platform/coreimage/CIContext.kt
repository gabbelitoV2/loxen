package com.moblin.android.platform.coreimage

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.Renderer
import com.moblin.android.platform.coreimage.internal.SoftwareRenderer
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.SoftwareRendering
import com.moblin.android.platform.video.TransferFunctions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

enum class CIContextOption {
    workingColorSpace,
    outputColorSpace,
    useSoftwareRenderer,
    cacheIntermediates,
    workingFormat,
    highQualityDownsample,
    priorityRequestLow,
}

object CIFormat {
    const val RGBA8 = 0x52474241
    const val BGRA8 = 0x42475241
    const val RGBAf = 0x52476666
    const val RGBAh = 0x52476868
}

class CIContext(options: Map<CIContextOption, Any>? = null) {
    private val softwareRenderer = options?.get(CIContextOption.useSoftwareRenderer) == true

    init {
        if (options?.get(CIContextOption.workingColorSpace) != null) {
            EffectsLog.once("contextWorkingColorSpace", "CIContext working color space option ignored")
        }
    }

    private val rendersOnCpu: Boolean
        get() = softwareRenderer || SoftwareRendering.isActive

    fun render(image: CIImage, to: CVPixelBuffer) {
        render(image, to, CGRect(0.0, 0.0, to.width.toDouble(), to.height.toDouble()), CGColorSpace(CGColorSpace.sRGB))
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
        if (rendersOnCpu) {
            SoftwareRenderer.renderToBuffer(image.node, bounds, to, encode = colorSpace != null)
            return
        }
        Renderer.onPipeline("CIContext.render", Unit) {
            Renderer.renderCoreImageToBuffer(image.node, bounds, to, encode = colorSpace != null)
        }
    }

    fun render(
        image: CIImage,
        toBitmap: FloatArray,
        rowBytes: Int,
        bounds: CGRect,
        format: Int,
        colorSpace: CGColorSpace?,
    ) {
        val pixels = renderBitmap(image, bounds, colorSpace) ?: return
        val width = max(1, bounds.width.roundToInt())
        val height = max(1, bounds.height.roundToInt())
        val floatsPerRow = rowBytes / 4
        for (row in 0 until height) {
            for (column in 0 until width) {
                val source = (row * width + column) * 4
                val destination = row * floatsPerRow + column * 4
                if (destination + 3 >= toBitmap.size) {
                    return
                }
                for (channel in 0 until 4) {
                    toBitmap[destination + channel] = pixels[source + channel].toFloat()
                }
            }
        }
    }

    fun render(
        image: CIImage,
        toBitmap: ByteArray,
        rowBytes: Int,
        bounds: CGRect,
        format: Int,
        colorSpace: CGColorSpace?,
    ) {
        val pixels = renderBitmap(image, bounds, colorSpace) ?: return
        val width = max(1, bounds.width.roundToInt())
        val height = max(1, bounds.height.roundToInt())
        val order = if (format == CIFormat.BGRA8) intArrayOf(2, 1, 0, 3) else intArrayOf(0, 1, 2, 3)
        val buffer = ByteBuffer.wrap(toBitmap).order(ByteOrder.LITTLE_ENDIAN)
        for (row in 0 until height) {
            for (column in 0 until width) {
                val source = (row * width + column) * 4
                val destination = row * rowBytes + column * 4
                if (destination + 3 >= toBitmap.size) {
                    return
                }
                for (channel in 0 until 4) {
                    val value = (pixels[source + order[channel]].coerceIn(0.0, 1.0) * 255).roundToInt()
                    buffer.put(destination + channel, value.toByte())
                }
            }
        }
    }

    fun createCGImage(image: CIImage, from: CGRect): Bitmap? {
        if (from.isNull || from.isInfinite || from.isEmpty) {
            return null
        }
        if (rendersOnCpu) {
            return SoftwareRenderer.createBitmap(image.node, from)
        }
        return Renderer.onPipeline("CIContext.createCGImage", null) {
            Renderer.createBitmapFromCoreImage(image.node, from)
        }
    }

    fun clearCaches() {
        if (rendersOnCpu) {
            return
        }
        Renderer.onPipeline("CIContext.clearCaches", Unit) {
            Renderer.clearCaches()
        }
    }

    private fun renderBitmap(image: CIImage, bounds: CGRect, colorSpace: CGColorSpace?): DoubleArray? {
        if (bounds.isNull || bounds.isInfinite || bounds.isEmpty) {
            return null
        }
        val width = max(1, bounds.width.roundToInt())
        val height = max(1, bounds.height.roundToInt())
        if (rendersOnCpu) {
            return SoftwareRenderer.render(image.node, bounds.minX, bounds.minY, width, height, colorSpace != null, true)
        }
        val bitmap = Renderer.onPipeline("CIContext.render(toBitmap:)", null) {
            Renderer.createBitmapFromCoreImage(image.node, bounds)
        } ?: return null
        try {
            val colors = IntArray(width * height)
            bitmap.getPixels(colors, 0, width, 0, 0, width, height)
            val pixels = DoubleArray(colors.size * 4)
            for ((index, color) in colors.withIndex()) {
                val alpha = ((color ushr 24) and 0xFF) / 255.0
                val channels = doubleArrayOf(
                    ((color ushr 16) and 0xFF) / 255.0,
                    ((color ushr 8) and 0xFF) / 255.0,
                    (color and 0xFF) / 255.0,
                )
                for (channel in 0 until 3) {
                    val value = if (colorSpace == null) {
                        TransferFunctions.srgbToLinear(channels[channel])
                    } else {
                        channels[channel]
                    }
                    pixels[index * 4 + channel] = value * alpha
                }
                pixels[index * 4 + 3] = alpha
            }
            return pixels
        } finally {
            bitmap.recycle()
        }
    }
}
