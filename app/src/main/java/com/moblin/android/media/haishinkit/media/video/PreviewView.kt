package com.moblin.android.media.haishinkit.media.video

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.TextureView
import com.moblin.android.media.MediaSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.moblin.android.platform.video.layer

enum class VideoGravity(val rawValue: String) {
    resizeAspect("resizeAspect"),
    resizeAspectFill("resizeAspectFill"),
    resize("resize"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): VideoGravity? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

class PreviewView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs) {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    var videoGravity: VideoGravity = VideoGravity.resizeAspect

    var isMirrored: Boolean = false

    init {
        setup()
    }

    private fun setup() {
        layer.setup()
    }

    private fun applyIsMirrored() {
        val scaleX = if (isMirrored) -1.0f else 1.0f
        setTransform(
            Matrix().apply {
                this@PreviewView.scaleX = scaleX
            }
        )
    }

    private fun flushAndRemoveImage() {
        layer.flushAndRemoveImage()
    }

    private fun render(sample: MediaSample) {
        layer.enqueue(sample)
    }

    fun enqueue(sampleBuffer: MediaSample?, isFirstAfterAttach: Boolean) {
        val sample = sampleBuffer ?: return
        com.moblin.android.platform.video.retainLease(sample)
        mainScope.launch {
            if (layer.status == com.moblin.android.platform.video.AVSampleBufferDisplayLayer.Status.failed) layer.flush()
            if (isFirstAfterAttach) {
                flushAndRemoveImage()
                applyIsMirrored()
            } else {
                render(sample)
            }
            com.moblin.android.platform.video.releaseLease(sample)
        }
    }
}
