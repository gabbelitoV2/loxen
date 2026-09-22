package com.moblin.android.media.haishinkit.extension

import java.nio.ByteBuffer
import java.nio.ByteOrder

val Byte.data: ByteArray
    get() = byteArrayOf(this)

val UByte.data: ByteArray
    get() = byteArrayOf(this.toByte())

val Short.data: ByteArray
    get() = ByteBuffer.allocate(Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(this).array()

val UShort.data: ByteArray
    get() = ByteBuffer.allocate(UShort.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(this.toShort()).array()

val Int.data: ByteArray
    get() = ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array()

val UInt.data: ByteArray
    get() = ByteBuffer.allocate(UInt.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(this.toInt()).array()

val Long.data: ByteArray
    get() = ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()

val ULong.data: ByteArray
    get() = ByteBuffer.allocate(ULong.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(this.toLong()).array()
