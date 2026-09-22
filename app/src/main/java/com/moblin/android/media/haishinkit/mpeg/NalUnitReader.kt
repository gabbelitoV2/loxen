package com.moblin.android.media.haishinkit.mpeg

class NalUnitReader(data: ByteArray, offset: Int = 0) {
    private var data: ByteArray
    private var byteOffset: Int
    private var bitOffset: Int = 7

    init {
        this.data = data
        this.byteOffset = offset
    }

    fun available(): Int {
        return 8 * (data.size - byteOffset) - (7 - bitOffset)
    }

    fun readRawBytes(): ByteArray {
        checkOutOfData()
        if (bitOffset != 7) {
            throw IllegalStateException("Cannot read remaining bytes when not at byte boundary")
        }
        return data.copyOfRange(byteOffset, data.size)
    }

    fun readBit(): Boolean {
        checkOutOfData()
        val value = ((data[byteOffset].toInt() shr bitOffset) and 1) == 1
        bitOffset -= 1
        if (bitOffset == -1) {
            bitOffset = 7
            byteOffset += 1
            if (available() > 0 &&
                byteOffset >= 2 &&
                data[byteOffset - 2] == 0.toByte() &&
                data[byteOffset - 1] == 0.toByte() &&
                data[byteOffset] == nalUnitEmulationPreventionByte.toByte()
            ) {
                byteOffset += 1
            }
        }
        return value
    }

    fun skipBits(count: Int) {
        repeat(count) {
            readBit()
        }
    }

    fun readBits(count: Int): UByte {
        var value: UByte = 0u
        for (i in 0 until count) {
            value = ((value.toInt() shl 1) or (if (readBit()) 1 else 0)).toUByte()
        }
        return value
    }

    fun readBitsU32(count: Int): UInt {
        var value: UInt = 0u
        for (i in 0 until count) {
            value = (value shl 1) or (if (readBit()) 1u else 0u)
        }
        return value
    }

    private fun checkOutOfData() {
        if (byteOffset >= data.size) {
            throw IllegalStateException("Out of data")
        }
    }
}
