package com.moblin.android.media.haishinkit.mpeg

class Crc32 private constructor(polynomial: UInt) {
    companion object {
        val mpeg2 = Crc32(0x04C11DB7u)
    }

    private val table: UIntArray

    init {
        val table = UIntArray(256)
        for (i in 0 until table.size) {
            var crc = i.toUInt() shl 24
            for (j in 0 until 8) {
                crc = (crc shl 1) xor (if ((crc and 0x80000000u) == 0x80000000u) polynomial else 0u)
            }
            table[i] = crc
        }
        this.table = table
    }

    fun calculate(data: ByteArray): UInt {
        var crc = 0xFFFFFFFFu
        for (i in 0 until data.size) {
            crc = (crc shl 8) xor table[((crc shr 24) xor (data[i].toUInt() and 0xFFu)).toInt()]
        }
        return crc
    }
}
