package com.moblin.android.media.haishinkit.mpeg

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.Test

class NalUnitWriterSuite {
    @Test
    fun writeBitMsbFirst() {
        val writer = NalUnitWriter()
        for (value in booleanArrayOf(true, false, true, false, false, true, false, true)) {
            writer.writeBit(value)
        }
        assertContentEquals(byteArrayOf(0xA5.toByte()), writer.data)
    }

    @Test
    fun writeBitsAcrossByteBoundary() {
        val writer = NalUnitWriter()
        writer.writeBits(0x0B, 4)
        writer.writeBits(0x04, 4)
        writer.writeBitsU32(0x1234, 16)
        assertContentEquals(
            byteArrayOf(0xB4.toByte(), 0x12.toByte(), 0x34.toByte()),
            writer.data,
        )
    }

    @Test
    fun emulationPreventionByteInserted() {
        val writer = NalUnitWriter()
        writer.writeBytes(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        assertContentEquals(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x03.toByte(), 0x00.toByte()),
            writer.data,
        )
    }

    @Test
    fun emulationPreventionByteInsertedBeforeLowValue() {
        val writer = NalUnitWriter()
        for (value in byteArrayOf(0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte())) {
            writer.writeBytes(byteArrayOf(0x00.toByte(), 0x00.toByte(), value))
            assertContentEquals(
                byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x03.toByte(), value),
                writer.data,
            )
        }
    }

    @Test
    fun noEmulationPreventionByteBeforeHighValue() {
        val writer = NalUnitWriter()
        for (value in byteArrayOf(0x04.toByte(), 0x30.toByte(), 0x80.toByte(), 0xFF.toByte())) {
            writer.writeBytes(byteArrayOf(0x00.toByte(), 0x00.toByte(), value))
            assertContentEquals(
                byteArrayOf(0x00.toByte(), 0x00.toByte(), value),
                writer.data,
            )
        }
    }

    @Test
    fun emulationPreventionByteRestartsZeroRun() {
        val writer = NalUnitWriter()
        writer.writeBytes(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()),
        )
        assertContentEquals(
            byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            writer.data,
        )
    }

    @Test
    fun noEmulationPreventionByteWhenDisabled() {
        val writer = NalUnitWriter(emulationPrevention = false)
        writer.writeBytes(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte()),
        )
        assertContentEquals(
            byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte()),
            writer.data,
        )
    }

    @Test
    fun emulationPreventionByteRoundTrip() {
        val writer = NalUnitWriter()
        writer.writeBytes(
            byteArrayOf(
                0xAB.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x30.toByte(),
            ),
        )
        assertContentEquals(
            byteArrayOf(
                0xAB.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x30.toByte(),
            ),
            writer.data,
        )
        val reader = NalUnitReader(writer.data)
        assertEquals(0xAB, reader.readBits(8))
        assertEquals(0x000001, reader.readBitsU32(24))
        assertEquals(0x000030, reader.readBitsU32(24))
    }
}
