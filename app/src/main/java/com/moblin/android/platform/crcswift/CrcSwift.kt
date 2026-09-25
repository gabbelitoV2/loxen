package com.moblin.android.platform.crcswift

object CrcSwift {
    fun computeCrc8(
        data: ByteArray,
        initialCrc: UByte = 0x00u,
        polynom: UByte = 0x07u,
        xor: UByte = 0x00u,
        refIn: Boolean = false,
        refOut: Boolean = false,
    ): UByte {
        if (data.isEmpty()) {
            return 0u
        }
        var crc = initialCrc.toInt()
        for (byte in data) {
            val value = byte.toInt() and 0xFF
            crc = crc.xor(if (refIn) reverseBits(value, 8) else value)
            repeat(8) {
                val check = crc and 0x80
                crc = (crc shl 1) and 0xFF
                if (check != 0) {
                    crc = crc.xor(polynom.toInt())
                }
            }
        }
        if (refOut) {
            crc = reverseBits(crc, 8)
        }
        return crc.xor(xor.toInt()).toUByte()
    }

    fun computeCrc16(
        data: ByteArray,
        initialCrc: UShort = 0x00u,
        polynom: UShort = 0x07u,
        xor: UShort = 0x00u,
        refIn: Boolean = false,
        refOut: Boolean = false,
    ): UShort {
        if (data.isEmpty()) {
            return 0u
        }
        var crc = initialCrc.toInt()
        for (byte in data) {
            val value = byte.toInt() and 0xFF
            crc = crc.xor((if (refIn) reverseBits(value, 8) else value) shl 8)
            repeat(8) {
                val check = crc and 0x8000
                crc = (crc shl 1) and 0xFFFF
                if (check != 0) {
                    crc = crc.xor(polynom.toInt())
                }
            }
        }
        if (refOut) {
            crc = reverseBits(crc, 16)
        }
        return crc.xor(xor.toInt()).toUShort()
    }

    fun computeCrc32(
        data: ByteArray,
        initialCrc: UInt = 0x00u,
        polynom: UInt = 0x07u,
        xor: UInt = 0x00u,
        refIn: Boolean = false,
        refOut: Boolean = false,
    ): UInt {
        var crc = initialCrc.toLong()
        for (byte in data) {
            val value = (byte.toInt() and 0xFF).let { if (refIn) reverseBits(it, 8) else it }.toLong()
            crc = crc.xor(value shl 24)
            repeat(8) {
                val check = crc and 0x8000_0000L
                crc = (crc shl 1) and 0xFFFF_FFFFL
                if (check != 0L) {
                    crc = crc.xor(polynom.toLong())
                }
            }
        }
        if (refOut) {
            crc = Integer.reverse(crc.toInt()).toLong() and 0xFFFF_FFFFL
        }
        return crc.xor(xor.toLong()).toUInt()
    }

    private fun reverseBits(value: Int, bits: Int): Int {
        return Integer.reverse(value) ushr (32 - bits)
    }
}
