package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnit
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnit
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType

val nalUnitStartCode: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x01)

data class NalUnitInfo(
    val startCodeOffset: Int,
    val startCodeLength: Int,
    val dataLength: Int,
) {
    fun dataOffset(): Int {
        return startCodeOffset + startCodeLength
    }
}

fun getNalUnits(data: ByteArray): List<NalUnitInfo> {
    val nalUnits = mutableListOf<NalUnitInfo>()
    parseNalUnits(data) { startCodeIndex, startCodeLength, dataLength ->
        nalUnits.add(
            NalUnitInfo(
                startCodeOffset = startCodeIndex,
                startCodeLength = startCodeLength,
                dataLength = dataLength,
            ),
        )
    }
    return nalUnits
}

fun addNalUnitStartCodes(data: ByteArray): ByteArray {
    var index = 0
    while (index + 3 < data.size) {
        val length = readFourBytesBe(data, index)
        System.arraycopy(nalUnitStartCode, 0, data, index, 4)
        index += length + 4
    }
    return data
}

fun removeNalUnitStartCodes(data: ByteArray, nalUnits: List<NalUnitInfo>): ByteArray {
    var numberOfThreeBytesStartCodes = nalUnits.count { it.startCodeLength != 4 }
    if (numberOfThreeBytesStartCodes == 0) {
        for (nalUnit in nalUnits) {
            writeFourBytesBe(data, nalUnit.startCodeOffset, nalUnit.dataLength)
        }
        return data
    } else {
        val source = data.copyOf()
        val result = ByteArray(data.size + numberOfThreeBytesStartCodes)
        System.arraycopy(source, 0, result, 0, source.size)
        var endOffset = result.size
        for (nalUnit in nalUnits) {
            val dataOffset = nalUnit.dataOffset()
            if (numberOfThreeBytesStartCodes > 0) {
                System.arraycopy(source, dataOffset, result, endOffset - nalUnit.dataLength, nalUnit.dataLength)
            }
            endOffset -= nalUnit.dataLength
            writeFourBytesBe(result, endOffset - 4, nalUnit.dataLength)
            endOffset -= 4
            if (nalUnit.startCodeLength != 4) {
                numberOfThreeBytesStartCodes -= 1
            }
        }
        return result
    }
}

interface NalUnit

fun readH264NalUnits(
    data: ByteArray,
    nalUnits: List<NalUnitInfo>,
    filter: List<AvcNalUnitType>,
): List<AvcNalUnit> {
    return readNalUnits<AvcNalUnit>(
        data,
        nalUnits,
        { byte ->
            filter.contains(
                AvcNalUnitType.fromRawValue((byte.toInt() and 0x1F).toUByte()) ?: AvcNalUnitType.unspec,
            )
        },
        { nalUnitData, offset -> AvcNalUnit.create(nalUnitData, offset) },
    )
}

fun readH265NalUnits(
    data: ByteArray,
    nalUnits: List<NalUnitInfo>,
    filter: List<HevcNalUnitType>,
): List<HevcNalUnit> {
    return readNalUnits<HevcNalUnit>(
        data,
        nalUnits,
        { byte ->
            filter.contains(
                HevcNalUnitType.fromRawValue((((byte.toInt() and 0x7E) shr 1)).toUByte()) ?: HevcNalUnitType.unspec,
            )
        },
        { nalUnitData, offset -> HevcNalUnit(nalUnitData, offset) },
    )
}

private fun <TNalUnit : NalUnit> readNalUnits(
    data: ByteArray,
    nalUnits: List<NalUnitInfo>,
    filter: (Byte) -> Boolean,
    create: (ByteArray, Int) -> TNalUnit?,
): List<TNalUnit> {
    val units = mutableListOf<TNalUnit>()
    for (nalUnit in nalUnits) {
        val dataOffset = nalUnit.dataOffset()
        if (filter(data[dataOffset])) {
            val unit = create(data.copyOfRange(dataOffset, dataOffset + nalUnit.dataLength), 0)
            if (unit != null) {
                units.add(unit)
            }
        }
    }
    return units
}

private fun parseNalUnits(data: ByteArray, onNalUnit: (Int, Int, Int) -> Unit) {
    var lastIndexOf = data.size - 1
    var index = lastIndexOf - 2
    while (index >= 0) {
        if ((data[index].toInt() and 0xFF) > 1) {
            index -= 3
            continue
        }
        if (!(data[index + 2].toInt() == 1 && data[index + 1].toInt() == 0 && data[index].toInt() == 0)) {
            index -= 1
            continue
        }
        val startCodeLength = if (index - 1 >= 0 && data[index - 1].toInt() == 0) 4 else 3
        val dataLength = lastIndexOf - index - 2
        if (dataLength <= 0) {
            index -= 1
            continue
        }
        val startCodeIndex = index + 3 - startCodeLength
        onNalUnit(startCodeIndex, startCodeLength, dataLength)
        lastIndexOf = startCodeIndex - 1
        index = lastIndexOf
    }
}

private fun readFourBytesBe(data: ByteArray, offset: Int): Int {
    return ((data[offset].toInt() and 0xFF) shl 24) or
        ((data[offset + 1].toInt() and 0xFF) shl 16) or
        ((data[offset + 2].toInt() and 0xFF) shl 8) or
        (data[offset + 3].toInt() and 0xFF)
}

private fun writeFourBytesBe(data: ByteArray, offset: Int, value: Int) {
    data[offset] = ((value ushr 24) and 0xFF).toByte()
    data[offset + 1] = ((value ushr 16) and 0xFF).toByte()
    data[offset + 2] = ((value ushr 8) and 0xFF).toByte()
    data[offset + 3] = (value and 0xFF).toByte()
}
