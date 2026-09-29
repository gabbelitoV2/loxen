package com.moblin.android.platform.coreimage.internal

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIKernelLibrary
import com.moblin.android.platform.coreimage.KernelColorOp
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.TransferFunctions
import com.moblin.android.platform.video.YCbCrCoding
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_32RGBA
import com.moblin.android.platform.video.memory
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

internal object SoftwareRenderer {
    fun renderToBuffer(root: ImageNode, bounds: CGRect, target: CVPixelBuffer, encode: Boolean) {
        if (!target.checkReadable("CIContext.render target")) {
            return
        }
        val width = target.width
        val height = target.height
        val keepAlpha = target.pixelFormatType == kCVPixelFormatType_32BGRA ||
            target.pixelFormatType == kCVPixelFormatType_32RGBA
        val image = render(root, bounds.minX, bounds.minY, width, height, encode, keepAlpha)
        target.memory().writeRgb(image, width, height, YCbCrCoding.forBuffer(target), keepAlpha)
    }

    fun render(
        root: ImageNode,
        originX: Double,
        originY: Double,
        width: Int,
        height: Int,
        encode: Boolean,
        keepAlpha: Boolean,
    ): DoubleArray {
        val evaluator = Evaluator()
        val image = DoubleArray(width * height * 4)
        val pixel = DoubleArray(4)
        for (row in 0 until height) {
            val y = originY + (height - 1 - row) + 0.5
            for (column in 0 until width) {
                val x = originX + column + 0.5
                evaluator.sample(root, x, y, pixel)
                val index = (row * width + column) * 4
                val alpha = pixel[3].coerceIn(0.0, 1.0)
                if (keepAlpha) {
                    if (alpha <= 0.0) {
                        continue
                    }
                    for (channel in 0 until 3) {
                        val straight = pixel[channel] / alpha
                        image[index + channel] = (if (encode) TransferFunctions.linearToSrgb(straight) else straight) * alpha
                    }
                    image[index + 3] = alpha
                } else {
                    for (channel in 0 until 3) {
                        val value = pixel[channel].coerceIn(0.0, 1.0)
                        image[index + channel] = if (encode) TransferFunctions.linearToSrgb(value) else value
                    }
                    image[index + 3] = 1.0
                }
            }
        }
        return image
    }

    fun createBitmap(root: ImageNode, rect: CGRect): Bitmap? {
        val width = max(1, rect.width.roundToInt())
        val height = max(1, rect.height.roundToInt())
        val image = render(root, rect.minX, rect.minY, width, height, true, true)
        val pixels = IntArray(width * height) { index ->
            val alpha = image[index * 4 + 3]
            fun channel(value: Double): Int {
                val straight = if (alpha > 0.0) value / alpha else 0.0
                return (straight.coerceIn(0.0, 1.0) * 255).roundToInt()
            }
            ((alpha.coerceIn(0.0, 1.0) * 255).roundToInt() shl 24) or
                (channel(image[index * 4]) shl 16) or
                (channel(image[index * 4 + 1]) shl 8) or
                channel(image[index * 4 + 2])
        }
        return try {
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (error: Throwable) {
            EffectsLog.once("softwareBitmap", "Software renderer cannot create a bitmap: $error")
            null
        }
    }

    private class Evaluator {
        private val scratch = ArrayDeque<DoubleArray>()

        private fun take(): DoubleArray {
            return scratch.removeLastOrNull() ?: DoubleArray(4)
        }

        private fun give(value: DoubleArray) {
            scratch.addLast(value)
        }

        fun sample(node: ImageNode, x: Double, y: Double, output: DoubleArray) {
            when (node) {
                is EmptyNode -> clear(output)
                is ConstantNode -> constant(node, x, y, output)
                is CropNode -> {
                    if (contains(node.rect, x, y)) {
                        sample(node.input, x, y, output)
                    } else {
                        clear(output)
                    }
                }
                is PixelBufferNode -> pixelBuffer(node, x, y, output)
                is BitmapNode -> bitmap(node, x, y, output)
                is TransformNode -> {
                    val inverse = node.transform.inverted()
                    val sourceX = inverse.a * x + inverse.c * y + inverse.tx
                    val sourceY = inverse.b * x + inverse.d * y + inverse.ty
                    sample(node.input, sourceX, sourceY, output)
                }
                is ClampNode -> {
                    val extent = node.input.extent
                    if (extent.isNull || extent.isInfinite) {
                        sample(node.input, x, y, output)
                    } else {
                        val clampedX = x.coerceIn(extent.minX + 0.5, max(extent.minX + 0.5, extent.maxX - 0.5))
                        val clampedY = y.coerceIn(extent.minY + 0.5, max(extent.minY + 0.5, extent.maxY - 0.5))
                        sample(node.input, clampedX, clampedY, output)
                    }
                }
                is SamplingNode -> sample(node.input, x, y, output)
                is ColorOpNode -> {
                    if (!contains(node.extent, x, y)) {
                        clear(output)
                        return
                    }
                    sample(node.input, x, y, output)
                    colorOp(node.op, x, y, output)
                }
                is CompositeNode -> {
                    val background = take()
                    sample(node.foreground, x, y, output)
                    sample(node.background, x, y, background)
                    val remaining = 1 - output[3]
                    for (channel in 0 until 4) {
                        output[channel] += background[channel] * remaining
                    }
                    give(background)
                }
                is CombineNode -> combine(node, x, y, output)
                is CiBoundaryNode -> {
                    if (!contains(node.extent, x, y)) {
                        clear(output)
                        return
                    }
                    val scaleX = node.bounds.width / node.width
                    val scaleY = node.bounds.height / node.height
                    sample(node.ciNode, node.bounds.minX + x * scaleX, node.bounds.minY + y * scaleY, output)
                    if (node.opaque) {
                        output[3] = 1.0
                    }
                }
                else -> {
                    EffectsLog.once(
                        "software:${node.javaClass.simpleName}",
                        "Software renderer does not support ${node.javaClass.simpleName}",
                    )
                    clear(output)
                }
            }
        }

        private fun constant(node: ConstantNode, x: Double, y: Double, output: DoubleArray) {
            if (!contains(node.extent, x, y)) {
                clear(output)
                return
            }
            val alpha = node.alpha.coerceIn(0.0, 1.0)
            output[0] = decode(node.red, node.encoding) * alpha
            output[1] = decode(node.green, node.encoding) * alpha
            output[2] = decode(node.blue, node.encoding) * alpha
            output[3] = alpha
        }

        private fun pixelBuffer(node: PixelBufferNode, x: Double, y: Double, output: DoubleArray) {
            val buffer = node.buffer
            val column = floor(x).toInt()
            val row = buffer.height - 1 - floor(y).toInt()
            if (column !in 0 until buffer.width || row !in 0 until buffer.height || !buffer.isValid) {
                clear(output)
                return
            }
            buffer.memory().readRgb(column, row, YCbCrCoding.forBuffer(buffer), output)
            source(node.alpha, node.encoding, output, straight = YCbCrCoding.forBuffer(buffer) != null)
        }

        private fun bitmap(node: BitmapNode, x: Double, y: Double, output: DoubleArray) {
            val bitmap = node.bitmap
            val column = floor(x).toInt()
            val row = bitmap.height - 1 - floor(y).toInt()
            if (column !in 0 until bitmap.width || row !in 0 until bitmap.height || bitmap.isRecycled) {
                clear(output)
                return
            }
            val pixel = bitmap.getPixel(column, row)
            output[0] = ((pixel ushr 16) and 0xFF) / 255.0
            output[1] = ((pixel ushr 8) and 0xFF) / 255.0
            output[2] = (pixel and 0xFF) / 255.0
            output[3] = ((pixel ushr 24) and 0xFF) / 255.0
            source(node.alpha, node.encoding, output, straight = true)
        }

        private fun source(alpha: SourceAlpha, encoding: SourceEncoding, output: DoubleArray, straight: Boolean) {
            when (alpha) {
                SourceAlpha.opaque -> output[3] = 1.0
                SourceAlpha.premultiply -> for (channel in 0 until 3) {
                    output[channel] *= output[3]
                }
                SourceAlpha.asIs -> if (straight) {
                    for (channel in 0 until 3) {
                        output[channel] *= output[3]
                    }
                }
            }
            if (encoding == SourceEncoding.srgb || encoding == SourceEncoding.srgbAlways) {
                val a = output[3]
                if (a <= 0.0) {
                    clear(output)
                    return
                }
                for (channel in 0 until 3) {
                    output[channel] = TransferFunctions.srgbToLinear(output[channel] / a) * a
                }
            }
        }

        private fun colorOp(op: ColorOp, x: Double, y: Double, output: DoubleArray) {
            when (op) {
                is PremultiplyOp -> for (channel in 0 until 3) {
                    output[channel] *= output[3]
                }
                is UnpremultiplyOp -> unpremultiply(output)
                is AlphaOneOp -> {
                    val rect = op.rect
                    if (rect != null && !rect.isInfinite && (rect.isNull || !contains(rect, x, y))) {
                        clear(output)
                    } else {
                        output[3] = 1.0
                    }
                }
                is UnpremultiplyAlphaOneOp -> {
                    if (output[3] > 0.0) {
                        unpremultiply(output)
                        output[3] = 1.0
                    } else {
                        output[0] = 0.0
                        output[1] = 0.0
                        output[2] = 0.0
                        output[3] = 1.0
                    }
                }
                is ColorMatrixOp -> {
                    unpremultiply(output)
                    val red = output[0]
                    val green = output[1]
                    val blue = output[2]
                    val alpha = output[3]
                    val rows = arrayOf(op.red, op.green, op.blue, op.alpha)
                    val result = DoubleArray(4) { index ->
                        val row = rows[index]
                        row[0] * red + row[1] * green + row[2] * blue + row[3] * alpha + op.bias[index]
                    }
                    val a = result[3].coerceIn(0.0, 1.0)
                    output[0] = result[0] * a
                    output[1] = result[1] * a
                    output[2] = result[2] * a
                    output[3] = a
                }
                else -> EffectsLog.once(
                    "softwareColorOp:${op.javaClass.simpleName}",
                    "Software renderer does not support ${op.javaClass.simpleName}",
                )
            }
        }

        private fun combine(node: CombineNode, x: Double, y: Double, output: DoubleArray) {
            if (!contains(node.extent, x, y)) {
                clear(output)
                return
            }
            val inputs = node.inputs.map { input ->
                val value = DoubleArray(4)
                sample(input, x, y, value)
                value
            }
            when (val combiner = node.combiner) {
                is SourceOverCombine -> {
                    val remaining = 1 - inputs[0][3]
                    for (channel in 0 until 4) {
                        output[channel] = inputs[0][channel] + inputs[1][channel] * remaining
                    }
                }
                is MixCombine -> for (channel in 0 until 4) {
                    output[channel] = inputs[0][channel] + (inputs[1][channel] - inputs[0][channel]) * combiner.amount
                }
                is BlendWithMaskCombine -> {
                    val mask = inputs[2][1].coerceIn(0.0, 1.0)
                    for (channel in 0 until 4) {
                        output[channel] = inputs[1][channel] + (inputs[0][channel] - inputs[1][channel]) * mask
                    }
                }
                is KernelColorOp -> {
                    val kernel = CIKernelLibrary.cpuColorKernel(combiner.kernelName)
                    if (kernel == null) {
                        EffectsLog.once(
                            "softwareKernel:${combiner.kernelName}",
                            "Software renderer has no CPU port of the kernel ${combiner.kernelName}",
                        )
                        clear(output)
                        return
                    }
                    val result = kernel.evaluate(inputs, combiner.floatArguments())
                    for (channel in 0 until 4) {
                        output[channel] = result[channel]
                    }
                }
                else -> {
                    EffectsLog.once(
                        "softwareCombine:${combiner.javaClass.simpleName}",
                        "Software renderer does not support ${combiner.javaClass.simpleName}",
                    )
                    clear(output)
                }
            }
        }

        private fun decode(value: Double, encoding: SourceEncoding): Double {
            return if (encoding == SourceEncoding.srgb || encoding == SourceEncoding.srgbAlways) {
                TransferFunctions.srgbToLinear(value)
            } else {
                value
            }
        }

        private fun unpremultiply(output: DoubleArray) {
            val alpha = output[3]
            if (alpha > 0.0) {
                for (channel in 0 until 3) {
                    output[channel] /= alpha
                }
            } else {
                clear(output)
            }
        }

        private fun contains(rect: CGRect, x: Double, y: Double): Boolean {
            if (rect.isNull) {
                return false
            }
            if (rect.isInfinite) {
                return true
            }
            return x >= rect.minX && x < rect.maxX && y >= rect.minY && y < rect.maxY
        }

        private fun clear(output: DoubleArray) {
            output[0] = 0.0
            output[1] = 0.0
            output[2] = 0.0
            output[3] = 0.0
        }
    }
}
