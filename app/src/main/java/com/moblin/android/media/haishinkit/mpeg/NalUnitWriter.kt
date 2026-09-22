package com.moblin.android.media.haishinkit.mpeg

const val nalUnitEmulationPreventionByte: UByte = 0x03u

class NalUnitWriter(emulationPrevention: Boolean = true) {
    var data: ByteArray = ByteArray(0)
        private set
    var bitOffset: Int = 0
        private set
    private val emulationPrevention: Boolean = emulationPrevention

    fun writeBit(value: Boolean) {
        if (bitOffset == 0) {
            data += 0.toByte()
        }
        if (value) {
            data[data.size - 1] = (data[data.size - 1].toInt() or (1 shl (7 - bitOffset))).toByte()
        }
        bitOffset += 1
        bitOffset %= 8
        if (bitOffset == 0) {
            insertEmulationPreventionByteIfNeeded()
        }
    }

    private fun insertEmulationPreventionByteIfNeeded() {
        if (!emulationPrevention) {
            return
        }
        if (data.size < 3) {
            return
        }
        if (data[data.size - 1].toUByte() > nalUnitEmulationPreventionByte) {
            return
        }
        if (data[data.size - 2] != 0.toByte()) {
            return
        }
        if (data[data.size - 3] != 0.toByte()) {
            return
        }
        val index = data.size - 1
        val newData = ByteArray(data.size + 1)
        data.copyInto(newData, 0, 0, index)
        newData[index] = nalUnitEmulationPreventionByte.toByte()
        data.copyInto(newData, index + 1, index, data.size)
        data = newData
    }

    fun writeBits(value: UByte, count: Int) {
        for (i in 0 until count) {
            val mask = (1u shl (count - i - 1)).toUByte()
            writeBit((value and mask) == mask)
        }
    }

    fun writeBitsU32(value: UInt, count: Int) {
        for (i in 0 until count) {
            val mask = 1u shl (count - i - 1)
            writeBit((value and mask) == mask)
        }
    }

    fun writeBytes(data: ByteArray) {
        for (value in data) {
            writeBits(value.toUByte(), 8)
        }
    }

    fun writeRawBytes(data: ByteArray) {
        this.data = this.data + data
    }
}
