package com.moblin.android.platform.video

import android.graphics.Bitmap
import android.util.Log
import com.moblin.android.platform.core.PipelineThread
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

private const val TAG = "MoblinPipeline"

object CVPixelBufferLockFlags {
    const val readOnly = 1
}

internal object SoftwareRendering {
    @Volatile
    private var active: Boolean? = null

    val isActive: Boolean
        get() {
            active?.let { return it }
            if (!EglCore.isSetUp) {
                try {
                    PipelineThread.runSync(timeoutMs = 5000) { EglCore.hasGpu }
                } catch (error: Throwable) {
                    return false
                }
            }
            val value = !(EglCore.isReady && !EglCore.renderer.isNullOrEmpty())
            active = value
            if (value) {
                Log.i(TAG, "No GPU, pixel buffers live in memory and are rendered on the CPU")
            }
            return value
        }
}

internal class PixelBufferMemory(val pixelFormatType: Int, val width: Int, val height: Int) {
    val info: PixelFormatInfo = PixelFormats.info(pixelFormatType) ?: PixelFormats.bgra
    val chromaWidth = (width + 1) / 2
    val chromaHeight = (height + 1) / 2
    val planeCount = info.planeCount
    val heights = IntArray(planeCount)
    val bytesPerRow = IntArray(planeCount)
    val planes: Array<ByteBuffer>

    init {
        if (info.isYCbCr) {
            heights[0] = height
            bytesPerRow[0] = width * info.bytesPerComponent
            heights[1] = chromaHeight
            bytesPerRow[1] = chromaWidth * 2 * info.bytesPerComponent
        } else {
            heights[0] = height
            bytesPerRow[0] = width * 4
        }
        planes = Array(planeCount) { index ->
            ByteBuffer.allocate(bytesPerRow[index] * heights[index]).order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    private val shift = if (info.bytesPerComponent == 2) 16 - info.bitsPerComponent else 0

    fun luma(x: Int, y: Int): Int {
        return sample(0, y * bytesPerRow[0] + x * info.bytesPerComponent)
    }

    fun cb(chromaX: Int, chromaY: Int): Int {
        return sample(1, chromaY * bytesPerRow[1] + chromaX * 2 * info.bytesPerComponent)
    }

    fun cr(chromaX: Int, chromaY: Int): Int {
        return sample(1, chromaY * bytesPerRow[1] + (chromaX * 2 + 1) * info.bytesPerComponent)
    }

    fun setLuma(x: Int, y: Int, code: Int) {
        setSample(0, y * bytesPerRow[0] + x * info.bytesPerComponent, code)
    }

    fun setChroma(chromaX: Int, chromaY: Int, cb: Int, cr: Int) {
        val offset = chromaY * bytesPerRow[1] + chromaX * 2 * info.bytesPerComponent
        setSample(1, offset, cb)
        setSample(1, offset + info.bytesPerComponent, cr)
    }

    fun rgba(x: Int, y: Int, output: DoubleArray) {
        val offset = y * bytesPerRow[0] + x * 4
        val plane = planes[0]
        val first = (plane.get(offset).toInt() and 0xFF) / 255.0
        val second = (plane.get(offset + 1).toInt() and 0xFF) / 255.0
        val third = (plane.get(offset + 2).toInt() and 0xFF) / 255.0
        val alpha = (plane.get(offset + 3).toInt() and 0xFF) / 255.0
        if (info.isBgra) {
            output[0] = third
            output[1] = second
            output[2] = first
        } else {
            output[0] = first
            output[1] = second
            output[2] = third
        }
        output[3] = alpha
    }

    fun setRgba(x: Int, y: Int, red: Double, green: Double, blue: Double, alpha: Double) {
        val offset = y * bytesPerRow[0] + x * 4
        val plane = planes[0]
        val first = if (info.isBgra) blue else red
        val third = if (info.isBgra) red else blue
        plane.put(offset, unit8(first))
        plane.put(offset + 1, unit8(green))
        plane.put(offset + 2, unit8(third))
        plane.put(offset + 3, unit8(alpha))
    }

    fun readRgb(x: Int, y: Int, coding: YCbCrCoding?, output: DoubleArray) {
        if (coding == null || !info.isYCbCr) {
            rgba(x, y, output)
            return
        }
        coding.decode(luma(x, y).toDouble(), cb(x / 2, y / 2).toDouble(), cr(x / 2, y / 2).toDouble(), output)
        output[3] = 1.0
    }

    fun writeRgb(
        image: DoubleArray,
        imageWidth: Int,
        imageHeight: Int,
        coding: YCbCrCoding?,
        keepAlpha: Boolean,
    ) {
        val rows = minOf(imageHeight, height)
        val columns = minOf(imageWidth, width)
        if (coding == null || !info.isYCbCr) {
            for (y in 0 until rows) {
                for (x in 0 until columns) {
                    val index = (y * imageWidth + x) * 4
                    setRgba(x, y, image[index], image[index + 1], image[index + 2], if (keepAlpha) image[index + 3] else 1.0)
                }
            }
            return
        }
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                val index = (y * imageWidth + x) * 4
                setLuma(x, y, coding.code(coding.luma(image[index], image[index + 1], image[index + 2])))
            }
        }
        for (chromaY in 0 until (rows + 1) / 2) {
            for (chromaX in 0 until (columns + 1) / 2) {
                var cb = 0.0
                var cr = 0.0
                var count = 0
                for (dy in 0..1) {
                    val y = chromaY * 2 + dy
                    if (y >= rows) {
                        continue
                    }
                    for (dx in 0..1) {
                        val x = chromaX * 2 + dx
                        if (x >= columns) {
                            continue
                        }
                        val index = (y * imageWidth + x) * 4
                        cb += coding.cb(image[index], image[index + 1], image[index + 2])
                        cr += coding.cr(image[index], image[index + 1], image[index + 2])
                        count += 1
                    }
                }
                setChroma(chromaX, chromaY, coding.code(cb / count), coding.code(cr / count))
            }
        }
    }

    private fun sample(plane: Int, offset: Int): Int {
        return if (info.bytesPerComponent == 2) {
            (planes[plane].getShort(offset).toInt() and 0xFFFF) ushr shift
        } else {
            planes[plane].get(offset).toInt() and 0xFF
        }
    }

    private fun setSample(plane: Int, offset: Int, code: Int) {
        if (info.bytesPerComponent == 2) {
            planes[plane].putShort(offset, (code shl shift).toShort())
        } else {
            planes[plane].put(offset, code.toByte())
        }
    }

    private fun unit8(value: Double): Byte {
        return (value.coerceIn(0.0, 1.0) * 255).roundToInt().toByte()
    }
}

internal fun CVPixelBuffer.memory(): PixelBufferMemory {
    val backing = backing
    synchronized(backing) {
        val existing = backing.memory
        if (existing != null && existing.pixelFormatType == pixelFormatType && existing.width == width &&
            existing.height == height
        ) {
            return existing
        }
        val memory = PixelBufferMemory(pixelFormatType, width, height)
        backing.memory = memory
        return memory
    }
}

internal fun CVPixelBuffer.isMemoryAuthoritative(): Boolean {
    return SoftwareRendering.isActive || !backing.isGpuBacked
}

fun CVPixelBufferCreate(
    allocator: Any?,
    width: Int,
    height: Int,
    pixelFormatType: Int,
    pixelBufferAttributes: Map<String, Any>?,
): CVPixelBuffer? {
    if (width <= 0 || height <= 0 || PixelFormats.info(pixelFormatType) == null) {
        return null
    }
    val buffer = if (SoftwareRendering.isActive) {
        val backing = PixelBufferBacking(0, 0, width, height)
        PixelBufferReaper.wrapUnpooled(backing, pixelFormatType).also { it.memory() }
    } else {
        try {
            PipelineThread.runSync {
                val backing = PixelBufferGl.allocate(width, height)
                if (backing == null) {
                    null
                } else {
                    PixelBufferGl.clear(backing, 0f, 0f, 0f, 1f)
                    PixelBufferReaper.wrapUnpooled(backing, PixelFormats.eightBitFormat(pixelFormatType))
                }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "CVPixelBufferCreate failed: $error")
            null
        }
    } ?: return null
    val attachments = pixelBufferAttributes?.get(kCVBufferPropagatedAttachmentsKey) as? Map<*, *>
    if (attachments != null) {
        for ((key, value) in attachments) {
            if (key is String && value != null) {
                buffer.attachments[key] = value
            }
        }
    }
    return buffer
}

fun CVPixelBufferLockBaseAddress(pixelBuffer: CVPixelBuffer, lockFlags: Int): Int {
    if (!pixelBuffer.checkReadable("CVPixelBufferLockBaseAddress")) {
        return kCVReturnInvalidArgument
    }
    val memory = pixelBuffer.memory()
    if (pixelBuffer.isMemoryAuthoritative()) {
        return kCVReturnSuccess
    }
    val read = try {
        PipelineThread.runSync(timeoutMs = 3000) { PixelBufferMemorySync.readBack(pixelBuffer, memory) }
    } catch (error: Throwable) {
        Log.w(TAG, "CVPixelBufferLockBaseAddress readback failed: $error")
        false
    }
    return if (read) kCVReturnSuccess else kCVReturnInvalidArgument
}

fun CVPixelBufferUnlockBaseAddress(pixelBuffer: CVPixelBuffer, unlockFlags: Int): Int {
    if ((unlockFlags and CVPixelBufferLockFlags.readOnly) != 0 || pixelBuffer.isMemoryAuthoritative()) {
        return kCVReturnSuccess
    }
    val memory = pixelBuffer.backing.memory ?: return kCVReturnSuccess
    try {
        PipelineThread.runSync(timeoutMs = 3000) { PixelBufferMemorySync.upload(memory, pixelBuffer) }
    } catch (error: Throwable) {
        Log.w(TAG, "CVPixelBufferUnlockBaseAddress upload failed: $error")
        return kCVReturnInvalidArgument
    }
    return kCVReturnSuccess
}

fun CVPixelBufferGetBaseAddressOfPlane(pixelBuffer: CVPixelBuffer, planeIndex: Int): ByteBuffer? {
    val memory = pixelBuffer.memory()
    if (!memory.info.isYCbCr || planeIndex !in 0 until memory.planeCount) {
        return null
    }
    return memory.planes[planeIndex].duplicate().order(ByteOrder.LITTLE_ENDIAN)
}

fun CVPixelBufferGetBaseAddress(pixelBuffer: CVPixelBuffer): ByteBuffer? {
    return pixelBuffer.memory().planes[0].duplicate().order(ByteOrder.LITTLE_ENDIAN)
}

fun CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer: CVPixelBuffer, planeIndex: Int): Int {
    val memory = pixelBuffer.memory()
    return if (memory.info.isYCbCr && planeIndex in 0 until memory.planeCount) memory.bytesPerRow[planeIndex] else 0
}

fun CVPixelBufferGetBytesPerRow(pixelBuffer: CVPixelBuffer): Int {
    return pixelBuffer.memory().bytesPerRow[0]
}

fun CVPixelBufferGetWidthOfPlane(pixelBuffer: CVPixelBuffer, planeIndex: Int): Int {
    val info = PixelFormats.info(pixelBuffer.pixelFormatType) ?: return 0
    return when {
        !info.isYCbCr -> 0
        planeIndex == 0 -> pixelBuffer.width
        planeIndex == 1 -> (pixelBuffer.width + 1) / 2
        else -> 0
    }
}

fun CVPixelBufferGetHeightOfPlane(pixelBuffer: CVPixelBuffer, planeIndex: Int): Int {
    val info = PixelFormats.info(pixelBuffer.pixelFormatType) ?: return 0
    return when {
        !info.isYCbCr -> 0
        planeIndex == 0 -> pixelBuffer.height
        planeIndex == 1 -> (pixelBuffer.height + 1) / 2
        else -> 0
    }
}

fun CVBufferPropagateAttachments(sourceBuffer: CVPixelBuffer, destinationBuffer: CVPixelBuffer) {
    destinationBuffer.attachments.putAll(sourceBuffer.attachments)
}

internal object PixelBufferMemorySync {
    fun readBack(buffer: CVPixelBuffer, memory: PixelBufferMemory): Boolean {
        val bitmap = PixelBufferGl.readBitmap(buffer, buffer.width, buffer.height) ?: return false
        try {
            val pixels = IntArray(buffer.width * buffer.height)
            bitmap.getPixels(pixels, 0, buffer.width, 0, 0, buffer.width, buffer.height)
            val image = DoubleArray(pixels.size * 4)
            for ((index, pixel) in pixels.withIndex()) {
                image[index * 4] = ((pixel ushr 16) and 0xFF) / 255.0
                image[index * 4 + 1] = ((pixel ushr 8) and 0xFF) / 255.0
                image[index * 4 + 2] = (pixel and 0xFF) / 255.0
                image[index * 4 + 3] = ((pixel ushr 24) and 0xFF) / 255.0
            }
            memory.writeRgb(image, buffer.width, buffer.height, YCbCrCoding.forBuffer(buffer), true)
            return true
        } finally {
            bitmap.recycle()
        }
    }

    fun upload(memory: PixelBufferMemory, buffer: CVPixelBuffer) {
        if (buffer.layout.isPlanar) {
            Log.w(TAG, "Writing planar GPU pixel buffers from the CPU is not supported")
            return
        }
        val coding = YCbCrCoding.forBuffer(buffer)
        val pixel = DoubleArray(4)
        val pixels = IntArray(buffer.width * buffer.height)
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                memory.readRgb(x, y, coding, pixel)
                val alpha = (pixel[3].coerceIn(0.0, 1.0) * 255).roundToInt()
                val red = (pixel[0].coerceIn(0.0, 1.0) * 255).roundToInt()
                val green = (pixel[1].coerceIn(0.0, 1.0) * 255).roundToInt()
                val blue = (pixel[2].coerceIn(0.0, 1.0) * 255).roundToInt()
                pixels[y * buffer.width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, buffer.width, buffer.height, Bitmap.Config.ARGB_8888)
        try {
            PixelBufferGl.upload(bitmap, buffer)
        } finally {
            bitmap.recycle()
        }
    }
}
