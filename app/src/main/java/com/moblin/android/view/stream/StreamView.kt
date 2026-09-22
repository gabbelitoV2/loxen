package com.moblin.android.view.stream

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Show
import java.util.UUID

class SharedUiViewContainerView(
    context: Context,
    private val sharedView: View,
) : FrameLayout(context) {
    fun attachSharedView() {
        if (!isAttachedToWindow || sharedView.parent === this) {
            return
        }
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
        attachSharedView()
        sharedView.layout(0, 0, width, height)
    }
}

class StreamPreviewView(private val model: Model) {
    @Composable
    fun body() {
        AndroidView(
            factory = { context -> SharedUiViewContainerView(context, model.streamPreviewView) },
            update = { it.attachSharedView() },
        )
    }
}

class CameraPreviewUiView(context: Context) : FrameLayout(context) {
    val previewLayers: MutableMap<UUID, PreviewView> = mutableMapOf()

    fun setDevices(ids: List<UUID>) {
        val iterator = previewLayers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!ids.contains(entry.key)) {
                removeView(entry.value)
                iterator.remove()
            }
        }
        for (id in ids) {
            if (previewLayers[id] == null) {
                val previewLayer = PreviewView(context)
                previewLayer.visibility = View.INVISIBLE
                addView(
                    previewLayer,
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                )
                previewLayers[id] = previewLayer
            }
        }
    }

    fun select(id: UUID?) {
        for ((previewLayerId, previewLayer) in previewLayers) {
            previewLayer.visibility = if (previewLayerId == id) View.VISIBLE else View.INVISIBLE
        }
    }

    fun setVideoOrientation(videoOrientation: Int) {
        TODO("CameraX PreviewView has no AVCaptureVideoOrientation equivalent")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        for (previewLayer in previewLayers.values) {
            previewLayer.layout(0, 0, width, height)
        }
    }
}

class CameraPreviewView(private val model: Model) {
    @Composable
    fun body() {
        AndroidView(
            factory = { context -> SharedUiViewContainerView(context, model.cameraPreviewView) },
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
        if (show.chatPhone.value) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                streamPreviewView.body()
                if (show.cameraPreview.value) {
                    cameraPreviewView.body()
                }
            }
        }
    }
}
