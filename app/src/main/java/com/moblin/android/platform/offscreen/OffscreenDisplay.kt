package com.moblin.android.platform.offscreen

import android.app.Presentation
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.PipelineStats
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MoblinOverlay"
private const val displaySize = 16
private const val sweepIntervalMs = 5_000L

internal val offscreenMainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

internal fun isOffscreenMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

internal fun logOverlayOnce(message: String) {
    if (loggedMessages.add(message)) {
        Log.i(TAG, message)
    }
}

internal interface OffscreenHostedView {
    val widthSpec: Int
    val heightSpec: Int
}

internal class OffscreenRoot(context: Context) : ViewGroup(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val hosted = child as? OffscreenHostedView
            try {
                if (hosted != null) {
                    child.measure(hosted.widthSpec, hosted.heightSpec)
                } else {
                    val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    child.measure(unspecified, unspecified)
                }
            } catch (error: Throwable) {
                logOverlayOnce("hosted view failed to measure: $error")
            }
        }
        setMeasuredDimension(
            getDefaultSize(displaySize, widthMeasureSpec),
            getDefaultSize(displaySize, heightMeasureSpec),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            try {
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            } catch (error: Throwable) {
                logOverlayOnce("hosted view failed to lay out: $error")
            }
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val saveCount = canvas.saveCount
        return try {
            super.drawChild(canvas, child, drawingTime)
        } catch (error: Throwable) {
            logOverlayOnce("hosted view failed to draw: $error")
            canvas.restoreToCount(saveCount)
            false
        }
    }
}

private object OffscreenOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private var started = false

    override val lifecycle: Lifecycle
        get() = registry

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry

    fun start() {
        if (started) {
            return
        }
        started = true
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }
}

object OffscreenDisplay {
    private enum class State { idle, ready, failed }

    private var state = State.idle
    private var root: OffscreenRoot? = null
    private var presentation: Presentation? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var drainThread: HandlerThread? = null

    fun prewarm() {
        if (isOffscreenMainThread()) {
            start()
        } else {
            offscreenMainHandler.post { start() }
        }
    }

    internal val isReady: Boolean
        get() = root?.isAttachedToWindow == true

    internal fun hostContext(): Context? {
        start()
        return root?.context
    }

    internal fun attach(view: View): Boolean {
        start()
        val root = root ?: return false
        val parent = view.parent
        if (parent !== root) {
            try {
                (parent as? ViewGroup)?.removeView(view)
                root.addView(view)
            } catch (error: Throwable) {
                logOverlayOnce("hosted view failed to attach: $error")
            }
            updateGauge()
        }
        return root.isAttachedToWindow && view.parent === root
    }

    internal fun detach(view: View) {
        val root = root ?: return
        if (view.parent === root) {
            try {
                root.removeView(view)
            } catch (error: Throwable) {
                logOverlayOnce("hosted view failed to detach: $error")
            }
            updateGauge()
        }
    }

    private fun updateGauge() {
        PipelineStats.gauge("ovViews", (root?.childCount ?: 0).toLong())
    }

    private fun start() {
        if (state != State.idle || !isOffscreenMainThread()) {
            return
        }
        try {
            val context = AppDelegate.context.applicationContext
            val displayManager = context.getSystemService(DisplayManager::class.java)
                ?: throw IllegalStateException("no DisplayManager")
            val thread = HandlerThread("MoblinOffscreen").also { it.start() }
            drainThread = thread
            val reader = ImageReader.newInstance(displaySize, displaySize, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ source ->
                try {
                    source.acquireLatestImage()?.close()
                } catch (_: Throwable) {
                }
            }, Handler(thread.looper))
            val display = displayManager.createVirtualDisplay(
                "MoblinOffscreen",
                displaySize,
                displaySize,
                DisplayMetrics.DENSITY_MEDIUM,
                reader.surface,
                0,
            ) ?: throw IllegalStateException("no virtual display")
            virtualDisplay = display
            OffscreenOwner.start()
            val presentation = Presentation(context, display.display)
            this.presentation = presentation
            val root = OffscreenRoot(presentation.context)
            root.setViewTreeLifecycleOwner(OffscreenOwner)
            root.setViewTreeSavedStateRegistryOwner(OffscreenOwner)
            root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    Log.i(TAG, "host display ready")
                    updateGauge()
                }

                override fun onViewDetachedFromWindow(view: View) {
                    Log.i(TAG, "host display detached")
                }
            })
            presentation.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                )
            }
            presentation.setCancelable(false)
            presentation.setContentView(
                root,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
            this.root = root
            presentation.show()
            state = State.ready
        } catch (error: Throwable) {
            state = State.failed
            Log.e(TAG, "host display failed: $error")
            root = null
            runCatching { presentation?.dismiss() }
            presentation = null
            runCatching { virtualDisplay?.release() }
            virtualDisplay = null
            runCatching { imageReader?.close() }
            imageReader = null
            runCatching { drainThread?.quitSafely() }
            drainThread = null
        }
    }
}

internal object OffscreenSweep {
    private class Entry(referent: Any, queue: ReferenceQueue<Any>, val onCollected: () -> Unit) :
        WeakReference<Any>(referent, queue)

    private val queue = ReferenceQueue<Any>()
    private val entries = Collections.newSetFromMap(ConcurrentHashMap<Entry, Boolean>())
    private val sweepPosted = AtomicBoolean(false)
    private val timerRunning = AtomicBoolean(false)

    fun track(owner: Any, onCollected: () -> Unit): Any {
        val entry = Entry(owner, queue, onCollected)
        entries.add(entry)
        requestSweep()
        startTimer()
        return entry
    }

    fun untrack(token: Any) {
        val entry = token as? Entry ?: return
        entries.remove(entry)
        entry.clear()
    }

    private fun requestSweep() {
        if (!sweepPosted.compareAndSet(false, true)) {
            return
        }
        val posted = runCatching {
            offscreenMainHandler.post {
                sweepPosted.set(false)
                sweep()
            }
        }.getOrDefault(false)
        if (!posted) {
            sweepPosted.set(false)
        }
    }

    private fun startTimer() {
        if (!timerRunning.compareAndSet(false, true)) {
            return
        }
        val posted = runCatching { offscreenMainHandler.postDelayed({ tick() }, sweepIntervalMs) }.getOrDefault(false)
        if (!posted) {
            timerRunning.set(false)
        }
    }

    private fun tick() {
        sweep()
        timerRunning.set(false)
        if (entries.isNotEmpty()) {
            startTimer()
        }
    }

    private fun sweep() {
        while (true) {
            val entry = queue.poll() as? Entry ?: break
            if (!entries.remove(entry)) {
                continue
            }
            try {
                entry.onCollected()
            } catch (error: Throwable) {
                logOverlayOnce("sweep cleanup failed: $error")
            }
        }
    }
}
