package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import kotlin.math.roundToLong

class OptionalHeader {

    companion object {
        const val fixedSectionSize: Int = 3
        const val invalidTimestamp: Long = Long.MIN_VALUE
    }

    var markerBits: UByte = 2u
    var scramblingControl: UByte = 0u
    var priority: Boolean = false
    var dataAlignmentIndicator: Boolean = false
    var copyright: Boolean = false
    var originalOrCopy: Boolean = false
    var ptsDtsIndicator: UByte = 0u
    var esCRFlag: Boolean = false
    var esRateFlag: Boolean = false
    var dsmTrickModeFlag: Boolean = false
    var additionalCopyInfoFlag: Boolean = false
    var crcFlag: Boolean = false
    var extentionFlag: Boolean = false
    var pesHeaderLength: UByte = 0u
    var optionalFields: ByteArray = ByteArray(0)
    var stuffingBytes: ByteArray = ByteArray(0)

    constructor()

    constructor(data: ByteArray) {
        val reader = ByteReader(data)
        val bytes = reader.readBytes(fixedSectionSize)
        val byte0 = bytes[0].toInt() and 0xFF
        val byte1 = bytes[1].toInt() and 0xFF
        markerBits = ((byte0 and 0b1100_0000) shr 6).toUByte()
        scramblingControl = ((byte0 and 0b0011_0000) shr 4).toUByte()
        priority = (byte0 and 0b0000_1000) == 0b0000_1000
        dataAlignmentIndicator = (byte0 and 0b0000_0100) == 0b0000_0100
        copyright = (byte0 and 0b0000_0010) == 0b0000_0010
        originalOrCopy = (byte0 and 0b0000_0001) == 0b0000_0001
        ptsDtsIndicator = ((byte1 and 0b1100_0000) shr 6).toUByte()
        esCRFlag = (byte1 and 0b0010_0000) == 0b0010_0000
        esRateFlag = (byte1 and 0b0001_0000) == 0b0001_0000
        dsmTrickModeFlag = (byte1 and 0b0000_1000) == 0b0000_1000
        additionalCopyInfoFlag = (byte1 and 0b0000_0100) == 0b0000_0100
        crcFlag = (byte1 and 0b0000_0010) == 0b0000_0010
        extentionFlag = (byte1 and 0b0000_0001) == 0b0000_0001
        pesHeaderLength = bytes[2].toUByte()
        optionalFields = reader.readBytes(pesHeaderLength.toInt())
    }

    fun setTimestamp(presentationTimeStamp: Long, decodeTimeStamp: Long) {
        if (presentationTimeStamp != invalidTimestamp) {
            ptsDtsIndicator = (ptsDtsIndicator.toInt() or 0x02).toUByte()
        }
        if (decodeTimeStamp != invalidTimestamp) {
            ptsDtsIndicator = (ptsDtsIndicator.toInt() or 0x01).toUByte()
        }
        if ((ptsDtsIndicator.toInt() and 0x02) == 0x02) {
            val value = (presentationTimeStamp / 1_000_000.0 * TSTimestamp.resolution).roundToLong()
            optionalFields += TSTimestamp.encode(value, (ptsDtsIndicator.toInt() shl 4).toUByte())
        }
        if ((ptsDtsIndicator.toInt() and 0x01) == 0x01) {
            val value = (decodeTimeStamp / 1_000_000.0 * TSTimestamp.resolution).roundToLong()
            optionalFields += TSTimestamp.encode(value, (0x01 shl 4).toUByte())
        }
        pesHeaderLength = optionalFields.size.toUByte()
    }

    fun encode(): ByteArray {
        var byte0 = 0
        byte0 = byte0 or (markerBits.toInt() shl 6)
        byte0 = byte0 or (scramblingControl.toInt() shl 4)
        byte0 = byte0 or (if (priority) 1 shl 3 else 0)
        byte0 = byte0 or (if (dataAlignmentIndicator) 1 shl 2 else 0)
        byte0 = byte0 or (if (copyright) 1 shl 1 else 0)
        byte0 = byte0 or (if (originalOrCopy) 1 else 0)
        var byte1 = 0
        byte1 = byte1 or (ptsDtsIndicator.toInt() shl 6)
        byte1 = byte1 or (if (esCRFlag) 1 shl 5 else 0)
        byte1 = byte1 or (if (esRateFlag) 1 shl 4 else 0)
        byte1 = byte1 or (if (dsmTrickModeFlag) 1 shl 3 else 0)
        byte1 = byte1 or (if (additionalCopyInfoFlag) 1 shl 2 else 0)
        byte1 = byte1 or (if (crcFlag) 1 shl 1 else 0)
        byte1 = byte1 or (if (extentionFlag) 1 else 0)
        val writer = ByteWriter()
        writer.writeBytes(byteArrayOf(byte0.toByte(), byte1.toByte()))
        writer.writeUInt8(pesHeaderLength)
        writer.writeBytes(optionalFields)
        writer.writeBytes(stuffingBytes)
        return writer.data
    }

    fun getPresentationTimeStamp(): Long {
        if ((ptsDtsIndicator.toInt() and 0x02) != 0x02) {
            return invalidTimestamp
        }
        val value = TSTimestamp.decode(optionalFields, 0) ?: return invalidTimestamp
        return value * 1_000_000 / TSTimestamp.resolution.toLong()
    }

    fun getDecodeTimeStamp(): Long {
        if ((ptsDtsIndicator.toInt() and 0x01) != 0x01) {
            return invalidTimestamp
        }
        val value = TSTimestamp.decode(optionalFields, TSTimestamp.dataSize) ?: return invalidTimestamp
        return value * 1_000_000 / TSTimestamp.resolution.toLong()
    }
}

class MpegTsPacketizedElementaryStream {

    companion object {
        private const val untilPacketLengthSize: Int = 6
        private val startCode = byteArrayOf(0x00, 0x00, 0x01)
    }

    private val streamId: UByte
    private val packetLength: UShort
    var optionalHeader = OptionalHeader()
    var data = ByteArray(0)

    constructor(
        streamId: UByte,
        presentationTimeStamp: Long,
        decodeTimeStamp: Long,
        data: ByteArray
    ) {
        this.data = data
        optionalHeader.dataAlignmentIndicator = true
        optionalHeader.setTimestamp(presentationTimeStamp, decodeTimeStamp)
        val length = this.data.size + optionalHeader.encode().size
        packetLength = if (length < UShort.MAX_VALUE.toInt()) length.toUShort() else 0.toUShort()
        this.streamId = streamId
    }

    constructor(data: ByteArray) {
        val reader = ByteReader(data)
        if (!reader.readBytes(3).contentEquals(startCode)) {
            throw IllegalStateException("Bad PES start code")
        }
        streamId = reader.readUInt8()
        packetLength = reader.readUInt16()
        optionalHeader = OptionalHeader(reader.readBytes(reader.bytesAvailable))
        reader.position = untilPacketLengthSize + 3 + optionalHeader.pesHeaderLength.toInt()
        this.data = reader.readBytes(reader.bytesAvailable)
    }

    fun append(data: ByteArray) {
        this.data += data
    }

    private fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(startCode)
        writer.writeUInt8(streamId)
        writer.writeUInt16(packetLength)
        writer.writeBytes(optionalHeader.encode())
        writer.writeBytes(data)
        return writer.data
    }

    fun arrayOfPackets(
        packetId: UShort,
        randomAccessIndicator: Boolean,
        programClockReference: ULong?
    ): List<MpegTsPacket> {
        val payload = encode()
        val packets = mutableListOf<MpegTsPacket>()
        var payloadOffset = 0
        payloadOffset = appendFirstPacket(
            packetId,
            randomAccessIndicator,
            programClockReference,
            packets,
            payloadOffset,
            payload
        )
        payloadOffset = appendMiddlePackets(packetId, packets, payloadOffset, payload)
        appendLastPackets(packetId, packets, payloadOffset, payload)
        return packets
    }

    private fun appendFirstPacket(
        packetId: UShort,
        randomAccessIndicator: Boolean,
        programClockReference: ULong?,
        packets: MutableList<MpegTsPacket>,
        payloadOffset: Int,
        payload: ByteArray
    ): Int {
        val packet = MpegTsPacket(packetId)
        packet.payloadUnitStartIndicator = true
        val adaptationField = MpegTsAdaptationField()
        adaptationField.randomAccessIndicator = randomAccessIndicator
        programClockReference?.let {
            adaptationField.programClockReference = TSProgramClockReference.encode(it, 0.toUShort())
        }
        packet.adaptationField = adaptationField
        val maximumPayloadSize = packet.maximumPayloadSize()
        val offset = minOf(maximumPayloadSize, payload.size)
        packet.payload = payload.copyOfRange(0, offset)
        if (offset < maximumPayloadSize) {
            packet.setAdaptionFieldStuffing(maximumPayloadSize - offset)
        }
        packets.add(packet)
        return offset
    }

    private fun appendMiddlePackets(
        packetId: UShort,
        packets: MutableList<MpegTsPacket>,
        payloadOffset: Int,
        payload: ByteArray
    ): Int {
        var offset = payloadOffset
        while (offset <= payload.size - 184) {
            val packet = MpegTsPacket(packetId)
            packet.payload = payload.copyOfRange(offset, offset + 184)
            packets.add(packet)
            offset += 184
        }
        return offset
    }

    private fun appendLastPackets(
        packetId: UShort,
        packets: MutableList<MpegTsPacket>,
        payloadOffset: Int,
        payload: ByteArray
    ) {
        var offset = payloadOffset
        val rest = (payload.size - offset) % 184
        when (rest) {
            0 -> {
            }
            183 -> {
                var packet = MpegTsPacket(packetId)
                packet.adaptationField = MpegTsAdaptationField()
                packet.payload = payload.copyOfRange(offset, offset + 182)
                offset += 182
                packets.add(packet)
                packet = MpegTsPacket(packetId)
                packet.adaptationField = MpegTsAdaptationField()
                packet.payload = payload.copyOfRange(offset, payload.size)
                packet.setAdaptionFieldStuffing(182 - packet.payload.size)
                packets.add(packet)
            }
            else -> {
                val packet = MpegTsPacket(packetId)
                packet.adaptationField = MpegTsAdaptationField()
                packet.payload = payload.copyOfRange(offset, payload.size)
                packet.setAdaptionFieldStuffing(182 - packet.payload.size)
                packets.add(packet)
            }
        }
    }
}
