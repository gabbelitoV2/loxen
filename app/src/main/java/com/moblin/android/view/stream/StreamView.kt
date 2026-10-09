package com.moblin.android.view.stream

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.VideoGravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.platform.avfoundation.session
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Show
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.WidgetShape
import java.util.UUID

class SharedUiViewContainerView(
    context: Context,
    private val sharedView: View,
) : FrameLayout(context) {
    fun attachSharedView() {
        if (!isAttachedToWindow || sharedView.parent === this) {
            return
        }
        (sharedView.parent as? ViewGroup)?.removeView(sharedView)
        addView(
            sharedView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachSharedView()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (sharedView.parent == null) {
            attachSharedView()
        }
        if (sharedView.parent === this) {
            sharedView.layout(0, 0, width, height)
        }
    }
}

class StreamPreviewView(private val model: Model) {
    @Composable
    fun body() {
        AndroidView(
            factory = { context -> SharedUiViewContainerView(context, model.streamPreviewView) },
            modifier = Modifier.fillMaxSize(),
            update = { it.attachSharedView() },
        )
    }
}

data class CameraPreviewWidget(
    val id: UUID,
    val deviceId: UUID,
    val layout: SettingsWidgetLayout,
    val mirror: Boolean,
    var shape: WidgetShape,
)

private class CameraPreviewWidgetLayer(
    context: Context,
) {
    val borderLayer = FrameLayout(context)
    private val contentLayer = FrameLayout(context)

    init {
        borderLayer.visibility = View.INVISIBLE
        contentLayer.clipChildren = true
        borderLayer.addView(contentLayer, FrameLayout.LayoutParams(0, 0))
    }

    fun layout(
        widget: CameraPreviewWidget,
        previewLayer: PreviewView,
        index: Int,
        canvasSize: CGSize,
        streamSize: CGSize,
    ) {
        val shape = widget.shape
        val contentRegion = shape.contentRegion
        if (contentRegion.isEmpty) {
            return
        }
        val placement = shape.placement(widget.layout, streamSize)
        val scale = placement.scale
        val borderWidth = placement.borderWidth
        val borderColor = shape.borderColor
        val borderSize = placement.borderSize
        borderLayer.visibility = View.VISIBLE
        borderLayer.translationZ = index.toFloat()
        borderLayer.rotation = Math.toDegrees(shape.rotationRadians()).toFloat()
        borderLayer.scaleX = if (widget.mirror) -1f else 1f
        borderLayer.scaleY = 1f
        setFrame(
            borderLayer,
            placement.center.x - borderSize.width / 2,
            placement.center.y - borderSize.height / 2,
            borderSize.width,
            borderSize.height,
        )
        if (borderWidth > 0) {
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.setColor(
                android.graphics.Color.argb(
                    (borderColor.alpha * 255).toInt(),
                    (borderColor.red * 255).toInt(),
                    (borderColor.green * 255).toInt(),
                    (borderColor.blue * 255).toInt(),
                ),
            )
            drawable.cornerRadius = shape.cornerRadiusPixels(borderSize)
            borderLayer.background = drawable
        } else {
            borderLayer.background = null
        }
        setFrame(contentLayer, borderWidth, borderWidth, placement.size.width, placement.size.height)
        clipToRoundedRect(contentLayer, shape.cornerRadiusPixels(placement.size))
        if (previewLayer.parent !== contentLayer) {
            (previewLayer.parent as? ViewGroup)?.removeView(previewLayer)
            contentLayer.addView(previewLayer, FrameLayout.LayoutParams(0, 0))
        }
        previewLayer.videoGravity = VideoGravity.resizeAspectFill
        setFrame(
            previewLayer,
            -contentRegion.minX * scale,
            -contentRegion.minY * scale,
            canvasSize.width * scale,
            canvasSize.height * scale,
        )
        previewLayer.visibility = View.VISIBLE
    }
}

class CameraPreviewUiView(context: Context) : FrameLayout(context) {
    private val deviceLayers: MutableMap<UUID, PreviewView> = mutableMapOf()
    private val widgetLayers: MutableMap<UUID, CameraPreviewWidgetLayer> = mutableMapOf()
    private val widgetsLayer = FrameLayout(context)
    private var widgets: List<CameraPreviewWidget> = emptyList()
    private var canvasSize: CGSize = CGSize.zero
    private var selectedDeviceId: UUID? = null

    init {
        widgetsLayer.translationZ = 1f
        widgetsLayer.clipChildren = true
        addView(
            widgetsLayer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    val previewLayers: Map<PreviewView, UUID>
        get() {
            val previewLayers = mutableMapOf<PreviewView, UUID>()
            for ((id, previewLayer) in deviceLayers) {
                previewLayers[previewLayer] = id
            }
            return previewLayers
        }

    fun setDevices(ids: List<UUID>) {
        val iterator = deviceLayers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!ids.contains(entry.key)) {
                removeView(entry.value)
                iterator.remove()
            }
        }
        for (id in ids) {
            if (deviceLayers[id] == null) {
                val previewLayer = PreviewView(context)
                previewLayer.visibility = View.INVISIBLE
                addView(
                    previewLayer,
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                )
                deviceLayers[id] = previewLayer
            }
        }
    }

    fun select(id: UUID?, isMirrored: Boolean) {
        selectedDeviceId = id
        scaleX = if (isMirrored) -1f else 1f
        layoutLayers()
    }

    fun setWidgets(widgets: List<CameraPreviewWidget>, canvasSize: CGSize) {
        this.widgets = widgets
        this.canvasSize = canvasSize
        layoutLayers()
    }

    fun setVideoOrientation(videoOrientation: Int) {
        for (previewLayer in deviceLayers.values) {
            val connection = previewLayer.session?.connections?.firstOrNull { it.videoPreviewLayer === previewLayer }
            connection?.videoOrientation = videoOrientation
        }
    }

    private fun layoutLayers() {
        for (previewLayer in deviceLayers.values) {
            previewLayer.visibility = View.INVISIBLE
        }
        val deviceId = selectedDeviceId
        if (deviceId != null) {
            val previewLayer = deviceLayers[deviceId]
            if (previewLayer != null) {
                if (previewLayer.parent !== this) {
                    (previewLayer.parent as? ViewGroup)?.removeView(previewLayer)
                    addView(
                        previewLayer,
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                    )
                }
                previewLayer.videoGravity = VideoGravity.resizeAspect
                previewLayer.layout(0, 0, width, height)
                previewLayer.visibility = View.VISIBLE
            }
        }
        layoutWidgets()
    }

    private fun layoutWidgets() {
        val iterator = widgetLayers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (widgets.none { it.id == entry.key }) {
                widgetsLayer.removeView(entry.value.borderLayer)
                iterator.remove()
            }
        }
        for (widgetLayer in widgetLayers.values) {
            widgetLayer.borderLayer.visibility = View.INVISIBLE
        }
        if (widgets.isEmpty()) {
            return
        }
        val rect = avMakeRect(canvasSize, CGRect(0.0, 0.0, width.toDouble(), height.toDouble()))
        widgetsLayer.layout(
            rect.origin.x.toInt(),
            rect.origin.y.toInt(),
            rect.maxX.toInt(),
            rect.maxY.toInt(),
        )
        for ((index, widget) in widgets.withIndex()) {
            if (widget.deviceId == selectedDeviceId) {
                continue
            }
            val previewLayer = deviceLayers[widget.deviceId] ?: continue
            var widgetLayer = widgetLayers[widget.id]
            if (widgetLayer == null) {
                widgetLayer = CameraPreviewWidgetLayer(context)
                widgetsLayer.addView(widgetLayer.borderLayer)
                widgetLayers[widget.id] = widgetLayer
            }
            widgetLayer.layout(
                widget = widget,
                previewLayer = previewLayer,
                index = index,
                canvasSize = canvasSize,
                streamSize = CGSize(widgetsLayer.width, widgetsLayer.height),
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutLayers()
    }
}

class CameraPreviewView(private val model: Model) {
    @Composable
    fun body() {
        AndroidView(
            factory = { context -> SharedUiViewContainerView(context, model.cameraPreviewView) },
            modifier = Modifier.fillMaxSize(),
            update = { it.attachSharedView() },
        )
    }
}

class StreamView(
    private val show: Show,
    private val cameraPreviewView: CameraPreviewView,
    private val streamPreviewView: StreamPreviewView,
) {
    @Composable
    fun body() {
        val chatPhone by show.chatPhone.collectAsState()
        val cameraPreview by show.cameraPreview.collectAsState()
        if (chatPhone) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                streamPreviewView.body()
                if (cameraPreview) {
                    cameraPreviewView.body()
                }
            }
        }
    }
}

private fun avMakeRect(aspectRatio: CGSize, insideRect: CGRect): CGRect {
    if (aspectRatio.width <= 0.0 || aspectRatio.height <= 0.0) {
        return CGRect(insideRect.origin.x, insideRect.origin.y, 0.0, 0.0)
    }
    var width = insideRect.width
    var height = width * aspectRatio.height / aspectRatio.width
    if (height > insideRect.height) {
        height = insideRect.height
        width = height * aspectRatio.width / aspectRatio.height
    }
    return CGRect(
        insideRect.origin.x + (insideRect.width - width) / 2,
        insideRect.origin.y + (insideRect.height - height) / 2,
        width,
        height,
    )
}

private fun setFrame(view: View, x: Double, y: Double, width: Double, height: Double) {
    val params = (view.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(0, 0)
    params.width = width.toInt()
    params.height = height.toInt()
    params.leftMargin = x.toInt()
    params.topMargin = y.toInt()
    view.layoutParams = params
}

private fun clipToRoundedRect(view: View, radius: Float) {
    view.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
    view.clipToOutline = true
}
