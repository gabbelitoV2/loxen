package com.moblin.android.media.haishinkit.extension

import android.media.Image
import android.util.Size

val Image.width: Int
    get() = width

val Image.height: Int
    get() = height

val Image.size: Size
    get() = Size(width, height)

fun Image.isPortrait(): Boolean {
    return height > width
}
