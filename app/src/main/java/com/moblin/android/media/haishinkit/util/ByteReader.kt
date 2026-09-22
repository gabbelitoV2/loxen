package com.moblin.android.media.haishinkit.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

class ByteReader(data: ByteArray) {
    companion object {
        const val sizeOfInt8: Int = 1
        const val sizeOfDouble: Int = 8
    }

    private sealed class Error : Exception() {
        object eof : Error()
        object utf8 : Error()
    }

    var data: ByteArray = data
        private set

    var position: Int = 0

    val bytesAvailable: Int
        get() = data.size - position

    fun readUInt8(): UByte {
        if (bytesAvailable < sizeOfInt8) {
            throw Error.eof
        }
        val value = data[position].toUByte()
        position += 1
        return value
    }

    fun readUInt16(): UShort {
        if (bytesAvailable < 2) {
            throw Error.eof
        }
        val value = ((data[position].toInt() and 0xFF) shl 8) or
            (data[position + 1].toInt() and 0xFF)
        position += 2
        return value.toUShort()
    }

    fun readUInt16Le(): UShort {
        val low = readUInt8().toUInt()
        val high = readUInt8().toUInt()
        return (low or (high shl 8)).toUShort()
    }

    fun readUInt24(): UInt {
        val high = readUInt8().toUInt()
        val middle = readUInt8().toUInt()
        val low = readUInt8().toUInt()
        return (high shl 16) or (middle shl 8) or low
    }

    fun readUInt24Le(): UInt {
        val low = readUInt8().toUInt()
        val middle = readUInt8().toUInt()
        val high = readUInt8().toUInt()
        return low or (middle shl 8) or (high shl 16)
    }

    fun readUInt32(): UInt {
        if (bytesAvailable < 4) {
            throw Error.eof
        }
        val value = ((data[position].toInt() and 0xFF).toUInt() shl 24) or
            ((data[position + 1].toInt() and 0xFF).toUInt() shl 16) or
            ((data[position + 2].toInt() and 0xFF).toUInt() shl 8) or
            (data[position + 3].toInt() and 0xFF).toUInt()
        position += 4
        return value
    }

    fun readUInt32Le(): UInt {
        val b0 = readUInt8().toUInt()
        val b1 = readUInt8().toUInt()
        val b2 = readUInt8().toUInt()
        val b3 = readUInt8().toUInt()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readUInt64(): ULong {
        val high = readUInt32()
        val low = readUInt32()
        return (high.toULong() shl 32) or low.toULong()
    }

    fun readDouble(): Double {
        if (bytesAvailable < sizeOfDouble) {
            throw Error.eof
        }
        position += sizeOfDouble
        val start = position - sizeOfDouble
        val reversed = ByteArray(sizeOfDouble)
        for (index in 0 until sizeOfDouble) {
            reversed[index] = data[start + sizeOfDouble - 1 - index]
        }
        return ByteBuffer.wrap(reversed).order(ByteOrder.LITTLE_ENDIAN).double
    }

    fun readUtf8Bytes(length: Int): String {
        if (bytesAvailable < length) {
            throw Error.eof
        }
        val start = position
        position += length
        val bytes = data.copyOfRange(start, start + length)
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            throw Error.utf8
        }
    }

    fun readBytes(length: Int): ByteArray {
        if (length < 0 || bytesAvailable < length) {
            throw Error.eof
        }
        val start = position
        position += length
        return data.copyOfRange(start, start + length)
    }

    fun skipBytes(length: Int) {
        if (length < 0 || bytesAvailable < length) {
            throw Error.eof
        }
        position += length
    }
}
