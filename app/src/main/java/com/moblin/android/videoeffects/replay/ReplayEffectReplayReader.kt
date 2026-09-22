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

class ReplayEffectReplayReader internal constructor(
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
            replayEffectQueue.launch {
                val startTimeUs = (start * 1_000_000.0).toLong()
                val durationUs = (duration * 1_000_000.0).toLong()
                Unit
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
        Unit
    }

    private fun markCompleted() {
        processorPipelineQueue.launch {
            images.addLast(ReplayImage(image = null, offset = null, isLast = true))
        }
    }

    private fun fill() {
        replayEffectQueue.launch {
            fillInternal()
        }
    }

    private fun fillInternal() {
        if (trackOutput == null) {
            return
        }
        Unit
    }

    private fun createOverlay(size: Size): EffectImageCiImage? {
        return null
    }
}
