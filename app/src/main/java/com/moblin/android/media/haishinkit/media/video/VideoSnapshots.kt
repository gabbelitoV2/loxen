package com.moblin.android.media.haishinkit.media.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.vision.CalculateImageAestheticsScoresRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoSnapshots(private val context: Context) {
    private var cleanSnapshots = false
    private var takeSnapshotAge: Float = 0.0f
    private var takeSnapshotComplete: ((Bitmap, Bitmap, Bitmap) -> Unit)? = null
    private val takeSnapshotSampleBuffers = ArrayDeque<MediaSample>()

    fun setCleanSnapshots(enabled: Boolean) {
        cleanSnapshots = enabled
    }

    fun takeSnapshot(age: Float, onComplete: (Bitmap, Bitmap, Bitmap) -> Unit) {
        takeSnapshotAge = age
        takeSnapshotComplete = onComplete
    }

    fun takeVideoSourceSnapshot(imageBuffer: CVPixelBuffer, onComplete: (Bitmap?) -> Unit) {
        val bitmap = createBitmap(imageBuffer)
        if (bitmap == null) {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                onComplete(null)
            }
            return
        }
        CoroutineScope(Dispatchers.Main.immediate).launch {
            onComplete(bitmap)
        }
    }

    fun handleTakeSnapshot(cleanSampleBuffer: MediaSample,
                           modSampleBuffer: MediaSample,
                           presentationTimeStamp: Double,
                           makeCopy: (MediaSample) -> MediaSample?)
    {
        val sampleBuffer = if (cleanSnapshots) cleanSampleBuffer else modSampleBuffer
        val latestPresentationTimeStamp = takeSnapshotSampleBuffers.lastOrNull()
            ?.let { it.presentationTimeUs / 1_000_000.0 } ?: 0.0
        if (presentationTimeStamp > latestPresentationTimeStamp + 3.0) {
            val copy = makeCopy(sampleBuffer) ?: return
            takeSnapshotSampleBuffers.addLast(com.moblin.android.platform.video.retainLease(copy))
            if (takeSnapshotSampleBuffers.size > 3) {
                com.moblin.android.platform.video.releaseLease(takeSnapshotSampleBuffers.removeFirst())
            }
        }
        val complete = takeSnapshotComplete ?: return
        val copy = makeCopy(sampleBuffer) ?: return
        val sampleBuffers = ArrayDeque(takeSnapshotSampleBuffers)
        val age = takeSnapshotAge
        com.moblin.android.platform.video.retainLeases(sampleBuffers + copy)
        CoroutineScope(Dispatchers.Default).launch {
            takeSnapshot(copy, sampleBuffers, presentationTimeStamp, age, complete)
        }
        takeSnapshotComplete = null
    }

    private fun findBestSnapshot(sampleBuffer: MediaSample,
                                 sampleBuffers: ArrayDeque<MediaSample>,
                                 presentationTimeStamp: Double,
                                 age: Float,
                                 onCompleted: (CVPixelBuffer?) -> Unit)
    {
        if (age == 0.0f) {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                onCompleted(imageBufferOf(sampleBuffer))
            }
        } else {
            val requestedPresentationTimeStamp = presentationTimeStamp - age.toDouble()
            val sampleBufferAtAge = sampleBuffers.lastOrNull {
                it.presentationTimeUs / 1_000_000.0 <= requestedPresentationTimeStamp
            } ?: sampleBuffers.firstOrNull() ?: sampleBuffer
            val buffers = ArrayDeque(sampleBuffers)
            buffers.addLast(sampleBuffer)
            findBestSnapshotUsingAesthetics(sampleBufferAtAge, buffers, onCompleted)
        }
    }

    private fun findBestSnapshotUsingAesthetics(preferredSampleBuffer: MediaSample,
                                                sampleBuffers: ArrayDeque<MediaSample>,
                                                onComplete: (CVPixelBuffer?) -> Unit)
    {
        CoroutineScope(Dispatchers.Default).launch {
            var bestSampleBuffer = preferredSampleBuffer
            var bestResult = runCatching {
                CalculateImageAestheticsScoresRequest()
                    .perform(on = preferredSampleBuffer)
            }.getOrNull()
            for (sampleBuffer in sampleBuffers) {
                val result = runCatching {
                    CalculateImageAestheticsScoresRequest()
                        .perform(on = sampleBuffer)
                }.getOrNull() ?: continue
                if (bestResult == null || result.overallScore > bestResult.overallScore + 0.2f) {
                    bestSampleBuffer = sampleBuffer
                    bestResult = result
                }
            }
            CoroutineScope(Dispatchers.Main.immediate).launch {
                onComplete(imageBufferOf(bestSampleBuffer))
            }
        }
    }

    private fun takeSnapshot(sampleBuffer: MediaSample,
                             sampleBuffers: ArrayDeque<MediaSample>,
                             presentationTimeStamp: Double,
                             age: Float,
                             onComplete: (Bitmap, Bitmap, Bitmap) -> Unit)
    {
        findBestSnapshot(sampleBuffer, sampleBuffers, presentationTimeStamp, age) { imageBuffer ->
            if (imageBuffer == null) {
                return@findBestSnapshot
            }
            val image = createBitmap(imageBuffer).also { com.moblin.android.platform.video.releaseLeases(sampleBuffers + sampleBuffer) } ?: return@findBestSnapshot
            var portraitImage = image
            if (!imageBuffer.isPortrait()) {
                portraitImage = orientedLeft(portraitImage)
            }
            onComplete(image, image, portraitImage)
        }
    }

    private fun imageBufferOf(sampleBuffer: MediaSample): CVPixelBuffer? =
        sampleBuffer.imageBuffer

    private fun createBitmap(imageBuffer: CVPixelBuffer): Bitmap? =
        imageBuffer.toBitmap()

    private fun orientedLeft(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(-90.0f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
