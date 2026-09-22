package com.moblin.android.videoeffects.replay

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Size
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import kotlinx.coroutines.launch

enum class ReplayEffectStingerReaderSetupState {
    working,
    ok,
    failed,
}

class ReplayEffectStingerReader(path: String, size: Size) {
    private var images: ArrayDeque<ReplayImage> = ArrayDeque()
    private var reader: MediaExtractor? = null
    private var trackOutput: MediaCodec? = null
    var duration: Double = 0.0
        private set
    var setupState: ReplayEffectStingerReaderSetupState = ReplayEffectStingerReaderSetupState.working
        private set
    private val size: Size = size

    init {
        setup(path)
    }

    fun getImage(offset: Double): ReplayImage? {
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

    private fun fill() {
        replayEffectQueue.launch {
            fillInternal()
        }
    }

    private fun fillInternal() {
        val trackOutput = trackOutput ?: return
        val newImages = mutableListOf<ReplayImage>()
        for (i in 0..10) {
            val sampleBuffer = copyNextSampleBuffer(trackOutput)
            if (sampleBuffer != null) {
                newImages.add(
                    ReplayImage(
                        image = renderEffectImage(sampleBuffer, size, isOpaque = false),
                        offset = sampleBuffer.presentationTimeUs / 1_000_000.0,
                        isLast = false
                    )
                )
            } else {
                newImages.add(ReplayImage(image = null, offset = null, isLast = true))
                break
            }
        }
        processorPipelineQueue.launch {
            images.addAll(newImages)
        }
    }

    private fun setup(path: String) {
        replayEffectQueue.launch {
            val asset = MediaExtractor()
            val readerResult = runCatching {
                asset.setDataSource(path)
                asset
            }
            reader = readerResult.getOrNull()
            duration = readerResult.getOrNull()?.let { extractDuration(it) } ?: 0.0
            val trackResult = readerResult.mapCatching { videoTrackIndex(it) }
            replayEffectQueue.launch {
                loadVideoTrackCompletion(
                    track = trackResult.getOrNull()?.takeIf { it >= 0 },
                    error = trackResult.exceptionOrNull()
                )
            }
        }
    }

    private fun loadVideoTrackCompletion(track: Int?, error: Throwable?) {
        if (error != null || track == null) {
            setupComplete(ReplayEffectStingerReaderSetupState.failed)
            return
        }
        trackOutput = createVideoDecoder(track)
        val output = trackOutput
        if (output == null) {
            setupComplete(ReplayEffectStingerReaderSetupState.failed)
            return
        }
        reader?.selectTrack(track)
        output.start()
        fillInternal()
        setupComplete(ReplayEffectStingerReaderSetupState.ok)
    }

    private fun setupComplete(state: ReplayEffectStingerReaderSetupState) {
        processorPipelineQueue.launch {
            setupState = state
        }
    }

    private fun extractDuration(extractor: MediaExtractor): Double {
        val index = videoTrackIndex(extractor)
        if (index < 0) {
            return 0.0
        }
        val format = extractor.getTrackFormat(index)
        if (!format.containsKey(MediaFormat.KEY_DURATION)) {
            return 0.0
        }
        return format.getLong(MediaFormat.KEY_DURATION) / 1_000_000.0
    }

    private fun videoTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("video/")) {
                return i
            }
        }
        return -1
    }

    private fun createVideoDecoder(trackIndex: Int): MediaCodec? {
        val extractor = reader ?: return null
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        return runCatching {
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec
        }.getOrNull()
    }

    private fun copyNextSampleBuffer(trackOutput: MediaCodec): MediaSample? =
        null
    private fun renderEffectImage(frame: MediaSample, size: Size, isOpaque: Boolean): Nothing =
        TODO()
}
