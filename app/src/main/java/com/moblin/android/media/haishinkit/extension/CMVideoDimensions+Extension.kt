package com.moblin.android.media.haishinkit.extension

import android.util.Size
import android.util.SizeF

fun Size.isPortrait(): Boolean {
    return height > width
}

fun Size.aspectRatio(): Double {
    return width.toDouble() / height.toDouble()
}

fun Size.convertTo(dimension: Int): Size? {
    if (isPortrait()) {
        var height = this.height * dimension / this.width
        if (height % 2 == 1) {
            height += 1
        }
        return Size(dimension, height)
    } else {
        var width = this.width * dimension / this.height
        if (width % 2 == 1) {
            width += 1
        }
        return Size(width, dimension)
    }
}

fun Size.toSize(): SizeF {
    return SizeF(width.toFloat(), height.toFloat())
}
