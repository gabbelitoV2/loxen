package com.moblin.android.platform.webkit

import android.app.Activity
import android.app.Application
import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.webkit.ScriptHandler
import com.moblin.android.AppDelegate
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.core.PipelineStats
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

private const val maxReaderImages = 3
private const val settleMs = 200L

internal object WebKitSnapshotThread {
    val handler: Handler by lazy {
        val thread = HandlerThread("MoblinWebSnapshot")
        thread.start()
        Handler(thread.looper)
    }
}

internal fun configurePresentationWindow(presentation: Presentation) {
    val window = presentation.window ?: return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        window.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
    }
    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    window.setFormat(PixelFormat.TRANSLUCENT)
    window.setWindowAnimations(0)
    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    window.addFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
    )
    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    presentation.setCancelable(false)
}

internal object WebKitCompositionProbe {
    @Volatile
    var preservesAlpha: Boolean = true
        private set

    private var started = false

    fun start() {
        if (started || !isWebKitMainThread()) {
            return
        }
        started = true
        val size = 8
        var reader: ImageReader? = null
        var display: VirtualDisplay? = null
        var presentation: Presentation? = null
        val finished = AtomicBoolean(false)
        fun cleanUp() {
            runCatching { presentation?.dismiss() }
            runCatching { reader?.setOnImageAvailableListener(null, null) }
            runCatching { display?.release() }
            runCatching { reader?.close() }
        }
        fun finish(result: Boolean?, message: String) {
            if (!finished.compareAndSet(false, true)) {
                return
            }
            if (result != null) {
                preservesAlpha = result
            }
            WebKitLog.info(message)
            webKitMainHandler.post { cleanUp() }
        }
        try {
            val context = AppDelegate.context
            val displayManager = context.getSystemService(DisplayManager::class.java)
                ?: throw IllegalStateException("no DisplayManager")
            val probeReader = ImageReader.newInstance(size, size, PixelFormat.RGBA_8888, 2)
            reader = probeReader
            probeReader.setOnImageAvailableListener({ source ->
                val image = try {
                    source.acquireLatestImage()
                } catch (_: Throwable) {
                    null
                } ?: return@setOnImageAvailableListener
                try {
                    val plane = image.planes[0]
                    val offset = (size / 2) * plane.rowStride + (size / 2) * plane.pixelStride
                    val buffer = plane.buffer
                    val first = buffer.get(offset).toInt() and 0xFF
                    val alpha = buffer.get(offset + 3).toInt() and 0xFF
                    if (first != 0) {
                        val keeps = alpha < 250
                        finish(
                            keeps,
                            "virtual display composition keeps alpha: ${if (keeps) "yes" else "no"} " +
                                "(probe value $first, alpha $alpha); snapshots use " +
                                if (keeps) "the display reader" else "PixelCopy",
                        )
                    }
                } catch (error: Throwable) {
                    finish(null, "virtual display alpha probe failed: $error")
                } finally {
                    image.close()
                }
            }, WebKitSnapshotThread.handler)
            val probeDisplay = displayManager.createVirtualDisplay(
                "MoblinWebProbe",
                size,
                size,
                DisplayMetrics.DENSITY_MEDIUM,
                probeReader.surface,
                0,
            ) ?: throw IllegalStateException("no virtual display")
            display = probeDisplay
            val probePresentation = Presentation(context, probeDisplay.display)
            presentation = probePresentation
            configurePresentationWindow(probePresentation)
            val view = View(probePresentation.context)
            view.setBackgroundColor(Color.argb(128, 255, 255, 255))
            probePresentation.setContentView(
                view,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            probePresentation.show()
            webKitMainHandler.postDelayed({
                finish(null, "virtual display alpha probe timed out; snapshots use the display reader")
            }, 3000)
        } catch (error: Throwable) {
            finish(null, "virtual display alpha probe failed: $error")
        }
    }
}

internal object WebKitHosts {
    private class Owner(owner: Any, queue: ReferenceQueue<Any>, val host: WebViewHost) :
        WeakReference<Any>(owner, queue)

    private val queue = ReferenceQueue<Any>()
    private val owners = HashSet<Owner>()
    private var callbacksRegistered = false
    private var sweepTimerRunning = false
    private val collector by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "MoblinWebGc").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }
        }
    }
    private val collectRunnable = Runnable {
        try {
            collector.execute {
                Runtime.getRuntime().gc()
                webKitMainHandler.postDelayed({ sweep() }, 200)
                webKitMainHandler.postDelayed({ sweep() }, 1500)
            }
        } catch (error: Throwable) {
            WebKitLog.once("collect:$error", "web view collection failed: $error")
        }
    }

    fun track(owner: Any, host: WebViewHost) {
        sweep()
        owners.add(Owner(owner, queue, host))
        registerCallbacks()
        startSweepTimer()
        requestCollection()
    }

    fun requestCollection() {
        if (owners.isEmpty()) {
            return
        }
        webKitMainHandler.removeCallbacks(collectRunnable)
        webKitMainHandler.postDelayed(collectRunnable, 1000)
    }

    private fun sweep() {
        while (true) {
            val owner = queue.poll() as? Owner ?: break
            if (!owners.remove(owner)) {
                continue
            }
            try {
                owner.host.destroy()
            } catch (error: Throwable) {
                WebKitLog.once("sweep:$error", "web view cleanup failed: $error")
            }
        }
    }

    private fun startSweepTimer() {
        if (sweepTimerRunning) {
            return
        }
        sweepTimerRunning = true
        webKitMainHandler.postDelayed({ sweepTick() }, 5000)
    }

    private fun sweepTick() {
        sweep()
        sweepTimerRunning = false
        if (owners.isNotEmpty()) {
            startSweepTimer()
        }
    }

    private fun refreshAll() {
        for (delayMs in longArrayOf(0L, 150L, 500L, 1500L)) {
            webKitMainHandler.postDelayed({
                for (owner in owners.toList()) {
                    owner.host.refreshPlacement()
                }
            }, delayMs)
        }
    }

    private fun registerCallbacks() {
        if (callbacksRegistered) {
            return
        }
        val application = (AppDelegate.context as? Application)
            ?: (AppDelegate.context.applicationContext as? Application)
            ?: return
        callbacksRegistered = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityStarted(activity: Activity) {
                refreshAll()
            }

            override fun onActivityResumed(activity: Activity) {
                refreshAll()
            }

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) {
                refreshAll()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}

internal class HostedWebView(context: Context) : WebView(context) {
    var frameWidth = 1
    var frameHeight = 1
    var fillsParent = false
        set(value) {
            field = value
            updateScale()
        }
    var placementListener: (() -> Unit)? = null
    val scriptHandlers = mutableListOf<ScriptHandler>()
    private var observedParent: View? = null
    private val parentLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateScale()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(frameWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(frameHeight, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateScale()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observeParent()
        placementListener?.invoke()
    }

    override fun onDetachedFromWindow() {
        stopObservingParent()
        super.onDetachedFromWindow()
        placementListener?.invoke()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        placementListener?.invoke()
    }

    fun updateScale() {
        val parentView = parent as? View
        var newScaleX = 1f
        var newScaleY = 1f
        if (fillsParent && parentView != null && parentView.width > 0 && parentView.height > 0) {
            newScaleX = parentView.width / frameWidth.toFloat()
            newScaleY = parentView.height / frameHeight.toFloat()
        }
        if (pivotX != 0f) {
            pivotX = 0f
        }
        if (pivotY != 0f) {
            pivotY = 0f
        }
        if (scaleX != newScaleX) {
            scaleX = newScaleX
        }
        if (scaleY != newScaleY) {
            scaleY = newScaleY
        }
    }

    private fun observeParent() {
        val parentView = parent as? View ?: return
        if (observedParent === parentView) {
            return
        }
        stopObservingParent()
        parentView.addOnLayoutChangeListener(parentLayoutListener)
        observedParent = parentView
    }

    private fun stopObservingParent() {
        observedParent?.removeOnLayoutChangeListener(parentLayoutListener)
        observedParent = null
    }
}

private val hostIds = AtomicInteger(0)

internal class WebViewHost {
    var webView: HostedWebView? = null
        private set
    private var container: FrameLayout? = null
    private var presentation: Presentation? = null
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var reader: ImageReader? = null
    private var width = 1
    private var height = 1
    private val name = "MoblinWeb${hostIds.incrementAndGet()}"
    private var borrowCount = 0
    private var borrowVisible = true
    private var borrowParent: WeakReference<ViewGroup>? = null
    private var borrowLayoutParams: ViewGroup.LayoutParams? = null
    private var placementPosted = false
    private var homeSinceMs = 0L
    private var started = false

    @Volatile
    private var destroyed = false
    private val imageLock = Any()
    private var latestImage: Image? = null

    @Volatile
    private var latestImageMs = 0L
    private var scratch: ByteArray? = null
    private val snapshotLogs = ConcurrentHashMap<String, Int>()
    private val borrowParentAttachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            schedulePlacement()
        }

        override fun onViewDetachedFromWindow(view: View) {
            schedulePlacement()
        }
    }

    val isStarted: Boolean
        get() = started

    val viewContext: Context
        get() = presentation?.context ?: AppDelegate.context

    fun start(width: Int, height: Int) {
        if (started) {
            return
        }
        started = true
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        WebKitCompositionProbe.start()
        createDisplay()
    }

    fun attach(view: HostedWebView) {
        view.frameWidth = width
        view.frameHeight = height
        view.placementListener = { schedulePlacement() }
        webView = view
        val home = container
        if (home != null) {
            home.addView(view, FrameLayout.LayoutParams(width, height))
            enteredHome(view)
        } else {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
    }

    fun detachWebView(view: WebView) {
        if (webView === view) {
            webView = null
        }
    }

    fun resize(width: Int, height: Int) {
        val newWidth = width.coerceAtLeast(1)
        val newHeight = height.coerceAtLeast(1)
        if (destroyed || (newWidth == this.width && newHeight == this.height)) {
            return
        }
        this.width = newWidth
        this.height = newHeight
        webView?.let {
            it.frameWidth = newWidth
            it.frameHeight = newHeight
            it.requestLayout()
            it.updateScale()
        }
        val display = virtualDisplay ?: return
        try {
            val view = webView
            val home = container
            if (view != null && view.parent === home) {
                home?.removeView(view)
            }
            runCatching { presentation?.dismiss() }
            presentation = null
            container = null
            val oldReader = reader
            val newReader = createReader()
            display.resize(newWidth, newHeight, DisplayMetrics.DENSITY_MEDIUM)
            display.surface = newReader.surface
            synchronized(imageLock) {
                latestImage?.close()
                latestImage = null
            }
            oldReader?.setOnImageAvailableListener(null, null)
            runCatching { oldReader?.close() }
            createPresentation(display)
            updatePlacement()
        } catch (error: Throwable) {
            WebKitLog.error("$name: resize to ${newWidth}x$newHeight failed: $error")
        }
    }

    fun borrow(): WebView? {
        val view = webView ?: return null
        borrowCount += 1
        val home = container
        if (home != null && view.parent === home) {
            home.removeView(view)
        }
        view.fillsParent = true
        WebKitLog.once(
            "borrowed",
            "web view borrowed by the interactive overlay; snapshots use software drawing while it is on screen",
        )
        return view
    }

    fun giveBack() {
        if (borrowCount == 0) {
            return
        }
        borrowCount -= 1
        if (borrowCount == 0) {
            setBorrowParent(null)
            borrowVisible = true
            WebKitHosts.requestCollection()
        }
        updatePlacement()
    }

    fun setBorrowVisible(visible: Boolean) {
        if (borrowVisible == visible) {
            return
        }
        borrowVisible = visible
        schedulePlacement()
    }

    fun refreshPlacement() {
        if (borrowCount > 0) {
            schedulePlacement()
        }
    }

    fun snapshot(configuration: WKSnapshotConfiguration?, completionHandler: (Bitmap?, Throwable?) -> Unit) {
        val view = webView
        if (destroyed || view == null) {
            deliver(null, IllegalStateException("The web view is gone"), completionHandler)
            return
        }
        try {
            val home = container
            val atHome = home != null && view.parent === home && view.isAttachedToWindow
            if (home != null && !atHome && (!view.isAttachedToWindow || view.windowVisibility != View.VISIBLE)) {
                WebKitLog.once(
                    "$name:betweenWindows",
                    "$name: web view is between windows; keeping the previous snapshot",
                )
                deliver(null, IllegalStateException("The web view is between windows"), completionHandler)
                return
            }
            val settled = atHome && latestImageMs >= homeSinceMs + settleMs
            when {
                !atHome || !settled -> snapshotSoftware(view, configuration, completionHandler, atHome)
                WebKitCompositionProbe.preservesAlpha -> snapshotReader(configuration, completionHandler)
                else -> snapshotPixelCopy(view, configuration, completionHandler)
            }
        } catch (error: Throwable) {
            WebKitLog.once("snapshot:$error", "$name: snapshot failed: $error")
            deliver(null, error, completionHandler)
        }
    }

    fun destroy() {
        if (destroyed) {
            return
        }
        destroyed = true
        WebKitLog.info("$name: web view released")
        synchronized(imageLock) {
            latestImage?.close()
            latestImage = null
        }
        setBorrowParent(null)
        borrowCount = 0
        val view = webView
        webView = null
        if (view != null) {
            destroyWebView(view)
        }
        releaseDisplay()
    }

    private fun createDisplay() {
        try {
            val displayManager = AppDelegate.context.getSystemService(DisplayManager::class.java)
                ?: throw IllegalStateException("no DisplayManager")
            val newReader = createReader()
            val display = displayManager.createVirtualDisplay(
                name,
                width,
                height,
                DisplayMetrics.DENSITY_MEDIUM,
                newReader.surface,
                0,
            ) ?: throw IllegalStateException("no virtual display")
            virtualDisplay = display
            createPresentation(display)
            WebKitLog.info("$name: offscreen web view host ready ${width}x$height")
        } catch (error: Throwable) {
            WebKitLog.error("$name: offscreen web view host failed: $error")
            releaseDisplay()
        }
    }

    private fun createReader(): ImageReader {
        val newReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, maxReaderImages)
        newReader.setOnImageAvailableListener({ onImageAvailable(it) }, WebKitSnapshotThread.handler)
        reader = newReader
        return newReader
    }

    private fun createPresentation(display: VirtualDisplay) {
        val newPresentation = Presentation(AppDelegate.context, display.display)
        configurePresentationWindow(newPresentation)
        val home = FrameLayout(newPresentation.context)
        home.setBackgroundColor(Color.TRANSPARENT)
        newPresentation.setContentView(
            home,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        newPresentation.setOnDismissListener {
            if (!destroyed && presentation === newPresentation) {
                WebKitLog.once("$name:dismissed", "$name: presentation dismissed by the system")
            }
        }
        presentation = newPresentation
        container = home
        newPresentation.show()
    }

    private fun releaseDisplay() {
        val oldPresentation = presentation
        presentation = null
        container = null
        runCatching { oldPresentation?.dismiss() }
        val oldReader = reader
        reader = null
        runCatching { oldReader?.setOnImageAvailableListener(null, null) }
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { oldReader?.close() }
    }

    private fun onImageAvailable(source: ImageReader) {
        val image = try {
            source.acquireLatestImage()
        } catch (_: Throwable) {
            null
        } ?: return
        synchronized(imageLock) {
            if (destroyed || source !== reader) {
                image.close()
                return
            }
            latestImage?.close()
            latestImage = image
            latestImageMs = SystemClock.uptimeMillis()
        }
    }

    private fun schedulePlacement() {
        if (placementPosted || destroyed) {
            return
        }
        placementPosted = true
        webKitMainHandler.post {
            placementPosted = false
            updatePlacement()
        }
    }

    private fun setBorrowParent(parent: ViewGroup?) {
        val previous = borrowParent?.get()
        if (parent != null && previous === parent) {
            return
        }
        previous?.removeOnAttachStateChangeListener(borrowParentAttachListener)
        if (parent == null) {
            borrowParent = null
            borrowLayoutParams = null
            return
        }
        borrowParent = WeakReference(parent)
        borrowLayoutParams = null
        parent.addOnAttachStateChangeListener(borrowParentAttachListener)
    }

    private fun updatePlacement() {
        if (destroyed) {
            return
        }
        val view = webView ?: return
        try {
            placeWebView(view)
        } catch (error: Throwable) {
            WebKitLog.once("placement:$error", "$name: moving the web view failed: $error")
        }
    }

    private fun placeWebView(view: HostedWebView) {
        val home = container
        val currentParent = view.parent as? ViewGroup
        if (borrowCount > 0 && currentParent != null && currentParent !== home) {
            if (borrowParent?.get() !== currentParent) {
                setBorrowParent(currentParent)
            }
            borrowLayoutParams = view.layoutParams
        }
        val target = if (borrowCount > 0) borrowParent?.get() else null
        if (borrowCount > 0 && target == null && currentParent == null) {
            return
        }
        if (target != null && borrowVisible) {
            if (target.isAttachedToWindow && target.windowVisibility == View.VISIBLE) {
                if (currentParent !== target) {
                    currentParent?.removeView(view)
                    val params = borrowLayoutParams
                    if (params != null) {
                        target.addView(view, params)
                    } else {
                        target.addView(view)
                    }
                    WebKitLog.info("$name: web view back in the interactive overlay")
                }
                view.fillsParent = true
                return
            }
            if (!target.isAttachedToWindow && currentParent === target) {
                return
            }
        }
        if (home == null || currentParent === home) {
            view.fillsParent = false
            return
        }
        currentParent?.removeView(view)
        view.fillsParent = false
        home.addView(view, FrameLayout.LayoutParams(width, height))
        enteredHome(view)
        if (borrowCount > 0) {
            WebKitLog.info("$name: interactive overlay not shown; web view renders offscreen")
        }
    }

    private fun enteredHome(view: View) {
        homeSinceMs = SystemClock.uptimeMillis()
        view.postInvalidateDelayed(settleMs + 50)
        view.postInvalidateDelayed(settleMs * 3)
    }

    private fun snapshotSoftware(
        view: WebView,
        configuration: WKSnapshotConfiguration?,
        completionHandler: (Bitmap?, Throwable?) -> Unit,
        atHome: Boolean,
    ) {
        if (!atHome) {
            WebKitLog.once("software", "web view snapshots use software drawing while the view is borrowed")
        }
        val bitmap = try {
            drawSoftware(view)
        } catch (error: Throwable) {
            WebKitLog.once("softwareFailed:$error", "$name: software snapshot failed: $error")
            null
        }
        if (bitmap == null) {
            deliver(null, IllegalStateException("The web view has no size"), completionHandler)
            return
        }
        logSnapshot(bitmap, "software drawing")
        deliverComposed(bitmap, configuration, completionHandler)
    }

    private fun drawSoftware(view: WebView): Bitmap? {
        if (view.width <= 0 || view.height <= 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(width / view.width.toFloat(), height / view.height.toFloat())
        canvas.translate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
        view.draw(canvas)
        return bitmap
    }

    private fun snapshotReader(
        configuration: WKSnapshotConfiguration?,
        completionHandler: (Bitmap?, Throwable?) -> Unit,
    ) {
        WebKitSnapshotThread.handler.post {
            val bitmap = try {
                copyLatestImage()
            } catch (error: Throwable) {
                WebKitLog.once("readerFailed:$error", "$name: display reader snapshot failed: $error")
                null
            }
            if (bitmap == null) {
                deliver(null, IllegalStateException("No web view frame yet"), completionHandler)
                return@post
            }
            logSnapshot(bitmap, "display reader")
            deliverComposed(bitmap, configuration, completionHandler)
        }
    }

    private fun snapshotPixelCopy(
        view: WebView,
        configuration: WKSnapshotConfiguration?,
        completionHandler: (Bitmap?, Throwable?) -> Unit,
    ) {
        val window = presentation?.window
        if (window == null) {
            snapshotSoftware(view, configuration, completionHandler, true)
            return
        }
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(window, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    logSnapshot(bitmap, "PixelCopy")
                    deliverComposed(bitmap, configuration, completionHandler)
                } else {
                    deliver(null, IllegalStateException("PixelCopy failed with $result"), completionHandler)
                }
            }, WebKitSnapshotThread.handler)
        } catch (error: Throwable) {
            WebKitLog.once("pixelCopyFailed:$error", "$name: PixelCopy snapshot failed: $error")
            snapshotSoftware(view, configuration, completionHandler, true)
        }
    }

    private fun copyLatestImage(): Bitmap? {
        synchronized(imageLock) {
            if (destroyed) {
                return null
            }
            val image = latestImage ?: return null
            return copyImage(image)
        }
    }

    private fun copyImage(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        if (plane.pixelStride != 4) {
            return null
        }
        val imageWidth = image.width
        val imageHeight = image.height
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null
        }
        val rowBytes = imageWidth * 4
        val rowStride = plane.rowStride
        val buffer = plane.buffer.duplicate()
        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        if (rowStride == rowBytes && buffer.capacity() >= rowBytes * imageHeight) {
            buffer.clear()
            buffer.limit(rowBytes * imageHeight)
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            val size = rowBytes * imageHeight
            var packed = scratch
            if (packed == null || packed.size != size) {
                packed = ByteArray(size)
                scratch = packed
            }
            buffer.clear()
            for (row in 0 until imageHeight) {
                buffer.position(row * rowStride)
                buffer.get(packed, row * rowBytes, rowBytes)
            }
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(packed))
        }
        return bitmap
    }

    private fun composeSnapshot(source: Bitmap, configuration: WKSnapshotConfiguration?): Bitmap {
        val bounds = CGRect(0.0, 0.0, width.toDouble(), height.toDouble())
        val requested = configuration?.rect
        val rect = if (requested == null || requested.isNull || requested.isInfinite || requested.isEmpty) {
            bounds
        } else {
            requested.intersection(bounds).takeIf { !it.isNull && !it.isEmpty } ?: bounds
        }
        val pointsWidth = configuration?.snapshotWidth ?: rect.width
        val scale = com.moblin.android.various.utils.screenScale().toDouble()
        val outputWidth = (pointsWidth * scale).roundToInt().coerceAtLeast(1)
        val outputHeight = (outputWidth * rect.height / rect.width).roundToInt().coerceAtLeast(1)
        val sourceScaleX = source.width / width.toDouble()
        val sourceScaleY = source.height / height.toDouble()
        val sourceRect = Rect(
            (rect.minX * sourceScaleX).roundToInt(),
            (rect.minY * sourceScaleY).roundToInt(),
            (rect.maxX * sourceScaleX).roundToInt(),
            (rect.maxY * sourceScaleY).roundToInt(),
        )
        if (sourceRect.left == 0 &&
            sourceRect.top == 0 &&
            sourceRect.right == source.width &&
            sourceRect.bottom == source.height &&
            outputWidth == source.width &&
            outputHeight == source.height
        ) {
            return source
        }
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            source,
            sourceRect,
            Rect(0, 0, outputWidth, outputHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        return output
    }

    private fun logSnapshot(bitmap: Bitmap, path: String) {
        val count = snapshotLogs.merge(path, 1, Int::plus) ?: 1
        if (count > 300 || snapshotLogs.containsKey("$path+content")) {
            return
        }
        var translucent = 0
        var transparent = 0
        var samples = 0
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var y = stepY / 2
        while (y < bitmap.height) {
            var x = stepX / 2
            while (x < bitmap.width) {
                samples += 1
                val alpha = Color.alpha(bitmap.getPixel(x, y))
                if (alpha < 255) {
                    translucent += 1
                }
                if (alpha == 0) {
                    transparent += 1
                }
                x += stepX
            }
            y += stepY
        }
        val summary = "${bitmap.width}x${bitmap.height} from the $path, " +
            "$translucent of $samples samples translucent, $transparent transparent"
        if (count == 1) {
            WebKitLog.info("$name: first snapshot $summary")
        }
        if (transparent < samples && snapshotLogs.putIfAbsent("$path+content", 1) == null) {
            WebKitLog.info("$name: first snapshot with content $summary")
        }
    }

    private fun deliverComposed(
        bitmap: Bitmap,
        configuration: WKSnapshotConfiguration?,
        completionHandler: (Bitmap?, Throwable?) -> Unit,
    ) {
        val composed = try {
            composeSnapshot(bitmap, configuration)
        } catch (error: Throwable) {
            WebKitLog.once("compose:$error", "$name: scaling the snapshot failed: $error")
            deliver(null, error, completionHandler)
            return
        }
        deliver(composed, null, completionHandler)
    }

    private fun deliver(
        bitmap: Bitmap?,
        error: Throwable?,
        completionHandler: (Bitmap?, Throwable?) -> Unit,
    ) {
        if (bitmap != null) {
            PipelineStats.increment("webSnaps")
        }
        webKitMainHandler.post {
            completionHandler(bitmap, error)
        }
    }
}

internal fun destroyWebView(view: WebView) {
    runOnWebKitMain {
        try {
            (view.parent as? ViewGroup)?.removeView(view)
            if (view is HostedWebView) {
                view.placementListener = null
                for (handler in view.scriptHandlers) {
                    runCatching { handler.remove() }
                }
                view.scriptHandlers.clear()
            }
            view.stopLoading()
            view.removeJavascriptInterface(webKitBridgeName)
            view.destroy()
        } catch (error: Throwable) {
            WebKitLog.once("destroy:$error", "WebView destroy failed: $error")
        }
    }
}
