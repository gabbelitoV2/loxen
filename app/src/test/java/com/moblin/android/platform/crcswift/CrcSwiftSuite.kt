package com.moblin.android.platform.crcswift

import kotlin.test.assertEquals
import org.junit.Test

class CrcSwiftSuite {
    private val check = "123456789".toByteArray()

    @Test
    fun crc8MatchesTheCatalogueCheckValues() {
        assertEquals(0xF4u.toUByte(), CrcSwift.computeCrc8(check))
        assertEquals(
            0xA1u.toUByte(),
            CrcSwift.computeCrc8(check, initialCrc = 0x00u, polynom = 0x31u, xor = 0x00u, refIn = true, refOut = true),
        )
    }

    @Test
    fun crc8ReflectsTheInputBytesButNotTheInitialValueLikeCrcSwift() {
        assertEquals(
            0x66u.toUByte(),
            CrcSwift.computeCrc8(
                byteArrayOf(0x55, 0x0E, 0x04),
                initialCrc = 0xEEu,
                polynom = 0x31u,
                xor = 0x00u,
                refIn = true,
                refOut = true,
            ),
        )
        assertEquals(
            0xFBu.toUByte(),
            CrcSwift.computeCrc8(check, initialCrc = 0xEEu, polynom = 0x31u, xor = 0x00u, refIn = true, refOut = true),
        )
    }

    @Test
    fun crc16MatchesTheCatalogueCheckValues() {
        assertEquals(
            0x2189u.toUShort(),
            CrcSwift.computeCrc16(check, initialCrc = 0x0000u, polynom = 0x1021u, xor = 0x0000u, refIn = true, refOut = true),
        )
        assertEquals(
            0x7109u.toUShort(),
            CrcSwift.computeCrc16(check, initialCrc = 0x496Cu, polynom = 0x1021u, xor = 0x0000u, refIn = true, refOut = true),
        )
        assertEquals(0xEF6Fu.toUShort(), CrcSwift.computeCrc16(check))
    }

    @Test
    fun crc32MatchesTheCatalogueCheckValues() {
        assertEquals(
            0xCBF43926u,
            CrcSwift.computeCrc32(
                check,
                initialCrc = 0xFFFFFFFFu,
                polynom = 0x04C11DB7u,
                xor = 0xFFFFFFFFu,
                refIn = true,
                refOut = true,
            ),
        )
        assertEquals(0x5B4904ACu, CrcSwift.computeCrc32(check))
    }

    @Test
    fun emptyDataGivesZeroLikeTheDataOverloads() {
        assertEquals(0u.toUByte(), CrcSwift.computeCrc8(ByteArray(0), initialCrc = 0xEEu, refOut = true))
        assertEquals(0u.toUShort(), CrcSwift.computeCrc16(ByteArray(0), initialCrc = 0x496Cu, refOut = true))
    }
}
