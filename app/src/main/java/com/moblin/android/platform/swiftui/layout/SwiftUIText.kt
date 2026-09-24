package com.moblin.android.platform.swiftui.layout

import androidx.compose.ui.graphics.Color
import com.moblin.android.view.utils.FontDesign
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class TextAlignment {
    leading,
    center,
    trailing,
}

internal data class ResolvedFont(
    val size: Double,
    val weight: Int,
    val design: FontDesign,
    val monospacedDigit: Boolean,
)

internal class FontMetricsInfo(
    val ascender: Double,
    val descender: Double,
    val leading: Double,
    val capHeight: Double,
)

internal class TextMeasure(
    val width: Double,
    val height: Double,
    val lineCount: Int,
    val truncated: Boolean,
    val firstBaseline: Double,
    val lastBaseline: Double,
    val fittingWidth: Double = width,
    val fittingHeight: Double = height,
)

internal interface TextMeasurer {
    val pixelLength: Double

    fun fontMetrics(font: ResolvedFont): FontMetricsInfo

    fun measure(
        text: String,
        font: ResolvedFont,
        scale: Double,
        width: Double?,
        maxLines: Int?,
        ellipsize: Boolean,
    ): TextMeasure
}

private const val fitTolerance = 0.01

internal class TextNode(
    private val text: String,
    private val font: ResolvedFont,
    private val color: Color,
    private val lineLimit: Int?,
    private val minimumScaleFactor: Double,
    private val alignment: TextAlignment,
    private val measurer: TextMeasurer,
) : Node() {
    private class Result(val size: LayoutSize, val scale: Double, val width: Double?, val maxLines: Int?)

    private val results = HashMap<Proposal, Result>()
    private val metrics by lazy { measurer.fontMetrics(font) }

    private fun lineHeight(): Double = metrics.ascender - metrics.descender + metrics.leading

    override fun computeSpacing(): Spacing {
        val ideal = measurer.measure(text, font, 1.0, null, lineLimit, true)
        return Spacing.text(
            ascender = metrics.ascender,
            descender = metrics.descender,
            leading = metrics.leading,
            capHeight = metrics.capHeight,
            firstBaseline = ideal.firstBaseline,
            lastBaseline = ideal.lastBaseline,
            height = ideal.height,
            pixelLength = measurer.pixelLength,
        )
    }

    private fun finite(value: Double?): Double? = value?.takeIf { it.isFinite() }

    private fun fits(measure: TextMeasure, width: Double?, height: Double?): Boolean {
        if (lineLimit != null && measure.lineCount > lineLimit) {
            return false
        }
        if (height != null && measure.fittingHeight > height + fitTolerance) {
            return false
        }
        if (width != null && measure.fittingWidth > width + fitTolerance) {
            return false
        }
        return true
    }

    private fun measureAt(scale: Double, width: Double?): TextMeasure =
        measurer.measure(text, font, scale, width, null, false)

    private fun compute(proposal: Proposal): Result {
        val width = finite(proposal.width)
        val height = finite(proposal.height)
        var maxLines = lineLimit
        if (height != null) {
            val fitting = max(1, floor((height + fitTolerance) / lineHeight()).toInt())
            maxLines = min(maxLines ?: Int.MAX_VALUE, fitting)
        }
        if (text.isEmpty()) {
            return Result(LayoutSize(0.0, measurer.pixelLength), 1.0, width, maxLines)
        }
        if (minimumScaleFactor >= 1.0) {
            val measure = measurer.measure(text, font, 1.0, width, maxLines, true)
            return Result(LayoutSize(measure.width, measure.height), 1.0, width, maxLines)
        }
        val unscaled = measureAt(1.0, width)
        if (fits(unscaled, width, height)) {
            return Result(LayoutSize(unscaled.width, unscaled.height), 1.0, width, null)
        }
        val minimum = max(minimumScaleFactor, 0.0001)
        val scale = if (!fits(measureAt(minimum, width), width, height)) {
            minimum
        } else if (lineLimit == 1 && width != null && height == null) {
            singleLineScale(width, minimum)
        } else {
            var low = minimum
            var high = 1.0
            repeat(16) {
                val middle = (low + high) / 2
                if (fits(measureAt(middle, width), width, height)) {
                    low = middle
                } else {
                    high = middle
                }
            }
            low
        }
        val scaledMaxLines = if (height != null) {
            val fitting = max(1, floor((height + fitTolerance) / (lineHeight() * scale)).toInt())
            min(lineLimit ?: Int.MAX_VALUE, fitting)
        } else {
            lineLimit
        }
        val measure = measurer.measure(text, font, scale, width, scaledMaxLines, true)
        return Result(LayoutSize(measure.width, measure.height), scale, width, scaledMaxLines)
    }

    private fun singleLineScale(width: Double, minimum: Double): Double {
        val ideal = measureAt(1.0, null).fittingWidth
        if (ideal <= 0.0) {
            return 1.0
        }
        var scale = min(1.0, width / ideal)
        repeat(20) {
            if (scale <= minimum) {
                return minimum
            }
            if (fits(measureAt(scale, width), width, null)) {
                return scale
            }
            scale *= 0.995
        }
        return max(minimum, scale)
    }

    private fun result(proposal: Proposal): Result = results.getOrPut(proposal) { compute(proposal) }

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        if (proposal.width == 0.0 && proposal.height == 0.0) {
            return LayoutSize.zero
        }
        return result(proposal).size
    }

    override fun lengthThatFits(proposal: Proposal, axis: Axis): Double {
        if (axis == Axis.horizontal && proposal.width == 0.0) {
            return 0.0
        }
        return super.lengthThatFits(proposal, axis)
    }

    override fun draw(context: DrawContext) {
        if (width <= 0.0 || height <= 0.0) {
            return
        }
        val result = result(placedProposal)
        context.drawText(
            TextDrawRequest(
                text = text,
                font = font,
                color = color,
                scale = result.scale,
                layoutWidth = result.width,
                maxLines = result.maxLines,
                alignment = alignment,
                x = x,
                y = y,
                width = width,
                height = height,
            ),
        )
    }
}
