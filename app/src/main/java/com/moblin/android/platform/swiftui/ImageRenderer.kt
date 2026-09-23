package com.moblin.android.platform.swiftui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.moblin.android.platform.combine.ObservableObjectPublisher
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.offscreen.OffscreenDisplay
import com.moblin.android.platform.offscreen.OffscreenHostedView
import com.moblin.android.platform.offscreen.OffscreenSweep
import com.moblin.android.platform.offscreen.isOffscreenMainThread
import com.moblin.android.platform.offscreen.logOverlayOnce
import com.moblin.android.platform.offscreen.offscreenMainHandler
import java.lang.ref.WeakReference
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProposedViewSize(val width: Double?, val height: Double?) {
    constructor(size: CGSize) : this(size.width, size.height)

    companion object {
        val unspecified = ProposedViewSize(null, null)
        val zero = ProposedViewSize(0.0, 0.0)
        val infinity = ProposedViewSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    }
}

class ImageRenderer(content: @Composable () -> Unit) {
    val objectWillChange = ObservableObjectPublisher()
    private val host = ImageRendererHost(content, objectWillChange)

    var scale: Float = 1f
        set(value) {
            field = value
            host.update(value, proposedSize)
        }

    var proposedSize: ProposedViewSize = ProposedViewSize.unspecified
        set(value) {
            field = value
            host.update(scale, value)
        }

    var isOpaque: Boolean = false

    val cgImage: Bitmap?
        get() = render()

    val uiImage: Bitmap?
        get() = render()

    init {
        val rendererHost = host
        OffscreenSweep.track(this) { rendererHost.release() }
        if (isOffscreenMainThread()) {
            rendererHost.attachSafely()
        } else {
            offscreenMainHandler.post { rendererHost.attachSafely() }
        }
    }

    private fun render(): Bitmap? {
        if (!isOffscreenMainThread()) {
            logOverlayOnce("ImageRenderer.cgImage called off the main thread")
            return null
        }
        return try {
            host.render(isOpaque = isOpaque, keepAttached = objectWillChange.hasSubscribers)
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer.cgImage failed: $error")
            null
        }
    }
}

private class ImageRendererHost(
    private val content: @Composable () -> Unit,
    publisher: ObservableObjectPublisher,
) {
    private val publisherRef = WeakReference(publisher)
    private var view: ImageRendererView? = null
    private var recomposerScope: CoroutineScope? = null
    private var released = false
    private var scale = 1f
    private var proposal = ProposedViewSize.unspecified

    fun update(scale: Float, proposal: ProposedViewSize) {
        this.scale = scale
        this.proposal = proposal
        if (isOffscreenMainThread()) {
            view?.configure(scale, proposal)
        } else {
            offscreenMainHandler.post { view?.configure(this.scale, this.proposal) }
        }
    }

    fun attach(): Boolean {
        if (released) {
            return false
        }
        val view = view ?: createView() ?: return false
        return OffscreenDisplay.attach(view)
    }

    fun attachSafely() {
        try {
            attach()
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer content failed to attach: $error")
        }
    }

    fun release() {
        released = true
        view?.let { OffscreenDisplay.detach(it) }
        view = null
        recomposerScope?.cancel()
        recomposerScope = null
    }

    fun render(isOpaque: Boolean, keepAttached: Boolean): Bitmap? {
        if (!attach()) {
            logOverlayOnce("ImageRenderer.cgImage before the host display is ready")
            if (!keepAttached) {
                view?.let { OffscreenDisplay.detach(it) }
            }
            return null
        }
        val view = view ?: return null
        try {
            if (!view.isAttachedToWindow) {
                return null
            }
            view.measure(view.widthSpec, view.heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val width = view.measuredWidth
            val height = view.measuredHeight
            if (width <= 0 || height <= 0) {
                return null
            }
            val bitmap = view.capture(width, height, isOpaque)
            if (bitmap != null) {
                PipelineStats.increment("ovRenders")
            }
            return bitmap
        } finally {
            if (!keepAttached) {
                OffscreenDisplay.detach(view)
            }
        }
    }

    private fun createView(): ImageRendererView? {
        if (!isOffscreenMainThread()) {
            return null
        }
        val context = OffscreenDisplay.hostContext() ?: return null
        val view = ImageRendererView(context, content, publisherRef)
        val scope = CoroutineScope(
            AndroidUiDispatcher.Main + SupervisorJob() + CoroutineExceptionHandler { _, error ->
                logOverlayOnce("ImageRenderer content failed: $error")
            },
        )
        val recomposer = Recomposer(scope.coroutineContext)
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        recomposerScope = scope
        view.setParentCompositionContext(recomposer)
        view.configure(scale, proposal)
        this.view = view
        return view
    }
}

private class ImageRendererView(
    context: Context,
    private val userContent: @Composable () -> Unit,
    private val publisherRef: WeakReference<ObservableObjectPublisher>,
) : AbstractComposeView(context), OffscreenHostedView {
    private val scaleState = mutableFloatStateOf(1f)
    private var suppressed = false
    private var emissionPending = false

    override var widthSpec: Int = unspecifiedSpec
        private set

    override var heightSpec: Int = unspecifiedSpec
        private set

    fun configure(scale: Float, proposal: ProposedViewSize) {
        scaleState.floatValue = scale
        val newWidthSpec = proposalSpec(proposal.width, scale)
        val newHeightSpec = proposalSpec(proposal.height, scale)
        if (newWidthSpec != widthSpec || newHeightSpec != heightSpec) {
            widthSpec = newWidthSpec
            heightSpec = newHeightSpec
            requestLayout()
        }
    }

    override fun onAttachedToWindow() {
        try {
            super.onAttachedToWindow()
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer content failed to compose: $error")
        }
    }

    override fun onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow()
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer content failed to dispose: $error")
        }
    }

    @Composable
    override fun Content() {
        val density = Density(scaleState.floatValue, 1f)
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalLayoutDirection provides LayoutDirection.Ltr,
        ) {
            Box(
                Modifier.drawWithContent {
                    drawContent()
                    onDrawn()
                },
            ) {
                userContent()
            }
        }
    }

    private fun onDrawn() {
        if (suppressed || emissionPending) {
            return
        }
        emissionPending = true
        offscreenMainHandler.post {
            emissionPending = false
            publisherRef.get()?.send()
        }
    }

    fun capture(width: Int, height: Int, isOpaque: Boolean): Bitmap? {
        suppressed = true
        try {
            return try {
                captureInSoftware(width, height, isOpaque)
            } catch (error: Throwable) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    throw error
                }
                logOverlayOnce("ImageRenderer software draw failed, using HardwareRenderer: $error")
                captureWithHardwareRenderer(this, width, height, isOpaque)
            }
        } finally {
            suppressed = false
        }
    }

    private fun captureInSoftware(width: Int, height: Int, isOpaque: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (isOpaque) {
            bitmap.eraseColor(Color.BLACK)
        }
        draw(Canvas(bitmap))
        if (isOpaque) {
            bitmap.setHasAlpha(false)
        }
        return bitmap
    }

    companion object {
        private val unspecifiedSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        private fun proposalSpec(value: Double?, scale: Float): Int {
            if (value == null || value.isNaN() || value.isInfinite()) {
                return unspecifiedSpec
            }
            val pixels = ceil(value.coerceAtLeast(0.0) * scale - 1e-6).toInt().coerceAtLeast(0)
            return View.MeasureSpec.makeMeasureSpec(pixels, View.MeasureSpec.AT_MOST)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun captureWithHardwareRenderer(view: View, width: Int, height: Int, isOpaque: Boolean): Bitmap? {
    val reader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        1,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    )
    val renderer = HardwareRenderer()
    val node = RenderNode("MoblinImageRenderer")
    try {
        node.setPosition(0, 0, width, height)
        val canvas = node.beginRecording(width, height)
        try {
            if (isOpaque) {
                canvas.drawColor(Color.BLACK)
            }
            view.draw(canvas)
        } finally {
            node.endRecording()
        }
        renderer.setOpaque(isOpaque)
        renderer.setContentRoot(node)
        renderer.setSurface(reader.surface)
        renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
        val image = reader.acquireLatestImage() ?: return null
        image.use {
            val buffer = it.hardwareBuffer ?: return null
            buffer.use { hardwareBuffer ->
                val wrapped = Bitmap.wrapHardwareBuffer(hardwareBuffer, ColorSpace.get(ColorSpace.Named.SRGB))
                    ?: return null
                val copy = wrapped.copy(Bitmap.Config.ARGB_8888, false)
                wrapped.recycle()
                return copy
            }
        }
    } finally {
        renderer.destroy()
        node.discardDisplayList()
        reader.close()
    }
}
