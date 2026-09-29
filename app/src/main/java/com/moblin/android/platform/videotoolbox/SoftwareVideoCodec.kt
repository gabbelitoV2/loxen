package com.moblin.android.platform.videotoolbox

import android.media.MediaFormat
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.ColorDescription
import com.moblin.android.platform.video.YCbCrCoding
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey
import com.moblin.android.platform.video.memory
import java.io.ByteArrayOutputStream

internal class BitWriter {
    private val output = ByteArrayOutputStream()
    private var current = 0
    private var count = 0

    val isByteAligned: Boolean
        get() = count == 0

    fun bit(value: Int) {
        current = (current shl 1) or (value and 1)
        count += 1
        if (count == 8) {
            output.write(current)
            current = 0
            count = 0
        }
    }

    fun bits(value: Long, length: Int) {
        for (index in length - 1 downTo 0) {
            bit(((value ushr index) and 1L).toInt())
        }
    }

    fun bits(value: Int, length: Int) {
        bits(value.toLong() and 0xFFFF_FFFFL, length)
    }

    fun flag(value: Boolean) {
        bit(if (value) 1 else 0)
    }

    fun ue(value: Int) {
        val code = value.toLong() + 1
        val length = 64 - java.lang.Long.numberOfLeadingZeros(code)
        bits(0L, length - 1)
        bits(code, length)
    }

    fun se(value: Int) {
        ue(if (value > 0) 2 * value - 1 else -2 * value)
    }

    fun alignZero() {
        while (count != 0) {
            bit(0)
        }
    }

    fun trailingBits() {
        bit(1)
        alignZero()
    }

    fun bytes(): ByteArray {
        return output.toByteArray()
    }
}

internal class BitReader(private val data: ByteArray) {
    private var position = 0L

    val bitsLeft: Long
        get() = data.size * 8L - position

    fun bit(): Int {
        if (position >= data.size * 8L) {
            throw IllegalStateException("Out of data")
        }
        val byte = data[(position / 8).toInt()].toInt()
        val value = (byte shr (7 - (position % 8).toInt())) and 1
        position += 1
        return value
    }

    fun bits(length: Int): Int {
        var value = 0
        repeat(length) {
            value = (value shl 1) or bit()
        }
        return value
    }

    fun flag(): Boolean {
        return bit() == 1
    }

    fun ue(): Int {
        var leadingZeros = 0
        while (bit() == 0) {
            leadingZeros += 1
            if (leadingZeros > 31) {
                throw IllegalStateException("Invalid exp-Golomb code")
            }
        }
        if (leadingZeros == 0) {
            return 0
        }
        return ((1L shl leadingZeros) - 1 + bits(leadingZeros).toLong()).toInt()
    }

    fun se(): Int {
        val value = ue()
        return if (value % 2 == 1) (value + 1) / 2 else -(value / 2)
    }

    fun skip(length: Int) {
        position += length
    }

    fun alignToByte() {
        position = (position + 7) / 8 * 8
    }

    fun byteAt(): Int {
        val value = data[(position / 8).toInt()].toInt() and 0xFF
        position += 8
        return value
    }

    fun moreRbspData(): Boolean {
        if (position >= data.size * 8L) {
            return false
        }
        var last = data.size - 1
        while (last >= 0 && data[last].toInt() == 0) {
            last -= 1
        }
        if (last < 0) {
            return false
        }
        val stopBit = last * 8L + 7 - Integer.numberOfTrailingZeros(data[last].toInt() and 0xFF)
        return position < stopBit
    }
}

internal object Rbsp {
    fun escape(rbsp: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(rbsp.size + rbsp.size / 64 + 8)
        var zeros = 0
        for (byte in rbsp) {
            val value = byte.toInt() and 0xFF
            if (zeros >= 2 && value <= 3) {
                output.write(3)
                zeros = 0
            }
            output.write(value)
            zeros = if (value == 0) zeros + 1 else 0
        }
        return output.toByteArray()
    }

    fun unescape(nalUnit: ByteArray, offset: Int = 0): ByteArray {
        val output = ByteArrayOutputStream(nalUnit.size)
        var zeros = 0
        var index = offset
        while (index < nalUnit.size) {
            val value = nalUnit[index].toInt() and 0xFF
            if (zeros >= 2 && value == 3) {
                zeros = 0
                index += 1
                continue
            }
            output.write(value)
            zeros = if (value == 0) zeros + 1 else 0
            index += 1
        }
        return output.toByteArray()
    }

    fun annexB(nalUnits: List<ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        for (nalUnit in nalUnits) {
            output.write(0)
            output.write(0)
            output.write(0)
            output.write(1)
            output.write(nalUnit, 0, nalUnit.size)
        }
        return output.toByteArray()
    }
}

internal class YuvFrame(val width: Int, val height: Int, val bits: Int) {
    val chromaWidth = width / 2
    val chromaHeight = height / 2
    val luma = IntArray(width * height)
    val cb = IntArray(chromaWidth * chromaHeight)
    val cr = IntArray(chromaWidth * chromaHeight)

    companion object {
        fun from(
            buffer: CVPixelBuffer,
            width: Int,
            height: Int,
            codedWidth: Int,
            codedHeight: Int,
            coding: YCbCrCoding,
        ): YuvFrame {
            val frame = YuvFrame(codedWidth, codedHeight, coding.bits)
            val memory = buffer.memory()
            val sourceCoding = YCbCrCoding.forBuffer(buffer)
            val sameCoding = sourceCoding != null && buffer.width == width && buffer.height == height &&
                sourceCoding.videoRange == coding.videoRange &&
                YCbCrCoding.coefficients(buffer.attachments[kCVImageBufferYCbCrMatrixKey] as? String) ==
                Pair(coding.kr, coding.kb)
            if (sameCoding && sourceCoding != null) {
                val shift = coding.bits - sourceCoding.bits
                for (y in 0 until codedHeight) {
                    val sourceY = minOf(y, height - 1)
                    for (x in 0 until codedWidth) {
                        frame.luma[y * codedWidth + x] = rescale(memory.luma(minOf(x, width - 1), sourceY), shift)
                    }
                }
                for (y in 0 until frame.chromaHeight) {
                    val sourceY = minOf(y, memory.chromaHeight - 1)
                    for (x in 0 until frame.chromaWidth) {
                        val sourceX = minOf(x, memory.chromaWidth - 1)
                        frame.cb[y * frame.chromaWidth + x] = rescale(memory.cb(sourceX, sourceY), shift)
                        frame.cr[y * frame.chromaWidth + x] = rescale(memory.cr(sourceX, sourceY), shift)
                    }
                }
                return frame
            }
            val pixel = DoubleArray(4)
            val cbSums = DoubleArray(frame.cb.size)
            val crSums = DoubleArray(frame.cr.size)
            for (y in 0 until codedHeight) {
                val sourceY = minOf(y, height - 1) * buffer.height / height
                for (x in 0 until codedWidth) {
                    val sourceX = minOf(x, width - 1) * buffer.width / width
                    memory.readRgb(sourceX, sourceY, sourceCoding, pixel)
                    val red = pixel[0].coerceIn(0.0, 1.0)
                    val green = pixel[1].coerceIn(0.0, 1.0)
                    val blue = pixel[2].coerceIn(0.0, 1.0)
                    frame.luma[y * codedWidth + x] = coding.code(coding.luma(red, green, blue))
                    val chromaIndex = (y / 2) * frame.chromaWidth + x / 2
                    cbSums[chromaIndex] += coding.cb(red, green, blue)
                    crSums[chromaIndex] += coding.cr(red, green, blue)
                }
            }
            for (index in frame.cb.indices) {
                frame.cb[index] = coding.code(cbSums[index] / 4)
                frame.cr[index] = coding.code(crSums[index] / 4)
            }
            return frame
        }

        private fun rescale(value: Int, shift: Int): Int {
            return when {
                shift > 0 -> value shl shift
                shift < 0 -> value shr -shift
                else -> value
            }
        }
    }
}

internal class SoftwareEncodedFrame(val annexB: ByteArray, val isKeyFrame: Boolean)

internal abstract class SoftwareVideoEncoder(
    val width: Int,
    val height: Int,
    val color: ColorDescription,
    private val keyFrameIntervalUs: Long,
) {
    abstract val bits: Int
    abstract val codedWidth: Int
    abstract val codedHeight: Int
    private var latestKeyFrameUs = Long.MIN_VALUE
    protected var frameNumber = 0

    val coding: YCbCrCoding
        get() = YCbCrCoding.make(color.yCbCrMatrix, bits, color.fullRange != true)

    fun encode(buffer: CVPixelBuffer, presentationTimeStamp: Long, forceKeyFrame: Boolean): SoftwareEncodedFrame {
        val keyFrame = forceKeyFrame || latestKeyFrameUs == Long.MIN_VALUE ||
            (keyFrameIntervalUs > 0 && presentationTimeStamp - latestKeyFrameUs >= keyFrameIntervalUs)
        if (keyFrame) {
            latestKeyFrameUs = presentationTimeStamp
            frameNumber = 0
        } else {
            frameNumber += 1
        }
        val frame = YuvFrame.from(buffer, width, height, codedWidth, codedHeight, coding)
        return SoftwareEncodedFrame(Rbsp.annexB(encodeFrame(frame, keyFrame)), keyFrame)
    }

    protected abstract fun encodeFrame(frame: YuvFrame, keyFrame: Boolean): List<ByteArray>

    protected fun nalUnit(header: ByteArray, rbsp: ByteArray): ByteArray {
        return header + Rbsp.escape(rbsp)
    }
}

internal class SoftwareH264Encoder(
    width: Int,
    height: Int,
    color: ColorDescription,
    keyFrameIntervalUs: Long,
    private val profileIdc: Int,
) : SoftwareVideoEncoder(width, height, color, keyFrameIntervalUs) {
    override val bits = 8
    private val widthInMacroblocks = (width + 15) / 16
    private val heightInMacroblocks = (height + 15) / 16
    override val codedWidth = widthInMacroblocks * 16
    override val codedHeight = heightInMacroblocks * 16
    private var idrPictureId = 0

    override fun encodeFrame(frame: YuvFrame, keyFrame: Boolean): List<ByteArray> {
        val nalUnits = mutableListOf<ByteArray>()
        if (keyFrame) {
            nalUnits.add(nalUnit(byteArrayOf(0x67), sequenceParameterSet()))
            nalUnits.add(nalUnit(byteArrayOf(0x68), pictureParameterSet()))
        }
        nalUnits.add(nalUnit(byteArrayOf(if (keyFrame) 0x65 else 0x01), slice(frame, keyFrame)))
        if (keyFrame) {
            idrPictureId = (idrPictureId + 1) % 2
        }
        return nalUnits
    }

    private fun levelIdc(): Int {
        val macroblocks = widthInMacroblocks * heightInMacroblocks
        return when {
            macroblocks <= 1620 -> 30
            macroblocks <= 3600 -> 31
            macroblocks <= 5120 -> 32
            macroblocks <= 8192 -> 40
            macroblocks <= 8704 -> 42
            macroblocks <= 22080 -> 50
            else -> 51
        }
    }

    private fun sequenceParameterSet(): ByteArray {
        val writer = BitWriter()
        writer.bits(profileIdc, 8)
        writer.bits(if (profileIdc == 66) 0xC0 else if (profileIdc == 77) 0x40 else 0x00, 8)
        writer.bits(levelIdc(), 8)
        writer.ue(0)
        if (profileIdc == 100) {
            writer.ue(1)
            writer.ue(0)
            writer.ue(0)
            writer.flag(false)
            writer.flag(false)
        }
        writer.ue(0)
        writer.ue(0)
        writer.ue(12)
        writer.ue(1)
        writer.flag(false)
        writer.ue(widthInMacroblocks - 1)
        writer.ue(heightInMacroblocks - 1)
        writer.flag(true)
        writer.flag(true)
        val cropRight = (codedWidth - width) / 2
        val cropBottom = (codedHeight - height) / 2
        if (cropRight > 0 || cropBottom > 0) {
            writer.flag(true)
            writer.ue(0)
            writer.ue(cropRight)
            writer.ue(0)
            writer.ue(cropBottom)
        } else {
            writer.flag(false)
        }
        writer.flag(true)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.bits(5, 3)
        writer.flag(color.fullRange == true)
        writer.flag(true)
        writer.bits(color.primariesCode(), 8)
        writer.bits(color.transferCode(), 8)
        writer.bits(color.matrixCode(), 8)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.trailingBits()
        return writer.bytes()
    }

    private fun pictureParameterSet(): ByteArray {
        val writer = BitWriter()
        writer.ue(0)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.ue(0)
        writer.ue(0)
        writer.ue(0)
        writer.flag(false)
        writer.bits(0, 2)
        writer.se(0)
        writer.se(0)
        writer.se(0)
        writer.flag(true)
        writer.flag(false)
        writer.flag(false)
        writer.trailingBits()
        return writer.bytes()
    }

    private fun slice(frame: YuvFrame, keyFrame: Boolean): ByteArray {
        val writer = BitWriter()
        writer.ue(0)
        writer.ue(7)
        writer.ue(0)
        writer.bits(if (keyFrame) 0 else 1, 4)
        if (keyFrame) {
            writer.ue(idrPictureId)
        }
        writer.bits((2 * frameNumber) and 0xFFFF, 16)
        if (keyFrame) {
            writer.flag(false)
            writer.flag(false)
        }
        writer.se(0)
        writer.ue(1)
        for (macroblockY in 0 until heightInMacroblocks) {
            for (macroblockX in 0 until widthInMacroblocks) {
                writer.ue(25)
                writer.alignZero()
                for (y in 0 until 16) {
                    val row = (macroblockY * 16 + y) * frame.width + macroblockX * 16
                    for (x in 0 until 16) {
                        writer.bits(frame.luma[row + x], 8)
                    }
                }
                for (plane in listOf(frame.cb, frame.cr)) {
                    for (y in 0 until 8) {
                        val row = (macroblockY * 8 + y) * frame.chromaWidth + macroblockX * 8
                        for (x in 0 until 8) {
                            writer.bits(plane[row + x], 8)
                        }
                    }
                }
            }
        }
        writer.trailingBits()
        return writer.bytes()
    }
}

private val rangeTabLps = arrayOf(
    intArrayOf(128, 176, 208, 240), intArrayOf(128, 167, 197, 227), intArrayOf(128, 158, 187, 216),
    intArrayOf(123, 150, 178, 205), intArrayOf(116, 142, 169, 195), intArrayOf(111, 135, 160, 185),
    intArrayOf(105, 128, 152, 175), intArrayOf(100, 122, 144, 166), intArrayOf(95, 116, 137, 158),
    intArrayOf(90, 110, 130, 150), intArrayOf(85, 104, 123, 142), intArrayOf(81, 99, 117, 135),
    intArrayOf(77, 94, 111, 128), intArrayOf(73, 89, 105, 122), intArrayOf(69, 85, 100, 116),
    intArrayOf(66, 80, 95, 110), intArrayOf(62, 76, 90, 104), intArrayOf(59, 72, 86, 99),
    intArrayOf(56, 69, 81, 94), intArrayOf(53, 65, 77, 89), intArrayOf(51, 62, 73, 85),
    intArrayOf(48, 59, 69, 80), intArrayOf(46, 56, 66, 76), intArrayOf(43, 53, 63, 72),
    intArrayOf(41, 50, 59, 69), intArrayOf(39, 48, 56, 65), intArrayOf(37, 45, 54, 62),
    intArrayOf(35, 43, 51, 59), intArrayOf(33, 41, 48, 56), intArrayOf(32, 39, 46, 53),
    intArrayOf(30, 37, 43, 50), intArrayOf(29, 35, 41, 48), intArrayOf(27, 33, 39, 45),
    intArrayOf(26, 31, 37, 43), intArrayOf(24, 30, 35, 41), intArrayOf(23, 28, 33, 39),
    intArrayOf(22, 27, 32, 37), intArrayOf(21, 26, 30, 35), intArrayOf(20, 24, 29, 33),
    intArrayOf(19, 23, 27, 31), intArrayOf(18, 22, 26, 30), intArrayOf(17, 21, 25, 28),
    intArrayOf(16, 20, 23, 27), intArrayOf(15, 19, 22, 25), intArrayOf(14, 18, 21, 24),
    intArrayOf(14, 17, 20, 23), intArrayOf(13, 16, 19, 22), intArrayOf(12, 15, 18, 21),
    intArrayOf(12, 14, 17, 20), intArrayOf(11, 14, 16, 19), intArrayOf(11, 13, 15, 18),
    intArrayOf(10, 12, 15, 17), intArrayOf(10, 12, 14, 16), intArrayOf(9, 11, 13, 15),
    intArrayOf(9, 11, 12, 14), intArrayOf(8, 10, 12, 14), intArrayOf(8, 9, 11, 13),
    intArrayOf(7, 9, 11, 12), intArrayOf(7, 9, 10, 12), intArrayOf(7, 8, 10, 11),
    intArrayOf(6, 8, 9, 11), intArrayOf(6, 7, 9, 10), intArrayOf(6, 7, 8, 9),
    intArrayOf(2, 2, 2, 2),
)

private val transIdxLps = intArrayOf(
    0, 0, 1, 2, 2, 4, 4, 5, 6, 7, 8, 9, 9, 11, 11, 12, 13, 13, 15, 15, 16, 16, 18, 18, 19, 19, 21, 21, 22, 22, 23, 24,
    24, 25, 26, 26, 27, 27, 28, 29, 29, 30, 30, 30, 31, 32, 32, 33, 33, 33, 34, 34, 35, 35, 35, 36, 36, 36, 37, 37, 37,
    38, 38, 63,
)

internal class CabacContext(initValue: Int, sliceQp: Int) {
    var state: Int
    var mps: Int

    init {
        val slope = (initValue shr 4) * 5 - 45
        val offset = ((initValue and 15) shl 3) - 16
        val preState = (((slope * sliceQp.coerceIn(0, 51)) shr 4) + offset).coerceIn(1, 126)
        if (preState <= 63) {
            state = 63 - preState
            mps = 0
        } else {
            state = preState - 64
            mps = 1
        }
    }
}

internal class CabacEncoder(private val writer: BitWriter) {
    private var low = 0
    private var range = 510
    private var firstBit = true
    private var outstanding = 0

    fun reset() {
        low = 0
        range = 510
        firstBit = true
        outstanding = 0
    }

    fun encodeDecision(context: CabacContext, value: Int) {
        val lps = rangeTabLps[context.state][(range shr 6) and 3]
        range -= lps
        if (value != context.mps) {
            low += range
            range = lps
            if (context.state == 0) {
                context.mps = 1 - context.mps
            }
            context.state = transIdxLps[context.state]
        } else {
            context.state = minOf(context.state + 1, 62)
        }
        renormalize()
    }

    fun encodeTerminate(value: Int) {
        range -= 2
        if (value != 0) {
            low += range
            flush()
        } else {
            renormalize()
        }
    }

    private fun flush() {
        range = 2
        renormalize()
        putBit((low shr 9) and 1)
        writer.bits(((low shr 7) and 3) or 1, 2)
    }

    private fun renormalize() {
        while (range < 256) {
            when {
                low < 256 -> putBit(0)
                low >= 512 -> {
                    low -= 512
                    putBit(1)
                }
                else -> {
                    low -= 256
                    outstanding += 1
                }
            }
            range = range shl 1
            low = low shl 1
        }
    }

    private fun putBit(value: Int) {
        if (firstBit) {
            firstBit = false
        } else {
            writer.bit(value)
        }
        while (outstanding > 0) {
            writer.bit(1 - value)
            outstanding -= 1
        }
    }
}

internal class SoftwareHevcEncoder(
    width: Int,
    height: Int,
    color: ColorDescription,
    keyFrameIntervalUs: Long,
    main10: Boolean,
) : SoftwareVideoEncoder(width, height, color, keyFrameIntervalUs) {
    override val bits = if (main10) 10 else 8
    private val profileIdc = if (main10) 2 else 1
    override val codedWidth = (width + 15) / 16 * 16
    override val codedHeight = (height + 15) / 16 * 16
    private val sliceQp = 26

    override fun encodeFrame(frame: YuvFrame, keyFrame: Boolean): List<ByteArray> {
        val nalUnits = mutableListOf<ByteArray>()
        if (keyFrame) {
            nalUnits.add(nalUnit(header(32), videoParameterSet()))
            nalUnits.add(nalUnit(header(33), sequenceParameterSet()))
            nalUnits.add(nalUnit(header(34), pictureParameterSet()))
        }
        nalUnits.add(nalUnit(header(if (keyFrame) 19 else 1), slice(frame, keyFrame)))
        return nalUnits
    }

    private fun header(type: Int): ByteArray {
        return byteArrayOf((type shl 1).toByte(), 1)
    }

    private fun levelIdc(): Int {
        val samples = codedWidth.toLong() * codedHeight
        return when {
            samples <= 122_880 -> 60
            samples <= 245_760 -> 63
            samples <= 552_960 -> 90
            samples <= 983_040 -> 93
            samples <= 2_228_224 -> 120
            samples <= 8_912_896 -> 150
            else -> 180
        }
    }

    private fun profileTierLevel(writer: BitWriter) {
        writer.bits(0, 2)
        writer.flag(false)
        writer.bits(profileIdc, 5)
        for (index in 0 until 32) {
            writer.flag(index == profileIdc || (profileIdc == 1 && index == 2))
        }
        writer.flag(true)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.bits(0L, 44)
        writer.bits(levelIdc(), 8)
    }

    private fun videoParameterSet(): ByteArray {
        val writer = BitWriter()
        writer.bits(0, 4)
        writer.flag(true)
        writer.flag(true)
        writer.bits(0, 6)
        writer.bits(0, 3)
        writer.flag(true)
        writer.bits(0xFFFF, 16)
        profileTierLevel(writer)
        writer.flag(true)
        writer.ue(0)
        writer.ue(0)
        writer.ue(0)
        writer.bits(0, 6)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.trailingBits()
        return writer.bytes()
    }

    private fun sequenceParameterSet(): ByteArray {
        val writer = BitWriter()
        writer.bits(0, 4)
        writer.bits(0, 3)
        writer.flag(true)
        profileTierLevel(writer)
        writer.ue(0)
        writer.ue(1)
        writer.ue(codedWidth)
        writer.ue(codedHeight)
        val cropRight = (codedWidth - width) / 2
        val cropBottom = (codedHeight - height) / 2
        if (cropRight > 0 || cropBottom > 0) {
            writer.flag(true)
            writer.ue(0)
            writer.ue(cropRight)
            writer.ue(0)
            writer.ue(cropBottom)
        } else {
            writer.flag(false)
        }
        writer.ue(bits - 8)
        writer.ue(bits - 8)
        writer.ue(4)
        writer.flag(true)
        writer.ue(0)
        writer.ue(0)
        writer.ue(0)
        writer.ue(1)
        writer.ue(0)
        writer.ue(0)
        writer.ue(2)
        writer.ue(0)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.bits(bits - 1, 4)
        writer.bits(bits - 1, 4)
        writer.ue(1)
        writer.ue(0)
        writer.flag(true)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.bits(5, 3)
        writer.flag(color.fullRange == true)
        writer.flag(true)
        writer.bits(color.primariesCode(), 8)
        writer.bits(color.transferCode(), 8)
        writer.bits(color.matrixCode(), 8)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.trailingBits()
        return writer.bytes()
    }

    private fun pictureParameterSet(): ByteArray {
        val writer = BitWriter()
        writer.ue(0)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.bits(0, 3)
        writer.flag(false)
        writer.flag(false)
        writer.ue(0)
        writer.ue(0)
        writer.se(0)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.se(0)
        writer.se(0)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(false)
        writer.flag(true)
        writer.flag(false)
        writer.flag(true)
        writer.flag(false)
        writer.flag(false)
        writer.ue(0)
        writer.flag(false)
        writer.flag(false)
        writer.trailingBits()
        return writer.bytes()
    }

    private fun slice(frame: YuvFrame, keyFrame: Boolean): ByteArray {
        val writer = BitWriter()
        writer.flag(true)
        if (keyFrame) {
            writer.flag(false)
        }
        writer.ue(0)
        writer.ue(2)
        if (!keyFrame) {
            writer.bits(frameNumber and 0xFF, 8)
            writer.flag(false)
            writer.ue(0)
            writer.ue(0)
        }
        writer.se(0)
        writer.bit(1)
        writer.alignZero()
        val partMode = CabacContext(184, sliceQp)
        val cabac = CabacEncoder(writer)
        val widthInCtbs = codedWidth / 16
        val heightInCtbs = codedHeight / 16
        for (ctbY in 0 until heightInCtbs) {
            for (ctbX in 0 until widthInCtbs) {
                cabac.encodeDecision(partMode, 1)
                cabac.encodeTerminate(1)
                writer.alignZero()
                for (y in 0 until 16) {
                    val row = (ctbY * 16 + y) * frame.width + ctbX * 16
                    for (x in 0 until 16) {
                        writer.bits(frame.luma[row + x], bits)
                    }
                }
                for (plane in listOf(frame.cb, frame.cr)) {
                    for (y in 0 until 8) {
                        val row = (ctbY * 8 + y) * frame.chromaWidth + ctbX * 8
                        for (x in 0 until 8) {
                            writer.bits(plane[row + x], bits)
                        }
                    }
                }
                cabac.reset()
                val last = ctbY == heightInCtbs - 1 && ctbX == widthInCtbs - 1
                cabac.encodeTerminate(if (last) 1 else 0)
            }
        }
        writer.alignZero()
        return writer.bytes()
    }
}

internal class SoftwareDecodedFrame(
    val originX: Int,
    val originY: Int,
    val width: Int,
    val height: Int,
    val frame: YuvFrame,
    val color: ColorDescription,
)

internal class SoftwareH264Decoder {
    private class SequenceParameterSet(
        val profileIdc: Int,
        val log2MaxFrameNum: Int,
        val pictureOrderCountType: Int,
        val log2MaxPictureOrderCountLsb: Int,
        val widthInMacroblocks: Int,
        val heightInMacroblocks: Int,
        val cropLeft: Int,
        val cropRight: Int,
        val cropTop: Int,
        val cropBottom: Int,
        val color: ColorDescription,
    )

    private class PictureParameterSet(
        val entropyCodingMode: Boolean,
        val bottomFieldPicOrderInFramePresent: Boolean,
        val deblockingFilterControlPresent: Boolean,
        val redundantPictureCountPresent: Boolean,
        val numberOfReferenceIndexes: Int,
    )

    private var sequenceParameterSet: SequenceParameterSet? = null
    private var pictureParameterSet: PictureParameterSet? = null
    private var reference: YuvFrame? = null

    fun configure(parameterSets: List<ByteArray>) {
        for (nalUnit in parameterSets) {
            handleParameterSet(nalUnit)
        }
    }

    fun decode(annexB: ByteArray): SoftwareDecodedFrame? {
        var decoded: SoftwareDecodedFrame? = null
        for (nalUnit in splitAnnexB(annexB)) {
            if (nalUnit.isEmpty()) {
                continue
            }
            val header = nalUnit[0].toInt() and 0xFF
            when (header and 0x1F) {
                7, 8 -> handleParameterSet(nalUnit)
                1, 5 -> decoded = decodeSlice(nalUnit, header)
                else -> Unit
            }
        }
        return decoded
    }

    private fun handleParameterSet(nalUnit: ByteArray) {
        if (nalUnit.isEmpty()) {
            return
        }
        when (nalUnit[0].toInt() and 0x1F) {
            7 -> sequenceParameterSet = parseSequenceParameterSet(BitReader(Rbsp.unescape(nalUnit, 1)))
            8 -> pictureParameterSet = parsePictureParameterSet(BitReader(Rbsp.unescape(nalUnit, 1)))
        }
    }

    private fun parseSequenceParameterSet(reader: BitReader): SequenceParameterSet {
        val profileIdc = reader.bits(8)
        reader.skip(16)
        reader.ue()
        if (profileIdc in listOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)) {
            val chromaFormat = reader.ue()
            if (chromaFormat != 1) {
                throw IllegalStateException("Only 4:2:0 is supported")
            }
            if (reader.ue() != 0 || reader.ue() != 0) {
                throw IllegalStateException("Only 8-bit is supported")
            }
            reader.skip(1)
            if (reader.flag()) {
                throw IllegalStateException("Scaling matrices are not supported")
            }
        }
        val log2MaxFrameNum = reader.ue() + 4
        val pictureOrderCountType = reader.ue()
        var log2MaxPictureOrderCountLsb = 0
        if (pictureOrderCountType == 0) {
            log2MaxPictureOrderCountLsb = reader.ue() + 4
        } else if (pictureOrderCountType == 1) {
            throw IllegalStateException("Picture order count type 1 is not supported")
        }
        reader.ue()
        reader.skip(1)
        val widthInMacroblocks = reader.ue() + 1
        val heightInMacroblocks = reader.ue() + 1
        if (!reader.flag()) {
            throw IllegalStateException("Interlaced video is not supported")
        }
        reader.skip(1)
        var cropLeft = 0
        var cropRight = 0
        var cropTop = 0
        var cropBottom = 0
        if (reader.flag()) {
            cropLeft = reader.ue()
            cropRight = reader.ue()
            cropTop = reader.ue()
            cropBottom = reader.ue()
        }
        var color = ColorDescription(fullRange = false)
        if (reader.flag()) {
            if (reader.flag()) {
                if (reader.bits(8) == 255) {
                    reader.skip(32)
                }
            }
            if (reader.flag()) {
                reader.skip(1)
            }
            if (reader.flag()) {
                reader.skip(3)
                val fullRange = reader.flag()
                color = if (reader.flag()) {
                    ColorDescription.fromCodes(reader.bits(8), reader.bits(8), reader.bits(8), fullRange)
                } else {
                    ColorDescription(fullRange = fullRange)
                }
            }
        }
        return SequenceParameterSet(
            profileIdc,
            log2MaxFrameNum,
            pictureOrderCountType,
            log2MaxPictureOrderCountLsb,
            widthInMacroblocks,
            heightInMacroblocks,
            cropLeft,
            cropRight,
            cropTop,
            cropBottom,
            color,
        )
    }

    private fun parsePictureParameterSet(reader: BitReader): PictureParameterSet {
        reader.ue()
        reader.ue()
        val entropyCodingMode = reader.flag()
        val bottomFieldPicOrderInFramePresent = reader.flag()
        if (reader.ue() != 0) {
            throw IllegalStateException("Slice groups are not supported")
        }
        val numberOfReferenceIndexes = reader.ue() + 1
        reader.ue()
        reader.skip(3)
        reader.se()
        reader.se()
        reader.se()
        val deblockingFilterControlPresent = reader.flag()
        reader.skip(1)
        val redundantPictureCountPresent = reader.flag()
        return PictureParameterSet(
            entropyCodingMode,
            bottomFieldPicOrderInFramePresent,
            deblockingFilterControlPresent,
            redundantPictureCountPresent,
            numberOfReferenceIndexes,
        )
    }

    private fun decodeSlice(nalUnit: ByteArray, header: Int): SoftwareDecodedFrame {
        val sequenceParameterSet = sequenceParameterSet ?: throw IllegalStateException("No sequence parameter set")
        val pictureParameterSet = pictureParameterSet ?: throw IllegalStateException("No picture parameter set")
        if (pictureParameterSet.entropyCodingMode) {
            throw IllegalStateException("CABAC is not supported")
        }
        val idr = (header and 0x1F) == 5
        val nalRefIdc = (header shr 5) and 3
        val reader = BitReader(Rbsp.unescape(nalUnit, 1))
        if (reader.ue() != 0) {
            throw IllegalStateException("Only one slice per picture is supported")
        }
        val sliceType = reader.ue() % 5
        reader.ue()
        reader.skip(sequenceParameterSet.log2MaxFrameNum)
        if (idr) {
            reader.ue()
        }
        if (sequenceParameterSet.pictureOrderCountType == 0) {
            reader.skip(sequenceParameterSet.log2MaxPictureOrderCountLsb)
            if (pictureParameterSet.bottomFieldPicOrderInFramePresent) {
                reader.se()
            }
        }
        if (pictureParameterSet.redundantPictureCountPresent) {
            reader.ue()
        }
        if (sliceType == 0) {
            if (reader.flag()) {
                reader.ue()
            }
            if (reader.flag()) {
                throw IllegalStateException("Reference list modification is not supported")
            }
        } else if (sliceType != 2) {
            throw IllegalStateException("Slice type $sliceType is not supported")
        }
        if (nalRefIdc != 0) {
            if (idr) {
                reader.skip(2)
            } else if (reader.flag()) {
                throw IllegalStateException("Adaptive reference picture marking is not supported")
            }
        }
        reader.se()
        if (pictureParameterSet.deblockingFilterControlPresent && reader.ue() != 1) {
            reader.se()
            reader.se()
        }
        val frame = YuvFrame(sequenceParameterSet.widthInMacroblocks * 16, sequenceParameterSet.heightInMacroblocks * 16, 8)
        val previous = reference
        val macroblocks = sequenceParameterSet.widthInMacroblocks * sequenceParameterSet.heightInMacroblocks
        var address = 0
        while (address < macroblocks) {
            if (sliceType == 0) {
                val skipped = reader.ue()
                repeat(skipped) {
                    if (address < macroblocks) {
                        copyMacroblock(previous, frame, address, sequenceParameterSet.widthInMacroblocks)
                        address += 1
                    }
                }
                if (address >= macroblocks || !reader.moreRbspData()) {
                    break
                }
            }
            val type = reader.ue()
            if ((sliceType == 2 && type != 25) || (sliceType == 0 && type != 30)) {
                throw IllegalStateException("Macroblock type $type is not supported")
            }
            reader.alignToByte()
            readPcmMacroblock(reader, frame, address, sequenceParameterSet.widthInMacroblocks)
            address += 1
        }
        reference = frame
        val width = frame.width - 2 * (sequenceParameterSet.cropLeft + sequenceParameterSet.cropRight)
        val height = frame.height - 2 * (sequenceParameterSet.cropTop + sequenceParameterSet.cropBottom)
        return SoftwareDecodedFrame(
            2 * sequenceParameterSet.cropLeft,
            2 * sequenceParameterSet.cropTop,
            width,
            height,
            frame,
            sequenceParameterSet.color,
        )
    }

    private fun copyMacroblock(source: YuvFrame?, frame: YuvFrame, address: Int, widthInMacroblocks: Int) {
        source ?: return
        val macroblockX = address % widthInMacroblocks
        val macroblockY = address / widthInMacroblocks
        for (y in 0 until 16) {
            val row = (macroblockY * 16 + y) * frame.width + macroblockX * 16
            System.arraycopy(source.luma, row, frame.luma, row, 16)
        }
        for (y in 0 until 8) {
            val row = (macroblockY * 8 + y) * frame.chromaWidth + macroblockX * 8
            System.arraycopy(source.cb, row, frame.cb, row, 8)
            System.arraycopy(source.cr, row, frame.cr, row, 8)
        }
    }

    private fun readPcmMacroblock(reader: BitReader, frame: YuvFrame, address: Int, widthInMacroblocks: Int) {
        val macroblockX = address % widthInMacroblocks
        val macroblockY = address / widthInMacroblocks
        for (y in 0 until 16) {
            val row = (macroblockY * 16 + y) * frame.width + macroblockX * 16
            for (x in 0 until 16) {
                frame.luma[row + x] = reader.byteAt()
            }
        }
        for (plane in listOf(frame.cb, frame.cr)) {
            for (y in 0 until 8) {
                val row = (macroblockY * 8 + y) * frame.chromaWidth + macroblockX * 8
                for (x in 0 until 8) {
                    plane[row + x] = reader.byteAt()
                }
            }
        }
    }

    companion object {
        fun splitAnnexB(data: ByteArray): List<ByteArray> {
            val nalUnits = mutableListOf<ByteArray>()
            var start = -1
            var index = 0
            while (index + 2 < data.size) {
                if (data[index].toInt() == 0 && data[index + 1].toInt() == 0 && data[index + 2].toInt() == 1) {
                    if (start >= 0) {
                        var end = index
                        while (end > start && data[end - 1].toInt() == 0) {
                            end -= 1
                        }
                        nalUnits.add(data.copyOfRange(start, end))
                    }
                    start = index + 3
                    index += 3
                } else {
                    index += 1
                }
            }
            if (start >= 0 && start < data.size) {
                nalUnits.add(data.copyOfRange(start, data.size))
            }
            return nalUnits
        }

        fun parameterSets(format: MediaFormat): List<ByteArray> {
            val nalUnits = mutableListOf<ByteArray>()
            for (key in listOf("csd-0", "csd-1")) {
                val bytes = readByteBufferBytes(format, key) ?: continue
                nalUnits.addAll(splitAnnexB(bytes))
            }
            return nalUnits
        }
    }
}

internal object SoftwareVideoCodecs {
    fun isAvailable(mimeType: String): Boolean {
        return com.moblin.android.platform.video.SoftwareRendering.isActive &&
            (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC || mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)
    }

    fun isDecoderAvailable(mimeType: String): Boolean {
        return com.moblin.android.platform.video.SoftwareRendering.isActive && mimeType == MediaFormat.MIMETYPE_VIDEO_AVC
    }

    fun h264ProfileIdc(profileLevel: String?): Int {
        return when {
            profileLevel == null -> 100
            profileLevel.contains("Baseline") -> 66
            profileLevel.contains("Main") -> 77
            else -> 100
        }
    }
}
