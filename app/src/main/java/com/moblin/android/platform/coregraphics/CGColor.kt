package com.moblin.android.platform.coregraphics

import androidx.compose.ui.graphics.colorspace.ColorSpaces

class CGColorSpace(val name: String) {
    override fun equals(other: Any?): Boolean {
        return other is CGColorSpace && name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "CGColorSpace($name)"
    }

    companion object {
        const val sRGB = "kCGColorSpaceSRGB"
        const val displayP3 = "kCGColorSpaceDisplayP3"
        const val itur_2020 = "kCGColorSpaceITUR_2020"
        const val itur_709 = "kCGColorSpaceITUR_709"
        const val extendedLinearSRGB = "kCGColorSpaceExtendedLinearSRGB"
        const val linearSRGB = "kCGColorSpaceLinearSRGB"
        const val genericGrayGamma2_2 = "kCGColorSpaceGenericGrayGamma2_2"
        const val deviceRGB = "kCGColorSpaceDeviceRGB"
        const val deviceGray = "kCGColorSpaceDeviceGray"
    }
}

fun CGColorSpaceCreateDeviceRGB(): CGColorSpace {
    return CGColorSpace(CGColorSpace.deviceRGB)
}

fun CGColorSpaceCreateDeviceGray(): CGColorSpace {
    return CGColorSpace(CGColorSpace.deviceGray)
}

class CGColor(val red: Double, val green: Double, val blue: Double, val alpha: Double = 1.0) {
    constructor(gray: Double, alpha: Double) : this(gray, gray, gray, alpha)
    constructor(red: Number, green: Number, blue: Number, alpha: Number = 1.0) : this(
        red.toDouble(),
        green.toDouble(),
        blue.toDouble(),
        alpha.toDouble()
    )
    constructor(color: androidx.compose.ui.graphics.Color) : this(srgbComponents(color))

    private constructor(values: DoubleArray) : this(values[0], values[1], values[2], values[3])

    val components: List<Double>
        get() = listOf(red, green, blue, alpha)

    val numberOfComponents: Int
        get() = 4

    fun copy(alpha: Double): CGColor {
        return CGColor(red, green, blue, alpha)
    }

    override fun equals(other: Any?): Boolean {
        return other is CGColor && red == other.red && green == other.green && blue == other.blue &&
            alpha == other.alpha
    }

    override fun hashCode(): Int {
        return listOf(red, green, blue, alpha).hashCode()
    }

    override fun toString(): String {
        return "CGColor(red=$red, green=$green, blue=$blue, alpha=$alpha)"
    }

    companion object {
        val white = CGColor(1.0, 1.0, 1.0, 1.0)
        val black = CGColor(0.0, 0.0, 0.0, 1.0)
        val clear = CGColor(0.0, 0.0, 0.0, 0.0)
    }
}

internal fun srgbComponents(color: androidx.compose.ui.graphics.Color): DoubleArray {
    val srgb = color.convert(ColorSpaces.Srgb)
    return doubleArrayOf(srgb.red.toDouble(), srgb.green.toDouble(), srgb.blue.toDouble(), srgb.alpha.toDouble())
}

enum class CGImageAlphaInfo(val rawValue: Int) {
    none(0),
    premultipliedLast(1),
    premultipliedFirst(2),
    last(3),
    first(4),
    noneSkipLast(5),
    noneSkipFirst(6),
    alphaOnly(7),
}
