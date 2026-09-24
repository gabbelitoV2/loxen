package com.moblin.android.videoeffects.replay

import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.CMTimeRange
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coregraphics.toSize
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.uikit.size
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.centered
import com.moblin.android.videoeffects.scaledTo
import com.moblin.android.videoeffects.toEffectImage
import com.moblin.android.videoeffects.translated
import com.moblin.android.view.utils.FontDesign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ReplayImage(
    val image: EffectImageCiImage?,
    val offset: Double?,
    val isLast: Boolean,
)

internal class ReplayEffectReplayReader internal constructor(
    private val video: ReplayBufferFile,
    start: Double,
    duration: Double,
    size: Size,
) {
    private val startTime: Double = start
    private val size: CGSize = size.toCGSize()
    private var reader: AVAssetReader? = null
    private var trackOutput: AVAssetReaderTrackOutput? = null
    private var images: ArrayDeque<ReplayImage> = ArrayDeque()
    private var overlay: CIImage? = null
    fun close() { val current = reader; kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { current?.cancelReading() }; replayEffectQueue.launch { reader?.cancelReading(); processorPipelineQueue.launch { com.moblin.android.platform.coreimage.drainImageLeases(images) { it.image?.getCiImage() } } } }

    init {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            overlay = createOverlay(size = this@ReplayEffectReplayReader.size.toSize())
            replayEffectQueue.launch {
                val asset = AVAsset(url = video.url)
                reader = runCatching { AVAssetReader(asset = asset) }.getOrNull()
                val startTime = (start * 1_000_000.0).toLong()
                val duration = (duration * 1_000_000.0).toLong()
                reader?.timeRange = CMTimeRange(start = startTime, duration = duration)
                asset.loadTracks(withMediaType = AVMediaType.video) { tracks, error ->
                    replayEffectQueue.launch {
                        loadVideoTrackCompletion(tracks = tracks, error = error)
                    }
                }
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
            com.moblin.android.platform.coreimage.releaseImageLeases(images.removeFirst().image?.getCiImage())
        }
        return ReplayImage(image = null, offset = null, isLast = false)
    }

    private fun loadVideoTrackCompletion(tracks: List<AVAssetTrack>?, error: Throwable?) {
        val track = tracks?.firstOrNull()
        if (error != null || track == null) {
            markCompleted()
            return
        }
        val outputSettings: Map<String, Any> = mapOf(
            kCVPixelBufferPixelFormatTypeKey to com.moblin.android.media.haishinkit.media.video.pixelFormatType,
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
        )
        val trackOutput = AVAssetReaderTrackOutput(track = track, outputSettings = outputSettings)
        this.trackOutput = trackOutput
        trackOutput.leasesSampleBuffers = true
        reader?.add(trackOutput)
        reader?.startReading()
        fillInternal()
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
        val trackOutput = this.trackOutput ?: return
        val newImages = mutableListOf<ReplayImage>()
        for (i in 0..10) {
            val sampleBuffer = trackOutput.copyNextSampleBuffer()
            val imageBuffer = sampleBuffer?.imageBuffer
            if (sampleBuffer != null && imageBuffer != null) {
                var image = CIImage(cvPixelBuffer = imageBuffer)
                    .scaledTo(size = size)
                    .centered(size = size)
                    .composited(over = CIImage.black.cropped(to = CGRect(origin = CGPoint.zero, size = size)))
                overlay?.let {
                    image = it.composited(over = image)
                }
                newImages.add(
                    ReplayImage(
                        image = image.toEffectImage(isOpaque = true),
                        offset = sampleBuffer.presentationTimeUs / 1_000_000.0 - startTime,
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

    private fun createOverlay(size: Size): CIImage? {
        val scale = size.width.toDouble() / (if (size.width < size.height) 1080 else 1920)
        val renderer = ImageRenderer(content = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width((40.0 * scale).dp)
                            .height((40.0 * scale).dp)
                            .background(color = Color.Red, shape = CircleShape)
                    )
                    SystemImage(
                        name = "play.fill",
                        fontSize = (25.0 * scale).sp,
                        modifier = Modifier,
                        tint = Color.White,
                    )
                }
                Text(
                    text = "REPLAY",
                    style = SwiftUIFonts.system(50.0 * scale, FontWeight.Bold, FontDesign.Monospaced),
                    color = Color.White,
                )
            }
        })
        val image = renderer.uiImage ?: return null
        val x = size.width.toDouble() - image.size.width - 25 * scale
        val y = size.height.toDouble() - image.size.height - 20 * scale
        return CIImage(image = image)?.translated(x = x, y = y)
    }
}
