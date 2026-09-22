package com.moblin.android.media.haishinkit.extension

data class AVFrameRateRange(
    val minFrameRate: Double,
    val maxFrameRate: Double
)

fun AVFrameRateRange.contains(frameRate: Double): Boolean {
    return frameRate in minFrameRate..maxFrameRate
}
