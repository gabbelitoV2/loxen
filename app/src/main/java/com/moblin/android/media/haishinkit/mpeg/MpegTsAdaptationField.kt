package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter

class MpegTsAdaptationField {
    var randomAccessIndicator = false
    var programClockReference: ByteArray? = null
    var stuffingBytes: ByteArray? = null

    constructor()

    constructor(reader: ByteReader, length: UByte) {
        val startPosition = reader.position.toInt()
        val byte = reader.readUInt8().toInt()
        randomAccessIndicator = (byte and 0x40) == 0x40
        val hasProgramClockReference = (byte and 0x10) == 0x10
        if (hasProgramClockReference) {
            programClockReference = reader.readBytes(6)
        }
        val stuffingCount = (startPosition + length.toInt()) - reader.position.toInt()
        if (stuffingCount > 0) {
            reader.readBytes(stuffingCount)
        }
    }

    fun calcLength(): UByte {
        var length = fixedSectionSize.toInt()
        programClockReference?.let { length += it.size }
        stuffingBytes?.let { length += it.size }
        return length.toUByte()
    }

    fun setStuffing(size: Int) {
        stuffingBytes = ByteArray(size) { 0xFF.toByte() }
    }

    fun encode(): ByteArray {
        var flags = 0
        if (randomAccessIndicator) {
            flags = flags or 0x40
        }
        if (programClockReference != null) {
            flags = flags or 0x10
        }
        val writer = ByteWriter()
        writer.writeUInt8((calcLength().toInt() - 1).toUByte())
        writer.writeUInt8(flags.toUByte())
        programClockReference?.let { writer.writeBytes(it) }
        stuffingBytes?.let { writer.writeBytes(it) }
        return writer.data
    }

    companion object {
        val fixedSectionSize: UByte = 2u
    }
}
