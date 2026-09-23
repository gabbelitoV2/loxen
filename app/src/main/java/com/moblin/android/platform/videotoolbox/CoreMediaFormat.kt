package com.moblin.android.platform.videotoolbox

import android.media.MediaFormat
import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

const val kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms = "SampleDescriptionExtensionAtoms"

private val atomKeys = listOf("avcC", "hvcC")

private val comparedIntegerKeys = listOf(
    MediaFormat.KEY_WIDTH,
    MediaFormat.KEY_HEIGHT,
    MediaFormat.KEY_SAMPLE_RATE,
    MediaFormat.KEY_CHANNEL_COUNT,
)

private val comparedByteBufferKeys = listOf("avcC", "hvcC", "csd-0", "csd-1", "csd-2")

private val extensionIntegerKeys = listOf(
    MediaFormat.KEY_COLOR_STANDARD,
    MediaFormat.KEY_COLOR_TRANSFER,
    MediaFormat.KEY_COLOR_RANGE,
)

private val avcHighProfileIdcs = setOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)

private val avcRecordExtensionProfileIdcs = setOf(100, 110, 122, 144)

fun CMFormatDescriptionGetExtension(formatDescription: MediaFormat, extensionKey: String): Any? {
    if (extensionKey == kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms) {
        val atoms = mutableMapOf<String, ByteArray>()
        for (key in atomKeys) {
            val bytes = readByteBufferBytes(formatDescription, key) ?: continue
            atoms[key] = bytes
        }
        return if (atoms.isEmpty()) null else atoms
    }
    return CMFormatDescriptionGetExtensions(formatDescription)?.get(extensionKey)
}

fun CMFormatDescriptionGetExtensions(formatDescription: MediaFormat): Map<String, Any>? {
    val extensions = mutableMapOf<String, Any>()
    for (key in extensionIntegerKeys) {
        val value = readInteger(formatDescription, key) ?: continue
        extensions[key] = value
    }
    val atoms = CMFormatDescriptionGetExtension(
        formatDescription,
        kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms,
    )
    if (atoms != null) {
        extensions[kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms] = atoms
    }
    return if (extensions.isEmpty()) null else extensions
}

fun CMFormatDescriptionEqual(a: MediaFormat?, b: MediaFormat?): Boolean {
    if (a === b) {
        return true
    }
    if (a == null || b == null) {
        return false
    }
    if (readString(a, MediaFormat.KEY_MIME) != readString(b, MediaFormat.KEY_MIME)) {
        return false
    }
    for (key in comparedIntegerKeys) {
        if (readInteger(a, key) != readInteger(b, key)) {
            return false
        }
    }
    for (key in comparedByteBufferKeys) {
        if (!(readByteBufferBytes(a, key) contentEquals readByteBufferBytes(b, key))) {
            return false
        }
    }
    return true
}

fun makeAvcDecoderConfigurationRecord(sps: ByteArray, pps: ByteArray): ByteArray {
    val output = ByteArrayOutputStream()
    output.write(1)
    output.write(byteAt(sps, 1))
    output.write(byteAt(sps, 2))
    output.write(byteAt(sps, 3))
    output.write(0xFF)
    output.write(0xE1)
    writeUInt16(output, sps.size)
    output.write(sps, 0, sps.size)
    output.write(1)
    writeUInt16(output, pps.size)
    output.write(pps, 0, pps.size)
    if (byteAt(sps, 1) in avcRecordExtensionProfileIdcs) {
        val chroma = readAvcChromaFormat(sps)
        output.write(0xFC or (chroma[0] and 0x03))
        output.write(0xF8 or (chroma[1] and 0x07))
        output.write(0xF8 or (chroma[2] and 0x07))
        output.write(0)
    }
    return output.toByteArray()
}

fun makeHevcDecoderConfigurationRecord(vps: ByteArray, sps: ByteArray, pps: ByteArray): ByteArray? {
    return try {
        val reader = NalUnitReader(sps)
        reader.skipBits(16)
        reader.skipBits(4)
        val maxSubLayersMinus1 = reader.readBits(3).toInt()
        val temporalIdNesting = if (reader.readBit()) 1 else 0
        val generalProfileTierLevel = ByteArray(12) { reader.readBits(8).toByte() }
        val subLayerProfilePresent = BooleanArray(maxSubLayersMinus1)
        val subLayerLevelPresent = BooleanArray(maxSubLayersMinus1)
        for (i in 0 until maxSubLayersMinus1) {
            subLayerProfilePresent[i] = reader.readBit()
            subLayerLevelPresent[i] = reader.readBit()
        }
        if (maxSubLayersMinus1 > 0) {
            for (i in maxSubLayersMinus1 until 8) {
                reader.skipBits(2)
            }
        }
        for (i in 0 until maxSubLayersMinus1) {
            if (subLayerProfilePresent[i]) {
                reader.skipBits(88)
            }
            if (subLayerLevelPresent[i]) {
                reader.skipBits(8)
            }
        }
        reader.readUEG()
        val chromaFormatIdc = reader.readUEG()
        if (chromaFormatIdc == 3) {
            reader.skipBits(1)
        }
        reader.readUEG()
        reader.readUEG()
        if (reader.readBit()) {
            repeat(4) {
                reader.readUEG()
            }
        }
        val bitDepthLumaMinus8 = reader.readUEG()
        val bitDepthChromaMinus8 = reader.readUEG()
        val output = ByteArrayOutputStream()
        output.write(1)
        output.write(generalProfileTierLevel, 0, generalProfileTierLevel.size)
        output.write(0xF0)
        output.write(0x00)
        output.write(0xFC)
        output.write(0xFC or (chromaFormatIdc and 0x03))
        output.write(0xF8 or (bitDepthLumaMinus8 and 0x07))
        output.write(0xF8 or (bitDepthChromaMinus8 and 0x07))
        output.write(0)
        output.write(0)
        output.write((((maxSubLayersMinus1 + 1) and 0x07) shl 3) or (temporalIdNesting shl 2) or 0x03)
        output.write(3)
        for ((type, nalUnit) in listOf(32 to vps, 33 to sps, 34 to pps)) {
            output.write(0x80 or type)
            writeUInt16(output, 1)
            writeUInt16(output, nalUnit.size)
            output.write(nalUnit, 0, nalUnit.size)
        }
        output.toByteArray()
    } catch (e: Exception) {
        null
    }
}

fun makeVideoFormatDescription(
    mimeType: String,
    width: Int,
    height: Int,
    vps: ByteArray?,
    sps: ByteArray,
    pps: ByteArray,
): MediaFormat {
    val format = MediaFormat.createVideoFormat(mimeType, width, height)
    if (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
        format.setByteBuffer("csd-0", ByteBuffer.wrap(makeAnnexB(listOfNotNull(vps, sps, pps))))
        if (vps != null) {
            val record = makeHevcDecoderConfigurationRecord(vps, sps, pps)
            if (record != null) {
                format.setByteBuffer("hvcC", ByteBuffer.wrap(record))
            }
        }
    } else {
        format.setByteBuffer("csd-0", ByteBuffer.wrap(makeAnnexB(listOf(sps))))
        format.setByteBuffer("csd-1", ByteBuffer.wrap(makeAnnexB(listOf(pps))))
        format.setByteBuffer("avcC", ByteBuffer.wrap(makeAvcDecoderConfigurationRecord(sps, pps)))
    }
    return format
}

fun NalUnitReader.readUEG(): Int {
    var leadingZeroBits = 0
    while (!readBit()) {
        leadingZeroBits += 1
        if (leadingZeroBits > 31) {
            throw IllegalStateException("Invalid exp-Golomb code")
        }
    }
    if (leadingZeroBits == 0) {
        return 0
    }
    val suffix = readBitsU32(leadingZeroBits).toLong()
    return ((1L shl leadingZeroBits) - 1 + suffix).toInt()
}

internal fun readByteBufferBytes(format: MediaFormat, key: String): ByteArray? {
    if (!format.containsKey(key)) {
        return null
    }
    val buffer = try {
        format.getByteBuffer(key)
    } catch (e: Exception) {
        null
    } ?: return null
    val duplicate = buffer.duplicate()
    if (!duplicate.hasRemaining()) {
        duplicate.rewind()
    }
    val bytes = ByteArray(duplicate.remaining())
    duplicate.get(bytes)
    return bytes
}

internal fun makeAnnexB(nalUnits: List<ByteArray>): ByteArray {
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

internal fun hexPrefix(bytes: ByteArray, count: Int): String {
    return bytes.take(count).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}

private fun readInteger(format: MediaFormat, key: String): Int? {
    if (!format.containsKey(key)) {
        return null
    }
    return try {
        format.getInteger(key)
    } catch (e: Exception) {
        null
    }
}

private fun readString(format: MediaFormat, key: String): String? {
    if (!format.containsKey(key)) {
        return null
    }
    return try {
        format.getString(key)
    } catch (e: Exception) {
        null
    }
}

private fun byteAt(data: ByteArray, index: Int): Int {
    return if (index < data.size) data[index].toInt() and 0xFF else 0
}

private fun writeUInt16(output: ByteArrayOutputStream, value: Int) {
    output.write((value ushr 8) and 0xFF)
    output.write(value and 0xFF)
}

private fun readAvcChromaFormat(sps: ByteArray): IntArray {
    return try {
        val reader = NalUnitReader(sps)
        reader.skipBits(8)
        val profileIdc = reader.readBits(8).toInt()
        reader.skipBits(16)
        reader.readUEG()
        if (profileIdc in avcHighProfileIdcs) {
            val chromaFormatIdc = reader.readUEG()
            if (chromaFormatIdc == 3) {
                reader.skipBits(1)
            }
            val bitDepthLumaMinus8 = reader.readUEG()
            val bitDepthChromaMinus8 = reader.readUEG()
            intArrayOf(chromaFormatIdc, bitDepthLumaMinus8, bitDepthChromaMinus8)
        } else {
            intArrayOf(1, 0, 0)
        }
    } catch (e: Exception) {
        intArrayOf(1, 0, 0)
    }
}
