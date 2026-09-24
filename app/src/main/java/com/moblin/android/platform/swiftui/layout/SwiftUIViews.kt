package com.moblin.android.platform.swiftui.layout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.platform.swiftui.ChartContentScope
import com.moblin.android.view.utils.FontDesign
import kotlin.math.max

class HorizontalAlignment private constructor(internal val fraction: Double) {
    companion object {
        val leading = HorizontalAlignment(0.0)
        val center = HorizontalAlignment(0.5)
        val trailing = HorizontalAlignment(1.0)
    }
}

class VerticalAlignment private constructor(internal val fraction: Double) {
    companion object {
        val top = VerticalAlignment(0.0)
        val center = VerticalAlignment(0.5)
        val bottom = VerticalAlignment(1.0)
    }
}

class Alignment(val horizontal: HorizontalAlignment, val vertical: VerticalAlignment) {
    companion object {
        val topLeading = Alignment(HorizontalAlignment.leading, VerticalAlignment.top)
        val top = Alignment(HorizontalAlignment.center, VerticalAlignment.top)
        val topTrailing = Alignment(HorizontalAlignment.trailing, VerticalAlignment.top)
        val leading = Alignment(HorizontalAlignment.leading, VerticalAlignment.center)
        val center = Alignment(HorizontalAlignment.center, VerticalAlignment.center)
        val trailing = Alignment(HorizontalAlignment.trailing, VerticalAlignment.center)
        val bottomLeading = Alignment(HorizontalAlignment.leading, VerticalAlignment.bottom)
        val bottom = Alignment(HorizontalAlignment.center, VerticalAlignment.bottom)
        val bottomTrailing = Alignment(HorizontalAlignment.trailing, VerticalAlignment.bottom)
    }
}

class EdgeSet private constructor(private val mask: Int) {
    operator fun plus(other: EdgeSet): EdgeSet = EdgeSet(mask or other.mask)

    internal fun contains(other: EdgeSet): Boolean = mask and other.mask == other.mask

    companion object {
        val top = EdgeSet(1)
        val leading = EdgeSet(2)
        val bottom = EdgeSet(4)
        val trailing = EdgeSet(8)
        val horizontal = EdgeSet(2 or 8)
        val vertical = EdgeSet(1 or 4)
        val all = EdgeSet(15)
    }
}

class Angle private constructor(val degrees: Double) {
    companion object {
        fun degrees(value: Double): Angle = Angle(value)

        fun radians(value: Double): Angle = Angle(Math.toDegrees(value))
    }
}

class UnitPoint(val x: Double, val y: Double) {
    companion object {
        val center = UnitPoint(0.5, 0.5)
        val topLeading = UnitPoint(0.0, 0.0)
    }
}

enum class ContentMode {
    fit,
    fill,
}

class Font private constructor(val size: Double, val weight: FontWeight?, val design: FontDesign?) {
    companion object {
        val body = Font(17.0, null, null)

        fun system(size: Double, weight: FontWeight? = null, design: FontDesign? = null): Font =
            Font(size, weight, design)
    }
}

internal data class Environment(
    val font: Font = Font.body,
    val fontDesign: FontDesign? = null,
    val bold: Boolean = false,
    val monospacedDigit: Boolean = false,
    val foreground: Color = Color.Black,
    val lineLimit: Int? = null,
    val minimumScaleFactor: Double = 1.0,
    val textAlignment: TextAlignment = TextAlignment.leading,
) {
    fun resolvedFont(): ResolvedFont {
        val baseWeight = font.weight?.weight ?: 400
        return ResolvedFont(
            size = font.size,
            weight = if (bold) max(baseWeight, 700) else baseWeight,
            design = font.design ?: fontDesign ?: FontDesign.Default,
            monospacedDigit = monospacedDigit,
        )
    }
}

internal interface ImageProvider {
    fun asset(name: String): ImageSource?

    fun symbol(name: String, pointSize: Double): ImageSource?
}

internal data class ResolveContext(
    val measurer: TextMeasurer,
    val images: ImageProvider,
    val stackAxis: Axis? = null,
)

abstract class View internal constructor() {
    internal var builder: ViewBuilder? = null
    internal var slot: Int = -1

    internal abstract fun resolve(environment: Environment, context: ResolveContext): Node

    internal fun <T : View> replaceWith(replacement: T): T {
        val owner = builder
        if (owner != null && owner.views.getOrNull(slot) === this) {
            owner.views[slot] = replacement
            replacement.builder = owner
            replacement.slot = slot
        }
        builder = null
        slot = -1
        return replacement
    }

    internal fun detach(): View {
        val owner = builder ?: return this
        val index = owner.views.indexOfFirst { it === this }
        if (index >= 0) {
            owner.views.removeAt(index)
            for (position in index until owner.views.size) {
                owner.views[position].slot = position
            }
        }
        builder = null
        slot = -1
        return this
    }
}

class ViewBuilder internal constructor() {
    internal val views = mutableListOf<View>()

    internal fun <T : View> add(view: T): T {
        view.builder = this
        view.slot = views.size
        views.add(view)
        return view
    }

    fun VStack(
        alignment: HorizontalAlignment = HorizontalAlignment.center,
        spacing: Double? = null,
        content: ViewBuilder.() -> Unit,
    ): View = add(StackView(Axis.vertical, alignment.fraction, spacing, buildViews(content)))

    fun HStack(
        alignment: VerticalAlignment = VerticalAlignment.center,
        spacing: Double? = null,
        content: ViewBuilder.() -> Unit,
    ): View = add(StackView(Axis.horizontal, alignment.fraction, spacing, buildViews(content)))

    fun ZStack(alignment: Alignment = Alignment.center, content: ViewBuilder.() -> Unit): View =
        add(ZStackView(alignment, buildViews(content)))

    fun Spacer(minLength: Double? = null): View = add(SpacerView(minLength))

    fun Text(text: String): View = add(TextView(text))

    fun Image(name: String? = null, systemName: String? = null): ImageView = add(ImageView(name, systemName))

    fun ColorView(color: Color): View = add(ShapeView(ShapeKind.Rectangle, color, null))

    fun Rectangle(): ShapeView = add(ShapeView(ShapeKind.Rectangle, null, null))

    fun RoundedRectangle(cornerRadius: Double): ShapeView =
        add(ShapeView(ShapeKind.RoundedRectangle(cornerRadius), null, null))

    fun Circle(): ShapeView = add(ShapeView(ShapeKind.Circle, null, null))

    fun <T> Chart(data: List<T>, content: ChartContentScope.(T) -> Unit): View {
        val scope = ChartContentScope()
        for (item in data) {
            scope.content(item)
        }
        val sectors = scope.sectors
            .filter { it.angle > 0 && it.angle.isFinite() }
            .map { it.angle to it.color }
        return add(ChartView(sectors))
    }
}

internal fun buildViews(content: ViewBuilder.() -> Unit): List<View> {
    val builder = ViewBuilder()
    builder.content()
    val views = builder.views.toList()
    for (view in views) {
        view.builder = null
        view.slot = -1
    }
    return views
}

internal fun singleView(views: List<View>): View = when (views.size) {
    1 -> views[0]
    else -> StackView(Axis.vertical, 0.5, null, views)
}

private fun secondaryView(alignment: Alignment, views: List<View>): View = when (views.size) {
    1 -> views[0]
    else -> ZStackView(alignment, views)
}

internal class StackView(
    private val axis: Axis,
    private val alignment: Double,
    private val spacing: Double?,
    private val children: List<View>,
) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node {
        val childContext = context.copy(stackAxis = axis)
        return StackNode(axis, alignment, spacing, children.map { it.resolve(environment, childContext) })
    }
}

internal class ZStackView(private val alignment: Alignment, private val children: List<View>) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node {
        val childContext = context.copy(stackAxis = null)
        return ZStackNode(
            alignment.horizontal.fraction,
            alignment.vertical.fraction,
            children.map { it.resolve(environment, childContext) },
        )
    }
}

internal class SpacerView(private val minLength: Double?) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node =
        SpacerNode(minLength, context.stackAxis)
}

internal class TextView(private val text: String) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node = TextNode(
        text = text,
        font = environment.resolvedFont(),
        color = environment.foreground,
        lineLimit = environment.lineLimit,
        minimumScaleFactor = environment.minimumScaleFactor,
        alignment = environment.textAlignment,
        measurer = context.measurer,
    )
}

class ImageView internal constructor(private val name: String?, private val systemName: String?) : View() {
    internal var isResizable = false

    override fun resolve(environment: Environment, context: ResolveContext): Node {
        if (systemName != null) {
            val source = context.images.symbol(systemName, environment.font.size) ?: return EmptyNode()
            return ImageNode(source, isResizable, environment.foreground)
        }
        val source = name?.let { context.images.asset(it) } ?: return EmptyNode()
        return ImageNode(source, isResizable, null)
    }
}

fun ImageView.resizable(): ImageView {
    isResizable = true
    return this
}

class ShapeView internal constructor(
    private val kind: ShapeKind,
    private val fillColor: Color?,
    private val strokeColor: Color?,
    private val lineWidth: Double = 1.0,
) : View() {
    internal val shapeKind: ShapeKind
        get() = kind

    override fun resolve(environment: Environment, context: ResolveContext): Node {
        val paint = when {
            strokeColor != null -> ShapePaint.Stroke(strokeColor, lineWidth)
            fillColor != null -> ShapePaint.Fill(fillColor)
            else -> ShapePaint.Fill(environment.foreground)
        }
        return ShapeNode(kind, paint)
    }

    fun fill(color: Color): View = replaceWith(ShapeView(kind, color, null))

    fun stroke(color: Color, lineWidth: Double = 1.0): View = replaceWith(ShapeView(kind, null, color, lineWidth))
}

internal class ChartView(private val sectors: List<Pair<Double, Color>>) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node = ChartNode(sectors)
}

internal class EnvironmentView(private val child: View, private val transform: (Environment) -> Environment) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node =
        child.resolve(transform(environment), context)
}

internal class NodeView(private val child: View, private val wrap: (Node) -> Node) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node =
        wrap(child.resolve(environment, context))
}

internal class SecondaryView(
    private val child: View,
    private val secondary: View,
    private val inFront: Boolean,
    private val alignment: Alignment,
) : View() {
    override fun resolve(environment: Environment, context: ResolveContext): Node = SecondaryNode(
        child.resolve(environment, context),
        secondary.resolve(environment, context.copy(stackAxis = null)),
        inFront,
        alignment.horizontal.fraction,
        alignment.vertical.fraction,
    )
}

private fun View.environment(transform: (Environment) -> Environment): View =
    replaceWith(EnvironmentView(this, transform))

private fun View.node(wrap: (Node) -> Node): View = replaceWith(NodeView(this, wrap))

fun View.padding(length: Double = 16.0): View = padding(EdgeSet.all, length)

fun View.padding(edges: EdgeSet, length: Double): View {
    val top = if (edges.contains(EdgeSet.top)) length else 0.0
    val leading = if (edges.contains(EdgeSet.leading)) length else 0.0
    val bottom = if (edges.contains(EdgeSet.bottom)) length else 0.0
    val trailing = if (edges.contains(EdgeSet.trailing)) length else 0.0
    return node { PaddingNode(it, top, leading, bottom, trailing) }
}

fun View.frame(width: Double? = null, height: Double? = null, alignment: Alignment = Alignment.center): View =
    node { FrameNode(it, width, height, alignment.horizontal.fraction, alignment.vertical.fraction) }

fun View.frame(
    minWidth: Double? = null,
    idealWidth: Double? = null,
    maxWidth: Double? = null,
    minHeight: Double? = null,
    idealHeight: Double? = null,
    maxHeight: Double? = null,
    alignment: Alignment = Alignment.center,
): View = node {
    FlexFrameNode(
        it,
        minWidth,
        idealWidth,
        maxWidth,
        minHeight,
        idealHeight,
        maxHeight,
        alignment.horizontal.fraction,
        alignment.vertical.fraction,
    )
}

fun View.background(color: Color): View =
    replaceWith(SecondaryView(this, ShapeView(ShapeKind.Rectangle, color, null), false, Alignment.center))

fun View.background(view: View, alignment: Alignment = Alignment.center): View {
    view.detach()
    return replaceWith(SecondaryView(this, view, false, alignment))
}

fun View.background(alignment: Alignment = Alignment.center, content: ViewBuilder.() -> Unit): View =
    replaceWith(SecondaryView(this, secondaryView(alignment, buildViews(content)), false, alignment))

fun View.overlay(view: View, alignment: Alignment = Alignment.center): View {
    view.detach()
    return replaceWith(SecondaryView(this, view, true, alignment))
}

fun View.overlay(alignment: Alignment = Alignment.center, content: ViewBuilder.() -> Unit): View =
    replaceWith(SecondaryView(this, secondaryView(alignment, buildViews(content)), true, alignment))

fun View.font(font: Font): View = environment { it.copy(font = font) }

fun View.fontDesign(design: FontDesign): View = environment { it.copy(fontDesign = design) }

fun View.bold(isActive: Boolean = true): View = environment { it.copy(bold = isActive) }

fun View.monospacedDigit(): View = environment { it.copy(monospacedDigit = true) }

fun View.foregroundStyle(color: Color): View = environment { it.copy(foreground = color) }

fun View.lineLimit(limit: Int?): View = environment { it.copy(lineLimit = limit) }

fun View.minimumScaleFactor(factor: Double): View = environment { it.copy(minimumScaleFactor = factor) }

fun View.multilineTextAlignment(alignment: TextAlignment): View =
    environment { it.copy(textAlignment = alignment) }

fun View.opacity(opacity: Double): View = node { OpacityNode(it, opacity) }

fun View.offset(x: Double = 0.0, y: Double = 0.0): View = node { OffsetNode(it, x, y) }

fun View.rotationEffect(angle: Angle, anchor: UnitPoint = UnitPoint.center): View =
    node { RotationNode(it, angle.degrees, anchor.x, anchor.y) }

fun View.clipShape(shape: ShapeView): View {
    shape.detach()
    val kind = shape.shapeKind
    return node { ClipNode(it, kind) }
}

fun View.cornerRadius(radius: Double): View = node { ClipNode(it, ShapeKind.RoundedRectangle(radius)) }

fun View.aspectRatio(ratio: Double? = null, contentMode: ContentMode): View =
    node { AspectRatioNode(it, ratio, contentMode == ContentMode.fill) }

fun View.scaledToFit(): View = aspectRatio(contentMode = ContentMode.fit)

fun View.scaledToFill(): View = aspectRatio(contentMode = ContentMode.fill)
