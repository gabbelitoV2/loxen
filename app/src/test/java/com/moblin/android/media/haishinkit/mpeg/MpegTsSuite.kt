package com.moblin.android.media.haishinkit.mpeg

import org.junit.Test
import kotlin.test.assertEquals

class MpegTsSuite {
    @Test
    fun firstPacketStuffing() {
        val stream = MpegTsPacketizedElementaryStream(
            streamId = 0xE0.toUByte(),
            presentationTimeStamp = 0L,
            decodeTimeStamp = OptionalHeader.invalidTimestamp,
            data = byteArrayOf(0x65),
        )
        val packets = stream.arrayOfPackets(256.toUShort(), true, null)
        assertEquals(1, packets.size)
        assertEquals(MpegTsPacket.size, packets[0].encode().size)
    }

    @Test
    fun firstPacketNoStuffing() {
        val stream = MpegTsPacketizedElementaryStream(
            streamId = 0xE0.toUByte(),
            presentationTimeStamp = 0L,
            decodeTimeStamp = OptionalHeader.invalidTimestamp,
            data = ByteArray(168) { 1 },
        )
        val packets = stream.arrayOfPackets(256.toUShort(), true, null)
        assertEquals(1, packets.size)
        assertEquals(MpegTsPacket.size, packets[0].encode().size)
    }

    @Test
    fun twoPackets() {
        val stream = MpegTsPacketizedElementaryStream(
            streamId = 0xE0.toUByte(),
            presentationTimeStamp = 0L,
            decodeTimeStamp = OptionalHeader.invalidTimestamp,
            data = ByteArray(169) { 1 },
        )
        val packets = stream.arrayOfPackets(256.toUShort(), true, null)
        assertEquals(2, packets.size)
        assertEquals(MpegTsPacket.size, packets[0].encode().size)
        assertEquals(MpegTsPacket.size, packets[1].encode().size)
    }

    @Test
    fun firstPacketWithClockReferenceStuffing() {
        val stream = MpegTsPacketizedElementaryStream(
            streamId = 0xE0.toUByte(),
            presentationTimeStamp = 0L,
            decodeTimeStamp = OptionalHeader.invalidTimestamp,
            data = ByteArray(161) { 1 },
        )
        val packets = stream.arrayOfPackets(256.toUShort(), true, 1234L.toULong())
        assertEquals(1, packets.size)
        assertEquals(MpegTsPacket.size, packets[0].encode().size)
    }

    @Test
    fun firstPacketWithClockReferenceNoStuffing() {
        val stream = MpegTsPacketizedElementaryStream(
            streamId = 0xE0.toUByte(),
            presentationTimeStamp = 0L,
            decodeTimeStamp = OptionalHeader.invalidTimestamp,
            data = ByteArray(162) { 1 },
        )
        val packets = stream.arrayOfPackets(256.toUShort(), true, 1234L.toULong())
        assertEquals(1, packets.size)
        assertEquals(MpegTsPacket.size, packets[0].encode().size)
    }
}
