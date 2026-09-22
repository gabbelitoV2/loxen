package com.moblin.android.media.haishinkit.extension

val Boolean.uint8: UByte
    get() = if (this) 1u.toUByte() else 0u.toUByte()
