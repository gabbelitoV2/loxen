package com.moblin.android.media.haishinkit.mpeg

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NalUnitReaderSuite {
    @Test
    fun readBitMsbFirst() {
        val reader = NalUnitReader(byteArrayOf(0xA5.toByte()))
        assertTrue(reader.readBit())
        assertFalse(reader.readBit())
        assertTrue(reader.readBit())
        assertFalse(reader.readBit())
        assertFalse(reader.readBit())
        assertTrue(reader.readBit())
        assertFalse(reader.readBit())
        assertTrue(reader.readBit())
    }

    @Test
    fun readBitsHighAndLowNibble() {
        val reader = NalUnitReader(byteArrayOf(0xB4.toByte()))
        assertEquals(0x0BL, reader.readBits(4).toLong())
        assertEquals(0x04L, reader.readBits(4).toLong())
    }

    @Test
    fun readBitsFullByte() {
        val reader = NalUnitReader(byteArrayOf(0xFF.toByte()))
        assertEquals(0xFFL, reader.readBits(8).toLong())
    }

    @Test
    fun readBitsU32AcrossTwoBytes() {
        val reader = NalUnitReader(byteArrayOf(0x12.toByte(), 0x34.toByte()))
        assertEquals(0x1234L, reader.readBitsU32(16).toLong())
    }

    @Test
    fun readBitsU32AcrossThreeBytes() {
        val reader = NalUnitReader(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()))
        assertEquals(0xABCDEFL, reader.readBitsU32(24).toLong())
    }

    @Test
    fun skipBitsAdvancesPosition() {
        val reader = NalUnitReader(byteArrayOf(0x00.toByte(), 0xBE.toByte()))
        reader.skipBits(8)
        assertEquals(0xBEL, reader.readBits(8).toLong())
    }

    @Test
    fun availableDecrementsAfterReads() {
        val reader = NalUnitReader(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        assertEquals(16L, reader.available().toLong())
        reader.readBit()
        assertEquals(15L, reader.available().toLong())
        reader.skipBits(7)
        assertEquals(8L, reader.available().toLong())
        reader.readBits(8)
        assertEquals(0L, reader.available().toLong())
    }

    @Test
    fun emulationPreventionByteIsSkipped() {
        val reader = NalUnitReader(byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x03.toByte(), 0x80.toByte()))
        assertEquals(0x000080L, reader.readBitsU32(24).toLong())
    }

    @Test
    fun noFalseEmulationPreventionByteRemoval() {
        val reader = NalUnitReader(byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x03.toByte(), 0x80.toByte()))
        assertEquals(0x01000380L, reader.readBitsU32(32).toLong())
    }

    @Test
    fun readRawBytesReturnsRemainingData() {
        val reader = NalUnitReader(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte()))
        reader.readBits(8)
        val remaining = reader.readRawBytes()
        assertContentEquals(byteArrayOf(0x34.toByte(), 0x56.toByte()), remaining)
    }

    @Test
    fun readRawBytesAtStart() {
        val data = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val reader = NalUnitReader(data)
        assertContentEquals(data, reader.readRawBytes())
    }

    @Test
    fun readRawBytesThrowsWhenNotOnByteBoundary() {
        val reader = NalUnitReader(byteArrayOf(0xFF.toByte(), 0x00.toByte()))
        reader.readBit()
        assertFailsWith<Throwable> {
            reader.readRawBytes()
        }
    }

    @Test
    fun readBitThrowsOnEmptyData() {
        val reader = NalUnitReader(ByteArray(0))
        assertFailsWith<Throwable> {
            reader.readBit()
        }
    }

    @Test
    fun readBitThrowsAfterAllBitsConsumed() {
        val reader = NalUnitReader(byteArrayOf(0xFF.toByte()))
        reader.skipBits(8)
        assertFailsWith<Throwable> {
            reader.readBit()
        }
    }

    @Test
    fun offsetSkipsInitialBytes() {
        val reader = NalUnitReader(byteArrayOf(0x12.toByte(), 0x34.toByte(), 0x56.toByte()), 1)
        assertEquals(0x34L, reader.readBits(8).toLong())
        assertEquals(0x56L, reader.readBits(8).toLong())
    }
}
