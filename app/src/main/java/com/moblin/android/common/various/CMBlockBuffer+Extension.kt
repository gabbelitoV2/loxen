package com.moblin.android.common.various

import java.nio.ByteBuffer

class CMBlockBuffer(bytes: ByteArray) {

    private val bytes: ByteArray = bytes.copyOf()

    val byteBuffer: ByteBuffer
        get() = ByteBuffer.wrap(bytes).asReadOnlyBuffer()
}

val CMBlockBuffer.data: ByteArray?
    get() {
        val pointer = getDataPointer() ?: return null
        val buffer = pointer.first
        val length = pointer.second
        if (length == 0) {
            return ByteArray(0)
        }
        val out = ByteArray(length)
        val source = buffer.duplicate()
        source.position(0)
        source.get(out, 0, length)
        return out
    }

fun CMBlockBuffer.getDataPointer(): Pair<ByteBuffer, Int>? {
    val buffer = byteBuffer
    return Pair(buffer, buffer.remaining())
}
