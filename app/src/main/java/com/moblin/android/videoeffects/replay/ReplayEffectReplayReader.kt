package com.moblin.android.videoeffects.replay

import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.videoeffects.EffectImageCiImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ReplayImage(
    val image: EffectImageCiImage?,
    val offset: Double?,
    val isLast: Boolean,
)

class ReplayEffectReplayReader(
    video: ReplayBufferFile,
    start: Double,
    duration: Double,
    size: Size,
) {
    private val video: ReplayBufferFile
    private val startTime: Double
    private var reader: Any? = null
    private var trackOutput: Any? = null
    private val images = ArrayDeque<ReplayImage>()
    private var overlay: EffectImageCiImage? = null
    private val size: Size
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        this.video = video
        this.startTime = start
        this.size = size
        scope.launch(Dispatchers.Main) {
            overlay = createOverlay(size)
            scope.launch(replayEffectQueue) {
                val startTimeUs = (start * 1_000_000.0).toLong()
                val durationUs = (duration * 1_000_000.0).toLong()
                TODO("AVAsset and AVAssetReader have no Android counterpart; read ${video.url} from ${startTimeUs}us for ${durationUs}us with MediaExtractor and decode frames with MediaCodec")
            }
        }
    }

    fun getImage(offset: Double): ReplayImage {
        val image = findImage(offset)
        if (images.size < 10) {
            fill()
        }
        return image
    }

    private fun findImage(offset: Double): ReplayImage {
        while (true) {
            val image = images.firstOrNull() ?: break
            val imageOffset = image.offset
            if (imageOffset != null) {
                if (offset < imageOffset) {
                    return image
                }
            } else {
                return image
            }
            images.removeFirst()
        }
        return ReplayImage(image = null, offset = null, isLast = false)
    }

    private fun loadVideoTrackCompletion(tracks: List<Any>?, error: Throwable?) {
        if (error != null || tracks.isNullOrEmpty()) {
            markCompleted()
            return
        }
        TODO("AVAssetReaderTrackOutput has no Android counterpart; decode the video track with MediaCodec")
    }

    private fun markCompleted() {
        scope.launch(processorPipelineQueue) {
            images.addLast(ReplayImage(image = null, offset = null, isLast = true))
        }
    }

    private fun fill() {
        scope.launch(replayEffectQueue) {
            fillInternal()
        }
    }

    private fun fillInternal() {
        if (trackOutput == null) {
            return
        }
        TODO("AVAssetReaderTrackOutput.copyNextSampleBuffer and the Core Image composition pipeline have no Android counterpart; decode frames with MediaCodec and compose them with OpenGL ES")
    }

    private fun createOverlay(size: Size): EffectImageCiImage? {
        TODO("SwiftUI ImageRenderer and CIImage have no Android counterpart; draw the REPLAY overlay with android.graphics.Canvas")
    }
}
