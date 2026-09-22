package com.moblin.android.media.haishinkit.mpeg

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class TSTimestampSuite {
    @Test
    fun encodeKnownValue() {
        assertEquals(
            byteArrayOf(0x29, 0x8D.toByte(), 0x15, 0xCF.toByte(), 0x13),
            TSTimestamp.encode(0x1_2345_6789L, 0x20u),
        )
    }

    @Test
    fun encodeMaximumValue() {
        assertEquals(
            byteArrayOf(0x3F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            TSTimestamp.encode(0x1_FFFF_FFFFL, 0x30u),
        )
    }

    @Test
    fun encodeZero() {
        assertEquals(
            byteArrayOf(0x11, 0x00, 0x01, 0x00, 0x01),
            TSTimestamp.encode(0L, 0x10u),
        )
    }

    @Test
    fun encodeWrapsAt33Bits() {
        val uptime30Hours = 30L * 3600 * 90000
        val encoded = TSTimestamp.encode(uptime30Hours, 0x20u)
        assertEquals(0x20, encoded[0].toInt() and 0xF0)
        assertEquals(uptime30Hours and 0x1_FFFF_FFFFL, TSTimestamp.decode(encoded))
    }

    @Test
    fun encodeNegativeValue() {
        val encoded = TSTimestamp.encode(-1L, 0x20u)
        assertEquals(
            byteArrayOf(0x2F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            encoded,
        )
        assertEquals(0x1_FFFF_FFFFL, TSTimestamp.decode(encoded))
    }

    @Test
    fun roundTrip() {
        for (value in listOf(0L, 1L, 90000L, 0x1234_5678L, 0xFFFF_FFFFL, 0x1_0000_0000L, 0x1_FFFF_FFFEL)) {
            assertEquals(value, TSTimestamp.decode(TSTimestamp.encode(value, 0x20u)))
        }
    }

    @Test
    fun decodeIgnoresMarkerBits() {
        assertEquals(0L, TSTimestamp.decode(byteArrayOf(0xF1.toByte(), 0x00, 0x01, 0x00, 0x01)))
    }

    @Test
    fun decodeAtOffset() {
        val data = TSTimestamp.encode(1000L, 0x30u) + TSTimestamp.encode(900L, 0x10u)
        assertEquals(1000L, TSTimestamp.decode(data, 0))
        assertEquals(900L, TSTimestamp.decode(data, TSTimestamp.dataSize))
    }

    @Test
    fun decodeTooShort() {
        assertNull(TSTimestamp.decode(byteArrayOf()))
        assertNull(TSTimestamp.decode(byteArrayOf(0x21, 0x00, 0x01, 0x00)))
        assertNull(TSTimestamp.decode(byteArrayOf(0x21, 0x00, 0x01, 0x00, 0x01), 1))
    }

    @Test
    fun optionalHeaderTruncatedTimestamps() {
        var header = OptionalHeader(byteArrayOf(0x80.toByte(), 0xC0.toByte(), 0x03, 0x31, 0x00, 0x01))
        assertEquals(CM_TIME_INVALID, header.getPresentationTimeStamp())
        assertEquals(CM_TIME_INVALID, header.getDecodeTimeStamp())
        header = OptionalHeader(byteArrayOf(0x80.toByte(), 0xC0.toByte(), 0x05) + TSTimestamp.encode(1000L, 0x30u))
        assertEquals(cmTime(1000, 90000), header.getPresentationTimeStamp())
        assertEquals(CM_TIME_INVALID, header.getDecodeTimeStamp())
    }

    @Test
    fun optionalHeaderRoundTrip() {
        val header = OptionalHeader()
        header.setTimestamp(cmTime(1000, 90000), cmTime(900, 90000))
        val decoded = OptionalHeader(header.encode())
        assertEquals(cmTime(1000, 90000), decoded.getPresentationTimeStamp())
        assertEquals(cmTime(900, 90000), decoded.getDecodeTimeStamp())
    }
}

private const val CM_TIME_INVALID = Long.MIN_VALUE

private fun cmTime(value: Long, timescale: Int): Long = value * 1_000_000 / timescale
