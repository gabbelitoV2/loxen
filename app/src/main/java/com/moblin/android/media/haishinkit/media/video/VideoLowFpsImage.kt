package com.moblin.android.media.haishinkit.media.video

import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.media.haishinkit.media.Processor
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private val lowFpsImageQueue = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

class VideoLowFpsImage(context: Any) {
    private val context: Any = context
    var processor: Processor? = null
    private var enabled: Boolean = false
    private var interval: Double = 1.0
    private var latest: Double = 0.0
    private var frameNumber: Long = 0

    fun setFps(fps: Float) {
        interval = (1f / fps).toDouble().coerceIn(0.2, 1.0)
        enabled = fps != 0.0f
        latest = 0.0
    }

    fun handleImageBuffer(imageBuffer: Image, presentationTimeStamp: Double) {
        if (!enabled) {
            return
        }
        if (presentationTimeStamp <= latest + interval) {
            return
        }
        latest = presentationTimeStamp
        lowFpsImageQueue.launch {
            createImage(imageBuffer)
        }
    }

    private fun createImage(imageBuffer: Image) {
        val jpeg: ByteArray = imageBuffer.jpegData(longSide = 400, compressionQuality = 0.3f) ?: return
        processor?.delegate?.streamLowFpsImage(lowFpsImage = jpeg, frameNumber = frameNumber)
        frameNumber += 1
    }
}
