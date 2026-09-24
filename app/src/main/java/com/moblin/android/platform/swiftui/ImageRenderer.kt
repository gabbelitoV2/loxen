package com.moblin.android.platform.swiftui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Message
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.ViewParent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.moblin.android.platform.combine.ObservableObjectPublisher
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.offscreen.OffscreenDisplay
import com.moblin.android.platform.offscreen.OffscreenHostedView
import com.moblin.android.platform.offscreen.OffscreenSweep
import com.moblin.android.platform.offscreen.isOffscreenMainThread
import com.moblin.android.platform.offscreen.logOverlayOnce
import com.moblin.android.platform.offscreen.offscreenMainHandler
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val maximumSettlePasses = 40
private const val settleBudgetMs = 80L
private const val frameTimeoutMs = 100L

class ProposedViewSize(val width: Double?, val height: Double?) {
    constructor(size: CGSize) : this(size.width, size.height)

    companion object {
        val unspecified = ProposedViewSize(null, null)
        val zero = ProposedViewSize(0.0, 0.0)
        val infinity = ProposedViewSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    }
}

private fun runOnOffscreenMain(action: () -> Unit) {
    if (isOffscreenMainThread()) {
        action()
    } else {
        offscreenMainHandler.post(action)
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
        objectWillChange.onSubscribe = { runOnOffscreenMain { rendererHost.attachSafely() } }
        objectWillChange.onUnsubscribe = { runOnOffscreenMain { rendererHost.detachIfUnobserved() } }
        runOnOffscreenMain { rendererHost.attachSafely() }
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
    private var session: RendererSession? = null

    @Volatile
    private var released = false

    @Volatile
    private var scale = 1f

    @Volatile
    private var proposal = ProposedViewSize.unspecified

    fun update(scale: Float, proposal: ProposedViewSize) {
        this.scale = scale
        this.proposal = proposal
        runOnOffscreenMain { session?.configure(this.scale, this.proposal) }
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
        closeSession()
    }

    fun detachIfUnobserved() {
        if (publisherRef.get()?.hasSubscribers != true) {
            closeSession()
        }
    }

    fun render(isOpaque: Boolean, keepAttached: Boolean): Bitmap? {
        try {
            if (!attach()) {
                logOverlayOnce("ImageRenderer.cgImage before the host display is ready")
                return null
            }
            val bitmap = session?.capture(isOpaque) ?: return null
            PipelineStats.increment("ovRenders")
            return bitmap
        } finally {
            if (!keepAttached) {
                closeSession()
            }
        }
    }

    private fun attach(): Boolean {
        if (released) {
            return false
        }
        val current = session ?: openSession() ?: return false
        return current.attach()
    }

    private fun openSession(): RendererSession? {
        if (!isOffscreenMainThread()) {
            return null
        }
        val context = OffscreenDisplay.hostContext() ?: return null
        val hostRef = WeakReference(this)
        val created = RendererSession(context, content, publisherRef, scale, proposal) {
            hostRef.get()?.detachIfUnobserved()
        }
        session = created
        return created
    }

    private fun closeSession() {
        val current = session ?: return
        session = null
        current.close()
    }
}

private class RendererDispatcher : CoroutineDispatcher() {
    private val lock = Any()
    private val queue = ArrayDeque<Runnable>()
    private var posted = false
    private var draining = false
    private val drainRunnable = Runnable { drain() }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val post: Boolean
        synchronized(lock) {
            queue.addLast(block)
            post = !posted
            posted = true
        }
        if (post) {
            val message = Message.obtain(offscreenMainHandler, drainRunnable)
            message.isAsynchronous = true
            offscreenMainHandler.sendMessage(message)
        }
    }

    fun drain() {
        if (draining) {
            return
        }
        draining = true
        try {
            synchronized(lock) {
                posted = false
            }
            while (true) {
                val block = synchronized(lock) { queue.removeFirstOrNull() } ?: break
                try {
                    block.run()
                } catch (error: Throwable) {
                    logOverlayOnce("ImageRenderer task failed: $error")
                }
            }
        } finally {
            draining = false
        }
    }
}

private class RendererSession(
    context: Context,
    content: @Composable () -> Unit,
    publisherRef: WeakReference<ObservableObjectPublisher>,
    scale: Float,
    proposal: ProposedViewSize,
    onUnobserved: () -> Unit,
) {
    private val dispatcher = RendererDispatcher()
    private val clock = BroadcastFrameClock { scheduleFrame() }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(
        dispatcher + clock + job + CoroutineExceptionHandler { _, error ->
            logOverlayOnce("ImageRenderer content failed: $error")
        },
    )
    private val recomposer = Recomposer(scope.coroutineContext)
    private val view = ImageRendererView(context, content, publisherRef, onUnobserved)
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos -> onFrame(frameTimeNanos) }
    private val frameTimeout = Runnable { onFrameTimeout() }
    private var frameScheduled = false
    private var lastFrameTimeNanos = 0L
    private var closed = false

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        view.setParentCompositionContext(recomposer)
        view.configure(scale, proposal)
    }

    fun configure(scale: Float, proposal: ProposedViewSize) {
        if (!closed) {
            view.configure(scale, proposal)
        }
    }

    fun attach(): Boolean {
        if (closed) {
            return false
        }
        return OffscreenDisplay.attach(view) && view.isAttachedToWindow
    }

    fun capture(isOpaque: Boolean): Bitmap? {
        if (closed || !view.isAttachedToWindow) {
            return null
        }
        view.beginCapture()
        try {
            settle()
            view.measure(view.widthSpec, view.heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val width = view.measuredWidth
            val height = view.measuredHeight
            if (width <= 0 || height <= 0) {
                return null
            }
            return view.capture(width, height, isOpaque)
        } finally {
            view.endCapture()
        }
    }

    fun close() {
        if (closed) {
            return
        }
        closed = true
        view.close()
        OffscreenDisplay.detach(view)
        try {
            view.disposeComposition()
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer content failed to dispose: $error")
        }
        if (frameScheduled) {
            frameScheduled = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            offscreenMainHandler.removeCallbacks(frameTimeout)
        }
        recomposer.cancel()
        job.cancel()
        dispatcher.drain()
    }

    private fun settle() {
        val deadline = SystemClock.uptimeMillis() + settleBudgetMs
        repeat(maximumSettlePasses) {
            flushPendingWork()
            view.forceMeasure()
            view.measure(view.widthSpec, view.heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            Snapshot.sendApplyNotifications()
            dispatcher.drain()
            if (!recomposer.hasPendingWork || SystemClock.uptimeMillis() > deadline) {
                return
            }
            if (recomposer.currentState.value <= Recomposer.State.ShuttingDown) {
                return
            }
        }
    }

    private fun flushPendingWork() {
        Snapshot.sendApplyNotifications()
        dispatcher.drain()
        if (clock.hasAwaiters) {
            sendFrame(System.nanoTime())
            dispatcher.drain()
        }
    }

    private fun sendFrame(frameTimeNanos: Long) {
        val time = maxOf(frameTimeNanos, lastFrameTimeNanos)
        lastFrameTimeNanos = time
        clock.sendFrame(time)
    }

    private fun scheduleFrame() {
        runOnOffscreenMain {
            if (!closed && !frameScheduled) {
                frameScheduled = true
                Choreographer.getInstance().postFrameCallback(frameCallback)
                val message = Message.obtain(offscreenMainHandler, frameTimeout)
                message.isAsynchronous = true
                offscreenMainHandler.sendMessageDelayed(message, frameTimeoutMs)
            }
        }
    }

    private fun onFrameTimeout() {
        if (!frameScheduled) {
            return
        }
        logOverlayOnce("ImageRenderer frame ran from the timeout fallback (no vsync within ${frameTimeoutMs} ms)")
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        onFrame(System.nanoTime())
    }

    private fun onFrame(frameTimeNanos: Long) {
        frameScheduled = false
        offscreenMainHandler.removeCallbacks(frameTimeout)
        if (closed) {
            return
        }
        try {
            sendFrame(frameTimeNanos)
            dispatcher.drain()
        } catch (error: Throwable) {
            logOverlayOnce("ImageRenderer frame failed: $error")
        }
    }
}

private class ImageRendererView(
    context: Context,
    private val userContent: @Composable () -> Unit,
    private val publisherRef: WeakReference<ObservableObjectPublisher>,
    private val onUnobserved: () -> Unit,
) : AbstractComposeView(context), OffscreenHostedView {
    private val scaleState = mutableFloatStateOf(1f)
    private var ready = false
    private var closed = false
    private var capturing = false
    private var emissionPending = false
    private var changeGeneration = 0L
    private var capturedGeneration = 0L
    private val emitRunnable = Runnable { emit() }

    override var widthSpec: Int = unspecifiedSpec
        private set

    override var heightSpec: Int = unspecifiedSpec
        private set

    init {
        ready = true
    }

    fun configure(scale: Float, proposal: ProposedViewSize) {
        if (scaleState.floatValue != scale) {
            scaleState.floatValue = scale
        }
        val newWidthSpec = proposalSpec(proposal.width, scale)
        val newHeightSpec = proposalSpec(proposal.height, scale)
        if (newWidthSpec != widthSpec || newHeightSpec != heightSpec) {
            widthSpec = newWidthSpec
            heightSpec = newHeightSpec
            requestLayout()
        }
    }

    fun forceMeasure() {
        forceLayout()
        for (index in 0 until childCount) {
            getChildAt(index).forceLayout()
        }
    }

    fun beginCapture() {
        capturing = true
    }

    fun endCapture() {
        capturing = false
        capturedGeneration = changeGeneration
    }

    fun close() {
        closed = true
        offscreenMainHandler.removeCallbacks(emitRunnable)
        emissionPending = false
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

    override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        contentChanged()
    }

    @Deprecated("Deprecated in Java")
    override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): ViewParent? {
        @Suppress("DEPRECATION")
        val parent = super.invalidateChildInParent(location, dirty)
        contentChanged()
        return parent
    }

    override fun requestLayout() {
        super.requestLayout()
        contentChanged()
    }

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalDensity provides Density(scaleState.floatValue, 1f),
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalTextStyle provides SwiftUIFonts.body,
            LocalContentColor provides androidx.compose.ui.graphics.Color.Black,
        ) {
            Box {
                userContent()
            }
        }
    }

    private fun contentChanged() {
        if (!ready || closed || capturing || !isAttachedToWindow) {
            return
        }
        changeGeneration += 1
        if (emissionPending) {
            return
        }
        emissionPending = true
        val message = Message.obtain(offscreenMainHandler, emitRunnable)
        message.isAsynchronous = true
        offscreenMainHandler.sendMessage(message)
    }

    private fun emit() {
        emissionPending = false
        if (closed || changeGeneration == capturedGeneration) {
            return
        }
        val publisher = publisherRef.get()
        if (publisher == null || !publisher.hasSubscribers) {
            onUnobserved()
            return
        }
        publisher.send()
    }

    fun capture(width: Int, height: Int, isOpaque: Boolean): Bitmap? {
        return try {
            captureInSoftware(width, height, isOpaque)
        } catch (error: Throwable) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                throw error
            }
            logOverlayOnce("ImageRenderer software draw failed, using HardwareRenderer: $error")
            captureWithHardwareRenderer(this, width, height, isOpaque)
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
