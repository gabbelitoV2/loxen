package com.moblin.android.videoeffects.alerts

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.videoeffects.EffectImageCiImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private data class VideoImage(val image: EffectImageCiImage, val offset: Double)

@OptIn(ExperimentalCoroutinesApi::class)
private val lockScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))

class AlertsEffectVideoReader(path: String) {
    private var images: ArrayDeque<VideoImage> = ArrayDeque()
    private var reader: MediaExtractor? = null
    private var trackOutput: MediaCodec? = null
    private var fillEnded: Boolean = false
    private var basePresentationTimeStamp: Double? = null

    init {
        lockScope.launch {
            val extractor = MediaExtractor()
            val loaded = runCatching {
                extractor.setDataSource(path)
            }.isSuccess
            if (!loaded) {
                markFillEnded()
                return@launch
            }
            reader = extractor
            var videoTrackIndex: Int? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoTrackIndex = index
                    break
                }
            }
            loadVideoTrackCompletion(track: videoTrackIndex, error: null)
        }
    }

    fun getImage(presentationTimeStamp: Double): EffectImageCiImage? {
        if (basePresentationTimeStamp == null) {
            basePresentationTimeStamp = presentationTimeStamp
        }
        val timeOffset = presentationTimeStamp - basePresentationTimeStamp!!
        val image = findImage(offset = timeOffset)
        if (images.size < 10) {
            fill()
        }
        return image
    }

    fun hasEnded(): Boolean {
        return fillEnded && images.isEmpty()
    }

    private fun findImage(offset: Double): EffectImageCiImage? {
        while (images.isNotEmpty()) {
            val image = images.first()
            if (offset <= image.offset) {
                return image.image
            }
            images.removeFirst()
        }
        return null
    }

    private fun fill() {
        lockScope.launch {
            fillInternal()
        }
    }

    private fun fillInternal() {
        if (trackOutput == null) {
            return
        }
        val newImages = mutableListOf<VideoImage>()
        for (i in 0..10) {
            val image = nextVideoImage()
            if (image != null) {
                newImages.add(image)
            }
        }
        processorPipelineQueue.launch {
            images.addAll(newImages)
            fillEnded = newImages.isEmpty()
        }
    }

    private fun nextVideoImage(): VideoImage? {
        TODO("decode the next frame with MediaCodec and convert it to EffectImageCiImage")
    }

    private fun loadVideoTrackCompletion(track: Int?, error: Exception?) {
        if (error != null || track == null) {
            markFillEnded()
            return
        }
        val format = reader?.getTrackFormat(track) ?: run {
            markFillEnded()
            return
        }
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
            markFillEnded()
            return
        }
        val decoder = runCatching {
            MediaCodec.createDecoderByType(mime)
        }.getOrNull() ?: run {
            markFillEnded()
            return
        }
        reader?.selectTrack(track)
        decoder.configure(format, null, null, 0)
        decoder.start()
        trackOutput = decoder
        fillInternal()
    }

    private fun markFillEnded() {
        processorPipelineQueue.launch {
            fillEnded = true
        }
    }
}
