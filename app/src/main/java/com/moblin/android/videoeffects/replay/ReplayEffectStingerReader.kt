package com.moblin.android.videoeffects.replay

import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import kotlinx.coroutines.launch
import com.moblin.android.platform.coreimage.*
import com.moblin.android.videoeffects.*

enum class ReplayEffectStingerReaderSetupState {
    working,
    ok,
    failed,
}

open class ReplayEffectStingerReader(path: String, size: Size) {
    private var images: ArrayDeque<ReplayImage> = ArrayDeque()
    private var reader: AVAssetReader? = null
    private var trackOutput: AVAssetReaderTrackOutput? = null
    var duration: Double = 0.0
        private set
    var setupState: ReplayEffectStingerReaderSetupState = ReplayEffectStingerReaderSetupState.working
        private set
    private val size: CGSize = size.toCGSize()

    init {
        setup(path = path)
    }

    open fun getImage(offset: Double): ReplayImage? {
        val image = findImage(offset = offset)
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
        val trackOutput = this.trackOutput ?: return
        val newImages = mutableListOf<ReplayImage>()
        for (i in 0..10) {
            val sampleBuffer = trackOutput.copyNextSampleBuffer()
            val imageBuffer = sampleBuffer?.imageBuffer
            if (sampleBuffer != null && imageBuffer != null) {
                val image = CIImage(cvPixelBuffer = imageBuffer)
                    .scaledTo(size = size)
                    .centered(size = size)
                    .composited(over = CIImage.clear.cropped(to = CGRect(origin = CGPoint.zero, size = size)))
                newImages.add(
                    ReplayImage(
                        image = image.toEffectImage(isOpaque = false),
                        offset = sampleBuffer.presentationTimeUs / 1_000_000.0,
                        isLast = false,
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
            val asset = AVAsset(url = path)
            reader = runCatching { AVAssetReader(asset = asset) }.getOrNull()
            duration = asset.duration()
            asset.loadTracks(withMediaType = AVMediaType.video) { tracks, error ->
                replayEffectQueue.launch {
                    loadVideoTrackCompletion(track = tracks?.firstOrNull(), error = error)
                }
            }
        }
    }

    private fun loadVideoTrackCompletion(track: AVAssetTrack?, error: Throwable?) {
        if (error != null || track == null) {
            setupComplete(state = ReplayEffectStingerReaderSetupState.failed)
            return
        }
        val videoOutputSettings: Map<String, Any> = mapOf(
            kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA,
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
        )
        val output = AVAssetReaderTrackOutput(track = track, outputSettings = videoOutputSettings)
        trackOutput = output
        reader?.add(output)
        reader?.startReading()
        fillInternal()
        setupComplete(state = ReplayEffectStingerReaderSetupState.ok)
    }

    private fun setupComplete(state: ReplayEffectStingerReaderSetupState) {
        processorPipelineQueue.launch {
            setupState = state
        }
    }
}
