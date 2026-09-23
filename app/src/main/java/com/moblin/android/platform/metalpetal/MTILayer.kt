package com.moblin.android.platform.metalpetal

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize

enum class MTIColorComponent(val rawValue: Int) {
    red(0),
    green(1),
    blue(2),
    alpha(3),
}

enum class MTIMaskMode {
    normal,
    oneMinusMaskValue,
}

class MTIMask(
    val content: MTIImage,
    val component: MTIColorComponent = MTIColorComponent.red,
    val mode: MTIMaskMode = MTIMaskMode.normal,
)

class MTICornerRadius(val topLeft: Float, val topRight: Float, val bottomRight: Float, val bottomLeft: Float) {
    constructor(r: Float) : this(r, r, r, r)
    constructor(r: Double) : this(r.toFloat())

    val isZero: Boolean
        get() = topLeft == 0f && topRight == 0f && bottomRight == 0f && bottomLeft == 0f

    override fun equals(other: Any?): Boolean {
        return other is MTICornerRadius && topLeft == other.topLeft && topRight == other.topRight &&
            bottomRight == other.bottomRight && bottomLeft == other.bottomLeft
    }

    override fun hashCode(): Int {
        return listOf(topLeft, topRight, bottomRight, bottomLeft).hashCode()
    }
}

enum class MTICornerCurve {
    circular,
    continuous,
}

class MTIBlendMode private constructor(val rawValue: String) {
    override fun equals(other: Any?): Boolean {
        return other is MTIBlendMode && rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    override fun toString(): String {
        return rawValue
    }

    companion object {
        val normal = MTIBlendMode("Normal")
        val multiply = MTIBlendMode("Multiply")
        val screen = MTIBlendMode("Screen")
        val overlay = MTIBlendMode("Overlay")
        val darken = MTIBlendMode("Darken")
        val lighten = MTIBlendMode("Lighten")
        val colorDodge = MTIBlendMode("ColorDodge")
        val colorBurn = MTIBlendMode("ColorBurn")
        val softLight = MTIBlendMode("SoftLight")
        val hardLight = MTIBlendMode("HardLight")
        val difference = MTIBlendMode("Difference")
        val exclusion = MTIBlendMode("Exclusion")
        val hue = MTIBlendMode("Hue")
        val saturation = MTIBlendMode("Saturation")
        val color = MTIBlendMode("Color")
        val luminosity = MTIBlendMode("Luminosity")
        val add = MTIBlendMode("Add")
        val linearDodge = MTIBlendMode("LinearDodge")
        val subtract = MTIBlendMode("Subtract")
        val divide = MTIBlendMode("Divide")
    }
}

class MTILayer(
    val content: MTIImage,
    contentRegion: CGRect? = null,
    val contentFlipOptions: FlipOptions = FlipOptions.donotFlip,
    val mask: MTIMask? = null,
    val compositingMask: MTIMask? = null,
    val layoutUnit: LayoutUnit = LayoutUnit.pixel,
    position: CGPoint? = null,
    size: CGSize? = null,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val cornerRadius: MTICornerRadius = MTICornerRadius(0f),
    val cornerCurve: MTICornerCurve = MTICornerCurve.circular,
    val tintColor: MTIColor = MTIColor.clear,
    val blendMode: MTIBlendMode = MTIBlendMode.normal,
) {
    val contentRegion: CGRect = contentRegion ?: content.extent
    val position: CGPoint = position ?: CGPoint(content.size.width / 2, content.size.height / 2)
    val size: CGSize = size ?: content.size

    enum class LayoutUnit {
        pixel,
        fractionOfBackgroundSize,
    }

    class FlipOptions(val rawValue: Int) {
        operator fun plus(other: FlipOptions): FlipOptions {
            return FlipOptions(rawValue or other.rawValue)
        }

        fun union(other: FlipOptions): FlipOptions {
            return plus(other)
        }

        fun contains(o: FlipOptions): Boolean {
            return rawValue and o.rawValue == o.rawValue
        }

        val isEmpty: Boolean
            get() = rawValue == 0

        override fun equals(other: Any?): Boolean {
            return other is FlipOptions && rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            return rawValue
        }

        companion object {
            val donotFlip = FlipOptions(0)
            val flipVertically = FlipOptions(1)
            val flipHorizontally = FlipOptions(2)
        }
    }

    fun sizeInPixel(forBackgroundSize: CGSize): CGSize {
        return when (layoutUnit) {
            LayoutUnit.pixel -> size
            LayoutUnit.fractionOfBackgroundSize -> CGSize(
                forBackgroundSize.width * size.width,
                forBackgroundSize.height * size.height
            )
        }
    }

    fun positionInPixel(forBackgroundSize: CGSize): CGPoint {
        return when (layoutUnit) {
            LayoutUnit.pixel -> position
            LayoutUnit.fractionOfBackgroundSize -> CGPoint(
                forBackgroundSize.width * position.x,
                forBackgroundSize.height * position.y
            )
        }
    }
}
