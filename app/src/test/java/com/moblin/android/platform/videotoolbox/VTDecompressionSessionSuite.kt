package com.moblin.android.platform.videotoolbox

import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test

class VTDecompressionSessionSuite {
    private fun lengthPrefixed(vararg nalUnits: ByteArray): ByteArray {
        var data = ByteArray(0)
        for (nalUnit in nalUnits) {
            val size = nalUnit.size
            data += byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte())
            data += nalUnit
        }
        return data
    }

    private fun startCoded(vararg nalUnits: ByteArray): ByteArray {
        var data = ByteArray(0)
        for (nalUnit in nalUnits) {
            data += byteArrayOf(0, 0, 0, 1) + nalUnit
        }
        return data
    }

    @Test
    fun lengthPrefixedNalUnitsGetStartCodes() {
        val slice = byteArrayOf(0x41, 0x9A.toByte(), 0x02) + ByteArray(297) { 0x55 }
        val sei = byteArrayOf(0x06, 0x05, 0x01)
        val converted = sampleDataToAnnexB(lengthPrefixed(slice, sei))
        assertContentEquals(startCoded(slice, sei), converted)
    }

    @Test
    fun lengthPrefixedNalUnitOfOneByteGetsAStartCode() {
        val aud = byteArrayOf(0x09)
        val slice = byteArrayOf(0x65, 0x11, 0x22)
        assertContentEquals(startCoded(aud, slice), sampleDataToAnnexB(lengthPrefixed(aud, slice)))
    }

    @Test
    fun startCodedDataIsKept() {
        val data = startCoded(byteArrayOf(0x67, 0x42), byteArrayOf(0x68, 0x01), byteArrayOf(0x65, 0x11, 0x22))
        assertSame(data, sampleDataToAnnexB(data))
        val short = byteArrayOf(0, 0, 1, 0x65, 0x11, 0x22, 0x33)
        assertSame(short, sampleDataToAnnexB(short))
    }

    @Test
    fun badDataIsRejected() {
        assertNull(sampleDataToAnnexB(ByteArray(0)))
        assertNull(sampleDataToAnnexB(byteArrayOf(0, 0, 0, 9, 0x41, 0x01)))
        assertNull(sampleDataToAnnexB(byteArrayOf(0x41, 0x01)))
    }

    @Test
    fun inputIsNotModified() {
        val slice = byteArrayOf(0x41, 0x01, 0x02)
        val data = lengthPrefixed(slice)
        val copy = data.copyOf()
        sampleDataToAnnexB(data)
        assertContentEquals(copy, data)
    }

    @Test
    fun keyFramesAreFound() {
        val h264Idr = startCoded(byteArrayOf(0x67, 0x42), byteArrayOf(0x68, 0x01), byteArrayOf(0x65, 0x11))
        val h264P = startCoded(byteArrayOf(0x41, 0x9A.toByte(), 0x02))
        assertTrue(annexBContainsKeyFrame(h264Idr, isHevc = false))
        assertFalse(annexBContainsKeyFrame(h264P, isHevc = false))
        val h265Idr = startCoded(byteArrayOf(0x40, 0x01), byteArrayOf(0x26, 0x01, 0x11))
        val h265Cra = startCoded(byteArrayOf(0x2A, 0x01, 0x11))
        val h265Trail = startCoded(byteArrayOf(0x02, 0x01, 0x11))
        assertTrue(annexBContainsKeyFrame(h265Idr, isHevc = true))
        assertTrue(annexBContainsKeyFrame(h265Cra, isHevc = true))
        assertFalse(annexBContainsKeyFrame(h265Trail, isHevc = true))
    }
}
