package com.moblin.android.platform.avkit

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.moblin.android.AppDelegate
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.platform.video.AVSampleBufferDisplayLayer
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

internal object PictureInPictureWindow {
    private const val TAG = "PictureInPicture"
    private val maximumAspectRatio = Rational(239, 100)
    private val minimumAspectRatio = Rational(100, 239)
    private val handler = Handler(Looper.getMainLooper())
    internal var sdkInt = Build.VERSION.SDK_INT
    private val controllers = mutableListOf<WeakReference<AVPictureInPictureController>>()
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var observedLayer: AVSampleBufferDisplayLayer? = null
    private var observedView: View? = null
    private var activeController: AVPictureInPictureController? = null
    private var contentView: PictureInPictureContentView? = null

    internal var appliedParams: PictureInPictureParams? = null
        private set

    private val pictureInPictureModeChangedListener = Consumer<PictureInPictureModeChangedInfo> { info ->
        pictureInPictureModeChanged(info.isInPictureInPictureMode)
    }

    private val userLeaveHintListener = Runnable {
        userLeaveHint()
    }

    private val layoutChangeListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                applyParams()
            }
        }

    private val videoSizeObserver: () -> Unit = {
        handler.post { applyParams() }
    }

    fun isSupported(): Boolean {
        return AppDelegate.context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun install(activity: ComponentActivity) {
        val previous = activityRef?.get()
        if (previous === activity) {
            return
        }
        hideContent()
        activeController = null
        appliedParams = null
        previous?.removeOnPictureInPictureModeChangedListener(pictureInPictureModeChangedListener)
        previous?.removeOnUserLeaveHintListener(userLeaveHintListener)
        activityRef = WeakReference(activity)
        activity.addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedListener)
        activity.addOnUserLeaveHintListener(userLeaveHintListener)
        update()
    }

    fun register(controller: AVPictureInPictureController) {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            handler.post { register(controller) }
            return
        }
        controllers.removeAll { it.get() == null }
        controllers.add(WeakReference(controller))
        update()
    }

    fun isActive(controller: AVPictureInPictureController): Boolean {
        return activeController === controller
    }

    fun update() {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            handler.post { update() }
            return
        }
        observe(armedController()?.contentSource?.sampleBufferDisplayLayer)
        applyParams()
        val controller = activeController ?: return
        val contentSource = controller.contentSource
        if (contentSource == null) {
            stop()
        } else {
            showContent(controller, contentSource)
        }
    }

    private fun armedController(): AVPictureInPictureController? {
        for (reference in controllers.asReversed()) {
            val controller = reference.get() ?: continue
            if (controller.contentSource != null && controller.canStartPictureInPictureAutomaticallyFromInline) {
                return controller
            }
        }
        return null
    }

    private fun observe(layer: AVSampleBufferDisplayLayer?) {
        val view = layer?.view
        if (layer === observedLayer && view === observedView) {
            return
        }
        observedLayer?.removeVideoSizeObserver(videoSizeObserver)
        observedView?.removeOnLayoutChangeListener(layoutChangeListener)
        observedLayer = layer
        observedView = view
        layer?.addVideoSizeObserver(videoSizeObserver)
        view?.addOnLayoutChangeListener(layoutChangeListener)
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    private fun isAutoEnterSupported(): Boolean {
        return sdkInt >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    private fun applyParams() {
        val activity = activityRef?.get() ?: return
        if (activity.isDestroyed || !isAutoEnterSupported() || !isSupported()) {
            return
        }
        val params = makeParams(activity, armedController())
        if (params == appliedParams) {
            return
        }
        appliedParams = params
        try {
            activity.setPictureInPictureParams(params)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Setting picture in picture parameters failed: ${error.message}")
        }
    }

    private fun makeParams(
        activity: ComponentActivity,
        controller: AVPictureInPictureController?,
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        val layer = controller?.contentSource?.sampleBufferDisplayLayer
        val aspectRatio = layer?.videoSize?.let { aspectRatio(it) }
        if (aspectRatio != null) {
            builder.setAspectRatio(aspectRatio)
            if (!activity.isInPictureInPictureMode) {
                sourceRectHint(layer?.view, aspectRatio)?.let { builder.setSourceRectHint(it) }
            }
        }
        if (isAutoEnterSupported()) {
            builder.setAutoEnterEnabled(controller != null)
            builder.setSeamlessResizeEnabled(false)
        }
        return builder.build()
    }

    internal fun aspectRatio(size: Size): Rational? {
        if (size.width <= 0 || size.height <= 0) {
            return null
        }
        val aspectRatio = Rational(size.width, size.height)
        return when {
            aspectRatio > maximumAspectRatio -> maximumAspectRatio
            aspectRatio < minimumAspectRatio -> minimumAspectRatio
            else -> aspectRatio
        }
    }

    private fun sourceRectHint(view: View?, aspectRatio: Rational): Rect? {
        if (view == null || !view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
            return null
        }
        val location = IntArray(2)
        view.getLocationInWindow(location)
        var width = view.width
        var height = view.height
        if (aspectRatio.toDouble() > width.toDouble() / height) {
            height = (width / aspectRatio.toDouble()).roundToInt()
        } else {
            width = (height * aspectRatio.toDouble()).roundToInt()
        }
        val left = location[0] + (view.width - width) / 2
        val top = location[1] + (view.height - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    private fun userLeaveHint() {
        if (sdkInt >= Build.VERSION_CODES.S) {
            return
        }
        val activity = activityRef?.get() ?: return
        val controller = armedController() ?: return
        if (!isSupported() || activity.isInPictureInPictureMode || isPresentingInOwnTask(activity)) {
            return
        }
        try {
            activity.enterPictureInPictureMode(makeParams(activity, controller))
        } catch (error: RuntimeException) {
            Log.w(TAG, "Entering picture in picture failed: ${error.message}")
        }
    }

    private fun isPresentingInOwnTask(activity: ComponentActivity): Boolean {
        return try {
            val manager = activity.getSystemService(ActivityManager::class.java) ?: return false
            manager.appTasks.any {
                val info = it.taskInfo
                info.baseActivity == activity.componentName &&
                    info.topActivity != null &&
                    info.topActivity != activity.componentName
            }
        } catch (error: RuntimeException) {
            false
        }
    }

    private fun pictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {
            val controller = armedController()
            activeController = controller
            val contentSource = controller?.contentSource
            if (controller != null && contentSource != null) {
                showContent(controller, contentSource)
            }
        } else {
            activeController = null
            hideContent()
            applyParams()
        }
        SystemEvents.pictureInPictureModeChanged(isInPictureInPictureMode)
    }

    private fun showContent(
        controller: AVPictureInPictureController,
        contentSource: AVPictureInPictureController.ContentSource,
    ) {
        val activity = activityRef?.get() ?: return
        val view = contentSource.sampleBufferDisplayLayer.view ?: return
        val content = contentView ?: PictureInPictureContentView(activity).also {
            contentView = it
            activity.addContentView(
                it,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        content.onRenderSizeChanged = { width, height ->
            contentSource.playbackDelegate.pictureInPictureController(
                controller,
                didTransitionToRenderSize = CMVideoDimensions(width = width, height = height),
            )
        }
        content.show(view)
    }

    private fun hideContent() {
        val content = contentView ?: return
        contentView = null
        content.onRenderSizeChanged = null
        content.restore()
        (content.parent as? ViewGroup)?.removeView(content)
    }

    private fun stop() {
        activeController = null
        hideContent()
        val activity = activityRef?.get() ?: return
        if (activity.isInPictureInPictureMode) {
            activity.moveTaskToBack(true)
        }
    }
}

private class PictureInPictureContentView(context: Context) : FrameLayout(context) {
    var onRenderSizeChanged: ((Int, Int) -> Unit)? = null
    private var shownView: View? = null
    private var previousParent: WeakReference<ViewGroup>? = null
    private var previousIndex = 0
    private var previousLayoutParams: ViewGroup.LayoutParams? = null

    init {
        setBackgroundColor(Color.BLACK)
    }

    fun show(view: View) {
        if (shownView !== view) {
            restore()
            shownView = view
        }
        take()
    }

    fun restore() {
        val view = shownView ?: return
        shownView = null
        if (view.parent === this) {
            removeView(view)
        }
        val parent = previousParent?.get()
        previousParent = null
        if (parent == null || !parent.isAttachedToWindow || view.parent != null) {
            return
        }
        val layoutParams = previousLayoutParams
            ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        parent.addView(view, previousIndex.coerceIn(0, parent.childCount), layoutParams)
    }

    private fun take() {
        val view = shownView ?: return
        if (view.parent === this) {
            return
        }
        val parent = view.parent as? ViewGroup
        previousParent = parent?.let { WeakReference(it) }
        previousIndex = parent?.indexOfChild(view)?.coerceAtLeast(0) ?: 0
        previousLayoutParams = view.layoutParams
        parent?.removeView(view)
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val view = shownView ?: return
        if (view.parent !== this) {
            post {
                if (shownView === view) {
                    take()
                }
            }
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width > 0 && height > 0) {
            onRenderSizeChanged?.invoke(width, height)
        }
    }
}
