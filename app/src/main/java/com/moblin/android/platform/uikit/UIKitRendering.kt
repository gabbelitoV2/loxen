package com.moblin.android.platform.uikit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import com.moblin.android.AppDelegate
import com.moblin.android.platform.coregraphics.CGColor
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.offscreen.logOverlayOnce
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

class UIGraphicsImageRendererFormat {
    var scale: Float = 1f
    var opaque: Boolean = false
}

class UIGraphicsImageRendererContext internal constructor(val cgContext: Canvas)

class UIGraphicsImageRenderer(
    size: CGSize,
    format: UIGraphicsImageRendererFormat = UIGraphicsImageRendererFormat(),
) {
    private val width = size.width
    private val height = size.height
    private val scale = format.scale
    private val opaque = format.opaque

    fun image(actions: (UIGraphicsImageRendererContext) -> Unit): Bitmap {
        val pixelWidth = pixels(width)
        val pixelHeight = pixels(height)
        if (pixelWidth <= 0 || pixelHeight <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val bitmap = try {
            Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)
        } catch (error: Throwable) {
            logOverlayOnce("UIGraphicsImageRenderer ${pixelWidth}x$pixelHeight failed: $error")
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        if (opaque) {
            bitmap.eraseColor(android.graphics.Color.BLACK)
        }
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        try {
            actions(UIGraphicsImageRendererContext(canvas))
        } catch (error: Throwable) {
            logOverlayOnce("UIGraphicsImageRenderer drawing failed: $error")
        }
        if (opaque) {
            bitmap.setHasAlpha(false)
        }
        return bitmap
    }

    private fun pixels(points: Double): Int {
        val value = points * scale
        if (value.isNaN() || value <= 0) {
            return 0
        }
        return ceil(value - 1e-6).toInt()
    }
}

private val viewFrames: MutableMap<View, CGRect> = Collections.synchronizedMap(WeakHashMap())

private fun exactly(size: Int): Int = View.MeasureSpec.makeMeasureSpec(size.coerceAtLeast(0), View.MeasureSpec.EXACTLY)

private class FrameEdges(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    constructor(frame: CGRect) : this(
        edge(frame.minX),
        edge(frame.minY),
        edge(frame.maxX),
        edge(frame.maxY),
    )

    val width: Int
        get() = (right - left).coerceAtLeast(0)

    val height: Int
        get() = (bottom - top).coerceAtLeast(0)

    companion object {
        private fun edge(value: Double): Int = if (value.isFinite()) value.roundToInt() else 0
    }
}

private fun storedFrame(view: View): CGRect? = viewFrames[view]

private fun applyFrame(view: View, frame: CGRect) {
    viewFrames[view] = frame
    val edges = FrameEdges(frame)
    view.measure(exactly(edges.width), exactly(edges.height))
    view.layout(edges.left, edges.top, edges.left + edges.width, edges.top + edges.height)
}

private fun currentFrame(view: View): CGRect = storedFrame(view) ?: CGRect(
    x = view.left.toDouble(),
    y = view.top.toDouble(),
    width = view.width.toDouble(),
    height = view.height.toDouble(),
)

var View.frame: CGRect
    get() = currentFrame(this)
    set(value) = applyFrame(this, value)

fun View.removeFromSuperview() {
    (parent as? ViewGroup)?.removeView(this)
}

private fun CGColor.toArgb(): Int {
    fun channel(value: Double): Int = (value.coerceIn(0.0, 1.0) * 255.0).roundToInt()
    return android.graphics.Color.argb(channel(alpha), channel(red), channel(green), channel(blue))
}

class CALayer {
    var frame: CGRect = CGRect.zero
    var backgroundColor: CGColor? = null
    var actions: Map<String, Any?>? = null
    private val sublayers = mutableListOf<CALayer>()
    private var superlayer: CALayer? = null
    private val paint = Paint().apply { isAntiAlias = false }

    fun addSublayer(layer: CALayer) {
        if (layer === this) {
            return
        }
        layer.removeFromSuperlayer()
        sublayers.add(layer)
        layer.superlayer = this
    }

    fun removeFromSuperlayer() {
        superlayer?.sublayers?.remove(this)
        superlayer = null
    }

    internal fun drawBackground(canvas: Canvas, width: Float, height: Float) {
        val color = backgroundColor ?: return
        paint.color = color.toArgb()
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    internal fun drawSublayers(canvas: Canvas) {
        for (layer in sublayers.toList()) {
            layer.draw(canvas)
        }
    }

    private fun draw(canvas: Canvas) {
        val edges = FrameEdges(frame)
        val save = canvas.save()
        canvas.translate(edges.left.toFloat(), edges.top.toFloat())
        drawBackground(canvas, edges.width.toFloat(), edges.height.toFloat())
        drawSublayers(canvas)
        canvas.restoreToCount(save)
    }
}

open class UIView(context: Context = AppDelegate.context) : FrameLayout(context) {
    val layer: CALayer = CALayer()

    var backgroundColor: Color? = null
        set(value) {
            field = value
            layer.backgroundColor = value?.let { CGColor(color = it) }
            invalidate()
        }

    var frame: CGRect
        get() = currentFrame(this)
        set(value) {
            layer.frame = CGRect(x = 0.0, y = 0.0, width = value.width, height = value.height)
            applyFrame(this, value)
        }

    val bounds: CGRect
        get() {
            val current = frame
            return CGRect(x = 0.0, y = 0.0, width = current.width, height = current.height)
        }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    fun addSubview(view: View) {
        if (view.parent === this) {
            bringChildToFront(view)
            return
        }
        (view.parent as? ViewGroup)?.removeView(view)
        addView(view)
    }

    fun layoutIfNeeded() {
        if (isLayoutRequested) {
            storedFrame(this)?.let { applyFrame(this, it) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val childFrame = storedFrame(child)
            if (childFrame != null) {
                val edges = FrameEdges(childFrame)
                child.measure(exactly(edges.width), exactly(edges.height))
            } else {
                child.measure(unspecified, unspecified)
            }
        }
        val own = storedFrame(this)?.let { FrameEdges(it) }
        setMeasuredDimension(
            resolveSize(own?.width ?: 0, widthMeasureSpec),
            resolveSize(own?.height ?: 0, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val childFrame = storedFrame(child)
            if (childFrame != null) {
                val edges = FrameEdges(childFrame)
                child.layout(edges.left, edges.top, edges.left + edges.width, edges.top + edges.height)
            } else {
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        layer.drawBackground(canvas, width.toFloat(), height.toFloat())
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        layer.drawSublayers(canvas)
    }
}
