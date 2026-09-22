package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter

private enum class TableIdentifier(val rawValue: UByte) {
    programAssociation(0u),
    programMapping(2u),
}

open class MpegTsProgramSpecificInformation {
    private var pointerField: UByte = 0u
    private var pointerSkippedBytes: ByteArray = ByteArray(0)
    var tableId: UByte = TableIdentifier.programAssociation.rawValue
    private var privateBit = false
    private var tableIdExtension: UShort = 1u
    private var versionNumber: UByte = 0u
    private var currentNextIndicator = true
    private var sectionNumber: UByte = 0u
    private var lastSectionNumber: UByte = 0u

    constructor()

    constructor(data: ByteArray) {
        val reader = ByteReader(data)
        pointerField = reader.readUInt8()
        pointerSkippedBytes = reader.readBytes(pointerField.toInt())
        tableId = reader.readUInt8()
        val value = reader.readUInt16()
        val sectionSyntaxIndicator = (value.toInt() and 0x8000) == 0x8000
        privateBit = (value.toInt() and 0x4000) == 0x4000
        var sectionLength = (value and 0x3FFu).toInt()
        if (sectionSyntaxIndicator) {
            tableIdExtension = reader.readUInt16()
            val value2 = reader.readUInt8()
            currentNextIndicator = (value2.toInt() and 0x01) == 0x01
            versionNumber = ((value2.toInt() and 0b0011_1110) shr 1).toUByte()
            sectionNumber = reader.readUInt8()
            lastSectionNumber = reader.readUInt8()
            sectionLength -= 5
        }
        decodeSectionData(reader.readBytes(sectionLength - 4))
    }

    protected open fun encodeSectionData(): ByteArray {
        return ByteArray(0)
    }

    protected open fun decodeSectionData(data: ByteArray) {}

    fun packet(packetId: UShort): MpegTsPacket {
        val packet = MpegTsPacket(id = packetId)
        packet.payloadUnitStartIndicator = true
        val encoded = encode()
        packet.payload = encoded + ByteArray(184 - encoded.size) { 0xFF.toByte() }
        return packet
    }

    private fun encode(): ByteArray {
        val sectionData = encodeSectionData()
        val sectionLength = (sectionData.size + 9).toUShort()
        val sectionSyntaxIndicator = sectionData.isNotEmpty()
        val writer = ByteWriter()
        writer.writeUInt8(tableId)
        var value: UShort = 0u
        if (sectionSyntaxIndicator) {
            value = value or 0x8000u
        }
        if (privateBit) {
            value = value or 0x4000u
        }
        value = value or ((MpegTsProgramSpecificInformation.reservedBits.toInt() shl 12).toUShort())
        value = value or sectionLength
        writer.writeUInt16(value)
        writer.writeUInt16(tableIdExtension)
        var value2: UByte = 0u
        value2 = value2 or ((MpegTsProgramSpecificInformation.reservedBits.toInt() shl 6).toUByte())
        value2 = value2 or ((versionNumber.toInt() shl 1).toUByte())
        if (currentNextIndicator) {
            value2 = value2 or 1u
        }
        writer.writeUInt8(value2)
        writer.writeUInt8(sectionNumber)
        writer.writeUInt8(lastSectionNumber)
        writer.writeBytes(sectionData)
        writer.writeUInt32(Crc32.mpeg2.calculate(writer.data))
        return byteArrayOf(pointerField.toByte()) + pointerSkippedBytes + writer.data
    }

    companion object {
        private val reservedBits: UByte = 0x03u
    }
}

class MpegTsProgramAssociation : MpegTsProgramSpecificInformation {
    var programs: MutableMap<UShort, UShort> = mutableMapOf()

    override fun encodeSectionData(): ByteArray {
        val writer = ByteWriter()
        for ((programNumber, programId) in programs) {
            writer.writeUInt16(programNumber)
            writer.writeUInt16(programId or 0xE000u)
        }
        return writer.data
    }

    override fun decodeSectionData(data: ByteArray) {
        val reader = ByteReader(data)
        while (reader.bytesAvailable > 0) {
            val programNumber = reader.readUInt16()
            val programId = reader.readUInt16() and 0x1FFFu
            programs[programNumber] = programId
        }
    }
}

class MpegTsProgramMapping : MpegTsProgramSpecificInformation {
    var programClockReferencePacketId: UShort = 0u
    var elementaryStreamSpecificDatas: MutableList<ElementaryStreamSpecificData> = mutableListOf()

    constructor() : super() {
        tableId = TableIdentifier.programMapping.rawValue
    }

    constructor(data: ByteArray) : super(data)

    override fun encodeSectionData(): ByteArray {
        var encoded = ByteArray(0)
        elementaryStreamSpecificDatas.sortBy { it.elementaryPacketId }
        for (elementaryStreamSpecificData in elementaryStreamSpecificDatas) {
            encoded += elementaryStreamSpecificData.encode()
        }
        val writer = ByteWriter()
        writer.writeUInt16(programClockReferencePacketId or 0xE000u)
        writer.writeUInt16(0xF000u)
        writer.writeBytes(encoded)
        return writer.data
    }

    override fun decodeSectionData(data: ByteArray) {
        val reader = ByteReader(data)
        programClockReferencePacketId = reader.readUInt16() and 0x1FFFu
        val programInfoLength = (reader.readUInt16() and 0x03FFu).toInt()
        reader.skipBytes(programInfoLength)
        while (reader.bytesAvailable > 0) {
            elementaryStreamSpecificDatas.add(ElementaryStreamSpecificData(reader))
        }
    }
}
