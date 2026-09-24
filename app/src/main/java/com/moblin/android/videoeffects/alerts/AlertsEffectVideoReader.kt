package com.moblin.android.videoeffects.alerts

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.toEffectImage
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private data class VideoImage(val image: EffectImageCiImage, val offset: Double)

private val lockQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class AlertsEffectVideoReader(path: String) {
    private val images: ArrayDeque<VideoImage> = ArrayDeque()
    private var reader: AVAssetReader? = null
    private var trackOutput: AVAssetReaderTrackOutput? = null
    private var fillEnded: Boolean = false
    private var basePresentationTimeStamp: Double? = null

    init {
        CoroutineScope(lockQueue).launch {
            val asset = AVAsset(url = path)
            reader = runCatching { AVAssetReader(asset = asset) }.getOrNull()
            asset.loadTracks(withMediaType = AVMediaType.video) { tracks, error ->
                CoroutineScope(lockQueue).launch {
                    loadVideoTrackCompletion(track = tracks?.firstOrNull(), error = error)
                }
            }
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
        while (true) {
            val image = images.firstOrNull() ?: break
            if (offset <= image.offset) {
                return image.image
            }
            com.moblin.android.platform.coreimage.releaseImageLeases(images.removeFirst().image.getCiImage())
        }
        return null
    }

    private fun fill() {
        CoroutineScope(lockQueue).launch {
            fillInternal()
        }
    }

    private fun fillInternal() {
        val trackOutput = this.trackOutput ?: return
        val newImages = mutableListOf<VideoImage>()
        for (index in 0..10) {
            val sampleBuffer = trackOutput.copyNextSampleBuffer()
            if (sampleBuffer != null) {
                val imageBuffer = sampleBuffer.imageBuffer
                if (imageBuffer != null) {
                    newImages.add(
                        VideoImage(
                            image = CIImage(cvPixelBuffer = imageBuffer).toEffectImage(isOpaque = true),
                            offset = sampleBuffer.presentationTimeUs / 1_000_000.0,
                        ),
                    )
                }
            }
        }
        processorPipelineQueue.launch {
            images.addAll(newImages)
            fillEnded = newImages.isEmpty()
        }
    }

    private fun loadVideoTrackCompletion(track: AVAssetTrack?, error: Throwable?) {
        if (error != null || track == null) {
            markFillEnded()
            return
        }
        val videoOutputSettings: Map<String, Any> = mapOf(
            kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA,
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
        )
        trackOutput = AVAssetReaderTrackOutput(track = track, outputSettings = videoOutputSettings)
        val output = trackOutput
        if (output == null) {
            markFillEnded()
            return
        }
        reader?.add(output = output)
        output.leasesSampleBuffers = true
        reader?.startReading()
        fillInternal()
    }

    private fun markFillEnded() {
        processorPipelineQueue.launch {
            fillEnded = true
        }
    }
}
