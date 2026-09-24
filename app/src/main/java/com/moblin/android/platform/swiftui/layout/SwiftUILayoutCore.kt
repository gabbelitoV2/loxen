package com.moblin.android.platform.swiftui.layout

import androidx.compose.ui.graphics.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal const val defaultSpacingValue = 8.0

internal enum class Axis {
    horizontal,
    vertical,
    ;

    val other: Axis
        get() = if (this == horizontal) vertical else horizontal
}

internal data class Proposal(val width: Double?, val height: Double?) {
    operator fun get(axis: Axis): Double? = if (axis == Axis.horizontal) width else height

    fun replacingUnspecifiedDimensions(size: Double = 10.0): LayoutSize = LayoutSize(width ?: size, height ?: size)

    fun inset(horizontal: Double, vertical: Double): Proposal = Proposal(
        width?.let { max(it - horizontal, 0.0) },
        height?.let { max(it - vertical, 0.0) },
    )

    companion object {
        val unspecified = Proposal(null, null)
        val zero = Proposal(0.0, 0.0)

        fun of(major: Double?, axis: Axis, minor: Double?): Proposal = if (axis == Axis.horizontal) {
            Proposal(major, minor)
        } else {
            Proposal(minor, major)
        }
    }
}

internal data class LayoutSize(val width: Double, val height: Double) {
    operator fun get(axis: Axis): Double = if (axis == Axis.horizontal) width else height

    companion object {
        val zero = LayoutSize(0.0, 0.0)

        fun of(major: Double, axis: Axis, minor: Double): LayoutSize = if (axis == Axis.horizontal) {
            LayoutSize(major, minor)
        } else {
            LayoutSize(minor, major)
        }
    }
}

internal fun roundUp(value: Double, multiple: Double): Double {
    if (multiple <= 0.0) {
        return value
    }
    return ceil(value / multiple - 1e-9) * multiple
}

internal fun roundToNearestOrUp(value: Double, multiple: Double): Double {
    if (multiple <= 0.0) {
        return value
    }
    return floor((value + multiple / 2) / multiple + 1e-9) * multiple
}

internal enum class SpacingEdge { top, left, bottom, right }

internal enum class SpacingCategory {
    textToText,
    edgeAboveText,
    edgeBelowText,
    textBaseline,
    edgeLeftText,
    edgeRightText,
}

internal data class SpacingKey(val category: SpacingCategory?, val edge: SpacingEdge)

internal class TextMetrics(
    val ascend: Double,
    val descend: Double,
    val leading: Double,
    val pixelLength: Double,
) {
    val lineSpacing: Double
        get() = ascend + descend + leading

    fun isAlmostEqual(other: TextMetrics): Boolean = almostEqual(ascend, other.ascend) &&
        almostEqual(descend, other.descend) &&
        almostEqual(leading, other.leading)

    companion object {
        private fun almostEqual(a: Double, b: Double): Boolean = kotlin.math.abs(a - b) <= 1e-6 * max(1.0, max(kotlin.math.abs(a), kotlin.math.abs(b)))

        fun spacing(top: TextMetrics, bottom: TextMetrics): Double {
            var result = bottom.leading
            if (!top.isAlmostEqual(bottom)) {
                result = top.descend + bottom.lineSpacing - bottom.descend - top.descend - bottom.ascend
            }
            return roundUp(result, top.pixelLength)
        }
    }
}

internal sealed class SpacingValue : Comparable<SpacingValue> {
    class Distance(val value: Double) : SpacingValue()

    class TopTextMetrics(val metrics: TextMetrics) : SpacingValue()

    class BottomTextMetrics(val metrics: TextMetrics) : SpacingValue()

    val distanceValue: Double?
        get() = (this as? Distance)?.value

    fun distance(to: SpacingValue): Double? = when {
        this is Distance && to is Distance -> value + to.value
        this is Distance -> value
        to is Distance -> to.value
        this is TopTextMetrics && to is BottomTextMetrics -> TextMetrics.spacing(metrics, to.metrics)
        this is BottomTextMetrics && to is TopTextMetrics -> TextMetrics.spacing(to.metrics, metrics)
        else -> null
    }

    private val rank: Int
        get() = when (this) {
            is Distance -> 0
            is TopTextMetrics -> 1
            is BottomTextMetrics -> 2
        }

    override fun compareTo(other: SpacingValue): Int {
        if (rank != other.rank) {
            return rank.compareTo(other.rank)
        }
        return when (this) {
            is Distance -> value.compareTo((other as Distance).value)
            is TopTextMetrics -> metrics.lineSpacing.compareTo((other as TopTextMetrics).metrics.lineSpacing)
            is BottomTextMetrics -> metrics.lineSpacing.compareTo((other as BottomTextMetrics).metrics.lineSpacing)
        }
    }
}

internal class Spacing(val minima: MutableMap<SpacingKey, SpacingValue>) {
    fun copy(): Spacing = Spacing(minima.toMutableMap())

    fun incorporate(edges: Set<SpacingEdge>, other: Spacing) {
        if (edges.isEmpty()) {
            return
        }
        for ((key, value) in other.minima) {
            if (key.edge !in edges) {
                continue
            }
            val existing = minima[key]
            minima[key] = if (existing == null || existing <= value) value else existing
        }
    }

    fun reset(edges: Set<SpacingEdge>) {
        if (edges.isEmpty()) {
            return
        }
        minima.keys.removeAll { it.edge in edges }
        if (SpacingEdge.top in edges) {
            minima[SpacingKey(SpacingCategory.edgeBelowText, SpacingEdge.top)] = SpacingValue.Distance(0.0)
        }
        if (SpacingEdge.left in edges) {
            minima[SpacingKey(SpacingCategory.edgeRightText, SpacingEdge.left)] = SpacingValue.Distance(0.0)
        }
        if (SpacingEdge.bottom in edges) {
            minima[SpacingKey(SpacingCategory.edgeAboveText, SpacingEdge.bottom)] = SpacingValue.Distance(0.0)
        }
        if (SpacingEdge.right in edges) {
            minima[SpacingKey(SpacingCategory.edgeLeftText, SpacingEdge.right)] = SpacingValue.Distance(0.0)
        }
    }

    fun distanceToSuccessor(axis: Axis, next: Spacing): Double? {
        val bottomTrailing = if (axis == Axis.horizontal) SpacingEdge.right else SpacingEdge.bottom
        val topLeading = if (axis == Axis.horizontal) SpacingEdge.left else SpacingEdge.top
        return if (minima.size >= next.minima.size) {
            next.distance(topLeading, bottomTrailing, this)
        } else {
            distance(bottomTrailing, topLeading, next)
        }
    }

    private fun distance(fromEdge: SpacingEdge, toEdge: SpacingEdge, next: Spacing): Double? {
        var hasValue = false
        var result = Double.NEGATIVE_INFINITY
        for ((key, value) in minima) {
            val category = key.category ?: continue
            if (key.edge != fromEdge) {
                continue
            }
            val nextValue = next.minima[SpacingKey(category, toEdge)] ?: continue
            val newDistance = value.distance(nextValue) ?: continue
            hasValue = true
            result = max(result, newDistance)
        }
        if (hasValue) {
            return result
        }
        val fromValue = minima[SpacingKey(null, fromEdge)]?.distanceValue
        val toValue = next.minima[SpacingKey(null, toEdge)]?.distanceValue
        if (fromValue == null && toValue == null) {
            return null
        }
        return max(fromValue ?: Double.NEGATIVE_INFINITY, toValue ?: Double.NEGATIVE_INFINITY)
    }

    fun distanceTo(next: Spacing, axis: Axis): Double = distanceToSuccessor(axis, next) ?: defaultSpacingValue

    companion object {
        fun standard(): Spacing = Spacing(
            mutableMapOf(
                SpacingKey(SpacingCategory.edgeBelowText, SpacingEdge.top) to SpacingValue.Distance(0.0),
                SpacingKey(SpacingCategory.edgeAboveText, SpacingEdge.bottom) to SpacingValue.Distance(0.0),
                SpacingKey(SpacingCategory.edgeRightText, SpacingEdge.left) to SpacingValue.Distance(0.0),
                SpacingKey(SpacingCategory.edgeLeftText, SpacingEdge.right) to SpacingValue.Distance(0.0),
            ),
        )

        fun empty(): Spacing = Spacing(mutableMapOf())

        fun zero(): Spacing = Spacing(
            SpacingEdge.entries.associate { SpacingKey(null, it) to SpacingValue.Distance(0.0) }.toMutableMap(),
        )

        fun horizontalZero(): Spacing = Spacing(
            mutableMapOf(
                SpacingKey(null, SpacingEdge.left) to SpacingValue.Distance(0.0),
                SpacingKey(null, SpacingEdge.right) to SpacingValue.Distance(0.0),
            ),
        )

        fun verticalZero(): Spacing = Spacing(
            mutableMapOf(
                SpacingKey(null, SpacingEdge.top) to SpacingValue.Distance(0.0),
                SpacingKey(null, SpacingEdge.bottom) to SpacingValue.Distance(0.0),
            ),
        )

        fun text(
            ascender: Double,
            descender: Double,
            leading: Double,
            capHeight: Double,
            firstBaseline: Double,
            lastBaseline: Double,
            height: Double,
            pixelLength: Double,
        ): Spacing {
            val fontLineHeight = ascender - descender
            val defaultTextSpacing = roundUp(fontLineHeight * 0.1, pixelLength)
            val metrics = TextMetrics(
                ascend = ascender,
                descend = -descender,
                leading = leading,
                pixelLength = pixelLength,
            )
            val defaultLineSpacing = fontLineHeight + defaultTextSpacing
            return Spacing(
                mutableMapOf(
                    SpacingKey(SpacingCategory.textToText, SpacingEdge.top) to SpacingValue.BottomTextMetrics(metrics),
                    SpacingKey(SpacingCategory.textToText, SpacingEdge.bottom) to SpacingValue.TopTextMetrics(metrics),
                    SpacingKey(SpacingCategory.textBaseline, SpacingEdge.bottom) to
                        SpacingValue.Distance(lastBaseline - height),
                    SpacingKey(SpacingCategory.textBaseline, SpacingEdge.top) to SpacingValue.Distance(-firstBaseline),
                    SpacingKey(SpacingCategory.edgeAboveText, SpacingEdge.top) to
                        SpacingValue.Distance(defaultLineSpacing - metrics.ascend),
                    SpacingKey(SpacingCategory.edgeBelowText, SpacingEdge.bottom) to SpacingValue.Distance(
                        max(defaultLineSpacing - capHeight, defaultTextSpacing + metrics.descend),
                    ),
                ),
            )
        }
    }
}

internal fun edgesOf(top: Boolean, leading: Boolean, bottom: Boolean, trailing: Boolean): Set<SpacingEdge> {
    val edges = mutableSetOf<SpacingEdge>()
    if (top) {
        edges.add(SpacingEdge.top)
    }
    if (leading) {
        edges.add(SpacingEdge.left)
    }
    if (bottom) {
        edges.add(SpacingEdge.bottom)
    }
    if (trailing) {
        edges.add(SpacingEdge.right)
    }
    return edges
}

internal sealed class ShapeKind {
    data object Rectangle : ShapeKind()

    data class RoundedRectangle(val cornerRadius: Double) : ShapeKind()

    data object Circle : ShapeKind()
}

internal sealed class ShapePaint {
    data class Fill(val color: Color) : ShapePaint()

    data class Stroke(val color: Color, val lineWidth: Double) : ShapePaint()
}

internal class TextDrawRequest(
    val text: String,
    val font: ResolvedFont,
    val color: Color,
    val scale: Double,
    val layoutWidth: Double?,
    val maxLines: Int?,
    val alignment: TextAlignment,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

internal interface DrawContext {
    fun drawShape(kind: ShapeKind, paint: ShapePaint, x: Double, y: Double, width: Double, height: Double)

    fun drawText(request: TextDrawRequest)

    fun drawImage(source: ImageSource, tint: Color?, x: Double, y: Double, width: Double, height: Double)

    fun drawPie(sectors: List<Pair<Double, Color>>, x: Double, y: Double, width: Double, height: Double)

    fun save()

    fun restore()

    fun translate(dx: Double, dy: Double)

    fun rotate(degrees: Double, pivotX: Double, pivotY: Double)

    fun beginOpacity(alpha: Double)

    fun endOpacity()

    fun beginClip(kind: ShapeKind, x: Double, y: Double, width: Double, height: Double)

    fun endClip()
}

internal abstract class Node {
    var x = 0.0
        private set
    var y = 0.0
        private set
    var width = 0.0
        private set
    var height = 0.0
        private set
    var placedProposal = Proposal.unspecified
        private set
    private val sizeCache = HashMap<Proposal, LayoutSize>()
    private var spacingCache: Spacing? = null

    open val layoutPriority: Double
        get() = 0.0

    open val requiresSpacingProjection: Boolean
        get() = false

    fun size(proposal: Proposal): LayoutSize = sizeCache.getOrPut(proposal) { sizeThatFits(proposal) }

    open fun lengthThatFits(proposal: Proposal, axis: Axis): Double = size(proposal)[axis]

    fun spacing(): Spacing {
        val spacing = spacingCache ?: computeSpacing().also { spacingCache = it }
        return spacing.copy()
    }

    protected abstract fun sizeThatFits(proposal: Proposal): LayoutSize

    protected open fun computeSpacing(): Spacing = Spacing.standard()

    fun place(x: Double, y: Double, size: LayoutSize, proposal: Proposal) {
        this.x = x
        this.y = y
        width = size.width
        height = size.height
        placedProposal = proposal
        placeChildren(proposal)
    }

    protected open fun placeChildren(proposal: Proposal) {}

    abstract fun draw(context: DrawContext)
}

internal class StackNode(
    private val majorAxis: Axis,
    private val alignment: Double,
    private val uniformSpacing: Double?,
    private val children: List<Node>,
) : Node() {
    private val minorAxis = majorAxis.other

    private class Child(val node: Node, val priority: Double, val distanceToPrevious: Double, var fittingOrder: Int) {
        var min: Double? = null
        var max: Double? = null
        var originMajor = 0.0
        var originMinor = 0.0
        var size = LayoutSize.zero
        var proposal = Proposal.unspecified
    }

    private val items: List<Child>
    private val internalSpacing: Double
    private var lastProposal = Proposal(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
    private var stackSize = LayoutSize.zero

    init {
        var total = 0.0
        var previousSpacing: Spacing? = null
        items = children.mapIndexed { index, node ->
            val spacing = node.spacing()
            val distance = if (index == 0) {
                0.0
            } else {
                uniformSpacing ?: previousSpacing!!.distanceTo(spacing, majorAxis)
            }
            if (index > 0) {
                total += distance
            }
            previousSpacing = spacing
            Child(node, node.layoutPriority, distance, index)
        }
        internalSpacing = total
    }

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        placeChildrenIn(proposal)
        return stackSize
    }

    override fun computeSpacing(): Spacing {
        val spacing = if (items.isEmpty()) Spacing.zero() else Spacing.empty()
        for ((index, child) in items.withIndex()) {
            val edges = mutableSetOf<SpacingEdge>()
            if (majorAxis == Axis.horizontal) {
                edges.add(SpacingEdge.top)
                edges.add(SpacingEdge.bottom)
                if (index == 0) {
                    edges.add(SpacingEdge.left)
                }
                if (index == items.size - 1) {
                    edges.add(SpacingEdge.right)
                }
            } else {
                edges.add(SpacingEdge.left)
                edges.add(SpacingEdge.right)
                if (index == 0) {
                    edges.add(SpacingEdge.top)
                }
                if (index == items.size - 1) {
                    edges.add(SpacingEdge.bottom)
                }
            }
            spacing.incorporate(edges, child.node.spacing())
        }
        return spacing
    }

    override fun placeChildren(proposal: Proposal) {
        val placementProposal = if (majorAxis == Axis.horizontal) {
            Proposal(proposal.width, proposal.height ?: height)
        } else {
            Proposal(proposal.width ?: width, proposal.height)
        }
        placeChildrenIn(placementProposal)
        var current = if (majorAxis == Axis.horizontal) x else y
        for (child in items) {
            val majorOrigin = current + child.distanceToPrevious
            current = majorOrigin + child.size[majorAxis]
            val finalMajor = if (majorOrigin.isNaN()) child.originMajor else majorOrigin
            if (majorAxis == Axis.horizontal) {
                child.node.place(finalMajor, child.originMinor + y, child.size, child.proposal)
            } else {
                child.node.place(child.originMinor + x, finalMajor, child.size, child.proposal)
            }
        }
    }

    private fun placeChildrenIn(proposal: Proposal) {
        if (proposal == lastProposal || items.isEmpty()) {
            return
        }
        if (proposal[majorAxis] != null) {
            sizeChildrenGenerally(proposal)
        } else {
            sizeChildrenIdeally(proposal)
        }
        var lower = 0.0
        var upper = 0.0
        for (child in items) {
            lower = min(lower, child.originMinor)
            upper = max(upper, child.originMinor + child.size[minorAxis])
        }
        var majorValue = 0.0
        for (child in items) {
            val majorOrigin = majorValue + child.distanceToPrevious
            if (!majorOrigin.isNaN()) {
                child.originMajor = majorOrigin
            }
            val minorOrigin = child.originMinor - lower
            if (!minorOrigin.isNaN()) {
                child.originMinor = minorOrigin
            }
            majorValue = majorOrigin + child.size[majorAxis]
        }
        stackSize = LayoutSize.of(majorValue, majorAxis, upper - lower)
        lastProposal = proposal
    }

    private fun setGeometry(child: Child, proposal: Proposal) {
        val size = child.node.size(proposal)
        child.size = size
        child.proposal = proposal
        child.originMajor = 0.0
        child.originMinor = -(alignment * size[minorAxis])
    }

    private fun sizeChildrenIdeally(proposal: Proposal) {
        for (child in items) {
            setGeometry(child, Proposal.of(null, majorAxis, proposal[minorAxis]))
        }
    }

    private fun sizeChildrenGenerally(proposal: Proposal) {
        prioritize(proposal)
        val count = items.size
        var available = proposal[majorAxis]!! - internalSpacing
        var index = 0
        while (index != count) {
            val fittingOrder = items[index].fittingOrder
            val fittingPriority = items[fittingOrder].priority
            val targetIndex = if (!fittingPriority.isNaN()) {
                (index + 1 until count).firstOrNull { items[items[it].fittingOrder].priority != fittingPriority }
                    ?: count
            } else {
                index
            }
            if (fittingOrder == items[0].fittingOrder) {
                var total = 0.0
                for (position in targetIndex until count) {
                    total += items[items[position].fittingOrder].min ?: 0.0
                }
                available -= total
            } else {
                var total = 0.0
                for (position in index until targetIndex) {
                    total += items[items[position].fittingOrder].min ?: 0.0
                }
                available += total
            }
            if (targetIndex != index) {
                var remaining = targetIndex - index
                while (true) {
                    val current = items[items[index].fittingOrder]
                    val evenSplit = max(available / remaining, 0.0)
                    setGeometry(current, Proposal.of(evenSplit, majorAxis, proposal[minorAxis]))
                    val taken = current.size[majorAxis]
                    val next = available - taken
                    available = if (next.isNaN()) available else next
                    remaining -= 1
                    if (remaining == 0) {
                        break
                    }
                    index += 1
                }
            }
            index = targetIndex
        }
    }

    private fun minLength(child: Child, minorProposal: Double?): Double = child.min
        ?: child.node.lengthThatFits(Proposal.of(0.0, majorAxis, minorProposal), majorAxis).also { child.min = it }

    private fun maxLength(child: Child, minorProposal: Double?): Double = child.max
        ?: child.node.lengthThatFits(Proposal.of(Double.POSITIVE_INFINITY, majorAxis, minorProposal), majorAxis)
            .also { child.max = it }

    private fun lessFlexible(minA: Double, maxA: Double, minB: Double, maxB: Double): Boolean {
        val diffA = maxA - minA
        val diffB = maxB - minB
        val effectiveMinA = if (diffA == Double.POSITIVE_INFINITY) -minA else 0.0
        val effectiveMinB = if (diffB == Double.POSITIVE_INFINITY) -minB else 0.0
        return if (diffA == diffB) effectiveMinA < effectiveMinB else diffA < diffB
    }

    private fun areInDecreasingFittingPriority(first: Int, second: Int, minorProposal: Double?): Boolean {
        val priorityFirst = items[first].priority
        val prioritySecond = items[second].priority
        if (priorityFirst != prioritySecond) {
            return priorityFirst > prioritySecond
        }
        return lessFlexible(
            minLength(items[first], minorProposal),
            maxLength(items[first], minorProposal),
            minLength(items[second], minorProposal),
            maxLength(items[second], minorProposal),
        )
    }

    private fun prioritize(proposal: Proposal) {
        if (proposal[minorAxis] == lastProposal[minorAxis] && lastProposal[majorAxis] != null) {
            return
        }
        for (child in items) {
            child.min = null
            child.max = null
        }
        val minorProposal = proposal[minorAxis]
        val order = items.map { it.fittingOrder }.toMutableList()
        for (i in 1 until order.size) {
            val value = order[i]
            var j = i
            while (j > 0 && areInDecreasingFittingPriority(value, order[j - 1], minorProposal)) {
                order[j] = order[j - 1]
                j -= 1
            }
            order[j] = value
        }
        for ((position, value) in order.withIndex()) {
            items[position].fittingOrder = value
        }
        val firstPriority = items[order[0]].priority
        for (position in order.indices.reversed()) {
            val child = items[order[position]]
            if (child.priority == firstPriority) {
                break
            }
            if (child.min == null) {
                minLength(child, minorProposal)
            }
        }
    }

    override fun draw(context: DrawContext) {
        for (child in items) {
            child.node.draw(context)
        }
    }
}

internal class ZStackNode(
    private val alignmentX: Double,
    private val alignmentY: Double,
    private val children: List<Node>,
) : Node() {
    private val maxPriority = children.maxOfOrNull { it.layoutPriority } ?: 0.0

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        if (children.isEmpty()) {
            return LayoutSize.zero
        }
        var width = 0.0
        var height = 0.0
        for (child in children) {
            if (child.layoutPriority != maxPriority) {
                continue
            }
            val size = child.size(proposal)
            width = max(width, size.width)
            height = max(height, size.height)
        }
        return LayoutSize(width, height)
    }

    override fun computeSpacing(): Spacing {
        if (children.isEmpty()) {
            return Spacing.zero()
        }
        val spacing = Spacing.empty()
        for (child in children) {
            spacing.incorporate(SpacingEdge.entries.toSet(), child.spacing())
        }
        return spacing
    }

    override fun placeChildren(proposal: Proposal) {
        val childProposal = Proposal(width, height)
        var alignWidth = Double.NEGATIVE_INFINITY
        var alignHeight = Double.NEGATIVE_INFINITY
        for (child in children) {
            if (child.layoutPriority != maxPriority) {
                continue
            }
            val size = child.size(childProposal)
            alignWidth = max(alignWidth, alignmentX * size.width)
            alignHeight = max(alignHeight, alignmentY * size.height)
        }
        for (child in children) {
            val size = child.size(childProposal)
            child.place(
                alignWidth - alignmentX * size.width + x,
                alignHeight - alignmentY * size.height + y,
                size,
                childProposal,
            )
        }
    }

    override fun draw(context: DrawContext) {
        for (child in children) {
            child.draw(context)
        }
    }
}

internal class SpacerNode(private val minLength: Double?, private val orientation: Axis?) : Node() {
    override val layoutPriority: Double
        get() = Double.NEGATIVE_INFINITY

    override val requiresSpacingProjection: Boolean
        get() = true

    override fun computeSpacing(): Spacing = when (orientation) {
        null -> Spacing.zero()
        Axis.horizontal -> Spacing.horizontalZero()
        Axis.vertical -> Spacing.verticalZero()
    }

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        val value = minLength ?: defaultSpacingValue
        return when (orientation) {
            Axis.horizontal -> LayoutSize(max(proposal.width ?: Double.NEGATIVE_INFINITY, value), 0.0)
            Axis.vertical -> LayoutSize(0.0, max(proposal.height ?: Double.NEGATIVE_INFINITY, value))
            null -> LayoutSize(
                max(proposal.width ?: Double.NEGATIVE_INFINITY, value),
                max(proposal.height ?: Double.NEGATIVE_INFINITY, value),
            )
        }
    }

    override fun draw(context: DrawContext) {}
}

internal abstract class UnaryNode(protected val child: Node) : Node() {
    override fun computeSpacing(): Spacing = child.spacing()

    override fun draw(context: DrawContext) {
        child.draw(context)
    }
}

internal class PaddingNode(
    child: Node,
    private val top: Double,
    private val leading: Double,
    private val bottom: Double,
    private val trailing: Double,
) : UnaryNode(child) {
    private fun childProposal(proposal: Proposal): Proposal = proposal.inset(leading + trailing, top + bottom)

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        val size = child.size(childProposal(proposal))
        return LayoutSize(size.width + leading + trailing, size.height + top + bottom)
    }

    override fun computeSpacing(): Spacing {
        val spacing = child.spacing()
        spacing.reset(edgesOf(top != 0.0, leading != 0.0, bottom != 0.0, trailing != 0.0))
        return spacing
    }

    override fun placeChildren(proposal: Proposal) {
        val childProposal = childProposal(proposal)
        child.place(x + leading, y + top, child.size(childProposal), childProposal)
    }
}

internal class FrameNode(
    child: Node,
    private val frameWidth: Double?,
    private val frameHeight: Double?,
    private val alignmentX: Double,
    private val alignmentY: Double,
) : UnaryNode(child) {
    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        if (frameWidth != null && frameHeight != null) {
            return LayoutSize(frameWidth, frameHeight)
        }
        val size = child.size(Proposal(frameWidth ?: proposal.width, frameHeight ?: proposal.height))
        return LayoutSize(frameWidth ?: size.width, frameHeight ?: size.height)
    }

    override fun computeSpacing(): Spacing {
        val spacing = child.spacing()
        if (!child.requiresSpacingProjection) {
            spacing.reset(
                edgesOf(frameHeight != null, frameWidth != null, frameHeight != null, frameWidth != null),
            )
        }
        return spacing
    }

    override fun placeChildren(proposal: Proposal) {
        val childProposal = Proposal(frameWidth ?: proposal.width, frameHeight ?: proposal.height)
        val size = child.size(childProposal)
        child.place(
            x + alignmentX * width - alignmentX * size.width,
            y + alignmentY * height - alignmentY * size.height,
            size,
            childProposal,
        )
    }
}

internal class FlexFrameNode(
    child: Node,
    minWidth: Double?,
    idealWidth: Double?,
    maxWidth: Double?,
    minHeight: Double?,
    idealHeight: Double?,
    maxHeight: Double?,
    private val alignmentX: Double,
    private val alignmentY: Double,
) : UnaryNode(child) {
    private val minWidth = minWidth?.let { max(it, 0.0) }
    private val idealWidth = idealWidth?.let { max(this.minWidth ?: 0.0, it) }
    private val maxWidth = maxWidth?.let { max(this.idealWidth ?: 0.0, it) }
    private val minHeight = minHeight?.let { max(it, 0.0) }
    private val idealHeight = idealHeight?.let { max(this.minHeight ?: 0.0, it) }
    private val maxHeight = maxHeight?.let { max(this.idealHeight ?: 0.0, it) }

    private fun clamp(value: Double, lower: Double?, upper: Double?): Double =
        min(max(value, lower ?: Double.NEGATIVE_INFINITY), upper ?: Double.POSITIVE_INFINITY)

    private fun childProposal(proposal: Proposal): Proposal = Proposal(
        (proposal.width ?: idealWidth)?.let { clamp(it, minWidth, maxWidth) },
        (proposal.height ?: idealHeight)?.let { clamp(it, minHeight, maxHeight) },
    )

    private fun fixed(value: Double?, lower: Double?, upper: Double?, ideal: Double?): Double? = if (value != null) {
        if (lower != null && upper != null && lower <= upper) min(max(value, lower), upper) else null
    } else {
        ideal
    }

    private fun final(size: Double, childProposal: Double?, lower: Double?, upper: Double?): Double = when {
        lower != null && upper != null && lower <= upper -> min(max(lower, size), upper)
        lower != null && upper == null -> max(min(childProposal ?: Double.POSITIVE_INFINITY, size), lower)
        lower == null && upper != null -> min(max(childProposal ?: Double.NEGATIVE_INFINITY, size), upper)
        else -> size
    }

    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        val width = fixed(proposal.width, minWidth, maxWidth, idealWidth)
        val height = fixed(proposal.height, minHeight, maxHeight, idealHeight)
        if (width != null && height != null) {
            return LayoutSize(width, height)
        }
        val childProposal = childProposal(proposal)
        val size = child.size(childProposal)
        return LayoutSize(
            width ?: final(size.width, childProposal.width, minWidth, maxWidth),
            height ?: final(size.height, childProposal.height, minHeight, maxHeight),
        )
    }

    override fun computeSpacing(): Spacing {
        val spacing = child.spacing()
        if (!child.requiresSpacingProjection) {
            val vertical = minHeight != null || idealHeight != null || maxHeight != null
            val horizontal = minWidth != null || idealWidth != null || maxWidth != null
            spacing.reset(edgesOf(vertical, horizontal, vertical, horizontal))
        }
        return spacing
    }

    private fun placementDimension(
        value: Double,
        proposed: Double?,
        lower: Double?,
        ideal: Double?,
        upper: Double?,
    ): Double? {
        if (ideal == null && proposed == null && (lower ?: Double.NEGATIVE_INFINITY) < value &&
            value < (upper ?: Double.POSITIVE_INFINITY)
        ) {
            return null
        }
        return value
    }

    override fun placeChildren(proposal: Proposal) {
        val childProposal = Proposal(
            placementDimension(width, proposal.width, minWidth, idealWidth, maxWidth),
            placementDimension(height, proposal.height, minHeight, idealHeight, maxHeight),
        )
        val size = child.size(childProposal)
        child.place(
            x + alignmentX * width - alignmentX * size.width,
            y + alignmentY * height - alignmentY * size.height,
            size,
            childProposal,
        )
    }
}

internal class AspectRatioNode(child: Node, private val ratio: Double?, private val fill: Boolean) : UnaryNode(child) {
    private fun scaled(size: LayoutSize, target: Proposal): LayoutSize {
        val scaleX = if (target.width != null && (size.width != 0.0 || target.width != 0.0)) {
            target.width / size.width
        } else {
            Double.POSITIVE_INFINITY
        }
        val scaleY = if (target.height != null && (size.height != 0.0 || target.height != 0.0)) {
            target.height / size.height
        } else {
            Double.POSITIVE_INFINITY
        }
        val scale = if (fill) max(scaleX, scaleY) else min(scaleX, scaleY)
        return LayoutSize(size.width * scale, size.height * scale)
    }

    private fun spaceOffered(proposal: Proposal): Proposal {
        if (proposal == Proposal.unspecified) {
            return proposal
        }
        val size = if (ratio != null) LayoutSize(ratio, 1.0) else child.size(Proposal.unspecified)
        val ratioSize = if (size.width == size.height) LayoutSize(1.0, 1.0) else size
        val result = scaled(ratioSize, proposal)
        return Proposal(result.width, result.height)
    }

    override fun sizeThatFits(proposal: Proposal): LayoutSize = child.size(spaceOffered(proposal))

    override fun placeChildren(proposal: Proposal) {
        val childProposal = spaceOffered(proposal)
        val size = child.size(childProposal)
        child.place(x + (width - size.width) / 2, y + (height - size.height) / 2, size, childProposal)
    }
}

internal class SecondaryNode(
    child: Node,
    private val secondary: Node,
    private val inFront: Boolean,
    private val alignmentX: Double,
    private val alignmentY: Double,
) : UnaryNode(child) {
    override val layoutPriority: Double
        get() = child.layoutPriority

    override val requiresSpacingProjection: Boolean
        get() = child.requiresSpacingProjection

    override fun sizeThatFits(proposal: Proposal): LayoutSize = child.size(proposal)

    override fun lengthThatFits(proposal: Proposal, axis: Axis): Double = child.lengthThatFits(proposal, axis)

    override fun placeChildren(proposal: Proposal) {
        child.place(x, y, LayoutSize(width, height), proposal)
        val secondaryProposal = Proposal(width, height)
        val size = secondary.size(secondaryProposal)
        secondary.place(
            x + alignmentX * width - alignmentX * size.width,
            y + alignmentY * height - alignmentY * size.height,
            size,
            secondaryProposal,
        )
    }

    override fun draw(context: DrawContext) {
        if (inFront) {
            child.draw(context)
            secondary.draw(context)
        } else {
            secondary.draw(context)
            child.draw(context)
        }
    }
}

internal abstract class EffectNode(child: Node) : UnaryNode(child) {
    override val layoutPriority: Double
        get() = child.layoutPriority

    override val requiresSpacingProjection: Boolean
        get() = child.requiresSpacingProjection

    override fun sizeThatFits(proposal: Proposal): LayoutSize = child.size(proposal)

    override fun lengthThatFits(proposal: Proposal, axis: Axis): Double = child.lengthThatFits(proposal, axis)

    override fun placeChildren(proposal: Proposal) {
        child.place(x, y, LayoutSize(width, height), proposal)
    }
}

internal class OffsetNode(child: Node, private val dx: Double, private val dy: Double) : EffectNode(child) {
    override fun draw(context: DrawContext) {
        context.save()
        context.translate(dx, dy)
        child.draw(context)
        context.restore()
    }
}

internal class RotationNode(
    child: Node,
    private val degrees: Double,
    private val anchorX: Double,
    private val anchorY: Double,
) : EffectNode(child) {
    override fun draw(context: DrawContext) {
        context.save()
        context.rotate(degrees, x + anchorX * width, y + anchorY * height)
        child.draw(context)
        context.restore()
    }
}

internal class OpacityNode(child: Node, private val alpha: Double) : EffectNode(child) {
    override fun draw(context: DrawContext) {
        if (alpha >= 1.0) {
            child.draw(context)
            return
        }
        context.beginOpacity(alpha)
        child.draw(context)
        context.endOpacity()
    }
}

internal class ClipNode(child: Node, private val kind: ShapeKind) : EffectNode(child) {
    override fun draw(context: DrawContext) {
        context.beginClip(kind, x, y, width, height)
        child.draw(context)
        context.endClip()
    }
}

internal class ShapeNode(private val kind: ShapeKind, private val paint: ShapePaint) : Node() {
    override fun sizeThatFits(proposal: Proposal): LayoutSize = proposal.replacingUnspecifiedDimensions()

    override fun draw(context: DrawContext) {
        context.drawShape(kind, paint, x, y, width, height)
    }
}

internal class ChartNode(private val sectors: List<Pair<Double, Color>>) : Node() {
    override fun sizeThatFits(proposal: Proposal): LayoutSize = proposal.replacingUnspecifiedDimensions()

    override fun draw(context: DrawContext) {
        context.drawPie(sectors, x, y, width, height)
    }
}

internal sealed class ImageSource {
    abstract val naturalWidth: Double
    abstract val naturalHeight: Double

    class Bitmap(val bitmap: Any, override val naturalWidth: Double, override val naturalHeight: Double) : ImageSource()

    class Symbol(val vector: Any, val pointSize: Double) : ImageSource() {
        override val naturalWidth: Double
            get() = pointSize
        override val naturalHeight: Double
            get() = pointSize
    }
}

internal class ImageNode(private val source: ImageSource, private val resizable: Boolean, private val tint: Color?) :
    Node() {
    override fun sizeThatFits(proposal: Proposal): LayoutSize {
        if (!resizable) {
            return LayoutSize(source.naturalWidth, source.naturalHeight)
        }
        return LayoutSize(proposal.width ?: source.naturalWidth, proposal.height ?: source.naturalHeight)
    }

    override fun draw(context: DrawContext) {
        context.drawImage(source, tint, x, y, width, height)
    }
}

internal class EmptyNode : Node() {
    override fun sizeThatFits(proposal: Proposal): LayoutSize = LayoutSize.zero

    override fun draw(context: DrawContext) {}
}
