package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader

object OpusHeader {
    fun encode(length: Int): ByteArray {
        var header = ByteArray(2)
        val value = 0x3FF shl 5
        header[0] = (value and 0xFF).toByte()
        header[1] = ((value ushr 8) and 0xFF).toByte()
        var remaining = length
        while (remaining >= 0) {
            header += (if (remaining < 255) remaining.toByte() else 255.toByte())
            remaining -= 255
        }
        return header
    }

    fun decode(data: ByteArray): Pair<Int, Int>? {
        val reader = ByteReader(data)
        var length = 0
        return try {
            reader.readUInt16()
            var result: Pair<Int, Int>? = null
            while (true) {
                val value = reader.readUInt8().toInt()
                length += value
                if (value < 255) {
                    result = Pair(length, reader.position)
                    break
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}
