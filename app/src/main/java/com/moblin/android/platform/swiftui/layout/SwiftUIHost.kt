package com.moblin.android.platform.swiftui.layout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.LineHeightSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.offscreen.logOverlayOnce
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.systemImage
import com.moblin.android.view.utils.FontDesign
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun layoutRoot(measurer: AndroidTextMeasurer, content: ViewBuilder.() -> Unit): Node? = try {
    val node = singleView(buildViews(content)).resolve(Environment(), ResolveContext(measurer, AndroidImageProvider))
    val size = node.size(Proposal.unspecified)
    node.place(0.0, 0.0, size, Proposal.unspecified)
    node
} catch (error: Throwable) {
    logOverlayOnce("SwiftUIView layout failed: $error")
    null
}

private fun pixels(value: Double, density: Float): Int = ceil(value * density - 1e-6).toInt().coerceAtLeast(0)

internal fun renderSwiftUIView(context: Context, density: Float, content: ViewBuilder.() -> Unit): Bitmap? {
    val measurer = AndroidTextMeasurer(density, createFontFamilyResolver(context))
    val node = layoutRoot(measurer, content) ?: return null
    val width = pixels(node.width, density)
    val height = pixels(node.height, density)
    if (width <= 0 || height <= 0) {
        return null
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    node.draw(AndroidDrawContext(Canvas(bitmap), density, measurer))
    return bitmap
}

@Composable
fun SwiftUIView(content: ViewBuilder.() -> Unit) {
    val density = LocalDensity.current.density
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val measurer = remember(density, fontFamilyResolver) { AndroidTextMeasurer(density, fontFamilyResolver) }
    val node = layoutRoot(measurer, content)
    val widthPx = node?.let { pixels(it.width, density) } ?: 0
    val heightPx = node?.let { pixels(it.height, density) } ?: 0
    Layout(
        content = {},
        modifier = Modifier.drawBehind {
            if (node != null) {
                drawIntoCanvas { canvas ->
                    try {
                        node.draw(AndroidDrawContext(canvas.nativeCanvas, density, measurer))
                    } catch (error: Throwable) {
                        logOverlayOnce("SwiftUIView draw failed: $error")
                    }
                }
            }
        },
    ) { _, constraints ->
        layout(
            widthPx.coerceIn(constraints.minWidth, constraints.maxWidth),
            heightPx.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {}
    }
}

private data class TypefaceKey(val design: FontDesign, val opticalSize: Int, val weight: Int)

private const val systemFontAscender = 1950.0 / 2048.0
private const val systemFontDescender = 494.0 / 2048.0
private const val systemFontCapHeight = 1443.0 / 2048.0

private class LineMetrics(val ascent: Double, val descent: Double) {
    val height: Double
        get() = ascent + descent
    val ascentPixels: Int = floor(ascent + 0.5).toInt()
    val descentPixels: Int = floor(ascent + descent + 0.5).toInt() - ascentPixels
}

private class FixedLineHeightSpan(private val metrics: LineMetrics) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fontMetrics: Paint.FontMetricsInt,
    ) {
        fontMetrics.ascent = -metrics.ascentPixels
        fontMetrics.top = -metrics.ascentPixels
        fontMetrics.descent = metrics.descentPixels
        fontMetrics.bottom = metrics.descentPixels
    }
}

internal class AndroidTextMeasurer(private val density: Float, private val resolver: FontFamily.Resolver) :
    TextMeasurer {
    override val pixelLength: Double = 1.0 / density
    private val typefaces = HashMap<TypefaceKey, Typeface>()
    private val metrics = HashMap<ResolvedFont, FontMetricsInfo>()

    private fun typeface(font: ResolvedFont): Typeface {
        val size = font.size.toFloat()
        val key = TypefaceKey(font.design, SwiftUIFonts.opticalSize(size), font.weight)
        return typefaces.getOrPut(key) {
            try {
                val family = SwiftUIFonts.family(font.design, size)
                resolver.resolve(family, FontWeight(font.weight.coerceIn(1, 1000))).value as? Typeface
                    ?: Typeface.DEFAULT
            } catch (error: Throwable) {
                logOverlayOnce("SwiftUIView font resolution failed: $error")
                Typeface.DEFAULT
            }
        }
    }

    fun paint(font: ResolvedFont, scale: Double, color: Color = Color.Black): TextPaint {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        paint.typeface = typeface(font)
        paint.textSize = (font.size * scale * density).toFloat()
        if (font.monospacedDigit) {
            paint.fontFeatureSettings = "'tnum'"
        }
        paint.color = color.toArgb()
        return paint
    }

    private fun usesSystemFontMetrics(font: ResolvedFont): Boolean = font.design != FontDesign.Serif

    private fun lineMetrics(font: ResolvedFont, paint: TextPaint): LineMetrics {
        if (usesSystemFontMetrics(font)) {
            val size = paint.textSize.toDouble()
            return LineMetrics(systemFontAscender * size, systemFontDescender * size)
        }
        val fontMetrics = paint.fontMetrics
        return LineMetrics(-fontMetrics.ascent.toDouble(), fontMetrics.descent.toDouble())
    }

    override fun fontMetrics(font: ResolvedFont): FontMetricsInfo = metrics.getOrPut(font) {
        val paint = paint(font, 1.0)
        val lineMetrics = lineMetrics(font, paint)
        val capHeight = if (usesSystemFontMetrics(font)) {
            systemFontCapHeight * paint.textSize
        } else {
            val path = Path()
            paint.getTextPath("H", 0, 1, 0f, 0f, path)
            val bounds = RectF()
            path.computeBounds(bounds, true)
            -bounds.top.toDouble()
        }
        FontMetricsInfo(
            ascender = lineMetrics.ascent / density,
            descender = -lineMetrics.descent / density,
            leading = 0.0,
            capHeight = capHeight / density,
        )
    }

    fun layout(
        text: String,
        font: ResolvedFont,
        paint: TextPaint,
        width: Double?,
        maxLines: Int?,
        ellipsize: Boolean,
        alignment: Layout.Alignment,
    ): StaticLayout {
        val desired = Layout.getDesiredWidth(text, paint).toDouble()
        val widthPx = width?.let { it * density }
        val layoutWidth = if (widthPx == null || desired <= widthPx + 0.01) {
            ceil(desired - 1e-4).toInt()
        } else {
            floor(widthPx + 1e-4).toInt()
        }.coerceAtLeast(0)
        val spanned = SpannableString(text)
        spanned.setSpan(
            FixedLineHeightSpan(lineMetrics(font, paint)),
            0,
            text.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE,
        )
        val builder = StaticLayout.Builder.obtain(spanned, 0, spanned.length, paint, layoutWidth)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        if (maxLines != null && maxLines < Int.MAX_VALUE) {
            builder.setMaxLines(max(maxLines, 1))
            if (ellipsize) {
                builder.setEllipsize(TextUtils.TruncateAt.END)
            }
        }
        return builder.build()
    }

    private fun lineWidth(layout: StaticLayout, line: Int, text: String, paint: TextPaint): Double {
        if (layout.getEllipsisCount(line) <= 0) {
            return layout.getLineWidth(line).toDouble()
        }
        val start = layout.getLineStart(line)
        val visibleEnd = (start + layout.getEllipsisStart(line)).coerceIn(start, text.length)
        val visible = paint.measureText(text, start, visibleEnd).toDouble()
        val ellipsis = paint.measureText("…").toDouble()
        return min(visible + ellipsis, layout.width.toDouble())
    }

    override fun measure(
        text: String,
        font: ResolvedFont,
        scale: Double,
        width: Double?,
        maxLines: Int?,
        ellipsize: Boolean,
    ): TextMeasure {
        val paint = paint(font, scale)
        val layout = layout(text, font, paint, width, maxLines, ellipsize, Layout.Alignment.ALIGN_NORMAL)
        var lineWidth = 0.0
        var truncated = false
        for (line in 0 until layout.lineCount) {
            lineWidth = max(lineWidth, lineWidth(layout, line, text, paint))
            if (layout.getEllipsisCount(line) > 0) {
                truncated = true
            }
        }
        val lineCount = max(layout.lineCount, 1)
        val height = lineMetrics(font, paint).height * lineCount
        return TextMeasure(
            width = roundUpToPixel(lineWidth),
            height = roundUpToPixel(height),
            lineCount = layout.lineCount,
            truncated = truncated,
            firstBaseline = layout.getLineBaseline(0) / density.toDouble(),
            lastBaseline = layout.getLineBaseline(lineCount - 1) / density.toDouble(),
            fittingWidth = lineWidth / density,
            fittingHeight = height / density,
        )
    }

    private fun roundUpToPixel(pixels: Double): Double = ceil(pixels - 0.02).coerceAtLeast(0.0) / density
}

private val additionalSymbols: Map<String, ImageVector> by lazy {
    mapOf(
        "sun.max" to Icons.Filled.WbSunny,
        "bolt.circle" to Icons.Filled.OfflineBolt,
        "graduationcap" to Icons.Filled.School,
        "arrowtriangle.right.circle" to Icons.Filled.PlayCircle,
        "brain.head.profile" to Icons.Filled.Psychology,
        "pencil" to Icons.Filled.Edit,
        "target" to Icons.Filled.TrackChanges,
        "fork.knife" to Icons.Filled.Restaurant,
        "leaf" to Icons.Filled.Eco,
        "figure.dance" to Icons.Filled.EmojiPeople,
    )
}

internal object AndroidImageProvider : ImageProvider {
    private val assets = HashMap<String, ImageSource?>()

    override fun asset(name: String): ImageSource? = synchronized(assets) {
        assets.getOrPut(name) {
            Bundle.image(name)?.let { ImageSource.Bitmap(it, it.width.toDouble(), it.height.toDouble()) }
        }
    }

    override fun symbol(name: String, pointSize: Double): ImageSource? {
        val vector = additionalSymbols[name] ?: additionalSymbols[name.removeSuffix(".fill")] ?: systemImage(name)
        return ImageSource.Symbol(vector, pointSize)
    }
}

private fun continuousRoundedRect(path: Path, rect: RectF, radius: Float) {
    val halfSide = min(rect.width(), rect.height()) / 2f
    if (radius <= 0f || halfSide <= 0f) {
        path.addRect(rect, Path.Direction.CW)
        return
    }
    if (radius * 1.52866483f > halfSide) {
        val circular = min(radius, halfSide)
        path.addRoundRect(rect, circular, circular, Path.Direction.CW)
        return
    }
    val r = radius
    val left = rect.left
    val top = rect.top
    val right = rect.right
    val bottom = rect.bottom
    fun tlx(v: Float) = left + v * r
    fun tly(v: Float) = top + v * r
    fun trx(v: Float) = right - v * r
    fun bry(v: Float) = bottom - v * r
    path.moveTo(tlx(1.52866483f), top)
    path.lineTo(trx(1.52866471f), top)
    path.cubicTo(trx(1.08849323f), top, trx(0.86840689f), top, trx(0.66993427f), tly(0.06549600f))
    path.lineTo(trx(0.63149399f), tly(0.07491100f))
    path.cubicTo(trx(0.37282392f), tly(0.16905899f), trx(0.16906013f), tly(0.37282401f), trx(0.07491176f), tly(0.63149399f))
    path.cubicTo(right, tly(0.86840701f), right, tly(1.08849299f), right, tly(1.52866483f))
    path.lineTo(right, bry(1.52866471f))
    path.cubicTo(right, bry(1.08849323f), right, bry(0.86840689f), trx(0.06549569f), bry(0.66993493f))
    path.lineTo(trx(0.07491111f), bry(0.63149399f))
    path.cubicTo(trx(0.16905883f), bry(0.37282392f), trx(0.37282392f), bry(0.16905883f), trx(0.63149399f), bry(0.07491111f))
    path.cubicTo(trx(0.86840689f), bottom, trx(1.08849323f), bottom, trx(1.52866471f), bottom)
    path.lineTo(tlx(1.52866483f), bottom)
    path.cubicTo(tlx(1.08849299f), bottom, tlx(0.86840701f), bottom, tlx(0.66993397f), bry(0.06549569f))
    path.lineTo(tlx(0.63149399f), bry(0.07491111f))
    path.cubicTo(tlx(0.37282401f), bry(0.16905883f), tlx(0.16906001f), bry(0.37282392f), tlx(0.07491100f), bry(0.63149399f))
    path.cubicTo(left, bry(0.86840689f), left, bry(1.08849323f), left, bry(1.52866471f))
    path.lineTo(left, tly(1.52866483f))
    path.cubicTo(left, tly(1.08849299f), left, tly(0.86840701f), tlx(0.06549600f), tly(0.66993397f))
    path.lineTo(tlx(0.07491100f), tly(0.63149399f))
    path.cubicTo(tlx(0.16906001f), tly(0.37282401f), tlx(0.37282401f), tly(0.16906001f), tlx(0.63149399f), tly(0.07491100f))
    path.cubicTo(tlx(0.86840701f), top, tlx(1.08849299f), top, tlx(1.52866483f), top)
    path.close()
}

internal class AndroidDrawContext(
    private val canvas: Canvas,
    private val density: Float,
    private val measurer: AndroidTextMeasurer,
) : DrawContext {
    private val clips = ArrayDeque<Triple<Int, Path, RectF>>()
    private val opacities = ArrayDeque<Int>()

    private fun snap(value: Double): Float = floor(value * density + 0.5).toFloat()

    private fun snappedRect(x: Double, y: Double, width: Double, height: Double): RectF =
        RectF(snap(x), snap(y), snap(x + width), snap(y + height))

    private fun shapePath(kind: ShapeKind, rect: RectF): Path {
        val path = Path()
        when (kind) {
            ShapeKind.Rectangle -> path.addRect(rect, Path.Direction.CW)
            is ShapeKind.RoundedRectangle -> continuousRoundedRect(path, rect, (kind.cornerRadius * density).toFloat())
            ShapeKind.Circle -> {
                val radius = min(rect.width(), rect.height()) / 2
                path.addCircle(rect.centerX(), rect.centerY(), radius, Path.Direction.CW)
            }
        }
        return path
    }

    override fun drawShape(kind: ShapeKind, paint: ShapePaint, x: Double, y: Double, width: Double, height: Double) {
        val rect = snappedRect(x, y, width, height)
        val path = shapePath(kind, rect)
        val androidPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (paint) {
            is ShapePaint.Fill -> {
                if (paint.color.alpha <= 0f) {
                    return
                }
                androidPaint.style = Paint.Style.FILL
                androidPaint.color = paint.color.toArgb()
            }
            is ShapePaint.Stroke -> {
                if (paint.color.alpha <= 0f || paint.lineWidth <= 0.0) {
                    return
                }
                androidPaint.style = Paint.Style.STROKE
                androidPaint.strokeWidth = (paint.lineWidth * density).toFloat()
                androidPaint.strokeJoin = Paint.Join.MITER
                androidPaint.strokeCap = Paint.Cap.BUTT
                androidPaint.color = paint.color.toArgb()
            }
        }
        canvas.drawPath(path, androidPaint)
    }

    override fun drawText(request: TextDrawRequest) {
        if (request.text.isEmpty() || request.color.alpha <= 0f) {
            return
        }
        val alignment = when (request.alignment) {
            TextAlignment.leading -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.center -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.trailing -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val paint = measurer.paint(request.font, request.scale, request.color)
        val layout = measurer.layout(
            request.text,
            request.font,
            paint,
            request.layoutWidth,
            request.maxLines,
            true,
            alignment,
        )
        val frameWidth = request.width * density
        val shift = when (request.alignment) {
            TextAlignment.leading -> 0.0
            TextAlignment.center -> (layout.width - frameWidth) / 2
            TextAlignment.trailing -> layout.width - frameWidth
        }
        canvas.save()
        canvas.translate(snap(request.x) - shift.toFloat(), snap(request.y))
        layout.draw(canvas)
        canvas.restore()
    }

    override fun drawImage(source: ImageSource, tint: Color?, x: Double, y: Double, width: Double, height: Double) {
        val rect = snappedRect(x, y, width, height)
        if (rect.width() <= 0f || rect.height() <= 0f) {
            return
        }
        when (source) {
            is ImageSource.Bitmap -> {
                val bitmap = source.bitmap as? Bitmap ?: return
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, rect, paint)
            }
            is ImageSource.Symbol -> {
                val vector = source.vector as? ImageVector ?: return
                drawVector(vector, rect, tint)
            }
        }
    }

    private fun drawVector(vector: ImageVector, rect: RectF, tint: Color?) {
        if (vector.viewportWidth <= 0f || vector.viewportHeight <= 0f) {
            return
        }
        canvas.save()
        canvas.translate(rect.left, rect.top)
        canvas.scale(rect.width() / vector.viewportWidth, rect.height() / vector.viewportHeight)
        drawVectorGroup(vector.root, tint)
        canvas.restore()
    }

    private fun drawVectorGroup(group: VectorGroup, tint: Color?) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        if (group.clipPathData.isNotEmpty()) {
            canvas.clipPath(group.clipPathData.toPath().asAndroidPath())
        }
        for (child in group) {
            when (child) {
                is VectorGroup -> drawVectorGroup(child, tint)
                is VectorPath -> drawVectorPath(child, tint)
            }
        }
        canvas.restore()
    }

    private fun vectorColor(brush: androidx.compose.ui.graphics.Brush?, alpha: Float, tint: Color?): Int? {
        val base = when {
            brush == null -> return null
            tint != null -> tint
            brush is SolidColor -> brush.value
            else -> Color.Black
        }
        return base.copy(alpha = base.alpha * alpha).toArgb()
    }

    private fun drawVectorPath(vectorPath: VectorPath, tint: Color?) {
        val path = vectorPath.pathData.toPath().asAndroidPath()
        path.fillType = if (vectorPath.pathFillType == PathFillType.EvenOdd) {
            Path.FillType.EVEN_ODD
        } else {
            Path.FillType.WINDING
        }
        vectorColor(vectorPath.fill, vectorPath.fillAlpha, tint)?.let { color ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawPath(path, paint)
        }
        vectorColor(vectorPath.stroke, vectorPath.strokeAlpha, tint)?.let { color ->
            if (vectorPath.strokeLineWidth > 0f) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = vectorPath.strokeLineWidth
                paint.color = color
                canvas.drawPath(path, paint)
            }
        }
    }

    override fun drawPie(sectors: List<Pair<Double, Color>>, x: Double, y: Double, width: Double, height: Double) {
        val total = sectors.sumOf { it.first }
        if (total <= 0.0) {
            return
        }
        val rect = snappedRect(x, y, width, height)
        val diameter = min(rect.width(), rect.height())
        if (diameter <= 0f) {
            return
        }
        val oval = RectF(
            rect.centerX() - diameter / 2,
            rect.centerY() - diameter / 2,
            rect.centerX() + diameter / 2,
            rect.centerY() + diameter / 2,
        )
        var start = -90.0
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        for ((angle, color) in sectors) {
            val sweep = angle / total * 360.0
            paint.color = color.toArgb()
            canvas.drawArc(oval, start.toFloat(), sweep.toFloat(), true, paint)
            start += sweep
        }
    }

    override fun save() {
        canvas.save()
    }

    override fun restore() {
        canvas.restore()
    }

    override fun translate(dx: Double, dy: Double) {
        canvas.translate((dx * density).toFloat(), (dy * density).toFloat())
    }

    override fun rotate(degrees: Double, pivotX: Double, pivotY: Double) {
        canvas.rotate(degrees.toFloat(), (pivotX * density).toFloat(), (pivotY * density).toFloat())
    }

    override fun beginOpacity(alpha: Double) {
        opacities.addLast(canvas.saveLayerAlpha(null, (alpha.coerceIn(0.0, 1.0) * 255).roundToInt()))
    }

    override fun endOpacity() {
        opacities.removeLastOrNull()?.let { canvas.restoreToCount(it) }
    }

    override fun beginClip(kind: ShapeKind, x: Double, y: Double, width: Double, height: Double) {
        val rect = snappedRect(x, y, width, height)
        val count = canvas.saveLayer(RectF(rect), null)
        clips.addLast(Triple(count, shapePath(kind, rect), rect))
    }

    override fun endClip() {
        val (count, path, rect) = clips.removeLastOrNull() ?: return
        val maskPaint = Paint()
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        val mask = canvas.saveLayer(rect, maskPaint)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = android.graphics.Color.BLACK
        canvas.drawPath(path, fill)
        canvas.restoreToCount(mask)
        canvas.restoreToCount(count)
    }
}
