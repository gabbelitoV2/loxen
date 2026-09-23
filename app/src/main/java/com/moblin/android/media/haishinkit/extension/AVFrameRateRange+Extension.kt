package com.moblin.android.media.haishinkit.extension

import com.moblin.android.platform.avfoundation.AVFrameRateRange

fun AVFrameRateRange.contains(frameRate: Double): Boolean {
    return frameRate in minFrameRate..maxFrameRate
}
