package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnit
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitPayload
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitSei
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitSeiPayload
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.avc.AvcSeiPayloadPictureTiming
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnit
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitPayload
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitSei
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitSeiPayload
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcSeiPayloadTimeCode
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail

private val noon = Instant.ofEpochSecond(1_755_864_000L)

private fun clock(hours: Int, minutes: Int, seconds: Int): Instant {
    val startOfDay = noon.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS)
    return startOfDay.plusSeconds((3600 * hours + 60 * minutes + seconds).toLong()).toInstant()
}

class NalUnitSeiSuite {

    @Test
    fun hevcTimeCodePayloadEncode() {
        val timeCode = HevcSeiPayloadTimeCode(clock = clock(12, 34, 56), frame = 7u)
        assertContentEquals(
            byteArrayOf(0x70, 0x40, 0x3F, 0x11, 0x30, 0x10),
            timeCode.encode()
        )
    }

    @Test
    fun hevcTimeCodePayloadDecode() {
        val reader = NalUnitReader(data = byteArrayOf(0x70, 0x40, 0x3F, 0x11, 0x30, 0x10))
        val timeCode = HevcSeiPayloadTimeCode.from(reader)!!
        assertEquals(12.toUByte(), timeCode.hours)
        assertEquals(34.toUByte(), timeCode.minutes)
        assertEquals(56.toUByte(), timeCode.seconds)
        assertEquals(7u, timeCode.frame)
    }

    @Test
    fun hevcTimeCodePayloadRoundTrip() {
        val cases = listOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(23, 59, 59, 59),
            intArrayOf(12, 34, 56, 7),
            intArrayOf(1, 2, 3, 255),
            intArrayOf(9, 8, 7, 511)
        )
        for ((hours, minutes, seconds, frame) in cases) {
            val encoded = HevcSeiPayloadTimeCode(
                clock = clock(hours, minutes, seconds),
                frame = frame.toUInt()
            ).encode()
            val decoded = HevcSeiPayloadTimeCode.from(NalUnitReader(data = encoded))!!
            assertEquals(hours.toUByte(), decoded.hours)
            assertEquals(minutes.toUByte(), decoded.minutes)
            assertEquals(seconds.toUByte(), decoded.seconds)
            assertEquals(frame.toUInt(), decoded.frame)
        }
    }

    @Test
    fun hevcTimeCodePayloadMakeClock() {
        val timeCode = HevcSeiPayloadTimeCode(clock = clock(12, 34, 56), frame = 7u)
        val (decodedClock, frame) = timeCode.makeClock()
        assertEquals(
            (12 * 3600 + 34 * 60 + 56).toLong(),
            decodedClock.epochSecond % 86400L
        )
        assertEquals(7u, frame)
    }

    @Test
    fun hevcNalUnitRoundTrip() {
        val timeCode = HevcSeiPayloadTimeCode(clock = clock(12, 34, 56), frame = 7u)
        val sei = HevcNalUnitSei(payload = HevcNalUnitSeiPayload.TimeCode(timeCode))
        val encoded = HevcNalUnit(
            type = HevcNalUnitType.prefixSeiNut,
            temporalIdPlusOne = 1.toUByte(),
            payload = HevcNalUnitPayload.prefixSeiNut(sei)
        ).encode()
        assertContentEquals(byteArrayOf(0x4E, 0x01), encoded.copyOfRange(0, 2))
        assertEquals(136.toByte(), encoded[2])
        assertEquals(6.toByte(), encoded[3])
        val decoded = HevcNalUnit(data = encoded, offset = 0)!!
        assertEquals(HevcNalUnitType.prefixSeiNut, decoded.header.type)
        assertEquals(1.toUByte(), decoded.header.temporalIdPlusOne)
        val decodedSei = (decoded.payload as? HevcNalUnitPayload.prefixSeiNut)?.payload
            ?: fail("Not a time code SEI")
        val decodedTimeCode = (decodedSei.payload as? HevcNalUnitSeiPayload.TimeCode)?.payload
            ?: fail("Not a time code SEI")
        assertEquals(12.toUByte(), decodedTimeCode.hours)
        assertEquals(34.toUByte(), decodedTimeCode.minutes)
        assertEquals(56.toUByte(), decodedTimeCode.seconds)
        assertEquals(7u, decodedTimeCode.frame)
    }

    @Test
    fun hevcNalUnitEmulationPreventionByteInserted() {
        val timeCode = HevcSeiPayloadTimeCode(clock = clock(0, 0, 0), frame = 0u)
        val sei = HevcNalUnitSei(payload = HevcNalUnitSeiPayload.TimeCode(timeCode))
        val encoded = HevcNalUnit(
            type = HevcNalUnitType.prefixSeiNut,
            temporalIdPlusOne = 1.toUByte(),
            payload = HevcNalUnitPayload.prefixSeiNut(sei)
        ).encode()
        assertContentEquals(
            byteArrayOf(
                0x4E, 0x01, 0x88.toByte(), 0x06, 0x70, 0x40,
                0x00, 0x00, 0x03, 0x00, 0x10, 0x80.toByte()
            ),
            encoded
        )
    }

    @Test
    fun hevcNalUnitNoEmulationPreventionByteOnTheHour() {
        val timeCode = HevcSeiPayloadTimeCode(clock = clock(12, 0, 0), frame = 0u)
        val sei = HevcNalUnitSei(payload = HevcNalUnitSeiPayload.TimeCode(timeCode))
        val encoded = HevcNalUnit(
            type = HevcNalUnitType.prefixSeiNut,
            temporalIdPlusOne = 1.toUByte(),
            payload = HevcNalUnitPayload.prefixSeiNut(sei)
        ).encode()
        assertContentEquals(
            byteArrayOf(
                0x4E, 0x01, 0x88.toByte(), 0x06, 0x70, 0x40,
                0x00, 0x00, 0x30, 0x10, 0x80.toByte()
            ),
            encoded
        )
    }

    @Test
    fun hevcNalUnitRoundTripWithEmulationPreventionBytes() {
        val cases = listOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 1, 0, 0),
            intArrayOf(0, 2, 8, 32),
            intArrayOf(23, 59, 59, 59)
        )
        for ((hours, minutes, seconds, frame) in cases) {
            val timeCode = HevcSeiPayloadTimeCode(
                clock = clock(hours, minutes, seconds),
                frame = frame.toUInt()
            )
            val sei = HevcNalUnitSei(payload = HevcNalUnitSeiPayload.TimeCode(timeCode))
            val encoded = HevcNalUnit(
                type = HevcNalUnitType.prefixSeiNut,
                temporalIdPlusOne = 1.toUByte(),
                payload = HevcNalUnitPayload.prefixSeiNut(sei)
            ).encode()
            val decoded = HevcNalUnit(data = encoded, offset = 0)!!
            val decodedSei = (decoded.payload as? HevcNalUnitPayload.prefixSeiNut)?.payload
                ?: fail("Not a time code SEI")
            val decodedTimeCode = (decodedSei.payload as? HevcNalUnitSeiPayload.TimeCode)?.payload
                ?: fail("Not a time code SEI")
            assertEquals(hours.toUByte(), decodedTimeCode.hours)
            assertEquals(minutes.toUByte(), decodedTimeCode.minutes)
            assertEquals(seconds.toUByte(), decodedTimeCode.seconds)
            assertEquals(frame.toUInt(), decodedTimeCode.frame)
        }
    }

    @Test
    fun avcNalUnitRoundTrip() {
        val cases = listOf(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(23, 59, 59, 59),
            intArrayOf(12, 34, 56, 7)
        )
        for ((hours, minutes, seconds, frame) in cases) {
            val pictureTiming = AvcSeiPayloadPictureTiming(
                clock = clock(hours, minutes, seconds),
                frame = frame.toUInt()
            )
            val sei = AvcNalUnitSei(payload = AvcNalUnitSeiPayload.PictureTiming(pictureTiming))
            val encoded = AvcNalUnit(type = AvcNalUnitType.sei, payload = AvcNalUnitPayload.Sei(sei)).encode()
            assertEquals(6.toByte(), encoded[0])
            assertEquals(1.toByte(), encoded[1])
            val decoded = AvcNalUnit.create(data = encoded, offset = 0)!!
            assertEquals(AvcNalUnitType.sei, decoded.header.type)
            val decodedSei = (decoded.payload as? AvcNalUnitPayload.Sei)?.sei
                ?: fail("Not a picture timing SEI")
            val decodedPictureTiming = (decodedSei.payload as? AvcNalUnitSeiPayload.PictureTiming)?.value
                ?: fail("Not a picture timing SEI")
            assertEquals(hours.toUByte(), decodedPictureTiming.hours)
            assertEquals(minutes.toUByte(), decodedPictureTiming.minutes)
            assertEquals(seconds.toUByte(), decodedPictureTiming.seconds)
            assertEquals(frame.toUInt(), decodedPictureTiming.frame)
        }
    }
}
