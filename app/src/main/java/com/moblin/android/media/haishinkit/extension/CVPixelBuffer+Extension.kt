package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.video.CVPixelBuffer as Image
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
