package com.moblin.android.various

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.graphics.Color
import com.moblin.android.AppDelegate
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.UIView
import com.moblin.android.platform.uikit.UIWindow
import com.moblin.android.platform.uikit.UIWindowScene
import com.moblin.android.platform.uikit.removeFromSuperview

private const val tapDiameter: Float = 44f

private class ShowTapsGestureRecognizer(private val tapsView: UIView) {
    var cancelsTouchesInView = false
    var delaysTouchesBegan = false
    var delaysTouchesEnded = false

    private val tapViews = mutableMapOf<Int, UIView>()
    private var failed = false

    fun touchesBegan(event: MotionEvent) {
        (tapsView.parent as? ViewGroup)?.bringChildToFront(tapsView)
        for (index in 0 until event.pointerCount) {
            val id = event.getPointerId(index)
            if (tapViews.containsKey(id)) {
                continue
            }
            val tapView = createTapView()
            tapViews[id] = tapView
            tapsView.addSubview(tapView)
            tapView.frame = CGRect(
                x = event.getX(index) - tapDiameter / 2,
                y = event.getY(index) - tapDiameter / 2,
                width = tapDiameter,
                height = tapDiameter,
            )
        }
        failIfAllTouchesEnded(event)
    }

    fun touchesMoved(event: MotionEvent) {
        for (index in 0 until event.pointerCount) {
            val tapView = tapViews[event.getPointerId(index)] ?: continue
            tapView.frame = CGRect(
                x = event.getX(index) - tapDiameter / 2,
                y = event.getY(index) - tapDiameter / 2,
                width = tapDiameter,
                height = tapDiameter,
            )
        }
        failIfAllTouchesEnded(event)
    }

    fun touchesEnded(event: MotionEvent) {
        removeTapViews(event)
        failIfAllTouchesEnded(event)
    }

    fun touchesCancelled(event: MotionEvent) {
        removeTapViews(event)
        failIfAllTouchesEnded(event)
    }

    fun canPrevent(recognizer: Any?): Boolean = false

    fun canBePrevented(recognizer: Any?): Boolean = false

    fun gestureRecognizer(recognizer: Any?, shouldRecognizeSimultaneouslyWith: Any?): Boolean = true

    fun removeAllTapViews() {
        for (tapView in tapViews.values) {
            tapView.removeFromSuperview()
        }
        tapViews.clear()
    }

    private fun createTapView(): UIView {
        val tapView = TapMarkView(AppDelegate.context)
        tapView.isClickable = false
        tapView.frame = CGRect(0f, 0f, tapDiameter, tapDiameter)
        return tapView
    }

    private fun removeTapViews(event: MotionEvent) {
        val indices = when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_UP -> listOf(event.actionIndex)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> (0 until event.pointerCount).toList()
            else -> emptyList()
        }
        for (index in indices) {
            val tapView = tapViews.remove(event.getPointerId(index)) ?: continue
            tapView.animate()
                .alpha(0f)
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(300)
                .withEndAction { tapView.removeFromSuperview() }
                .start()
        }
    }

    private fun failIfAllTouchesEnded(event: MotionEvent) {
        val allEnded = event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL ||
            tapViews.isEmpty()
        if (allEnded) {
            failed = true
        }
    }
}

private class ShowTapsWindow(private val window: UIWindow) {
    private val tapsView = UIView(AppDelegate.context)
    private val gestureRecognizer = ShowTapsGestureRecognizer(tapsView)
    private var originalCallback: Window.Callback? = null

    init {
        tapsView.isClickable = false
        tapsView.backgroundColor = Color.Transparent
        val androidWindow = window.window
        val decorView = androidWindow?.decorView
        if (decorView != null) {
            tapsView.frame = CGRect(0f, 0f, decorView.width.toFloat(), decorView.height.toFloat())
            (decorView as? ViewGroup)?.addView(tapsView)
        }
        if (androidWindow != null) {
            val callback = androidWindow.callback
            if (callback != null) {
                originalCallback = callback
                androidWindow.callback = ShowTapsWindowCallback(callback) { event -> dispatch(event) }
            }
        }
    }

    fun isWindow(window: UIWindow): Boolean = this.window === window

    fun remove() {
        gestureRecognizer.removeAllTapViews()
        originalCallback?.let { window.window?.callback = it }
        originalCallback = null
        tapsView.removeFromSuperview()
    }

    private fun dispatch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> gestureRecognizer.touchesBegan(event)
            MotionEvent.ACTION_MOVE -> gestureRecognizer.touchesMoved(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> gestureRecognizer.touchesEnded(event)
            MotionEvent.ACTION_CANCEL -> gestureRecognizer.touchesCancelled(event)
        }
    }
}

open class ShowTaps {
    private val windows = mutableListOf<ShowTapsWindow>()

    open fun update(enabled: Boolean) {
        if (enabled) {
            for (window in applicationWindows()) {
                if (windows.none { it.isWindow(window) }) {
                    windows.add(ShowTapsWindow(window))
                }
            }
        } else {
            for (window in windows) {
                window.remove()
            }
            windows.clear()
        }
    }

    private fun applicationWindows(): List<UIWindow> {
        return UIApplication.shared.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows }
    }
}

private class TapMarkView(context: Context) : UIView(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(128, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(102, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = minOf(width, height) / 2f - strokePaint.strokeWidth / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, fillPaint)
        canvas.drawCircle(width / 2f, height / 2f, radius, strokePaint)
    }
}

private class ShowTapsWindowCallback(
    private val base: Window.Callback,
    private val onTouch: (MotionEvent) -> Unit,
) : Window.Callback by base {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        onTouch(event)
        return base.dispatchTouchEvent(event)
    }
}
