package com.moblin.android.platform.coregraphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "MoblinEffects"

class CGContext private constructor(
    val width: Int,
    val height: Int,
    private val isGray: Boolean,
    private val hasAlpha: Boolean,
    private val bitmap: Bitmap,
) {
    private val canvas = Canvas(bitmap)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val currentPath = Path()

    init {
        canvas.translate(0f, height.toFloat())
        canvas.scale(1f, -1f)
    }

    fun setFillColor(color: CGColor) {
        fillPaint.color = toArgb(color.red, color.green, color.blue, color.alpha)
    }

    fun setFillColor(gray: Double, alpha: Double) {
        fillPaint.color = toArgb(gray, gray, gray, alpha)
    }

    fun setFillColor(red: Double, green: Double, blue: Double, alpha: Double) {
        fillPaint.color = toArgb(red, green, blue, alpha)
    }

    fun setStrokeColor(color: CGColor) {
        strokePaint.color = toArgb(color.red, color.green, color.blue, color.alpha)
    }

    fun setStrokeColor(gray: Double, alpha: Double) {
        strokePaint.color = toArgb(gray, gray, gray, alpha)
    }

    fun setLineWidth(width: Double) {
        strokePaint.strokeWidth = width.toFloat()
    }

    fun setShouldAntialias(shouldAntialias: Boolean) {
        fillPaint.isAntiAlias = shouldAntialias
        strokePaint.isAntiAlias = shouldAntialias
    }

    fun fill(rect: CGRect) {
        if (rect.isNull) {
            return
        }
        canvas.drawRect(toRectF(rect), fillPaint)
    }

    fun fill(rects: List<CGRect>) {
        for (rect in rects) {
            fill(rect)
        }
    }

    fun fillEllipse(`in`: CGRect) {
        if (`in`.isNull) {
            return
        }
        canvas.drawOval(toRectF(`in`), fillPaint)
    }

    fun stroke(rect: CGRect) {
        if (rect.isNull) {
            return
        }
        canvas.drawRect(toRectF(rect), strokePaint)
    }

    fun strokeEllipse(`in`: CGRect) {
        if (`in`.isNull) {
            return
        }
        canvas.drawOval(toRectF(`in`), strokePaint)
    }

    fun clear(rect: CGRect) {
        if (rect.isNull) {
            return
        }
        canvas.save()
        canvas.clipRect(toRectF(rect))
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        canvas.restore()
    }

    fun move(to: CGPoint) {
        currentPath.moveTo(to.x.toFloat(), to.y.toFloat())
    }

    fun addLine(to: CGPoint) {
        currentPath.lineTo(to.x.toFloat(), to.y.toFloat())
    }

    fun addRect(rect: CGRect) {
        if (rect.isNull) {
            return
        }
        currentPath.addRect(toRectF(rect), Path.Direction.CW)
    }

    fun addEllipse(`in`: CGRect) {
        if (`in`.isNull) {
            return
        }
        currentPath.addOval(toRectF(`in`), Path.Direction.CW)
    }

    fun closePath() {
        currentPath.close()
    }

    fun addPath(path: CGPath) {
        currentPath.addPath(path)
    }

    fun fillPath() {
        canvas.drawPath(currentPath, fillPaint)
        currentPath.reset()
    }

    fun strokePath() {
        canvas.drawPath(currentPath, strokePaint)
        currentPath.reset()
    }

    fun makeImage(): Bitmap? {
        return try {
            val image = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
            if (hasAlpha) {
                return image
            }
            val pixels = IntArray(width * height)
            val premultiplied = ByteBuffer.allocate(width * height * 4).order(ByteOrder.nativeOrder())
            bitmap.copyPixelsToBuffer(premultiplied)
            val bytes = premultiplied.array()
            for (index in 0 until width * height) {
                val red = bytes[index * 4].toInt() and 0xFF
                val green = bytes[index * 4 + 1].toInt() and 0xFF
                val blue = bytes[index * 4 + 2].toInt() and 0xFF
                pixels[index] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }
            val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            opaque.setPixels(pixels, 0, width, 0, 0, width, height)
            image.recycle()
            opaque
        } catch (error: Throwable) {
            Log.w(TAG, "CGContext.makeImage failed: $error")
            null
        }
    }

    private fun toArgb(red: Double, green: Double, blue: Double, alpha: Double): Int {
        var r = red
        var g = green
        var b = blue
        if (isGray) {
            val gray = 0.299 * red + 0.587 * green + 0.114 * blue
            r = gray
            g = gray
            b = gray
        }
        return (toByte(alpha) shl 24) or (toByte(r) shl 16) or (toByte(g) shl 8) or toByte(b)
    }

    private fun toByte(value: Double): Int {
        return (min(max(value, 0.0), 1.0) * 255.0).roundToInt()
    }

    private fun toRectF(rect: CGRect): RectF {
        return RectF(rect.minX.toFloat(), rect.minY.toFloat(), rect.maxX.toFloat(), rect.maxY.toFloat())
    }

    companion object {
        operator fun invoke(
            data: ByteArray?,
            width: Int,
            height: Int,
            bitsPerComponent: Int,
            bytesPerRow: Int,
            space: CGColorSpace,
            bitmapInfo: Int,
        ): CGContext? {
            if (width <= 0 || height <= 0 || bitsPerComponent != 8) {
                return null
            }
            val isGray = space.name == CGColorSpace.deviceGray || space.name == CGColorSpace.genericGrayGamma2_2
            val alphaInfo = bitmapInfo and 0x1F
            val hasAlpha = !isGray && alphaInfo != CGImageAlphaInfo.none.rawValue &&
                alphaInfo != CGImageAlphaInfo.noneSkipLast.rawValue &&
                alphaInfo != CGImageAlphaInfo.noneSkipFirst.rawValue
            val bytesPerPixel = if (isGray) 1 else 4
            val rowBytes = if (bytesPerRow > 0) bytesPerRow else width * bytesPerPixel
            if (rowBytes < width * bytesPerPixel) {
                return null
            }
            val bitmap = try {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } catch (error: Throwable) {
                Log.w(TAG, "CGContext ${width}x$height failed: $error")
                return null
            }
            if (data != null && data.size >= rowBytes * (height - 1) + width * bytesPerPixel) {
                val pixels = ByteBuffer.allocate(width * height * 4).order(ByteOrder.nativeOrder())
                val out = pixels.array()
                for (row in 0 until height) {
                    for (column in 0 until width) {
                        val target = (row * width + column) * 4
                        if (isGray) {
                            val gray = data[row * rowBytes + column]
                            out[target] = gray
                            out[target + 1] = gray
                            out[target + 2] = gray
                            out[target + 3] = 0xFF.toByte()
                        } else {
                            val source = row * rowBytes + column * 4
                            val first = alphaInfo == CGImageAlphaInfo.premultipliedFirst.rawValue ||
                                alphaInfo == CGImageAlphaInfo.first.rawValue ||
                                alphaInfo == CGImageAlphaInfo.noneSkipFirst.rawValue
                            val offset = if (first) 1 else 0
                            val alpha = if (!hasAlpha) {
                                0xFF.toByte()
                            } else if (first) {
                                data[source]
                            } else {
                                data[source + 3]
                            }
                            out[target] = data[source + offset]
                            out[target + 1] = data[source + offset + 1]
                            out[target + 2] = data[source + offset + 2]
                            out[target + 3] = alpha
                        }
                    }
                }
                pixels.rewind()
                bitmap.copyPixelsFromBuffer(pixels)
            }
            return CGContext(width, height, isGray, hasAlpha, bitmap)
        }
    }
}

class CGImageSourceFormat(val bitsPerComponent: Int, val componentsPerPixel: Int)

object CGImageSourceFormats {
    private val formats = WeakHashMap<Bitmap, CGImageSourceFormat>()

    fun set(bitmap: Bitmap, format: CGImageSourceFormat) {
        synchronized(formats) {
            formats[bitmap] = format
        }
    }

    fun get(bitmap: Bitmap): CGImageSourceFormat? {
        return synchronized(formats) {
            formats[bitmap]
        }
    }
}

class CGDataProvider internal constructor(val data: ByteArray?)

val Bitmap.bitsPerComponent: Int
    get() = CGImageSourceFormats.get(this)?.bitsPerComponent ?: 8

val Bitmap.bitsPerPixel: Int
    get() {
        val format = CGImageSourceFormats.get(this) ?: return 32
        return format.bitsPerComponent * format.componentsPerPixel
    }

val Bitmap.bytesPerRowInSource: Int
    get() = width * bitsPerPixel / 8

val Bitmap.dataProvider: CGDataProvider?
    get() {
        if (isRecycled) {
            return null
        }
        return try {
            CGDataProvider(sourcePixels(this))
        } catch (error: Throwable) {
            Log.w(TAG, "Bitmap.dataProvider failed: $error")
            null
        }
    }

private fun sourcePixels(bitmap: Bitmap): ByteArray {
    val format = CGImageSourceFormats.get(bitmap) ?: CGImageSourceFormat(8, 4)
    val components = format.componentsPerPixel
    val width = bitmap.width
    val height = bitmap.height
    val count = width * height
    if (format.bitsPerComponent == 16) {
        val floats = readStraightFloats(bitmap)
        val out = ByteArray(count * components * 2)
        for (index in 0 until count) {
            for (component in 0 until components) {
                val channel = sourceChannel(component, components)
                val value = (min(max(floats[index * 4 + channel], 0f), 1f) * 65535f).roundToInt()
                val target = (index * components + component) * 2
                out[target] = (value and 0xFF).toByte()
                out[target + 1] = ((value shr 8) and 0xFF).toByte()
            }
        }
        return out
    }
    val pixels = IntArray(count)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val out = ByteArray(count * components)
    for (index in 0 until count) {
        val pixel = pixels[index]
        val rgba = intArrayOf(
            (pixel shr 16) and 0xFF,
            (pixel shr 8) and 0xFF,
            pixel and 0xFF,
            (pixel ushr 24) and 0xFF
        )
        for (component in 0 until components) {
            out[index * components + component] = rgba[sourceChannel(component, components)].toByte()
        }
    }
    return out
}

private fun sourceChannel(component: Int, components: Int): Int {
    return when (components) {
        1 -> 0
        2 -> if (component == 0) 0 else 3
        else -> component
    }
}

private fun readStraightFloats(bitmap: Bitmap): FloatArray {
    val count = bitmap.width * bitmap.height
    val out = FloatArray(count * 4)
    if (bitmap.config == Bitmap.Config.RGBA_F16) {
        val buffer = ByteBuffer.allocate(count * 8).order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        val shorts = buffer.asShortBuffer()
        for (index in 0 until count) {
            val red = halfToFloat(shorts.get(index * 4))
            val green = halfToFloat(shorts.get(index * 4 + 1))
            val blue = halfToFloat(shorts.get(index * 4 + 2))
            val alpha = halfToFloat(shorts.get(index * 4 + 3))
            val scale = if (bitmap.isPremultiplied && alpha > 0f) 1f / alpha else 1f
            out[index * 4] = red * scale
            out[index * 4 + 1] = green * scale
            out[index * 4 + 2] = blue * scale
            out[index * 4 + 3] = alpha
        }
        return out
    }
    val pixels = IntArray(count)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (index in 0 until count) {
        val pixel = pixels[index]
        out[index * 4] = ((pixel shr 16) and 0xFF) / 255f
        out[index * 4 + 1] = ((pixel shr 8) and 0xFF) / 255f
        out[index * 4 + 2] = (pixel and 0xFF) / 255f
        out[index * 4 + 3] = ((pixel ushr 24) and 0xFF) / 255f
    }
    return out
}

private fun halfToFloat(half: Short): Float {
    val bits = half.toInt() and 0xFFFF
    val sign = (bits shr 15) and 0x1
    val exponent = (bits shr 10) and 0x1F
    val mantissa = bits and 0x3FF
    val value = when (exponent) {
        0 -> mantissa / 1024f * Math.pow(2.0, -14.0).toFloat()
        0x1F -> if (mantissa == 0) Float.POSITIVE_INFINITY else Float.NaN
        else -> (1f + mantissa / 1024f) * Math.pow(2.0, (exponent - 15).toDouble()).toFloat()
    }
    return if (sign == 1) -value else value
}
