package com.moblin.android.view.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ChatLineTextStyle(
    var color: Color,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var strikethrough: Boolean = false,
    var singleLine: Boolean = false,
    var link: String? = null,
)

data class ChatLineImage(
    val source: ChatImageSource,
    var animated: Boolean = true,
    var height: Float? = null,
    var horizontalPadding: Float = 0f,
    var verticalPadding: Float = 0f,
    var opacity: Float = 1f,
)

sealed class ChatLineItem {
    data class Text(val text: String, val style: ChatLineTextStyle) : ChatLineItem()

    data class Image(val image: ChatLineImage) : ChatLineItem()
}

enum class FontDesign {
    Default,
    Serif,
    Rounded,
    Monospaced,
}

data class ChatLineContent(
    var items: List<ChatLineItem>,
    var fontSize: Float,
    var borderColor: Color? = null,
    var borderWidth: Float = 0f,
    var backgroundColor: Color? = null,
    var leadingPadding: Float = 0f,
    var topAligned: Boolean = false,
    var fontWeight: FontWeight = FontWeight.Normal,
    var fontDesign: FontDesign = FontDesign.Default,
)

private const val strikethroughKey = "moblinChatLineStrikethrough"
private const val imageIndexKey = "moblinChatLineImageIndex"
private const val linkKey = "moblinChatLineLink"
private const val runDelegateKey = "moblinChatLineRunDelegate"

private class FontSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }

    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }
}

private class LinkSpan(val link: String) : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
    }
}

private class ChatLineStrikethroughSpan : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
    }
}

private class ImageRunMetrics(val ascent: Float, val descent: Float, val width: Float) {
    fun makeRunDelegate(): ReplacementSpan = ImageSpanMetrics(this)
}

private class ImageSpanMetrics(private val metrics: ImageRunMetrics) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            fm.ascent = -ceil(metrics.ascent.toDouble()).toInt()
            fm.descent = ceil(metrics.descent.toDouble()).toInt()
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return ceil(metrics.width.toDouble()).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
    }
}

private class LayoutLine(
    val start: Int,
    val end: Int,
    val baseline: Float,
)

private class ChatLineLayout(
    val availableWidth: Float,
    val sizesVersion: Int,
    val hasUnknownImageSize: Boolean,
    val size: IntSize,
    val lines: List<LayoutLine>,
    val layout: StaticLayout,
    val strokeLayout: StaticLayout?,
    val images: List<ChatLineImage>,
    val imageFrames: List<Rect>,
    val metrics: List<ImageRunMetrics>,
)

private fun makeFont(content: ChatLineContent, style: ChatLineTextStyle): Typeface {
    val familyName = when (content.fontDesign) {
        FontDesign.Default -> null
        FontDesign.Serif -> "serif"
        FontDesign.Rounded -> "sans-serif-rounded"
        FontDesign.Monospaced -> "monospace"
    }
    var typefaceStyle = if (style.bold || content.fontWeight.weight >= 600) Typeface.BOLD else Typeface.NORMAL
    if (style.italic) {
        typefaceStyle = typefaceStyle or Typeface.ITALIC
    }
    return if (familyName != null) {
        Typeface.create(familyName, typefaceStyle)
    } else {
        Typeface.create(Typeface.DEFAULT, typefaceStyle)
    }
}

private fun makeLayout(content: ChatLineContent, availableWidth: Float): ChatLineLayout {
    val sizesVersion = EmotesPlayer.shared.sizesVersion.value
    val basePaint = TextPaint()
    basePaint.isAntiAlias = true
    basePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    basePaint.textSize = content.fontSize
    val fontMetrics = basePaint.fontMetrics
    val fontAscent = -fontMetrics.ascent
    val fontDescent = fontMetrics.descent
    val string = SpannableStringBuilder()
    val metrics = mutableListOf<ImageRunMetrics>()
    val images = mutableListOf<ChatLineImage>()
    val imageOffsets = mutableListOf<Int>()
    var hasUnknownImageSize = false
    for (item in content.items) {
        when (item) {
            is ChatLineItem.Text -> {
                var itemText = item.text
                if (item.style.singleLine) {
                    itemText = itemText.replace(" ", "\u00A0")
                }
                val start = string.length
                string.append(itemText)
                val end = string.length
                string.setSpan(
                    FontSpan(makeFont(content = content, style = item.style)),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                string.setSpan(
                    ForegroundColorSpan(item.style.color.toArgb()),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                if (item.style.strikethrough) {
                    string.setSpan(ChatLineStrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                val link = item.style.link
                if (link != null) {
                    string.setSpan(LinkSpan(link), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            is ChatLineItem.Image -> {
                val image = item.image
                var contentWidth = 0f
                var contentHeight = 0f
                val imageSize = EmotesPlayer.shared.size(image.source)
                if (imageSize != null && imageSize.width.toFloat() > 0f && imageSize.height.toFloat() > 0f) {
                    val height = image.height
                    if (height != null) {
                        contentHeight = max(height - 2 * image.verticalPadding, 0f)
                        contentWidth = contentHeight * imageSize.width.toFloat() / imageSize.height.toFloat()
                    } else {
                        contentWidth = imageSize.width.toFloat()
                        contentHeight = imageSize.height.toFloat()
                    }
                } else {
                    hasUnknownImageSize = true
                }
                val width = contentWidth + 2 * image.horizontalPadding
                val height = contentHeight + 2 * image.verticalPadding
                val ascent = if (content.topAligned) {
                    fontAscent
                } else {
                    (height + fontAscent - fontDescent) / 2
                }
                val imageMetrics = ImageRunMetrics(ascent = ascent, descent = height - ascent, width = width)
                metrics.add(imageMetrics)
                val start = string.length
                string.append("\uFFFC")
                val end = string.length
                string.setSpan(imageMetrics.makeRunDelegate(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                imageOffsets.add(start)
                images.add(image)
            }
        }
    }
    val textWidth = max(availableWidth - content.leadingPadding, 1f)
    val layoutWidth = ceil(textWidth.toDouble()).toInt()
    val fillPaint = TextPaint()
    fillPaint.set(basePaint)
    val textLayout = StaticLayout.Builder
        .obtain(string, 0, string.length, fillPaint, layoutWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()
    var strokeLayout: StaticLayout? = null
    val borderColor = content.borderColor
    if (borderColor != null && content.borderWidth > 0f) {
        val strokePaint = TextPaint()
        strokePaint.set(basePaint)
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2 * content.borderWidth
        strokePaint.color = borderColor.toArgb()
        strokeLayout = StaticLayout.Builder
            .obtain(string, 0, string.length, strokePaint, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
    }
    val lines = mutableListOf<LayoutLine>()
    var maxLineWidth = 0f
    for (index in 0 until textLayout.lineCount) {
        lines.add(
            LayoutLine(
                start = textLayout.getLineStart(index),
                end = textLayout.getLineEnd(index),
                baseline = textLayout.getLineBaseline(index).toFloat(),
            ),
        )
        maxLineWidth = max(maxLineWidth, min(textLayout.getLineWidth(index), textWidth))
    }
    val imageFrames = MutableList(images.size) { Rect.Zero }
    for (index in images.indices) {
        val image = images[index]
        val imageMetrics = metrics[index]
        val offset = imageOffsets[index]
        val lineIndex = textLayout.getLineForOffset(offset)
        val x = textLayout.getPrimaryHorizontal(offset)
        val baseline = textLayout.getLineBaseline(lineIndex).toFloat()
        val top = baseline - imageMetrics.ascent + image.verticalPadding
        imageFrames[index] = Rect(
            left = content.leadingPadding + x + image.horizontalPadding,
            top = top,
            right = content.leadingPadding + x + imageMetrics.width - image.horizontalPadding,
            bottom = top + imageMetrics.ascent + imageMetrics.descent - 2 * image.verticalPadding,
        )
    }
    return ChatLineLayout(
        availableWidth = availableWidth,
        sizesVersion = sizesVersion,
        hasUnknownImageSize = hasUnknownImageSize,
        size = IntSize(
            width = ceil((content.leadingPadding + maxLineWidth).toDouble()).toInt(),
            height = ceil(textLayout.height.toDouble()).toInt(),
        ),
        lines = lines,
        layout = textLayout,
        strokeLayout = strokeLayout,
        images = images,
        imageFrames = imageFrames,
        metrics = metrics,
    )
}

class ChatLineUiView(context: Context) : FrameLayout(context) {
    var onImageLoaded: (() -> Unit)? = null
    private var content: ChatLineContent? = null
    private val layouts = mutableListOf<ChatLineLayout>()
    private var currentLayout: ChatLineLayout? = null
    private var measuredWidth: Float? = null
    private val imageViews = mutableListOf<EmoteUiView>()
    private val backgroundPaint = Paint()

    init {
        setWillNotDraw(false)
        clipChildren = false
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        backgroundPaint.isAntiAlias = true
    }

    fun setContent(content: ChatLineContent) {
        if (content == this.content) {
            return
        }
        this.content = content
        layouts.clear()
        currentLayout = null
        requestLayout()
        invalidate()
    }

    fun size(availableWidth: Float): IntSize {
        measuredWidth = availableWidth
        return layout(availableWidth = availableWidth)?.size ?: IntSize.Zero
    }

    fun unregister() {
        for (imageView in imageViews) {
            imageView.unregister()
        }
    }

    fun link(at: Offset): String? {
        val content = this.content ?: return null
        val layout = currentLayout ?: return null
        val staticLayout = layout.layout
        val x = at.x - content.leadingPadding
        val lineIndex = staticLayout.getLineForVertical(at.y.toInt())
        val offset = staticLayout.getOffsetForHorizontal(lineIndex, x)
        val text = staticLayout.text as? Spanned ?: return null
        val spans = text.getSpans(offset, offset, LinkSpan::class.java)
        for (span in spans) {
            return span.link
        }
        return null
    }

    private fun layout(availableWidth: Float): ChatLineLayout? {
        val content = this.content ?: return null
        for (layout in layouts) {
            if (layout.availableWidth == availableWidth) {
                if (!layout.hasUnknownImageSize || layout.sizesVersion == EmotesPlayer.shared.sizesVersion.value) {
                    return layout
                }
                layouts.remove(layout)
                break
            }
        }
        val layout = makeLayout(content = content, availableWidth = availableWidth)
        layouts.add(layout)
        if (layouts.size > 3) {
            layouts.removeAt(0)
        }
        return layout
    }

    private fun availableWidthForBounds(): Float {
        val widthPixels = width
        val heightPixels = height
        for (layout in layouts) {
            if (layout.size.width == widthPixels && layout.size.height == heightPixels) {
                return layout.availableWidth
            }
        }
        return measuredWidth ?: width.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = View.MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val size = size(availableWidth = availableWidth)
        setMeasuredDimension(size.width, size.height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val content = this.content ?: return
        val layout = layout(availableWidth = availableWidthForBounds()) ?: return
        if (currentLayout?.sizesVersion != layout.sizesVersion ||
            currentLayout?.size != layout.size ||
            currentLayout?.availableWidth != layout.availableWidth
        ) {
            invalidate()
        }
        currentLayout = layout
        val images = layout.images
        while (imageViews.size < images.size) {
            val imageView = EmoteUiView()
            imageView.onLoaded = { onImageLoaded?.invoke() }
            imageViews.add(imageView)
        }
        while (imageViews.size > images.size) {
            val imageView = imageViews.removeAt(imageViews.size - 1)
            imageView.unregister()
        }
        val borderWidth = if (content.borderColor != null) content.borderWidth else 0f
        for (index in images.indices) {
            val image = images[index]
            val imageView = imageViews[index]
            imageView.setEmote(
                source = image.source,
                animated = image.animated,
                borderColor = content.borderColor,
                borderWidth = borderWidth,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val content = this.content ?: return
        val layout = currentLayout ?: return
        val backgroundColor = content.backgroundColor
        if (backgroundColor != null) {
            backgroundPaint.color = backgroundColor.toArgb()
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 5f, 5f, backgroundPaint)
        }
        canvas.save()
        canvas.translate(content.leadingPadding, 0f)
        val strokeLayout = layout.strokeLayout
        if (strokeLayout != null) {
            strokeLayout.draw(canvas)
        }
        layout.layout.draw(canvas)
        canvas.restore()
        drawStrikethroughs(content = content, layout = layout, canvas = canvas)
    }

    private fun drawStrikethroughs(content: ChatLineContent, layout: ChatLineLayout, canvas: Canvas) {
        val thickness = max(content.fontSize / 16f, 1f)
        val staticLayout = layout.layout
        val text = staticLayout.text as? Spanned ?: return
        val spans = text.getSpans(0, text.length, ChatLineStrikethroughSpan::class.java)
        val paint = Paint()
        paint.isAntiAlias = true
        for (span in spans) {
            val start = text.getSpanStart(span)
            val end = text.getSpanEnd(span)
            val lineIndex = staticLayout.getLineForOffset(start)
            val x = staticLayout.getPrimaryHorizontal(start)
            val width = staticLayout.getPrimaryHorizontal(end) - x
            val baseline = staticLayout.getLineBaseline(lineIndex).toFloat()
            val colorSpans = text.getSpans(start, end, ForegroundColorSpan::class.java)
            paint.color = if (colorSpans.isNotEmpty()) colorSpans[0].foregroundColor else android.graphics.Color.WHITE
            val y = baseline - content.fontSize / 4f
            canvas.drawRect(
                content.leadingPadding + x,
                y - thickness / 2,
                content.leadingPadding + x + width,
                y + thickness / 2,
                paint,
            )
        }
    }
}

class ChatLineViewCoordinator(var onTap: ((String?) -> Unit)?) {
    fun handleTap(view: ChatLineUiView, x: Float, y: Float) {
        onTap?.invoke(view.link(at = Offset(x, y)))
    }
}

@Composable
fun ChatLineView(content: ChatLineContent, onTap: ((String?) -> Unit)? = null) {
    val sizesVersion by EmotesPlayer.shared.sizesVersion.collectAsState()
    val coordinator = remember { ChatLineViewCoordinator(onTap) }
    val viewState = remember { mutableStateOf<ChatLineUiView?>(null) }
    AndroidView(
        factory = { context ->
            ChatLineUiView(context).also { view ->
                viewState.value = view
                view.setOnTouchListener { touched, event ->
                    if (coordinator.onTap == null) {
                        false
                    } else {
                        if (event.actionMasked == MotionEvent.ACTION_UP) {
                            coordinator.handleTap(touched as ChatLineUiView, event.x, event.y)
                        }
                        true
                    }
                }
            }
        },
        update = { view ->
            coordinator.onTap = onTap
            view.setContent(content)
        },
        onRelease = { view ->
            view.unregister()
        },
    )
    LaunchedEffect(sizesVersion) {
        viewState.value?.requestLayout()
        viewState.value?.invalidate()
    }
}
