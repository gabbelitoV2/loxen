package com.moblin.android.platform.coreimage

import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.moblin.android.platform.coregraphics.CGColor
import com.moblin.android.platform.coregraphics.CGColorSpace

class CIColor(
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double = 1.0,
    val colorSpace: CGColorSpace? = null,
) {
    constructor(red: Float, green: Float, blue: Float, alpha: Float = 1f) : this(
        red.toDouble(),
        green.toDouble(),
        blue.toDouble(),
        alpha.toDouble()
    )
    constructor(color: androidx.compose.ui.graphics.Color) : this(composeComponents(color))
    constructor(cgColor: CGColor) : this(cgColor.red, cgColor.green, cgColor.blue, cgColor.alpha)

    private constructor(values: DoubleArray) : this(values[0], values[1], values[2], values[3])

    val components: List<Double>
        get() = listOf(red, green, blue, alpha)

    val numberOfComponents: Int
        get() = 4

    val stringRepresentation: String
        get() = "$red $green $blue $alpha"

    override fun equals(other: Any?): Boolean {
        return other is CIColor && red == other.red && green == other.green && blue == other.blue &&
            alpha == other.alpha
    }

    override fun hashCode(): Int {
        return listOf(red, green, blue, alpha).hashCode()
    }

    override fun toString(): String {
        return "CIColor(red=$red, green=$green, blue=$blue, alpha=$alpha)"
    }

    companion object {
        val white = CIColor(1.0, 1.0, 1.0, 1.0)
        val black = CIColor(0.0, 0.0, 0.0, 1.0)
        val clear = CIColor(0.0, 0.0, 0.0, 0.0)
        val red = CIColor(1.0, 0.0, 0.0, 1.0)
        val green = CIColor(0.0, 1.0, 0.0, 1.0)
        val blue = CIColor(0.0, 0.0, 1.0, 1.0)
        val gray = CIColor(0.5, 0.5, 0.5, 1.0)
        val yellow = CIColor(1.0, 1.0, 0.0, 1.0)
        val cyan = CIColor(0.0, 1.0, 1.0, 1.0)
        val magenta = CIColor(1.0, 0.0, 1.0, 1.0)
    }
}

private fun composeComponents(color: androidx.compose.ui.graphics.Color): DoubleArray {
    val srgb = color.convert(ColorSpaces.Srgb)
    return doubleArrayOf(srgb.red.toDouble(), srgb.green.toDouble(), srgb.blue.toDouble(), srgb.alpha.toDouble())
}
