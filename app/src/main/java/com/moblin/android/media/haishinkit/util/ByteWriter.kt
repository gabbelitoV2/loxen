package com.moblin.android.media.haishinkit.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

class ByteWriter {
    var data: ByteArray = ByteArray(0)
        private set
    private var position = 0

    var length: Int
        get() = data.size
        set(newValue) {
            if (newValue > data.size) {
                data = data + ByteArray(newValue - data.size)
            } else if (newValue < data.size) {
                data = data.copyOfRange(0, newValue)
            }
        }

    constructor()

    constructor(data: ByteArray) {
        this.data = data
    }

    operator fun get(i: Int): UByte = data[i].toUByte()

    operator fun set(i: Int, value: UByte) {
        data[i] = value.toByte()
    }

    fun writeUInt8(value: UByte) {
        if (position == data.size) {
            data = data + byteArrayOf(value.toByte())
            position += 1
        } else {
            writeBytes(byteArrayOf(value.toByte()))
        }
    }

    fun writeUInt16(value: UShort) {
        writeUInt8(((value.toInt() shr 8) and 0xFF).toUByte())
        writeUInt8((value.toInt() and 0xFF).toUByte())
    }

    fun writeUInt16Le(value: UShort) {
        writeUInt8((value.toInt() and 0xFF).toUByte())
        writeUInt8(((value.toInt() shr 8) and 0xFF).toUByte())
    }

    fun writeUInt24(value: UInt) {
        writeUInt8(((value shr 16) and 0xFFu).toUByte())
        writeUInt8(((value shr 8) and 0xFFu).toUByte())
        writeUInt8((value and 0xFFu).toUByte())
    }

    fun writeUInt24Le(value: UInt) {
        writeUInt8((value and 0xFFu).toUByte())
        writeUInt8(((value shr 8) and 0xFFu).toUByte())
        writeUInt8(((value shr 16) and 0xFFu).toUByte())
    }

    fun writeUInt32(value: UInt) {
        writeUInt8(((value shr 24) and 0xFFu).toUByte())
        writeUInt8(((value shr 16) and 0xFFu).toUByte())
        writeUInt8(((value shr 8) and 0xFFu).toUByte())
        writeUInt8((value and 0xFFu).toUByte())
    }

    fun writeUInt32Le(value: UInt) {
        writeUInt8((value and 0xFFu).toUByte())
        writeUInt8(((value shr 8) and 0xFFu).toUByte())
        writeUInt8(((value shr 16) and 0xFFu).toUByte())
        writeUInt8(((value shr 24) and 0xFFu).toUByte())
    }

    fun writeUInt64(value: ULong) {
        writeUInt32(((value shr 32) and 0xFFFF_FFFFuL).toUInt())
        writeUInt32((value and 0xFFFF_FFFFuL).toUInt())
    }

    fun writeInt32(value: Int) {
        writeBytes(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array())
    }

    fun writeDouble(value: Double) {
        writeBytes(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(value).array())
    }

    fun writeUTF8Bytes(value: String) {
        writeBytes(value.toByteArray(Charsets.UTF_8))
    }

    fun writeBytes(value: ByteArray) {
        if (position == data.size) {
            data = data + value
            position = data.size
        } else {
            val length = minOf(data.size, value.size)
            for (offset in 0 until length) {
                data[position + offset] = value[offset]
            }
            if (length == data.size) {
                data = data + value.copyOfRange(length, value.size)
            }
            position += value.size
        }
    }

    fun writeBytes(value: ByteBuffer) {
        val remaining = value.remaining()
        val bytes = ByteArray(remaining)
        value.duplicate().get(bytes)
        writeBytes(bytes)
    }

    fun sequence(length: Int, lambda: (ByteWriter) -> Unit) {
        val r = (data.size - position) % length
        var index = position
        while (index < data.size - r) {
            lambda(ByteWriter(data.copyOfRange(index, index + length)))
            index += length
        }
        if (r > 0) {
            lambda(ByteWriter(data.copyOfRange(data.size - r, data.size)))
        }
    }

    fun toUInt32(): List<UInt> {
        val size = 4
        if ((data.size - position) % size != 0) {
            return emptyList()
        }
        val result = mutableListOf<UInt>()
        var index = position
        while (index < data.size) {
            result.add(ByteBuffer.wrap(data, index, size).order(ByteOrder.BIG_ENDIAN).int.toUInt())
            index += size
        }
        return result
    }
}
